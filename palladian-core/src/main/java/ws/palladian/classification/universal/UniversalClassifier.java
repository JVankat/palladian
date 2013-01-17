package ws.palladian.classification.universal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.numeric.KnnClassifier;
import ws.palladian.classification.numeric.KnnModel;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.FeatureSetting;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;

public class UniversalClassifier implements Classifier<UniversalClassifierModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalClassifier.class);

//    public static final String FEATURE_TERM = "ws.palladian.feature.term";

    public static enum ClassifierSetting {
        NUMERIC, TEXT, NOMINAL
    }

    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private final PalladianTextClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private final KnnClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private final NaiveBayesClassifier nominalClassifier;

    private final FeatureSetting featureSetting;

    private final EnumSet<ClassifierSetting> settings;

    public UniversalClassifier() {
        this(EnumSet.allOf(ClassifierSetting.class), new FeatureSetting());
    }

    public UniversalClassifier(FeatureSetting featureSetting) {
        this(EnumSet.allOf(ClassifierSetting.class), featureSetting);
    }

    public UniversalClassifier(EnumSet<ClassifierSetting> settings) {
        this(settings, new FeatureSetting());
    }

    public UniversalClassifier(EnumSet<ClassifierSetting> settings, FeatureSetting featureSetting) {
        textClassifier = new PalladianTextClassifier();
        this.featureSetting = featureSetting;
        numericClassifier = new KnnClassifier();
        nominalClassifier = new NaiveBayesClassifier();
        this.settings = settings;
    }

    private void learnClassifierWeights(List<Instance> instances, UniversalClassifierModel model) {
        int[] correctlyClassified = new int[3];
        Arrays.fill(correctlyClassified, 0);

        int c = 1;
        for (Instance instance : instances) {
            UniversalClassificationResult result = internalClassify(instance.getFeatureVector(), model);
            int[] evaluatedResult = evaluateResults(instance, result);
            correctlyClassified[0] += evaluatedResult[0];
            correctlyClassified[1] += evaluatedResult[1];
            correctlyClassified[2] += evaluatedResult[2];
            ProgressHelper.showProgress(c++, instances.size(), 0);
        }

        model.setWeights(correctlyClassified[0] / (double)instances.size(),
                correctlyClassified[1] / (double)instances.size(), correctlyClassified[2] / (double)instances.size());

        LOGGER.debug("weight text   : " + model.getWeights()[0]);
        LOGGER.debug("weight numeric: " + model.getWeights()[1]);
        LOGGER.debug("weight nominal: " + model.getWeights()[2]);
    }

    private int[] evaluateResults(Instance instance, UniversalClassificationResult result) {

        int[] correctlyClassified = new int[3];
        Arrays.fill(correctlyClassified, 0);

        // Since there are not weights yet the classifier weights all results with one.
        CategoryEntries textResult = result.getTextResults();
        if (textResult != null && textResult.getMostLikelyCategoryEntry() != null
                && textResult.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            correctlyClassified[0]++;
        }
        CategoryEntries numericResult = result.getNumericResults();
        if (numericResult != null && numericResult.getMostLikelyCategoryEntry() != null
                && numericResult.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            correctlyClassified[1]++;
        }
        CategoryEntries nominalResult = result.getNominalResults();
        if (nominalResult != null && nominalResult.getMostLikelyCategoryEntry() != null
                && nominalResult.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            correctlyClassified[2]++;
        }
        return correctlyClassified;
    }

    protected UniversalClassificationResult internalClassify(Classifiable classifiable, UniversalClassifierModel model) {

        CategoryEntries text = null;
        CategoryEntries numeric = null;
        CategoryEntries nominal = null;
        
        FeatureVector featureVectorWithoutTerms = new FeatureVector(classifiable.getFeatureVector());
        featureVectorWithoutTerms.removeAll(FEATURE_TERM);

        // classify text using the dictionary classifier
        if (model.getDictionaryModel() != null) {
            text = textClassifier.classify(classifiable.getFeatureVector(), model.getDictionaryModel());
        }

        // classify numeric features with the KNN
        if (model.getKnnModel() != null) {
            numeric = numericClassifier.classify(featureVectorWithoutTerms, model.getKnnModel());
        }

        // classify nominal features with the Bayes classifier
        if (model.getBayesModel() != null) {
            nominal = nominalClassifier.classify(featureVectorWithoutTerms, model.getBayesModel());
        }
        return new UniversalClassificationResult(text, numeric, nominal);
    }

    private CategoryEntries mergeResults(UniversalClassificationResult result, UniversalClassifierModel model) {
        Map<CategoryEntries, Double> weightedCategoryEntries = LazyMap.create(ConstantFactory.create(0.));

        // merge classification results
        if (model.getDictionaryModel() != null) {
            weightedCategoryEntries.put(result.getTextResults(), model.getWeights()[0]);
        }
        if (model.getKnnModel() != null) {
            weightedCategoryEntries.put(result.getNumericResults(), model.getWeights()[1]);
        }
        if (model.getBayesModel() != null) {
            weightedCategoryEntries.put(result.getNominalResults(), model.getWeights()[2]);
        }
        CategoryEntries normalizedCategoryEntries = normalize(weightedCategoryEntries);
        return normalizedCategoryEntries;
    }

    /**
     * <p>
     * Merges the results of the different classifiers and normalizes the resulting relevance score for each entry to be
     * in [0,1] and sum up to 1.
     * </p>
     * 
     * @param weightedCategoryEntries The classification result together with the weights for each of the classifiers.
     * @return Merged and normalized {@code CategoryEntries}.
     */
    protected CategoryEntries normalize(Map<CategoryEntries, Double> weightedCategoryEntries) {
        CategoryEntries normalizedCategoryEntries = new CategoryEntries();
        Map<String, Double> mergedCategoryEntries = LazyMap.create(ConstantFactory.create(0.));

        // merge entries from different classifiers
        for (Entry<CategoryEntries, Double> entries : weightedCategoryEntries.entrySet()) {
            for (CategoryEntry entry : entries.getKey()) {
                double relevance = entry.getProbability();
                double weight = entries.getValue();
                Double existingRelevance = mergedCategoryEntries.get(entry.getName());
                mergedCategoryEntries.put(entry.getName(), existingRelevance + relevance * weight);
            }
        }

        // calculate normalization value.
        double totalRelevance = 0.0;
        for (Entry<String, Double> entry : mergedCategoryEntries.entrySet()) {
            totalRelevance += entry.getValue();
        }

        // normalize entries
        for (Entry<String, Double> entry : mergedCategoryEntries.entrySet()) {
            normalizedCategoryEntries.add(new CategoryEntry(entry.getKey(), entry.getValue() / totalRelevance));
        }

        return normalizedCategoryEntries;
    }

    @Override
    public UniversalClassifierModel train(List<? extends Trainable> trainables) {
        NaiveBayesModel nominalModel = null;
        KnnModel numericModel = null;
        DictionaryModel textModel = null;


        // train the text classifier
        if (settings.contains(ClassifierSetting.TEXT)) {
            LOGGER.debug("training text classifier");
            textModel = textClassifier.train(trainables, featureSetting);
        }
        
        // XXX thats not really nice because we alter the original feature vector,
        // better would be to supply a filter or view on the existing one.
        for (Trainable trainable : trainables) {
            trainable.getFeatureVector().removeAll(FEATURE_TERM);
        }

        // train the numeric classifier
        if (settings.contains(ClassifierSetting.NUMERIC)) {
            LOGGER.debug("training numeric classifier");
            numericModel = numericClassifier.train(trainables);
        }

        // train the nominal classifier
        if (settings.contains(ClassifierSetting.NOMINAL)) {
            LOGGER.debug("training nominal classifier");
            nominalModel = nominalClassifier.train(trainables);
        }

        UniversalClassifierModel model = new UniversalClassifierModel(nominalModel, numericModel, textModel);
        LOGGER.debug("learning classifier weights");
        // learnClassifierWeights(instances, model);
        return model;
    }

    @Override
    public UniversalClassifierModel train(Dataset dataset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, UniversalClassifierModel model) {
        UniversalClassificationResult result = internalClassify(classifiable, model);
        return mergeResults(result, model);
    }

    public FeatureSetting getFeatureSetting() {
        return this.featureSetting;
    }

}

class UniversalClassificationResult {
    private final CategoryEntries textCategories;
    private final CategoryEntries numericResults;
    private final CategoryEntries nominalResults;

    UniversalClassificationResult(CategoryEntries text, CategoryEntries numeric, CategoryEntries nominal) {
        this.textCategories = text;
        this.numericResults = numeric;
        this.nominalResults = nominal;
    }

    public CategoryEntries getNominalResults() {
        return nominalResults;
    }

    public CategoryEntries getNumericResults() {
        return numericResults;
    }

    public CategoryEntries getTextResults() {
        return textCategories;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UniversalClassificationResult [textCategories=");
        builder.append(textCategories);
        builder.append(", numericResults=");
        builder.append(numericResults);
        builder.append(", nominalResults=");
        builder.append(nominalResults);
        builder.append("]");
        return builder.toString();
    }

}