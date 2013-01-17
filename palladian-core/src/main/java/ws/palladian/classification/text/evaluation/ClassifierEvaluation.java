package ws.palladian.classification.text.evaluation;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Model;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.ThresholdAnalyzer;
import ws.palladian.processing.Trainable;

public final class ClassifierEvaluation {

    // XXX integrate in ClassificationUtils?

    private ClassifierEvaluation() {
        // no instances.
    }

    public static <M extends Model, T extends Trainable> ConfusionMatrix evaluate(Classifier<M> classifier, M model,
            Iterable<T> testData) {

        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        for (T testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance, model);
            String classifiedCategory = classification.getMostLikelyCategoryEntry().getName();
            String realCategory = testInstance.getTargetClass();
            confusionMatrix.add(realCategory, classifiedCategory);
        }

        return confusionMatrix;
    }

    public static <M extends Model, T extends Trainable> ThresholdAnalyzer thresholdAnalysis(Classifier<M> classifier,
            M model, Iterable<T> testData, String correctClass) {

        ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer(100);

        for (T testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance, model);
            CategoryEntry categoryEntry = classification.getCategoryEntry(correctClass);
            String realCategory = testInstance.getTargetClass();
            thresholdAnalyzer.add(realCategory.equals(correctClass), categoryEntry.getProbability());
        }

        return thresholdAnalyzer;
    }
    
//    public static <M extends Model> ThresholdAnalyzer thresholdAnalysis(Classifier<M> classifier, M model,
//            Iterable<? extends Classified> testData) {
//
//        ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer(100);
//
//        for (Classified testInstance : testData) {
//            CategoryEntries classification = classifier.classify(testInstance.getFeatureVector(), model);
//            CategoryEntry categoryEntry = classification.getMostLikelyCategoryEntry();
//            String realCategory = testInstance.getTargetClass();
//            thresholdAnalyzer.add(realCategory.equals(categoryEntry.getName()), categoryEntry.getProbability());
//        }
//
//        return thresholdAnalyzer;
//
//    }

}