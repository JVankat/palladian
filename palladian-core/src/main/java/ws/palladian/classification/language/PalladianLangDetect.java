package ws.palladian.classification.language;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;

import java.io.IOException;
import java.util.Set;

/**
 * The best setting for medium to long texts is to use word n-grams with 1<=n<=3.
 * Evaluation results can be found in the Palladian book.
 *
 * @author David Urbansky
 */
public class PalladianLangDetect implements LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLangDetect.class);

    private final PalladianTextClassifier textClassifier;

    private final DictionaryModel dictionaryModel;

    /** We can specify which classes are possible and discard all others for the classification task. */
    private Set<String> possibleClasses = null;

    public PalladianLangDetect(String modelPath) {
        try {
            dictionaryModel = FileHelper.deserialize(modelPath);
            textClassifier = new PalladianTextClassifier(dictionaryModel.getFeatureSetting());
        } catch (IOException e) {
            throw new IllegalStateException("Could not deserialize model from \"" + modelPath + "\"", e);
        }
    }

    public Set<String> getPossibleClasses() {
        return possibleClasses;
    }

    public void setPossibleClasses(Set<String> possibleClasses) {
        this.possibleClasses = possibleClasses;
    }

    /**
     * Train the language detector on a dataset.
     *
     * @param dataset        The dataset to train on.
     * @param classifierName The name of the classifier. The name is added to the classifierPath.
     * @param classifierPath The path where the classifier should be saved to. For example, <tt>data/models/</tt>
     */
    public static void train(Dataset dataset, String classifierName, String classifierPath) {
        train(dataset, classifierName, classifierPath, null);
    }

    public static void train(Dataset dataset, String classifierName, String classifierPath, FeatureSetting fs) {
        StopWatch stopWatch = new StopWatch();
        FeatureSetting featureSetting = fs != null ? fs : FeatureSettingBuilder.chars(4, 7).create();
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);
        TextDatasetIterator datasetIterator = new TextDatasetIterator(dataset);
        DictionaryModel trainedModel = classifier.train(datasetIterator);
        String fileName = classifierPath + classifierName + ".gz";
        try {
            FileHelper.serialize(trainedModel, fileName);
        } catch (IOException e) {
            throw new IllegalStateException("Error while serializing to \"" + fileName + "\".", e);
        }
        LOGGER.info("finished training classifier in {}", stopWatch.getElapsedTimeString());
    }

    @Override
    public Language classify(String text) {
        String lanugageString = classifyAsCategoryEntry(text).getMostLikelyCategory();
        return Language.getByIso6391(lanugageString);
    }

    public CategoryEntries classifyAsCategoryEntry(String text) {
        CategoryEntries categoryEntries = textClassifier.classify(text, dictionaryModel);
        categoryEntries = narrowCategories(categoryEntries);
        return categoryEntries;
    }

    private CategoryEntries narrowCategories(CategoryEntries categoryEntries) {
        if (possibleClasses == null) {
            return categoryEntries;
        }
        CategoryEntriesBuilder narrowedCategories = new CategoryEntriesBuilder();
        for (Category category : categoryEntries) {
            if (possibleClasses.contains(category.getName())) {
                narrowedCategories.set(category.getName(), category.getProbability());
            }
        }
        return narrowedCategories.create();
    }

    public static void main(String[] args) throws IOException {

        // ///////////////// use the language classifier ///////////////////
        // String languageModelPath = "data/models/palladianLanguageClassifier/LanguageClassifier.gz";
        // String languageModelPath = "data/models/palladianLanguageJRC/palladianLanguageJRC.gz";
        // String languageModelPath =
        // "C:\\My Dropbox\\KeywordExtraction\\palladianLanguageJRC_o\\palladianLanguageJRC.gz";
        //
        // PalladianLangDetect pld0 = new PalladianLangDetect("data/models/language/wikipedia76Languages20ipc.gz");
        // // PalladianLangDetect pld0 = new PalladianLangDetect("data/models/language/languageMicroblogging.gz");
        // String language = pld0.classify("This is a sample text in English");
        // System.out.println("The text was classified as: " + language);
        // language = pld0.classify("Das ist ein Beispieltext auf Deutsch");
        // System.out.println("The text was classified as: " + language);
        // language = pld0.classify("Se trata de un texto de muestra en español");
        // System.out.println("The text was classified as: " + language);
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////

        // ///////////////// find the best performing settings ///////////////////
        // specify the dataset that should be used as training data
        // PalladianLangDetect pld0 = new PalladianLangDetect();
        // pld0.evaluateBestSetting();
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////

        // ///////////////// learn from a given dataset ///////////////////
        // String datasetRootFolder = "H:\\PalladianData\\Datasets\\JRCLanguageCorpus";

        // // create an index over the dataset
        // DatasetManager dsManager = new DatasetManager();
        // String path = dsManager.createIndex(datasetRootFolder, new String[] { "en", "es", "de" });
        // String path = dsManager.createIndex(datasetRootFolder);
        //
        // // create an excerpt with 1000 instances per class
        // String indexExcerpt = dsManager.createIndexExcerpt(
        // "H:\\PalladianData\\Datasets\\Wikipedia76Languages\\languageDocumentIndex.txt", " ", 20);
        //
        // // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // set the path to the dataset, the first field is a link, and columns are separated with a space
        dataset.setPath("H:\\PalladianData\\Datasets\\JRCLanguageCorpus\\indexAll22Languages_ipc20.txt");
        // dataset.setPath("H:\\PalladianData\\Datasets\\Microblogging35Languages\\languageDocumentIndex.txt");
        // dataset.setPath("H:\\PalladianData\\Datasets\\Wikipedia76Languages\\languageDocumentIndex.txt");
        // dataset.setPath(indexExcerpt);

        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");

        PalladianLangDetect.train(dataset, "jrc22Languages20ipc", "data/models/palladian/language/");
        // PalladianLangDetect.train(dataset, "microblogging35Languages", "data/models/palladian/language/");
        // PalladianLangDetect.train(dataset, "wikipedia76Languages20ipc", "data/models/palladian/language/");
        // ////////////////////////////////////////////////////////////////

    }

}
