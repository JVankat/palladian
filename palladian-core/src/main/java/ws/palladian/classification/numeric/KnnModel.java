package ws.palladian.classification.numeric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.classification.Instance;
import ws.palladian.classification.Model;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * The model used by KNN classification algorithms. Like the {@link KnnClassifier}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class KnnModel implements Model {

    /** Used for serializing objects of this class. Should only change if the attribute set of the class changes. */
    private static final long serialVersionUID = -6528509220813706056L;

    /** Training examples which are used for classification. */
    private List<TrainingExample> trainingExamples;

    /**
     * An object carrying the information to normalize {@link FeatureVector}s based on the normalized
     * {@link #trainingExamples}.
     */
    private Normalization normalization;

    /**
     * <p>
     * Creates a new unnormalized {@code KnnModel} based on a {@code List} of {@link Instance}s.
     * </p>
     * 
     * @param trainingInstances The {@link Instance}s this model is based on.
     */
    KnnModel(Iterable<? extends Trainable> trainingInstances, Normalization normalization) {
        this.trainingExamples = initTrainingInstances(trainingInstances, normalization);
        this.normalization = normalization;
    }

    private List<TrainingExample> initTrainingInstances(Iterable<? extends Trainable> instances,
            Normalization normalization) {
        normalization.normalize(instances);
        List<TrainingExample> ret = new ArrayList<TrainingExample>();
        for (Trainable instance : instances) {
            ret.add(new TrainingExample(instance));
        }
        return ret;
    }

    /**
     * @return The training instances underlying this {@link KnnModel}. They are used by the {@code KnnClassifier} to
     *         make a classification decision.
     */
    public List<Trainable> getTrainingExamples() {
        return convertTrainingInstances(trainingExamples);
    }

    private List<Trainable> convertTrainingInstances(List<TrainingExample> instances) {
        List<Trainable> nominalInstances = new ArrayList<Trainable>(instances.size());

        for (TrainingExample instance : trainingExamples) {
            FeatureVector featureVector = new BasicFeatureVector();
            for (Entry<String, Double> feature : instance.features.entrySet()) {
                featureVector.add(new NumericFeature(feature.getKey(), feature.getValue()));
            }
            Instance nominalInstance = new Instance(instance.targetClass, featureVector);
            nominalInstances.add(nominalInstance);
        }

        return nominalInstances;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("KnnModel [");
        toStringBuilder.append("# trainingInstances=").append(trainingExamples.size());
        toStringBuilder.append(" normalization=").append(normalization);
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }

    @Override
    public Set<String> getCategories() {
        Set<String> categories = CollectionHelper.newHashSet();
        for (TrainingExample example : trainingExamples) {
            categories.add(example.targetClass);
        }
        return categories;
    }

    public Normalization getNormalization() {
        return normalization;
    }

}

class TrainingExample implements Serializable {
    private static final long serialVersionUID = 6007693177447711704L;
    final String targetClass;
    final Map<String, Double> features;

    public TrainingExample(Trainable instance) {
        targetClass = instance.getTargetClass();
        features = new HashMap<String, Double>();
        Collection<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);
        for (NumericFeature feature : numericFeatures) {
            features.put(feature.getName(), feature.getValue());
        }
    }

    @Override
    public String toString() {
        return targetClass + ":" + features;
    }
}
