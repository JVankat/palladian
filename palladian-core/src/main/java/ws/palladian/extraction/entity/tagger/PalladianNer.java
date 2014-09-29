package ws.palladian.extraction.entity.tagger;

import static ws.palladian.core.Annotation.TAG_CONVERTER;
import static ws.palladian.core.Token.VALUE_CONVERTER;
import static ws.palladian.extraction.entity.TaggingFormat.COLUMN;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR1;
import static ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.LanguageMode.LanguageIndependent;
import static ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.TrainingMode.Complete;
import static ws.palladian.helper.functional.Filters.not;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.DictionaryBuilder;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryModel.DictionaryEntry;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.ExperimentalScorers;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.classification.text.PruningStrategies;
import ws.palladian.core.Annotation;
import ws.palladian.core.AnnotationFilters;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Tagger;
import ws.palladian.core.Token;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.RegExTagger;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.extraction.entity.dataset.DatasetCreator;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.TrainingMode;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.extraction.token.WordTokenizer;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Palladian's Named Entity Recognizer. It is based on rule-based entity delimitation (for English texts), a text
 * classification approach, and analyzes the contexts around annotations. The major difference to other NERs is that it
 * can be learned on seed entities (just the names) or classically using supervised learning on a tagged dataset.
 * 
 * <p>
 * Palladian NER provides two language modes:
 * 
 * <ol>
 * <li>{@link LanguageMode#LanguageIndependent}: token-based, that is you can learn any language, the performance is
 * rather poor though. Consider using another recognizer.
 * <li>{@link LanguageMode#English}: NED + NEC, English only, this recognizer has shown to reach similar performance on
 * the CoNLL 2003 dataset as the state-of-the-art. It works on English texts only.
 * </ol>
 * 
 * <p>
 * Palladian NER provides two learning modes:
 * 
 * <ol>
 * <li>{@link TrainingMode#Complete}: You must have a tagged corpus in column format where the first column is the token
 * and the second column (separated by a tabstop) is the entity type.
 * <li>{@link TrainingMode#Sparse}: You just need a set of seed entities per concept (the same number per concept is
 * preferred) and you can learn a sparse training file with the {@link DatasetCreator} to learn on. Alternatively you
 * can also learn on the seed entities alone but no context information can be learned which results in a slightly worse
 * performance.
 * </ol>
 * 
 * <p>
 * Parameters for performance tuning:
 * <ul>
 * <li>n-gram size of the entity classifier (2-8 seems good)
 * <li>n-gram size of the context classifier (4-6 seems good)
 * <li>window size of the Annotation: {@link #WINDOW_SIZE}
 * </ul>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianNer extends TrainableNamedEntityRecognizer implements ClassifyingTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianNer.class);

    private final static String NO_ENTITY = "###NO_ENTITY###";
    
    private PalladianNerTrainingSettings trainingSettings;

    private PalladianNerModel model;

    public PalladianNer(PalladianNerTrainingSettings trainingSettings) {
        Validate.notNull(trainingSettings, "trainingSettings must not be null");
        this.trainingSettings = trainingSettings;
    }

    @Override
    public String getModelFileEnding() {
        return "model.gz";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        model = null; // save memory
        try {
            model = FileHelper.deserialize(configModelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException("Error while loading model from \"" + configModelFilePath + "\".", e);
        }
        LOGGER.info("Model {} successfully loaded", configModelFilePath);
        return true;
    }

    /**
     * Save the tagger to the specified file.
     * 
     * @param modelFilePath The file where the tagger should be saved to. You do not need to add the file ending but if
     *            you do, it should be "model.gz".
     */
    private void saveModel(String modelFilePath) {
        LOGGER.info("Annotation dictionary size: {}", model.annotationDictionary.getNumUniqTerms());
        LOGGER.info("Entity dictionary size: {}", model.entityDictionary.getNumUniqTerms());
        LOGGER.info("Context dictionary size: {}", model.contextDictionary.getNumUniqTerms());
        if (model.lowerCaseDictionary != null) {
            LOGGER.info("Case dictionary size: {}", model.lowerCaseDictionary.size());
        }
        if (model.removeAnnotations != null) {
            LOGGER.info("Remove annotations: {}", model.removeAnnotations.size());
        }
        LOGGER.info("Left contexts size: {}", model.leftContexts.size());
        LOGGER.info("Tags: {}", StringUtils.join(model.getTags(), ", "));
        try {
            FileHelper.serialize(model, modelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException("Error while serializing to \"" + modelFilePath + "\".", e);
        }
        LOGGER.info("Serialized Palladian NER to {}", modelFilePath);
    }

    /**
     * Build a case dictionary. For the giving training text, the upper/lowercase statistics of all tokens within
     * sentences (i.e. of all tokens not at sentence beginning) are analyzed.
     * 
     * @param text The text from which to build the case dictionary.
     * @return The dictionary model with categories <code>A</code> and <code>a</code> for each token.
     */
    Set<String> buildCaseDictionary(String text) {
        LOGGER.info("Building case dictionary");
        DictionaryBuilder builder = createDictionaryBuilder();
        Iterator<Token> tokens = new WordTokenizer().iterateTokens(text);
        boolean skip = true; // skip the first, and tokens after a new sentence start
        while (tokens.hasNext()) {
            String token = tokens.next().getValue();
            if (skip) {
                skip = false;
            } else if (token.matches("[.?!]")) {
                skip = true;
            } else {
                String trimmedToken = token.trim();
                if (trimmedToken.length() > 1) {
                    String caseSignature = StringHelper.getCaseSignature(trimmedToken);
                    if (caseSignature.toLowerCase().startsWith("a")) {
                        builder.addDocument(Collections.singleton(trimmedToken.toLowerCase()),
                                caseSignature.substring(0, 1));
                    }
                }
            }
        }
        DictionaryModel temp = builder.create();
        Set<String> lowerCaseDictionary = CollectionHelper.newHashSet();
        for (DictionaryEntry entry : temp) {
            String token = entry.getTerm();
            if (entry.getCategoryEntries().getProbability("a") > 0.5) {
                lowerCaseDictionary.add(token);
            }
        }
        return lowerCaseDictionary;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        train(trainingFilePath, Collections.<Annotation> emptyList(), modelFilePath);
        return true;
    }

    /**
     * <p>
     * Similar to {@link train(String trainingFilePath, String modelFilePath)} method but an additional set of
     * annotations can be given to learn the classifier.
     * </p>
     * 
     * @param trainingFilePath The file of the training file.
     * @param annotations A set of annotations which are used for learning: Improving the text classifier AND adding
     *            them to the entity dictionary.
     * @param modelFilePath The path where the model should be saved to.
     */
    public void train(String trainingFilePath, List<Annotation> annotations, String modelFilePath) {
        if (trainingSettings.getLanguageMode() == LanguageIndependent) {
            trainLanguageIndependent(trainingFilePath, annotations);
        } else {
            trainEnglish(trainingFilePath, annotations);
        }
        saveModel(modelFilePath);
    }

    /**
     * <p>
     * Replace the trained entity dictionary with the one from the file. The file must contain a header with information
     * about the concept importance as follows:
     * </p>
     * 
     * <pre>
     * CONCEPT1>CONCEPT2>CONCEPT3>CONCEPT4>CONCEPT5>...
     * per>org>country>city>loc
     * </pre>
     * 
     * <p>
     * The concept importance is used when a candidate is ambiguous. For example, "Buddha" is usually used to refer to
     * the person but it is also the name of a city. Increasing the importance of the person concept above the city
     * concept we can make sure it will not be tagged incorrectly.
     * </p>
     * 
     * <p>
     * All subsequent lines must contain one entity and concept in the following format:
     * </p>
     * 
     * <pre>
     *   CONCEPT###ENTITY
     *   City###Dresden
     * </pre>
     * 
     * @param filePath The path to the dictionary file.
     */
    public void setEntityDictionary(String filePath) {
        final DictionaryBuilder entityDictionaryBuilder = createDictionaryBuilder(); // FIXME not here?
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    model.conceptLikelihoodOrder = CollectionHelper.newArrayList(line.split("\\>"));
                    return;
                }
                String[] split = line.split("###");
                if (split.length == 2) {
                    entityDictionaryBuilder.addDocument(Collections.singleton(split[1]), split[0]);
                }
            }
        });
        model.entityDictionary = entityDictionaryBuilder.create();
        LOGGER.info("Added {} entities to the dictionary", model.entityDictionary.getNumTerms());
    }

    private DictionaryBuilder createDictionaryBuilder() {
        DictionaryTrieModel.Builder builder = new DictionaryTrieModel.Builder();
        int minCount = trainingSettings.getMinDictionaryCount();
        if (minCount > 1) {
            builder.setPruningStrategy(new PruningStrategies.TermCountPruningStrategy(minCount));
        }
        return builder;
    }

    /**
     * <p>
     * Use only a set of annotations to learn, that is, no training file is required. Use this mostly in the English
     * language mode and do not expect great performance.
     * </p>
     * 
     * @param annotations A set of annotations which are used for learning.
     * @param modelFilePath The path where the model should be saved to.
     */
    public void train(List<Annotation> annotations, String modelFilePath) {
        model.entityDictionary = buildEntityDictionary(annotations);
        model.annotationDictionary = buildAnnotationDictionary(annotations);
        saveModel(modelFilePath);
    }

    private DictionaryModel buildEntityDictionary(Iterable<Annotation> annotations) {
        LOGGER.info("Building entity dictionary");
        DictionaryBuilder entityDictionaryBuilder = createDictionaryBuilder();
        for (Annotation annotation : annotations) {
            entityDictionaryBuilder.addDocument(Collections.singleton(annotation.getValue()), annotation.getTag());
        }
        return entityDictionaryBuilder.create();
    }

    private DictionaryModel buildAnnotationDictionary(Iterable<Annotation> annotations) {
        LOGGER.info("Building annotation dictionary");
        DictionaryBuilder builder = createDictionaryBuilder();
        PalladianTextClassifier textClassifier = new PalladianTextClassifier(PalladianNerTrainingSettings.ANNOTATION_FEATURE_SETTING, builder );
        Iterable<Instance> instances = CollectionHelper.convert(annotations, new Function<Annotation, Instance>() {
            @Override
            public Instance compute(Annotation input) {
                return new InstanceBuilder().setText(input.getValue()).create(input.getTag());
            }
        });
        return textClassifier.train(instances);
    }

    /**
     * Train the tagger in language independent mode.
     * 
     * @param trainingFilePath The path of the training file.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     */
    private void trainLanguageIndependent(String trainingFilePath, List<Annotation> additionalTrainingAnnotations) {
        String text = FileFormatParser.getText(trainingFilePath, COLUMN);

        // get all training annotations
        Annotations<Annotation> tokenAnnotations = FileFormatParser
                .getAnnotationsFromColumnTokenBased(trainingFilePath);
        tokenAnnotations.addAll(additionalTrainingAnnotations);

        // get annotations combined, e.g. "Phil Simmons", not "Phil" and "Simmons"
        Annotations<Annotation> combinedAnnotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);
        combinedAnnotations.addAll(additionalTrainingAnnotations);

        model = new PalladianNerModel();
        model.languageMode = LanguageIndependent;
        model.trainingMode = trainingSettings.getTrainingMode();
        model.leftContexts = buildLeftContexts(text, combinedAnnotations);
        model.contextDictionary = buildContextDictionary(text, combinedAnnotations);
        model.entityDictionary = buildEntityDictionary(combinedAnnotations);
        model.annotationDictionary = buildAnnotationDictionary(tokenAnnotations);
    }

    /**
     * Train the tagger in English mode.
     * 
     * @param trainingFilePath The path of the training file.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     */
    private void trainEnglish(String trainingFilePath, List<Annotation> additionalTrainingAnnotations) {
        String text = FileFormatParser.getText(trainingFilePath, COLUMN);
        Annotations<Annotation> fileAnnotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        model = new PalladianNerModel();
        model.languageMode = LanguageMode.English;
        model.trainingMode = trainingSettings.getTrainingMode();
        model.lowerCaseDictionary = buildCaseDictionary(text);

        if (trainingSettings.isEqualizeTypeCounts()) {
            // XXX also add to trainLanguageIndependent?
            Bag<String> typeCounts = Bag.create(CollectionHelper.convert(fileAnnotations, TAG_CONVERTER));
            int minCount = typeCounts.getMin().getValue();
            Annotations<Annotation> equalizedSampling = new Annotations<Annotation>();
            for (String type : typeCounts.uniqueItems()) {
                Iterable<Annotation> currentType = CollectionHelper
                        .filter(fileAnnotations, AnnotationFilters.tag(type));
                Collection<Annotation> sampled = MathHelper.sample(currentType, minCount);
                equalizedSampling.addAll(CollectionHelper.newHashSet(sampled));
            }
            LOGGER.info("Original distribution {}; reduced from {} to {} for equalization", typeCounts,
                    fileAnnotations.size(), equalizedSampling.size());
            fileAnnotations = equalizedSampling;
        }

        model.leftContexts = buildLeftContexts(text, fileAnnotations);
        model.contextDictionary = buildContextDictionary(text, fileAnnotations);

        Annotations<Annotation> annotations = new Annotations<Annotation>(fileAnnotations);
        if (additionalTrainingAnnotations.size() > 0) {
            annotations.addAll(additionalTrainingAnnotations);
            LOGGER.info("Add {} additional training annotations", additionalTrainingAnnotations.size());
        }

        model.entityDictionary = buildEntityDictionary(annotations);
        model.annotationDictionary = buildAnnotationDictionary(annotations);

        // in complete training mode, the tagger is learned twice on the training data
        if (trainingSettings.getTrainingMode() == Complete) {
            LOGGER.info("Start retraining (because of complete dataset, no sparse annotations)");
            model.removeAnnotations = CollectionHelper.newHashSet();
            EvaluationResult evaluationResult = evaluate(trainingFilePath, COLUMN);
            Set<String> goldAnnotations = CollectionHelper.convertSet(fileAnnotations, VALUE_CONVERTER);
            // get only those annotations that were incorrectly tagged and were never a real entity that is they have to
            // be in ERROR1 set and NOT in the gold standard
            for (Annotation wrongAnnotation : evaluationResult.getAnnotations(ERROR1)) {
                String wrongValue = wrongAnnotation.getValue();
                annotations.add(new ImmutableAnnotation(wrongAnnotation.getStartPosition(), wrongValue, NO_ENTITY));
                // check if annotation happens to be in the gold standard, if so, do not declare it completely wrong
                if (!goldAnnotations.contains(wrongValue)) {
                    model.removeAnnotations.add(wrongValue.toLowerCase());
                }
            }
            LOGGER.info("{} annotations need to be completely removed", model.removeAnnotations.size());
            model.annotationDictionary = buildAnnotationDictionary(annotations);
        }

    }

    /**
     * Classify candidate annotations.
     * 
     * @param entityCandidates The annotations to be classified.
     * @return Classified annotations.
     */
    private Annotations<ClassifiedAnnotation> classifyCandidates(Collection<Annotation> entityCandidates) {
        PalladianTextClassifier classifier = new PalladianTextClassifier(model.annotationDictionary.getFeatureSetting());
        Annotations<ClassifiedAnnotation> annotations = new Annotations<ClassifiedAnnotation>();
        for (Annotation annotation : entityCandidates) {
            CategoryEntries categoryEntries = classifier.classify(annotation.getValue(), model.annotationDictionary);
            if (categoryEntries.getProbability(NO_ENTITY) < 0.5) {
                annotations.add(new ClassifiedAnnotation(annotation, categoryEntries));
            }
        }
        return annotations;
    }

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String inputText) {
        Annotations<ClassifiedAnnotation> annotations = getAnnotationsInternal(inputText);
        // recognize and add URLs, remove annotations that were part of a URL
        if (model.getTaggingSettings().isTagUrls()) {
            LOGGER.info("Tagging URLs");
            annotations.addAll(getAnnotations(UrlTagger.INSTANCE, inputText));
        }
        // recognize and add dates, remove annotations that were part of a date
        if (model.getTaggingSettings().isTagDates()) {
            LOGGER.info("Tagging dates");
            annotations.addAll(getAnnotations(DateAndTimeTagger.DEFAULT, inputText));
        }
        annotations.removeNested();
        return annotations;
    }

    private static List<ClassifiedAnnotation> getAnnotations(Tagger tagger, String inputText) {
        List<ClassifiedAnnotation> result = CollectionHelper.newArrayList();
        for (Annotation annotation : tagger.getAnnotations(inputText)) {
            CategoryEntries categoryEntries = new CategoryEntriesBuilder().set(annotation.getTag(), 1).create();
            result.add(new ClassifiedAnnotation(annotation, categoryEntries));
        }
        return result;
    }

    /**
     * <p>
     * Here all classified annotations are processed again. Depending on the learning settings different actions are
     * performed: Entities are re-classified by their contexts or by a dictionary.
     * 
     * @param text The text.
     * @param annotations The classified annotations to process
     * @return The processed (and potentially re-classified) annotations.
     */
    private Annotations<ClassifiedAnnotation> postProcessAnnotations(String text,
            Annotations<ClassifiedAnnotation> annotations) {
        LOGGER.debug("Start post processing annotations");
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        // switch using pattern information
        if (model.getTaggingSettings().isSwitchTagAnnotationsUsingContext() && model.contextDictionary != null) {
            Annotations<ClassifiedAnnotation> switched = new Annotations<ClassifiedAnnotation>();
            int changed = 0;
            for (ClassifiedAnnotation annotation : annotations) {
                ClassifiedAnnotation result = applyContextAnalysis(annotation, text);
                if (!result.sameTag(annotation)) {
                    LOGGER.debug("Changed {} from {} to {}, context: {}", annotation.getValue(), annotation.getTag(),
                            result.getTag(), NerHelper.getCharacterContext(annotation, text, PalladianNerTrainingSettings.WINDOW_SIZE));
                    changed++;
                }
                switched.add(result);
            }
            double percentage = changed > 0 ? 100. * changed / annotations.size() : 0;
            LOGGER.debug("Changed {} % using patterns", format.format(percentage));
            annotations = switched;
        }
        // switch annotations that are in the dictionary
        if (model.getTaggingSettings().isSwitchTagAnnotationsUsingDictionary()) {
            Annotations<ClassifiedAnnotation> switched = new Annotations<ClassifiedAnnotation>();
            int changed = 0;
            for (ClassifiedAnnotation annotation : annotations) {
                CategoryEntries categoryEntries = model.entityDictionary.getCategoryEntries(annotation.getValue());
                if (categoryEntries.size() > 0) {
                    // get only the most likely concept
                    if (model.conceptLikelihoodOrder != null) {
                        for (String conceptName : model.conceptLikelihoodOrder) {
                            double probability = categoryEntries.getProbability(conceptName);
                            if (probability > 0) {
                                categoryEntries = new CategoryEntriesBuilder().set(conceptName, 1).create();
                                break;
                            }
                        }
                    }
                    if (!annotation.getTag().equals(categoryEntries.getMostLikelyCategory())) {
                        LOGGER.debug("Changed {} from {} to {} with dictionary", annotation.getValue(),
                                annotation.getTag(), categoryEntries.getMostLikelyCategory());
                        changed++;
                    }
                    annotation = new ClassifiedAnnotation(annotation, categoryEntries);
                }
                switched.add(annotation);
            }
            double percentage = changed > 0 ? 100. * changed / annotations.size() : 0;
            LOGGER.debug("Changed {} % using entity dictionary", format.format(percentage));
            annotations = switched;
        }
        return annotations;
    }

    private Annotations<ClassifiedAnnotation> getAnnotationsInternal(String inputText) {
        Tagger tagger;
        if (model.languageMode == LanguageIndependent) {
            // get the candidates, every token is potentially a (part of) an entity
            tagger = new RegExTagger(Tokenizer.TOKEN_SPLIT_REGEX, StringTagger.CANDIDATE_TAG);
        } else {
            // use the the string tagger to tag entities in English mode
            tagger = StringTagger.INSTANCE;
        }
        Set<Annotation> annotations = CollectionHelper.newHashSet(tagger.getAnnotations(inputText));
        preProcessAnnotations(annotations);
        Annotations<ClassifiedAnnotation> classifiedAnnotations = classifyCandidates(annotations);
        classifiedAnnotations = postProcessAnnotations(inputText, classifiedAnnotations);
        CollectionHelper.remove(classifiedAnnotations, not(AnnotationFilters.tag(NO_ENTITY)));
        if (model.languageMode == LanguageIndependent) {
            classifiedAnnotations = combineAnnotations(classifiedAnnotations);
        }
        return classifiedAnnotations;
    }

    /**
     * Combine annotations that are right next to each other having the same tag.
     * 
     * @param annotations The annotations to combine.
     * @return The combined annotations.
     */
    private static Annotations<ClassifiedAnnotation> combineAnnotations(Annotations<ClassifiedAnnotation> annotations) {
        Annotations<ClassifiedAnnotation> combinedAnnotations = new Annotations<ClassifiedAnnotation>();
        annotations.sort();
        ClassifiedAnnotation previous = null;
        ClassifiedAnnotation previousCombined = null;
        for (ClassifiedAnnotation current : annotations) {
            if (current.getTag().equalsIgnoreCase("o")) {
                continue;
            }
            if (previous != null && current.sameTag(previous)
                    && current.getStartPosition() == previous.getEndPosition() + 1) {
                if (previousCombined == null) {
                    previousCombined = previous;
                }
                int startPosition = previousCombined.getStartPosition();
                String value = previousCombined.getValue() + " " + current.getValue();
                ClassifiedAnnotation combined = new ClassifiedAnnotation(startPosition, value,
                        previous.getCategoryEntries());
                combinedAnnotations.add(combined);
                previousCombined = combined;
                combinedAnnotations.remove(previousCombined);
            } else {
                combinedAnnotations.add(current);
                previousCombined = null;
            }
            previous = current;
        }
        return combinedAnnotations;
    }

    private void preProcessAnnotations(Set<Annotation> annotations) {
        LOGGER.debug("Start pre processing annotations");
        if (model.getTaggingSettings().isRemoveIncorrectlyTaggedInTraining()) {
            removeIncorrectlyTaggedInTraining(annotations);
        }
        if (model.getTaggingSettings().isUnwrapEntities()) {
            unwrapEntities(annotations);
        }
        if (model.getTaggingSettings().isUnwrapEntitiesWithContext() && model.leftContexts != null) {
            unwrapWithContext(annotations);
        }
        if (model.getTaggingSettings().isRemoveDateFragments()) {
            removeDateFragments(annotations);
        }
        if (model.getTaggingSettings().isRemoveSentenceStartErrorsCaseDictionary() && model.lowerCaseDictionary != null) {
            removeSentenceStartErrors(annotations);
        }
        if (model.getTaggingSettings().isFixStartErrorsCaseDictionary() && model.lowerCaseDictionary != null) {
            fixStartErrorsWithCaseDictionary(annotations);
        }
        if (model.getTaggingSettings().isRemoveDates()) {
            removeDates(annotations);
        }
    }

    private void fixStartErrorsWithCaseDictionary(Set<Annotation> annotations) {
        Set<Annotation> toAdd = CollectionHelper.newHashSet();
        Set<Annotation> toRemove = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            String value = annotation.getValue();
            String[] parts = value.split("\\s");
            if (parts.length == 1) {
                continue;
            }
            int offsetCut = 0;
            String newValue = value;
            for (String token : parts) {
                if (model.entityDictionaryContains(newValue)) {
                    LOGGER.trace("'{}' is in entity dictionary, stop correcting", newValue);
                    break;
                }
                if (!model.lowerCaseDictionary.contains(token.toLowerCase())) {
                    LOGGER.trace("Stop correcting '{}' at '{}' because of lc/uc ratio of {}", new Object[] {value,
                            newValue, model.lowerCaseDictionary.contains(token.toLowerCase())});
                    break;
                }
                offsetCut += token.length() + 1;
                if (offsetCut >= value.length()) {
                    break;
                }
                newValue = value.substring(offsetCut);
            }
            if (offsetCut >= value.length()) {
                LOGGER.debug("Drop '{}' completely because of lc/uc ratio", value);
                toRemove.add(annotation);
            } else if (offsetCut > 0) { // annotation start was corrected
                LOGGER.debug("Correct '{}' to '{}' because of lc/uc ratios", value, newValue);
                int newStart = annotation.getStartPosition() + offsetCut;
                toRemove.add(annotation);
                toAdd.add(new ImmutableAnnotation(newStart, newValue, annotation.getTag()));
            }
        }
        LOGGER.debug("Adding {}, removing {} through case dictionary unwrapping", toAdd.size(), toRemove.size());
        annotations.removeAll(toRemove);
        annotations.addAll(toAdd);
    }

    private static void removeDateFragments(Set<Annotation> annotations) {
        Set<Annotation> toAdd = CollectionHelper.newHashSet();
        Set<Annotation> toRemove = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            Annotation result = removeDateFragment(annotation);
            if (result != null) {
                toRemove.add(annotation);
                toAdd.add(result);
            }
        }
        LOGGER.debug("Removed {} partial date annotations", toRemove.size());
        annotations.addAll(toAdd);
        annotations.removeAll(toRemove);
    }

    private static void removeDates(Set<Annotation> annotations) {
        int numRemoved = CollectionHelper.remove(annotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation annotation) {
                return !isDateFragment(annotation.getValue());
            }
        });
        LOGGER.debug("Removed {} purely date annotations", numRemoved);
    }

    private void unwrapWithContext(Set<Annotation> annotations) {
        Set<Annotation> toAdd = CollectionHelper.newHashSet();
        Set<Annotation> toRemove = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            String entity = annotation.getValue();
            // do not unwrap, in case we have the value in the entity dictionary
            if (model.entityDictionary.getCategoryEntries(entity).getTotalCount() > 0) {
                continue;
            }
            for (String leftContext : model.leftContexts) {
                int index1 = entity.indexOf(leftContext + " ");
                int index2 = entity.indexOf(" " + leftContext + " ");
                int length = -1;
                int index = -1;
                if (index1 == 0) {
                    length = leftContext.length() + 1;
                    index = index1;
                } else if (index2 > -1) {
                    length = leftContext.length() + 2;
                    index = index2;
                }
                if (index != -1) {
                    // get the annotation after the index
                    int startPosition = annotation.getStartPosition() + index + length;
                    String value = annotation.getValue().substring(index + length);
                    toAdd.add(new ImmutableAnnotation(startPosition, value, annotation.getTag()));
                    // search for a known instance in the prefix by going through the entity dictionary
                    String prefix = annotation.getValue().substring(0, index + length);
                    List<String> parts = StringHelper.getSubPhrases(prefix);
                    for (String part : parts) {
                        if (model.entityDictionaryContains(part)) {
                            int prefixStart = annotation.getStartPosition() + prefix.indexOf(part);
                            toAdd.add(new ImmutableAnnotation(prefixStart, value));
                            LOGGER.debug("Add from prefix {}", part);
                        }
                    }
                    toRemove.add(annotation);
                    LOGGER.debug("Add {}, delete {} (left context: {})", value, annotation.getValue(), leftContext);
                    break;
                }
            }
        }
        annotations.addAll(toAdd);
        annotations.removeAll(toRemove);
    }

    /**
     * Use a learned case dictionary to remove possibly incorrectly tagged sentence starts. For example ". This" is
     * removed since "this" is usually spelled using lowercase characters only. This is done NOT only for words at
     * sentence start but all single token words.
     * 
     * @param annotations The annotations.
     */
    private void removeSentenceStartErrors(Set<Annotation> annotations) {
        int removed = CollectionHelper.remove(annotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation annotation) {
                if (annotation.getValue().indexOf(" ") == -1) {
                    if (model.lowerCaseDictionary.contains(annotation.getValue().toLowerCase())) {
                        LOGGER.debug("Remove by case signature: {}", annotation.getValue());
                        return false;
                    }
                }
                return true;
            }
        });
        LOGGER.debug("Removed {} words using case dictionary", removed);
    }

    private void removeIncorrectlyTaggedInTraining(Set<Annotation> annotations) {
        int removed = CollectionHelper.remove(annotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation annotation) {
                return !model.removeAnnotations.contains(annotation.getValue().toLowerCase());
            }
        });
        LOGGER.debug("Removed {} incorrectly tagged entities in training data", removed);
    }

    private void unwrapEntities(Set<Annotation> annotations) {
        Set<Annotation> toAdd = CollectionHelper.newHashSet();
        Set<Annotation> toRemove = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            boolean isAllUppercase = StringHelper.isCompletelyUppercase(annotation.getValue());
            if (isAllUppercase) {
                Set<Annotation> unwrapped = unwrapAnnotations(annotation, annotations);
                if (unwrapped.size() > 0) {
                    toAdd.addAll(unwrapped);
                    toRemove.add(annotation);
                }
            }
        }
        annotations.removeAll(toRemove);
        annotations.addAll(toAdd);
        LOGGER.debug("Unwrapping removed {}, added {} entities", toRemove.size(), toAdd.size());
    }

    private ClassifiedAnnotation applyContextAnalysis(ClassifiedAnnotation annotation, String text) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        builder.add(annotation.getCategoryEntries());
        FeatureSetting featureSetting = model.contextDictionary.getFeatureSetting();
        Scorer scorer = new ExperimentalScorers.CategoryEqualizationScorer();
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, scorer);
        String context = NerHelper.getCharacterContext(annotation, text, PalladianNerTrainingSettings.WINDOW_SIZE);
        if (context.trim().length() > 2) {
            CategoryEntries contextClassification = classifier.classify(context, model.contextDictionary);
            builder.add(contextClassification);
        }
        return new ClassifiedAnnotation(annotation, builder.create());
    }

    /**
     * Check whether the given text is a date fragment, e.g. "June".
     * 
     * @param value The value to check.
     * @return <code>true</code> in case the text is a date fragment.
     */
    static boolean isDateFragment(String value) {
        for (String dateFragment : RegExp.DATE_FRAGMENTS) {
            if (StringUtils.isBlank(value.replaceAll(dateFragment, " "))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Try to remove date fragments from the given annotation, e.g. "June John Hiatt" becomes "John Hiatt".
     * 
     * @param annotation The annotation to process.
     * @return A new annotation with removed date fragments and fixed offsets, or <code>null</code> in case the given
     *         annotation did not contain a date fragment.
     */
    static Annotation removeDateFragment(Annotation annotation) {
        String newValue = annotation.getValue();
        int newOffset = annotation.getStartPosition();
        for (String dateFragment : RegExp.DATE_FRAGMENTS) {
            String regExp = "(?:" + dateFragment + ")\\.?";
            String beginRegExp = "^" + regExp + " ";
            String endRegExp = " " + regExp + "$";
            int textLength = newValue.length();
            if (StringHelper.countRegexMatches(newValue, beginRegExp) > 0) {
                newValue = newValue.replaceAll(beginRegExp, " ").trim();
                newOffset += textLength - newValue.length();
            }
            if (StringHelper.countRegexMatches(newValue, endRegExp) > 0) {
                newValue = newValue.replaceAll(endRegExp, " ").trim();
            }
        }
        if (annotation.getValue().equals(newValue)) {
            return null;
        }
        LOGGER.debug("Removed date fragment from '{}' gives '{}'", annotation.getValue(), newValue);
        return new ImmutableAnnotation(newOffset, newValue, annotation.getTag());
    }

    /**
     * Build a set with left contexts. These are tokens which appear to the left of an entity, e.g.
     * "President Barack Obama". From the available annotations we determine, whether "President" belongs to the entity,
     * or to the context. This information can be used later, to fix the boundaries of an annotation.
     *
     * @param text The text.
     * @param annotations The annotations.
     * @return A set with tokens which appear more often in the context, than within an entity (e.g. "President").
     */
    private Set<String> buildLeftContexts(String text, Annotations<Annotation> annotations) {
        LOGGER.info("Building left contexts");
        Bag<String> leftContextCounts = Bag.create();
        Bag<String> insideAnnotationCounts = Bag.create();
        for (Annotation annotation : annotations) {
            leftContextCounts.addAll(NerHelper.getLeftContexts(annotation, text, 3));
            String[] split = annotation.getValue().split("\\s");
            StringBuilder partBuilder = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i > 0) {
                    partBuilder.append(' ');
                }
                partBuilder.append(split[i]);
                insideAnnotationCounts.add(partBuilder.toString());
            }
        }
        Set<String> leftContexts = CollectionHelper.newHashSet();
        int minCount = trainingSettings.getMinDictionaryCount();
        for (Entry<String, Integer> entry : leftContextCounts.unique()) {
            String leftContext = entry.getKey();
            if (StringHelper.startsUppercase(leftContext)) {
                int outside = entry.getValue();
                int inside = insideAnnotationCounts.count(leftContext);
                if (outside + inside >= minCount) {
                    double ratio = (double)inside / outside;
                    if (ratio < 1 && outside >= 2) {
                        leftContexts.add(leftContext);
                    }
                }
            }
        }
        return leftContexts;
    }

    private DictionaryModel buildContextDictionary(final String text, Iterable<Annotation> annotations) {
        LOGGER.info("Building context dictionary");
        DictionaryBuilder builder = createDictionaryBuilder();
        PalladianTextClassifier contextClassifier = new PalladianTextClassifier(PalladianNerTrainingSettings.CONTEXT_FEATURE_SETTING, builder );
        Iterable<Instance> instances = CollectionHelper.convert(annotations, new Function<Annotation, Instance>() {
            @Override
            public Instance compute(Annotation input) {
                return new InstanceBuilder().setText(NerHelper.getCharacterContext(input, text, PalladianNerTrainingSettings.WINDOW_SIZE)).create(
                        input.getTag());
            }
        });
        return contextClassifier.train(instances);
    }

    public PalladianNerModel getModel() {
        return model;
    }

    /**
     * <p>
     * If the annotation is completely upper case, like "NEW YORK CITY AND DRESDEN", try to find which of the given
     * annotation are part of this entity. The given example contains two entities that might be in the given annotation
     * set. If so, we return the found annotations.
     * 
     * @param annotation The annotation to check.
     * @param annotations The annotations we are searching for in this entity.
     * @return A set of annotations found in this annotation.
     */
    private Set<Annotation> unwrapAnnotations(Annotation annotation, Set<Annotation> annotations) {
        Set<String> otherValues = CollectionHelper.newHashSet();
        for (Annotation currentAnnotation : annotations) {
            if (!currentAnnotation.equals(annotation)) {
                otherValues.add(currentAnnotation.getValue().toLowerCase());
            }
        }
        Set<Annotation> unwrappedAnnotations = CollectionHelper.newHashSet();
        String annotationValue = annotation.getValue().toLowerCase();
        List<String> parts = StringHelper.getSubPhrases(annotationValue);
        for (String part : parts) {
            String partValue = part.toLowerCase();
            if (otherValues.contains(partValue) || model.entityDictionaryContains(partValue)) {
                int startPosition = annotation.getStartPosition() + annotationValue.indexOf(part);
                unwrappedAnnotations.add(new ImmutableAnnotation(startPosition, part));
            }
        }
        if (LOGGER.isDebugEnabled() && unwrappedAnnotations.size() > 0) {
            List<String> unwrappedParts = CollectionHelper.convertList(unwrappedAnnotations, VALUE_CONVERTER);
            LOGGER.debug("Unwrapped {} in {} parts: {}", annotationValue, unwrappedAnnotations.size(), unwrappedParts);
        }
        return unwrappedAnnotations;
    }

    @Override
    public String getName() {
        return "Palladian NER";
    }

}
