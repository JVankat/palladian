package ws.palladian.helper.math;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>
 * The MathHelper provides mathematical functionality.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class MathHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MathHelper.class);

    public static final Random RANDOM = new Random();

    private static final Map<Double, String> FRACTION_MAP;

    private static final Map<Double, Double> LOC_Z_MAPPING;

    private static final Pattern FRACTION_PATTERN = Pattern.compile("(\\d+)/(\\d+)");
    private static final Pattern EX_PATTERN = Pattern.compile("\\d+\\.\\d+e\\d+");
    private static final Pattern CLEAN_PATTERN1 = Pattern.compile("[^0-9.]");
    private static final Pattern CLEAN_PATTERN2 = Pattern.compile("\\.(?!\\d)");
    private static final Pattern CLEAN_PATTERN3 = Pattern.compile("(?<!\\d)\\.");

    /** The supported confidence levels. */
    public static final Collection<Double> CONFIDENCE_LEVELS;

    static {
        FRACTION_MAP = new HashMap<>();
        FRACTION_MAP.put(0.5, "1/2");
        FRACTION_MAP.put(0.3333, "1/3");
        FRACTION_MAP.put(0.6667, "2/3");
        FRACTION_MAP.put(0.25, "1/4");
        FRACTION_MAP.put(0.75, "3/4");
        FRACTION_MAP.put(0.2, "1/5");
        FRACTION_MAP.put(0.4, "2/5");
        FRACTION_MAP.put(0.6, "3/5");
        FRACTION_MAP.put(0.8, "4/5");
        FRACTION_MAP.put(0.1667, "1/6");
        FRACTION_MAP.put(0.8333, "5/6");
        FRACTION_MAP.put(0.1429, "1/7");
        FRACTION_MAP.put(0.2857, "2/7");
        FRACTION_MAP.put(0.4286, "3/7");
        FRACTION_MAP.put(0.5714, "4/7");
        FRACTION_MAP.put(0.7143, "5/7");
        FRACTION_MAP.put(0.8571, "6/7");
        FRACTION_MAP.put(0.125, "1/8");
        FRACTION_MAP.put(0.375, "3/8");
        FRACTION_MAP.put(0.625, "5/8");
        FRACTION_MAP.put(0.875, "7/8");
        FRACTION_MAP.put(0.1111, "1/9");
        FRACTION_MAP.put(0.2222, "2/9");
        FRACTION_MAP.put(0.4444, "4/9");
        FRACTION_MAP.put(0.5556, "5/9");
        FRACTION_MAP.put(0.7778, "7/9");
        FRACTION_MAP.put(0.8889, "8/9");
        FRACTION_MAP.put(0.1, "1/10");
        FRACTION_MAP.put(0.3, "3/10");
        FRACTION_MAP.put(0.7, "7/10");
        FRACTION_MAP.put(0.9, "9/10");

        Map<Double, Double> locZMapping = new LinkedHashMap<>();
        locZMapping.put(0.75, 1.151);
        locZMapping.put(0.85, 1.139);
        locZMapping.put(0.90, 1.645);
        locZMapping.put(0.95, 1.96);
        locZMapping.put(0.975, 2.243);
        locZMapping.put(0.985, 2.43);
        locZMapping.put(0.99, 2.577);
        locZMapping.put(0.999, 3.3);
        LOC_Z_MAPPING = Collections.unmodifiableMap(locZMapping);
        CONFIDENCE_LEVELS = Collections.unmodifiableSet(locZMapping.keySet());
    }

    private MathHelper() {
        // no instances.
    }

    /**
     * <p>
     * Calculate the Jaccard similarity between two sets. <code>J(A, B) = |A intersection B| / |A union B|</code>.
     * </p>
     * 
     * @param setA The first set, not <code>null</code>.
     * @param setB The second set, not <code>null</code>.
     * @return The Jaccard similarity in the range [0, 1].
     * @deprecated Use {@link SetSimilarities#JACCARD}.
     */
    @Deprecated
    public static <T> double computeJaccardSimilarity(Set<T> setA, Set<T> setB) {
        // Validate.notNull(setA, "setA must not be null");
        // Validate.notNull(setB, "setB must not be null");
        //
        // Set<T> intersection = new HashSet<>();
        // intersection.addAll(setA);
        // intersection.retainAll(setB);
        //
        // if (intersection.size() == 0) {
        // return 0;
        // }
        //
        // Set<T> union = new HashSet<>();
        // union.addAll(setA);
        // union.addAll(setB);
        //
        // return (double)intersection.size() / union.size();

        return SetSimilarities.JACCARD.getSimilarity(setA, setB);
    }

    /**
     * <p>
     * Calculate the overlap coefficient between two sets.
     * <code>Overlap(A, B) = |A intersection B| / min(|A|, |B|)</code>.
     * </p>
     * 
     * @param setA The first set.
     * @param setB The second set.
     * @return The overlap coefficient in the range [0, 1].
     * @deprecated Use {@link SetSimilarities#OVERLAP}.
     */
    @Deprecated
    public static <T> double computeOverlapCoefficient(Set<T> setA, Set<T> setB) {
        // if (setA.size() == 0 || setB.size() == 0) {
        // return 0;
        // }
        //
        // Set<T> intersection = new HashSet<>();
        // intersection.addAll(setA);
        // intersection.retainAll(setB);
        //
        // return (double)intersection.size() / Math.min(setA.size(), setB.size());

        return SetSimilarities.OVERLAP.getSimilarity(setA, setB);
    }

    /** @deprecated Use {@link NumericVector} instead. */
    @Deprecated
    public static double computeCosineSimilarity(Double[] vector1, Double[] vector2) {

        double dotProduct = computeDotProduct(vector1, vector2);
        double magnitude1 = computeMagnitude(vector1);
        double magnitude2 = computeMagnitude(vector2);

        return dotProduct / (magnitude1 * magnitude2);
    }

    /** @deprecated Use {@link NumericVector} instead. */
    @Deprecated
    public static double computeDotProduct(Double[] vector1, Double[] vector2) {
        double dotProduct = 0.0;

        for (int i = 0; i < Math.min(vector1.length, vector2.length); i++) {
            dotProduct += vector1[i] * vector2[i];
        }

        return dotProduct;
    }

    /** @deprecated Use {@link NumericVector} instead. */
    @Deprecated
    public static double computeMagnitude(Double[] vector) {
        double magnitude = 0.0;

        for (Double double1 : vector) {
            magnitude += double1 * double1;
        }

        return Math.sqrt(magnitude);
    }

    /**
     * <p>
     * Calculate the confidence interval with a given confidence level and mean. For more information see here: <a
     * href="http://www.bioconsulting.com/calculation_of_the_confidence_interval.htm">Calculation Of The Confidence
     * Interval</a>.
     * </p>
     * 
     * @param samples The number of samples used, greater zero.
     * @param confidenceLevel The level of confidence. Must be one of the values in {@link #CONFIDENCE_LEVELS}.
     * @param mean The mean, in range [0,1]. If unknown, assume worst case with mean = 0.5.
     * 
     * @return The calculated confidence interval.
     */
    public static double computeConfidenceInterval(long samples, double confidenceLevel, double mean) {
        Validate.isTrue(samples > 0, "samples must be greater zero");
        Validate.isTrue(0 <= mean && mean <= 1, "mean must be in range [0,1]");
        Double z = LOC_Z_MAPPING.get(confidenceLevel);
        if (z == null) {
            throw new IllegalArgumentException("confidence level must be one of: {"
                    + StringUtils.join(CONFIDENCE_LEVELS, ", ") + "}, but was " + confidenceLevel);
        }
        return z * Math.sqrt(mean * (1 - mean) / samples);
    }

    public static double round(double number, int digits) {
        if (Double.isNaN(number)) {
            return Double.NaN;
        }
        double numberFactor = Math.pow(10.0, digits);
        return Math.round(numberFactor * number) / numberFactor;
    }

    /**
     * <p>
     * Check whether one value is in a certain range of another value. For example, value1: 5 is within the range: 2 of
     * value2: 3.
     * </p>
     * 
     * @param value1 The value to check whether it is in the range of the other value.
     * @param value2 The value for which the range is added or subtracted.
     * @param range The range.
     * @return <tt>True</tt>, if value1 <= value2 + range && value1 >= value2 - range, <tt>false</tt> otherwise.
     */
    public static boolean isWithinRange(double value1, double value2, double range) {
        double numMin = value2 - range;
        double numMax = value2 + range;

        return value1 <= numMax && value1 >= numMin;
    }

    /**
     * <p>
     * Check whether one value is in a certain interval. For example, value: 5 is within the interval min: 2 to max: 8.
     * </p>
     * 
     * @param value The value to check whether it is in the interval.
     * @param min The min value of the interval.
     * @param max the max value of the interval
     * @return <tt>True</tt>, if value >= min && value <= max, <tt>false</tt> otherwise.
     */
    public static boolean isWithinInterval(double value, double min, double max) {
        return value <= max && value >= min;
    }

    public static boolean isWithinMargin(double value1, double value2, double margin) {
        double numMin = value1 - margin * value1;
        double numMax = value1 + margin * value1;

        return value1 < numMax && value1 > numMin;
    }

    public static boolean isWithinCorrectnessMargin(double questionedValue, double correctValue,
            double correctnessMargin) {
        double numMin = correctValue - correctnessMargin * correctValue;
        double numMax = correctValue + correctnessMargin * correctValue;

        return questionedValue < numMax && questionedValue > numMin;
    }

    public static int faculty(int number) {
        int faculty = number;
        while (number > 1) {
            number--;
            faculty *= number;
        }
        return faculty;
    }

    /**
     * <p>
     * Check whether two numeric intervals overlap.
     * </p>
     * 
     * @param start1 The start1.
     * @param end1 The end1.
     * @param start2 The start2.
     * @param end2 The end2.
     * @return True, if the intervals overlap, false otherwise.
     */
    public static boolean overlap(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }

    // public static double computeRootMeanSquareError(String inputFile, final String columnSeparator) {
    // // array with correct and predicted values
    // final List<double[]> values = new ArrayList<double[]>();
    //
    // LineAction la = new LineAction() {
    // @Override
    // public void performAction(String line, int lineNumber) {
    // String[] parts = line.split(columnSeparator);
    //
    // double[] pair = new double[2];
    // pair[0] = Double.valueOf(parts[0]);
    // pair[1] = Double.valueOf(parts[1]);
    //
    // values.add(pair);
    // }
    // };
    //
    // FileHelper.performActionOnEveryLine(inputFile, la);
    //
    // return computeRootMeanSquareError(values);
    // }

    // /**
    // * @deprecated Use the {@link Stats} instead.
    // */
    // @Deprecated
    // public static double computeRootMeanSquareError(List<double[]> values) {
    // double sum = 0.0;
    // for (double[] d : values) {
    // sum += Math.pow(d[0] - d[1], 2);
    // }
    //
    // return Math.sqrt(sum / values.size());
    // }

    /**
     * Calculate similarity of two lists of the same size.
     * 
     * @param list1 The first list.
     * @param list2 The second list.
     * @return The similarity of the two lists.
     */
    public static ListSimilarity computeListSimilarity(List<String> list1, List<String> list2) {

        // get maximum possible distance
        int summedMaxDistance = 0;
        int summedMaxSquaredDistance = 0;
        int distance = list1.size() - 1;
        for (int i = list1.size(); i > 0; i -= 2) {
            summedMaxDistance += 2 * distance;
            summedMaxSquaredDistance += 2 * Math.pow(distance, 2);
            distance -= 2;
        }

        // get real distance between lists
        int summedRealDistance = 0;
        int summedRealSquaredDistance = 0;
        int position1 = 0;
        Stats stats = new SlimStats();

        for (String entry1 : list1) {

            int position2 = 0;
            for (String entry2 : list2) {
                if (entry1.equals(entry2)) {
                    summedRealDistance += Math.abs(position1 - position2);
                    summedRealSquaredDistance += Math.pow(position1 - position2, 2);

                    double[] values = new double[2];
                    values[0] = position1;
                    values[1] = position2;
                    stats.add(Math.abs(position1 - position2));
                    break;
                }
                position2++;
            }

            position1++;
        }

        double similarity = 1 - (double)summedRealDistance / (double)summedMaxDistance;
        double squaredShiftSimilarity = 1 - (double)summedRealSquaredDistance / (double)summedMaxSquaredDistance;
        double rootMeanSquareError = stats.getRmse();

        return new ListSimilarity(similarity, squaredShiftSimilarity, rootMeanSquareError);
    }

    public static ListSimilarity computeListSimilarity(String listFile, final String separator) {

        // two list
        final List<String> list1 = new ArrayList<String>();
        final List<String> list2 = new ArrayList<String>();

        LineAction la = new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(separator);
                list1.add(parts[0]);
                list2.add(parts[1]);
            }
        };

        FileHelper.performActionOnEveryLine(listFile, la);

        return computeListSimilarity(list1, list2);
    }

    /**
     * <p>
     * Transform an IP address to a number.
     * </p>
     * 
     * @param ipAddress The IP address given in w.x.y.z notation.
     * @return The integer of the IP address.
     */
    public static Long ipToNumber(String ipAddress) {
        String[] addrArray = ipAddress.split("\\.");

        long num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            int power = 3 - i;
            num += Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power);
        }
        return num;
    }

    /**
     * <p>
     * Transform a number into an IP address.
     * </p>
     * 
     * @param number The integer to be transformed.
     * @return The IP address.
     */
    public static String numberToIp(long number) {
        return (number >> 24 & 0xFF) + "." + (number >> 16 & 0xFF) + "." + (number >> 8 & 0xFF) + "." + (number & 0xFF);
    }

    /**
     * <p>
     * Return a random entry from a given collection.
     * </p>
     * 
     * @param collection The collection from we want to sample from.
     * @return A random entry from the collection.
     */
    public static <T> T randomEntry(Collection<T> collection) {
        // Collection<T> randomSample = randomSample(collection, 1);
        Collection<T> randomSample = sample(collection, 1);
        return CollectionHelper.getFirst(randomSample);
    }

    // /**
    // * <p>
    // * Create a random sample from a given collection.
    // * </p>
    // *
    // * @param collection The collection from we want to sample from.
    // * @param sampleSize The size of the sample.
    // * @return A collection with samples from the collection.
    // */
    // public static <T> Collection<T> randomSample(Collection<T> collection, int sampleSize) {
    //
    // if (collection.size() < sampleSize) {
    // LOGGER.debug(
    // "tried to sample from a collection that was smaller than the sample size (Collection: {}, sample size: {}",
    // collection.size(), sampleSize);
    // return collection;
    // } else if (collection.size() == sampleSize) {
    // return collection;
    // }
    //
    // Set<Integer> randomNumbers = MathHelper.createRandomNumbers(sampleSize, 0, collection.size());
    //
    // Set<Integer> indicesUsed = new HashSet<Integer>();
    // Set<T> sampledCollection = new HashSet<T>();
    //
    // for (int randomIndex : randomNumbers) {
    //
    // int currentIndex = 0;
    // for (T o : collection) {
    //
    // if (currentIndex < randomIndex) {
    // currentIndex++;
    // continue;
    // }
    //
    // sampledCollection.add(o);
    // indicesUsed.add(randomIndex);
    // break;
    // }
    //
    // }
    //
    // return sampledCollection;
    // }

    /**
     * <p>
     * Create a random sampling of the given size using a <a
     * href="http://en.wikipedia.org/wiki/Reservoir_sampling">Reservoir Sampling</a> algorithm. The input data can be
     * supplied as iterable, thus does not have to fit in memory. Only the created random sample is kept in memory.
     * 
     * @param input The iterable providing the input data, not <code>null</code>.
     * @param k The size of the sampling.
     * @return A {@link Collection} with the random sample of size k (or smaller, in case the input data did not provide
     *         enough samples).
     */
    public static <T> Collection<T> sample(Iterable<T> input, int k) {
        return sample(input.iterator(), k);
    }

    /**
     * <p>
     * Create a random sampling of the given size using a <a
     * href="http://en.wikipedia.org/wiki/Reservoir_sampling">Reservoir Sampling</a> algorithm. The input data can be
     * supplied as iterator, thus does not have to fit in memory. Only the created random sample is kept in memory.
     * 
     * @param input The iterator providing the input data, not <code>null</code>.
     * @param k The size of the sampling.
     * @return A {@link Collection} with the random sample of size k (or smaller, in case the input data did not provide
     *         enough samples).
     */
    public static <T> Collection<T> sample(Iterator<T> input, int k) {
        Validate.notNull(input, "input must not be null");
        Validate.isTrue(k >= 0, "k must be greater/equal zero");
        List<T> sample = new ArrayList<T>(k);
        for (int i = 0; i < k; i++) {
            if (input.hasNext()) {
                sample.add(input.next());
            } else {
                break;
            }
        }

        int i = k + 1;
        while (input.hasNext()) {
            T item = input.next();
            int j = RANDOM.nextInt(i++);
            if (j < k) {
                sample.set(j, item);
            }
        }
        return sample;
    }

    /**
     * <p>
     * Create numbers random numbers between [min,max).
     * </p>
     * 
     * @param numbers Number of numbers to generate.
     * @param min The minimum number.
     * @param max The maximum number.
     * @return A set of random numbers between min and max.
     */
    public static Set<Integer> createRandomNumbers(int numbers, int min, int max) {
        Set<Integer> randomNumbers = new HashSet<Integer>();

        if (max - min < numbers) {
            LOGGER.warn("the range between min ({}) and max ({}) is not enough to create enough random numbers", min,
                    max);
            return randomNumbers;
        }
        while (randomNumbers.size() < numbers) {
            double nd = RANDOM.nextDouble();
            int randomNumber = (int)(nd * max + min);
            randomNumbers.add(randomNumber);
        }

        return randomNumbers;
    }

    /**
     * <p>
     * Returns a random number in the interval [low,high].
     * </p>
     * 
     * @param low The minimum number.
     * @param high The maximum number.
     * @return The random number within the interval.
     */
    public static int getRandomIntBetween(int low, int high) {
        int hl = high - low;
        return (int)Math.round(RANDOM.nextDouble() * hl + low);
    }

    /**
     * Calculate the parameters for a regression line. A series of x and y must be given. y = beta * x + alpha
     * TODO multiple regression model:
     * http://www.google.com/url?sa=t&source=web&cd=6&ved=0CC8QFjAF&url=http%3A%2F%2Fwww.
     * bbn-school.org%2Fus%2Fmath%2Fap_stats
     * %2Fproject_abstracts_folder%2Fproj_student_learning_folder%2Fmultiple_reg__ludlow
     * .pps&ei=NQQ7TOHNCYacOPan6IoK&usg=AFQjCNEybhIQVP2xwNGHEdYMgqNYelp1lQ&sig2=cwCNr11vMv0PHwdwu_LIAQ,
     * http://www.stat.ufl.edu/~aa/sta6127/ch11.pdf
     * 
     * See <a href="http://en.wikipedia.org/wiki/Simple_linear_regression">http://en.wikipedia.org/wiki/
     * Simple_linear_regression</a> for an explanation.
     * 
     * @param x A series of x values.
     * @param y A series of y values.
     * @return The parameter alpha [0] and beta [1] for the regression line.
     */
    public static double[] performLinearRegression(double[] x, double[] y) {
        double[] alphaBeta = new double[2];

        if (x.length != y.length) {
            LOGGER.warn("linear regression input is not correct, for each x, there must be a y");
        }
        double n = x.length;
        double sx = 0;
        double sy = 0;
        double sxx = 0;
        // double syy = 0;
        double sxy = 0;

        for (int i = 0; i < n; i++) {
            sx += x[i];
            sy += y[i];
            sxx += x[i] * x[i];
            // syy += y[i] * y[i];
            sxy += x[i] * y[i];
        }

        double beta = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double alpha = sy / n - beta * sx / n;

        alphaBeta[0] = alpha;
        alphaBeta[1] = beta;

        return alphaBeta;
    }

    /**
     * <p>
     * Calculates the Precision and Average Precision for a ranked list. Pr and AP for each rank are returned as a two
     * dimensional array, where the first dimension indicates the Rank k, the second dimension distinguishes between Pr
     * and AP. Example:
     * </p>
     * 
     * <pre>
     * double[][] ap = MathHelper.calculateAP(rankedList);
     * int k = rankedList.size() - 1;
     * double prAtK = ap[k][0];
     * double apAtK = ap[k][1];
     * </pre>
     * 
     * @param rankedList The ranked list with Boolean values indicating the relevancies of the items.
     * @param totalNumberRelevantForQuery The total number of relevant documents for the query.
     * @return A two dimensional array containing Precision @ Rank k and Average Precision @ Rank k.
     */
    public static double[][] computeAveragePrecision(List<Boolean> rankedList, int totalNumberRelevantForQuery) {

        // number of relevant entries at k
        int numRelevant = 0;

        // sum of all relevant precisions at k
        double relPrSum = 0;
        double[][] result = new double[rankedList.size()][2];

        for (int k = 0; k < rankedList.size(); k++) {

            boolean relevant = rankedList.get(k);

            if (relevant) {
                numRelevant++;
            }

            double prAtK = (double)numRelevant / (k + 1);

            if (relevant) {
                relPrSum += prAtK;
            }

            double ap = relPrSum / totalNumberRelevantForQuery;

            result[k][0] = prAtK;
            result[k][1] = ap;
        }

        return result;
    }

    public static double log2(double num) {
        return Math.log(num) / Math.log(2);
    }

    public static long crossTotal(long s) {
        if (s < 10) {
            return s;
        }
        return crossTotal(s / 10) + s % 10;
    }

    /**
     * <p>
     * Compute the Pearson's correlation coefficient between to variables.
     * </p>
     * 
     * @param x A list of double values from the data series of the first variable.
     * @param y A list of double values from the data series of the second variable.
     * @return The Pearson correlation coefficient.
     */
    public static double computePearsonCorrelationCoefficient(List<Double> x, List<Double> y) {

        double sumX = 0.;
        double sumY = 0.;

        for (Double v : x) {
            sumX += v;
        }
        for (Double v : y) {
            sumY += v;
        }

        double avgX = sumX / x.size();
        double avgY = sumY / y.size();

        double nominator = 0.;
        double denominatorX = 0.;
        double denominatorY = 0.;

        for (int i = 0; i < x.size(); i++) {
            nominator += (x.get(i) - avgX) * (y.get(i) - avgY);
            denominatorX += Math.pow(x.get(i) - avgX, 2);
            denominatorY += Math.pow(y.get(i) - avgY, 2);
        }

        double denominator = Math.sqrt(denominatorX * denominatorY);

        return nominator / denominator;
    }

    /**
     * <p>
     * Try to translate a number into a fraction, e.g. 0.333 = 1/3.
     * </p>
     * 
     * @parameter number A number.
     * @return The fraction of the number if it was possible to transform, otherwise the number as a string.
     */
    public static String numberToFraction(Double number) {
        String fraction = StringUtils.EMPTY;

        String sign = number >= 0 ? StringUtils.EMPTY : "-";
        number = Math.abs(number);

        int fullPart = (int)Math.floor(number);
        number = number - fullPart;

        double minMargin = 1;
        for (Entry<Double, String> fractionEntry : FRACTION_MAP.entrySet()) {

            double margin = Math.abs(fractionEntry.getKey() - number);

            if (margin < minMargin) {
                fraction = fractionEntry.getValue();
                minMargin = margin;
            }

        }

        if (number < 0.05 && number >= 0) {
            fraction = "0";
        } else if (number > 0.95 && number <= 1) {
            fraction = "1";
        }

        if (fraction.isEmpty() || number > 1 || number < 0) {
            fraction = String.valueOf(number);
        } else if (fullPart > 0) {
            if (!fraction.equalsIgnoreCase("0")) {
                fraction = fullPart + " " + fraction;
            } else {
                fraction = String.valueOf(fullPart);
            }
        }

        return sign + fraction;
    }

    /**
     * <p>
     * Calculate all combinations for a given array of items.
     * </p>
     * <p>
     * For example, the string "a b c" will return 7 combinations (2^3=8 but all empty is not allowed, hence 7):
     * 
     * <pre>
     * a b c
     * a b
     * a c
     * b c
     * c
     * b
     * a
     * </pre>
     * 
     * </p>
     * 
     * @param string A tokenized string to get the spans for.
     * @return A collection of spans.
     */
    public static <T> Collection<List<T>> computeAllCombinations(T[] items) {

        // create bitvector (all bit combinations other than all zeros)
        int bits = items.length;
        List<List<T>> combinations = new ArrayList<List<T>>();

        int max = (int)Math.pow(2, bits);
        for (long i = 1; i < max; i++) {
            List<T> combination = new LinkedList<T>();
            if (computeCombinationRecursive(i, items, combination, 0)) {
                combinations.add(combination);
            }
        }

        return combinations;
    }

    /**
     * <p>
     * Recursive computation function for combinations.
     * </p>
     * 
     * @param bitPattern The pattern describing the indices in the list of {@code items} to include in the resulting
     *            combination.
     * @param items The list of items to construct combinations from.
     * @param combination The result combination will be constructed into this list.
     * @param currentIndex The current index in the list of items. For this call the algorithm needs to decide whether
     *            to include the item at that position in the combination or not based on whether the value in
     *            {@code bitPattern} module 2 is 1 ({@code true}) or 0 ({@code false}).
     * @return {@code true} if the computed combination was computed successfully.
     */
    private static <T> boolean computeCombinationRecursive(long bitPattern, T[] items, List<T> combination,
            int currentIndex) {
        if (bitPattern % 2 != 0) {
            combination.add(items[currentIndex]);
        }
        long nextBitPattern = bitPattern / 2;
        if (nextBitPattern < 1) {
            return true;
        } else {
            return computeCombinationRecursive(nextBitPattern, items, combination, ++currentIndex);
        }
    }

    /**
     * <p>
     * Parse a numeric expression in a string to a double.
     * </p>
     * 
     * <pre>
     * "0.5" => 0.5
     * "1/2" => 0.5
     * "½" => 0.5
     * "3 1/8" => 3.125
     * "1½" => 1.5
     * "1 ½" => 1.5
     * </pre>
     * 
     * @param stringNumber The string containing the numeric expression.
     * @return The parsed double.
     */
    public static double parseStringNumber(String stringNumber) {
        Validate.notNull(stringNumber);

        stringNumber = stringNumber.toLowerCase();

        double value = 0.;

        // find fraction characters
        Set<String> remove = new HashSet<>();
        if (stringNumber.contains("¼")) {
            value += 1 / 4.;
            remove.add("¼");
        }
        if (stringNumber.contains("½")) {
            value += 1 / 2.;
            remove.add("½");
        }
        if (stringNumber.contains("¾")) {
            value += 3 / 4.;
            remove.add("¾");
        }
        if (stringNumber.contains("⅓")) {
            value += 1 / 3.;
            remove.add("⅓");
        }
        if (stringNumber.contains("⅔")) {
            value += 2 / 3.;
            remove.add("⅔");
        }
        if (stringNumber.contains("⅕")) {
            value += 1 / 5.;
            remove.add("⅕");
        }
        if (stringNumber.contains("⅖")) {
            value += 2 / 5.;
            remove.add("⅖");
        }
        if (stringNumber.contains("⅗")) {
            value += 3 / 5.;
            remove.add("⅗");
        }
        if (stringNumber.contains("⅘")) {
            value += 4 / 5.;
            remove.add("⅘");
        }
        if (stringNumber.contains("⅙")) {
            value += 1 / 6.;
            remove.add("⅙");
        }
        if (stringNumber.contains("⅚")) {
            value += 5 / 6.;
            remove.add("⅚");
        }
        if (stringNumber.contains("⅛")) {
            value += 1 / 8.;
            remove.add("⅛");
        }
        if (stringNumber.contains("⅜")) {
            value += 3 / 8.;
            remove.add("⅜");
        }
        if (stringNumber.contains("⅝")) {
            value += 5 / 8.;
            remove.add("⅝");
        }
        if (stringNumber.contains("⅞")) {
            value += 7 / 8.;
            remove.add("⅞");
        }

        for (String string : remove) {
            stringNumber = stringNumber.replace(string, StringUtils.EMPTY);
        }

        // resolve fractions like "1/2"
        Matcher matcher = FRACTION_PATTERN.matcher(stringNumber);
        if (matcher.find()) {
            int nominator = Integer.parseInt(matcher.group(1));
            int denominator = Integer.parseInt(matcher.group(2));
            value += nominator / (double)denominator;
            stringNumber = stringNumber.replace(matcher.group(), StringUtils.EMPTY);
        }

        // number.numberEX e.g. 4.4353E3 = 4435.3
        Matcher exPattern = EX_PATTERN.matcher(stringNumber);
        if (exPattern.find()) {
            try {
                value += Double.valueOf(exPattern.group(0));
                return value;
            } catch (Exception e) {
                // ccl
            }
        }

        // parse the rest
        stringNumber = CLEAN_PATTERN1.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        stringNumber = CLEAN_PATTERN2.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        stringNumber = CLEAN_PATTERN3.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        stringNumber = stringNumber.trim();
        if (!stringNumber.isEmpty()) {
            try {
                value += Double.parseDouble(stringNumber);
            } catch (Exception e) {
                // ccl
            }
        }

        return value;
    }

    /**
     * Map two natural numbers (non-negative!) to a third natural number. N x N => N.
     * f(a,b) = c where there are now two settings for a and b that produce the same c.
     * @see https://en.wikipedia.org/wiki/Pairing_function
     * @param a The first number.
     * @param b The second number.
     * @return The target number.
     */
    public static int cantorize(int a, int b) {
        return ((a + b) * (a + b + 1) / 2) + b;
    }

    /**
     * <p>
     * Calculate the <a href="http://en.wikipedia.org/wiki/Order_of_magnitude">order of magnitude</a> for a given
     * number. E.g. <code>orderOfMagnitude(100) = 2</code>.
     * </p>
     * 
     * @param number The number.
     * @return The order of magnitude for the given number.
     */
    public static int getOrderOfMagnitude(double number) {
        if (number == 0) {
            // this version works fine for me, but don't know, if this is mathematically correct, see:
            // http://www.mathworks.com/matlabcentral/fileexchange/28559-order-of-magnitude-of-number
            return 0;
        }
        return (int)Math.floor(Math.log10(number));
    }

    /**
     * <p>
     * Add two int values and check for integer overflows.
     * 
     * @param a The first value.
     * @param b The second value (negative value to subtract).
     * @return The sum of the given values.
     * @throws ArithmeticException in case of a numeric overflow.
     */
    public static int add(int a, int b) throws ArithmeticException {
        int sum = a + b;
        if ((a & b & ~sum | ~a & ~b & sum) < 0) {
            throw new ArithmeticException("Overflow for " + a + "+" + b);
        }
        return sum;
    }

}
