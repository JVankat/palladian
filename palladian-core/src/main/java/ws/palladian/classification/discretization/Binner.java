package ws.palladian.classification.discretization;

import static java.lang.Math.pow;
import static ws.palladian.helper.math.MathHelper.log2;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.NullValue;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class Binner {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Binner.class);

    /**
     * Comparator to sort {@link Instance}s based on a {@link NumericValue}.
     * 
     * @author pk
     */
    private static final class ValueComparator implements Comparator<Instance> {
        private final String featureName;

        private ValueComparator(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public int compare(Instance i1, Instance i2) {
            Value value1 = i1.getVector().get(featureName);
            Value value2 = i2.getVector().get(featureName);
            // push NullValues to the end
            if (!(value1 instanceof NumericValue)) {
                return !(value2 instanceof NumericValue) ? 0 : 1;
            }
            if (!(value2 instanceof NumericValue)) {
                return -1;
            }
            double double1 = ((NumericValue)value1).getDouble();
            double double2 = ((NumericValue)value2).getDouble();
            return Double.compare(double1, double2);
        }

    }

    private final List<Double> boundaries;

    private final String featureName;

    /**
     * <p>
     * Create a new {@link Binner} for specified numeric feature following the algorithm proposed by Fayyad and Irani in
     * "<a href="http://ijcai.org/Past%20Proceedings/IJCAI-93-VOL2/PDF/022.pdf
     * ">Multi-Interval Discretization of Continuous-Valued Attributes for Classification Learning</a>", 1993.
     * </p>
     * 
     * @param dataset The dataset, not <code>null</code>.
     * @param featureName The name of the numeric feature for which to calculate bins.
     */
    public Binner(Iterable<? extends Instance> dataset, String featureName) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.notEmpty(featureName, "featureName must not be empty");
        List<Instance> sortedData = CollectionHelper.newArrayList(dataset);
        Collections.sort(sortedData, new ValueComparator(featureName));
        // exclude NullValues from boundary search
        int idx = 0;
        for (Instance instance : sortedData) {
            if (instance.getVector().get(featureName) == NullValue.NULL) {
                break;
            }
            idx++;
        }
        sortedData = sortedData.subList(0, idx);
        this.boundaries = findBoundaries(sortedData, featureName);
        this.featureName = featureName;
    }

    /**
     * Find all the boundary points within the provided dataset.
     * 
     * @param dataset The dataset, not <code>null</code>.
     * @return The values of the boundary points, each value denotes the beginning of a new bin, empty list in case no
     *         boundary points were found.
     */
    private static List<Double> findBoundaries(List<Instance> dataset, String featureName) {

        CategoryEntries categoryPriors = ClassificationUtils.getCategoryCounts(dataset);
        double entS = ClassificationUtils.entropy(categoryPriors);
        int k = categoryPriors.size();
        int n = dataset.size();

        double maxGain = 0;
        double currentBoundary = 0;
        int boundaryIdx = -1;

        for (int i = 1; i < n; i++) {

            double previous = ((NumericValue)dataset.get(i - 1).getVector().get(featureName)).getDouble();
            double current = ((NumericValue)dataset.get(i).getVector().get(featureName)).getDouble();

            if (previous < current) {

                List<Instance> s1 = dataset.subList(0, i);
                List<Instance> s2 = dataset.subList(i, n);
                CategoryEntries c1 = ClassificationUtils.getCategoryCounts(s1);
                CategoryEntries c2 = ClassificationUtils.getCategoryCounts(s2);
                double entS1 = ClassificationUtils.entropy(c1);
                double entS2 = ClassificationUtils.entropy(c2);

                double ent = (double)s1.size() / n * entS1 + (double)s2.size() / n * entS2;
                double gain = entS - ent;
                double delta = log2(pow(3, k) - 2) - (k * entS - c1.size() * entS1 - c2.size() * entS2);
                boolean mdlpcCriterion = gain > (log2(n - 1) + delta) / n;

                if (mdlpcCriterion && gain > maxGain) {
                    maxGain = gain;
                    currentBoundary = (previous + current) / 2;
                    boundaryIdx = i;
                }
            }
        }

        if (maxGain == 0) { // stop recursion
            return Collections.emptyList();
        }

        LOGGER.debug("cut point = {} @ {}, gain = {}", currentBoundary, boundaryIdx, maxGain);

        // search boundaries recursive; result: find[leftSplit], currentBoundary, find[rightSplit]
        List<Double> boundaries = CollectionHelper.newArrayList();
        boundaries.addAll(findBoundaries(dataset.subList(0, boundaryIdx), featureName));
        boundaries.add(currentBoundary);
        boundaries.addAll(findBoundaries(dataset.subList(boundaryIdx, n), featureName));
        return boundaries;
    }

    /**
     * Get the bin for the given value.
     * 
     * @param value The value.
     * @return The bin for the value.
     */
    public int bin(double value) {
        int position = Collections.binarySearch(boundaries, value);
        return position < 0 ? -position - 1 : position + 1;
    }

    /**
     * Get the number of boundary points (i.e. numBoundaryPoints + 1 = numBins).
     * 
     * @return The number of boundary points.
     */
    public int getNumBoundaryPoints() {
        return boundaries.size();
    }

    /**
     * @return The values of the boundary points, or an empty {@link List} in case no boundary points exist.
     */
    public List<Double> getBoundaries() {
        return Collections.unmodifiableList(boundaries);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(featureName).append('\t');
        stringBuilder.append("# ");
        stringBuilder.append(boundaries.size());
        stringBuilder.append('\t');
        boolean first = true;
        for (Double bin : boundaries) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('|');
            }
            stringBuilder.append(bin);
        }
        return stringBuilder.toString();
    }

}
