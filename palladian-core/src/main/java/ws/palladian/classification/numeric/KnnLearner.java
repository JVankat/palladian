package ws.palladian.classification.numeric;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Learner;
import ws.palladian.classification.utils.MinMaxNormalizer;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.classification.utils.Normalizer;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * A "learner" for KNN models. It stores all supplied instances to a model which are later used for prediction.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class KnnLearner implements Learner<KnnModel> {

    /** The normalizer for numeric values. */
    private final Normalizer normalizer;

    /**
     * <p>
     * Create a new {@link KnnLearner} with the specified {@link Normalizer}.
     * </p>
     * 
     * @param normalizer The normalizer to use, not <code>null</code>. (use {@link NoNormalizer} in case you do not want
     *            to perform normalization).
     */
    public KnnLearner(Normalizer normalizer) {
        Validate.notNull(normalizer, "normalizer must not be null");
        this.normalizer = normalizer;
    }

    /**
     * <p>
     * Create a new {@link KnnLearner} using a {@link MinMaxNormalizer}.
     * </p>
     */
    public KnnLearner() {
        this(new MinMaxNormalizer());
    }

    @Override
    public KnnModel train(Iterable<? extends Trainable> trainables) {
        Normalization normalization = normalizer.calculate(trainables);
        return new KnnModel(trainables, normalization);
    }

}