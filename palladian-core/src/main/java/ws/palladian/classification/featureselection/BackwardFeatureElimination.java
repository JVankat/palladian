package ws.palladian.classification.featureselection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickdt.randomForest.RandomForestBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.classification.Model;
import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.EqualsFilter;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.collection.InverseFilter;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * Simple, greedy backward feature elimination. Results are scored using accuracy. Does not consider sparse data. Split
 * for train/test data is 50:50. Important: The scores in the {@link FeatureRanking} returned by this class are no
 * scores, but simple ranking values. This is because the features depend on each other.
 * </p>
 * 
 * @author Philipp Katz
 * @param <M> Type of the model.
 */
public final class BackwardFeatureElimination<M extends Model> implements FeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BackwardFeatureElimination.class);

    private final Factory<? extends Learner<M>> learnerFactory;

    private final Factory<? extends Classifier<M>> classifierFactory;

    private final Function<ConfusionMatrix, Double> scorer;

    private final int numThreads;

    public static final Function<ConfusionMatrix, Double> ACCURACY_SCORER = new Function<ConfusionMatrix, Double>() {
        @Override
        public Double compute(ConfusionMatrix input) {
            return input.getAccuracy();
        }
    };

    private final class TestRun implements Callable<TestRunResult> {

        private final Collection<? extends Trainable> trainData;
        private final Collection<? extends Trainable> testData;
        private final List<String> featuresToEliminate;
        private final ProgressMonitor monitor;

        public TestRun(Collection<? extends Trainable> trainData, Collection<? extends Trainable> testData,
                List<String> featuresToEliminate, ProgressMonitor monitor) {
            this.trainData = trainData;
            this.testData = testData;
            this.featuresToEliminate = featuresToEliminate;
            this.monitor = monitor;
        }

        @Override
        public TestRunResult call() throws Exception {
            String eliminatedFeature = CollectionHelper.getLast(featuresToEliminate);
            LOGGER.debug("Starting elimination for {}", eliminatedFeature);

            Filter<String> filter = InverseFilter.create(EqualsFilter.create(featuresToEliminate));
            List<Trainable> eliminatedTrainData = ClassificationUtils.filterFeatures(trainData, filter);
            List<Trainable> eliminatedTestData = ClassificationUtils.filterFeatures(testData, filter);

            // create a new learner and classifier
            Learner<M> learner = learnerFactory.create();
            Classifier<M> classifier = classifierFactory.create();

            M model = learner.train(eliminatedTrainData);
            @SuppressWarnings("unchecked")
            ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(classifier, eliminatedTestData, model);
            Double score = scorer.compute(confusionMatrix);

            LOGGER.debug("Finished elimination for {}", eliminatedFeature);
            monitor.incrementAndPrintProgress();
            return new TestRunResult(score, eliminatedFeature);
        }

    }

    private static final class TestRunResult {
        private final Double score;
        private final String eliminatedFeature;

        public TestRunResult(Double score, String eliminatedFeature) {
            this.score = score;
            this.eliminatedFeature = eliminatedFeature;
        }
    }

    /**
     * <p>
     * Create a new {@link BackwardFeatureElimination} with the given learner and classifier. Not threading is used.
     * </p>
     * 
     * @param learner The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     */
    public BackwardFeatureElimination(Learner<M> learner, Classifier<M> classifier) {
        this(ConstantFactory.create(learner), ConstantFactory.create(classifier), ACCURACY_SCORER, 1);
    }

    /**
     * <p>
     * Create a new {@link BackwardFeatureElimination} with the given learner and classifier. The scoring can be
     * parameterized through the {@link Function} argument; it must return a ranking value which is used to for deciding
     * which feature to eliminate. The {@link Factory}s are necessary, because each thread needs its own {@link Learner}
     * and {@link Classifier}.
     * </p>
     * 
     * @param learnerFactory Factory for the learner, not <code>null</code>.
     * @param classifierFactory Factory for the classifier, not <code>null</code>.
     * @param scorer The function for determining the score, not <code>null</code>.
     * @param numThreads Use the specified number of threads to parallelize training/testing. Must be greater/equal one.
     */
    public BackwardFeatureElimination(Factory<? extends Learner<M>> learnerFactory,
            Factory<? extends Classifier<M>> classifierFactory, Function<ConfusionMatrix, Double> scorer, int numThreads) {
        Validate.notNull(learnerFactory, "learnerFactory must not be null");
        Validate.notNull(classifierFactory, "classifierFactory must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
        this.learnerFactory = learnerFactory;
        this.classifierFactory = classifierFactory;
        this.scorer = scorer;
        this.numThreads = numThreads;
    }

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Trainable> dataset) {
        List<Trainable> instances = new ArrayList<Trainable>(dataset);
        Collections.shuffle(instances);
        List<Trainable> trainData = instances.subList(0, instances.size() / 2);
        List<Trainable> testData = instances.subList(instances.size() / 2, instances.size());
        return rankFeatures(trainData, testData);
    }

    /**
     * <p>
     * Perform the backward feature elimination for the two specified training and validation set.
     * </p>
     * 
     * @param trainSet The training set, not <code>null</code>.
     * @param validationSet The validation/testing set, not <code>null</code>.
     * @return A {@link FeatureRanking} containing the features in the order in which they were eliminated.
     */
    public FeatureRanking rankFeatures(Collection<? extends Trainable> trainSet,
            Collection<? extends Trainable> validationSet) {
        final FeatureRanking result = new FeatureRanking();

        final Set<String> allFeatures = ClassificationUtils.getFeatureNames(trainSet);
        final List<String> eliminatedFeatures = CollectionHelper.newArrayList();
        final int iterations = allFeatures.size() * (allFeatures.size() + 1) / 2;
        final ProgressMonitor progressMonitor = new ProgressMonitor(iterations, 0);
        int featureIndex = 0;
        
        LOGGER.info("# of features in dataset: {}", allFeatures.size());
        LOGGER.info("# of iterations: {}", iterations);

        try {
            // run with all features
            TestRun initialRun = new TestRun(trainSet, validationSet, Arrays.asList("<none>"), progressMonitor);
            TestRunResult startScore = initialRun.call();
            LOGGER.info("Score with all features {}", startScore.score);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            // stepwise elimination
            for (;;) {
                Set<String> featuresToCheck = new HashSet<String>(allFeatures);
                featuresToCheck.removeAll(eliminatedFeatures);
                if (featuresToCheck.isEmpty()) {
                    break;
                }
                List<TestRun> runs = CollectionHelper.newArrayList();

                for (String currentFeature : featuresToCheck) {
                    List<String> featuresToEliminate = new ArrayList<String>(eliminatedFeatures);
                    featuresToEliminate.add(currentFeature);
                    runs.add(new TestRun(trainSet, validationSet, featuresToEliminate, progressMonitor));
                }

                List<Future<TestRunResult>> runFutures = executor.invokeAll(runs);
                String selectedFeature = null;
                double highestScore = 0;
                for (Future<TestRunResult> future : runFutures) {
                    TestRunResult testRunResult = future.get();
                    if (testRunResult.score >= highestScore || selectedFeature == null) {
                        highestScore = testRunResult.score;
                        selectedFeature = testRunResult.eliminatedFeature;
                    }
                }

                // LOGGER.debug("Eliminating {} gives {}", currentFeature, score);
                LOGGER.info("Selected {} for elimination, score {}", selectedFeature, highestScore);
                eliminatedFeatures.add(selectedFeature);
                result.add(selectedFeature, featureIndex++);
            }
            
            executor.shutdown();
            
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    public static void main(String[] args) {
        // List<Trainable> trainSet = ClassificationUtils.readCsv("data/temp/location_disambiguation_1376006525521_all_train.csv", true);
        // List<Trainable> validationSet = ClassificationUtils.readCsv("data/temp/location_disambiguation_1376012524940_all_valid.csv", true);
        
        // take a sub sampling of TUD, LGL and Clust; make # of samples roughly equal for each data set
        
        List<Trainable> tudTrain = ClassificationUtils.readCsv("/Users/pk/Dropbox/temp_bfe_location/fd_tud_train_1376394038036.csv", true);
        List<Trainable> lglTrain = ClassificationUtils.readCsv("/Users/pk/Dropbox/temp_bfe_location/fd_lgl_train_1376399225449.csv", true);
        List<Trainable> clustTrain = ClassificationUtils.readCsv("/Users/pk/Dropbox/temp_bfe_location/fd_clust_train_1376413884470.csv", true);
        lglTrain = ClassificationUtils.drawRandomSubset(lglTrain, 30);
        clustTrain = ClassificationUtils.drawRandomSubset(clustTrain, 15);
        List<Trainable> trainSet = CollectionHelper.newArrayList();
        trainSet.addAll(tudTrain);
        trainSet.addAll(lglTrain);
        trainSet.addAll(clustTrain);
        
        List<Trainable> tudValidate = ClassificationUtils.readCsv("/Users/pk/Dropbox/temp_bfe_location/fd_tud_validation_1376419927925.csv", true);
        List<Trainable> lglValidate = ClassificationUtils.readCsv("/Users/pk/Dropbox/temp_bfe_location/fd_lgl_validation_1376420924580.csv", true);
        List<Trainable> clustValidate = ClassificationUtils.readCsv("/Users/pk/Dropbox/temp_bfe_location/fd_clust_validation_1376422975187.csv", true);
        lglValidate = ClassificationUtils.drawRandomSubset(lglValidate, 30);
        clustValidate = ClassificationUtils.drawRandomSubset(clustValidate, 15);
        List<Trainable> validationSet = CollectionHelper.newArrayList();
        validationSet.addAll(tudValidate);
        validationSet.addAll(lglValidate);
        validationSet.addAll(clustValidate);
        
        ClassificationUtils.writeCsv(trainSet, new File("/Users/pk/Desktop/fd_merged_train.csv"));
        ClassificationUtils.writeCsv(validationSet, new File("/Users/pk/Desktop/fd_merged_validation.csv"));
        System.exit(0);
        
        // skip those features: indexScore (expensive); containsMarker(...) except the consolidated containsMarker(*)
        trainSet = ClassificationUtils.filterFeatures(trainSet, InverseFilter.create(EqualsFilter.create("indexScore")));
        validationSet = ClassificationUtils.filterFeatures(validationSet, InverseFilter.create(EqualsFilter.create("indexScore")));
        Filter<String> markerFilter = new Filter<String>() {
            @Override
            public boolean accept(String item) {
                if (item.startsWith("containsMarker")) {
                    return item.equals("containsMarker(*)");
                }
                return true;
            }
        };
        trainSet = ClassificationUtils.filterFeatures(trainSet, markerFilter);
        validationSet = ClassificationUtils.filterFeatures(validationSet, markerFilter);
        
        // should be 106 features without indexScore
        // should be 82 features without individual markers

        // the classifier/predictor to use; when using threading, they have to be created through the factory, as we
        // require them for each thread
        Factory<QuickDtLearner> learnerFactory = new Factory<QuickDtLearner>() {
            @Override
            public QuickDtLearner create() {
                return new QuickDtLearner(new RandomForestBuilder().numTrees(10));
            }
        };
        // we can share this, because it has no state
        Factory<QuickDtClassifier> predictorFactory = ConstantFactory.create(new QuickDtClassifier());

        // scoring function used for deciding which feature to eliminate; we use the F1 measure here, but in general all
        // measures as provided by the ConfusionMatrix can be used (e.g. accuracy, precision, ...).
        Function<ConfusionMatrix, Double> scorer = new Function<ConfusionMatrix, Double>() {
            @Override
            public Double compute(ConfusionMatrix input) {
                return input.getF(1.0, "true");
            }
        };

        BackwardFeatureElimination<QuickDtModel> elimination = new BackwardFeatureElimination<QuickDtModel>(
                learnerFactory, predictorFactory, scorer, 4);
        FeatureRanking featureRanking = elimination.rankFeatures(trainSet, validationSet);
        CollectionHelper.print(featureRanking.getAll());
    }

}