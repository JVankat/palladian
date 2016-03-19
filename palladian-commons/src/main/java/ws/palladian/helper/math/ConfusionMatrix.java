package ws.palladian.helper.math;

import static java.lang.Math.sqrt;

import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMatrix;

/**
 * <p>
 * A confusion matrix which can be used to evaluate classification results.
 * </p>
 *
 * @author Philipp Katz
 * @author David Urbansky
 * @see <a href="http://en.wikipedia.org/wiki/Confusion_matrix">Wikipedia: Confusion matrix</a>
 */
public class ConfusionMatrix {

    private final CountMatrix<String> confusionMatrix;

    /**
     * <p>
     * Create a new, empty confusion matrix.
     * </p>
     */
    public ConfusionMatrix() {
        confusionMatrix = CountMatrix.create();
    }

    /**
     * <p>
     * Add a classification result to this confusion matrix.
     * </p>
     *
     * @param realCategory      The real category of the item.
     * @param predictedCategory The category which was predicted by the classification.
     */
    public void add(String realCategory, String predictedCategory) {
        add(realCategory, predictedCategory, 1);
    }

    /**
     * <p>
     * Add a classification result to this confusion matrix.
     * </p>
     *
     * @param realCategory      The real category of the item.
     * @param predictedCategory The category which was predicted by the classification.
     * @param count             The number of the classification.
     */
    public void add(String realCategory, String predictedCategory, int count) {
        confusionMatrix.add(predictedCategory, realCategory, count);
    }

    /**
     * <p>
     * Get the accuracy which is defined as <code>accuracy = |correctlyClassified| / |totalDocuments|</code>.
     * </p>
     *
     * @return The accuracy.
     */
    public double getAccuracy() {
        return (double) getTotalCorrect() / getTotalDocuments();
    }

    /**
     * <p>
     * Get the number of correctly classified documents.
     * </p>
     *
     * @return Number of correctly classified documents.
     */
    public int getTotalCorrect() {
        int correct = 0;
        for (String value : getCategories()) {
            correct += confusionMatrix.getCount(value, value);
        }
        return correct;
    }

    /**
     * <p>
     * Get the number of correctly classified documents in a given category.
     * </p>
     *
     * @param category The category.
     * @return Number of correct classified documents in a given category.
     */
    public int getCorrectlyClassifiedDocuments(String category) {
        return confusionMatrix.getCount(category, category);
    }

    /**
     * <p>
     * Get the number of documents classified in a given category.
     * </p>
     *
     * @param category The category.
     * @return Number of documents classified in the given category.
     */
    public int getClassifiedDocuments(String category) {
        return confusionMatrix.getColumn(category).getSum();
    }

    /**
     * <p>
     * Get the number of documents which are actually in the given category.
     * </p>
     *
     * @param category The category.
     * @return Number of documents in the given category.
     */
    public int getRealDocuments(String category) {
        return confusionMatrix.getRow(category).getSum();
    }

    /**
     * <p>
     * Get the number of confusions between two categories.
     * </p>
     *
     * @param realCategory      The real category.
     * @param predictedCategory The category which was predicted by the classifier.
     * @return The number of confusions between the real and the predicted category.
     */
    public int getConfusions(String realCategory, String predictedCategory) {
        return confusionMatrix.getCount(predictedCategory, realCategory);
    }

    /**
     * <p>
     * Get the confusions ordered by severity.
     * </p>
     *
     * @return An ordered list of class confusions.
     */
    public List<Triple<String, String, Double>> getOrderedConfusions() {
        List<Triple<String, String, Double>> list = new ArrayList<>();

        for (String c1 : getCategories()) {
            int possible = getRealDocuments(c1);
            for (String c2 : getCategories()) {
                if (c2.equals(c1)) {
                    continue;
                }
                int confusions = getConfusions(c1, c2);
                double confusion = (double) confusions / possible;
                list.add(Triple.of(c1, c2, confusion));
            }
        }

        Collections.sort(list, (o1, o2) -> Double.compare(o2.getRight(), o1.getRight()));

        return list;
    }

    /**
     * <p>
     * Get the most confusing categories, i.e. categories that were classified frequently even though it should have become a different category.
     * </p>
     *
     * @return An ordered list categories.
     */
    public List<String> getMostConfusingCategories() {
        List<Pair<String, Double>> list = new ArrayList<>();

        for (String c1 : getCategories()) {
            int real = getRealDocuments(c1);
            int classified = getClassifiedDocuments(c1);
            double confusion = (double) classified / real;
            list.add(Pair.of(c1, confusion));
        }

        Collections.sort(list, (o1, o2) -> Double.compare(o2.getRight(), o1.getRight()));

        return list.stream().map(Pair::getLeft).collect(Collectors.toList());
    }

    /**
     * <p>
     * Get all categories in the data set.
     * </p>
     *
     * @return The categories in the data set.
     */
    public Set<String> getCategories() {
        return confusionMatrix.getRowKeys();
    }

    /**
     * <p>
     * Get the total number of documents in this confusion matrix.
     * </p>
     *
     * @return The number of documents in the matrix.
     */
    public int getTotalDocuments() {
        int total = 0;
        for (String value : confusionMatrix.getRowKeys()) {
            total += confusionMatrix.getRow(value).getSum();
        }
        return total;
    }

    /**
     * <p>
     * Get the prior of the most likely category. In a data set with evenly distributed classes the highest prior should
     * be <code>1/|categories|</code>.
     * </p>
     *
     * @return The highest prior.
     */
    public double getHighestPrior() {
        int max = 0;
        for (String value : confusionMatrix.getColumnKeys()) {
            max = Math.max(max, confusionMatrix.getRow(value).getSum());
        }
        int sum = getTotalDocuments();
        if (sum == 0) {
            return 0;
        }
        return (double) max / sum;
    }

    /**
     * <p>
     * Get the superiority for the classification result. Superiority is the factor with which the classifier is better
     * than the highest prior in the data set: <code>superiority
     * = percentCorrectlyClassified / percentHighestPrior</code>. A superiority of 1 means it doesn't make sense
     * classifying at all since we could simply always take the category with the highest prior. A superiority smaller 1
     * means the classifier is harmful.
     * </p>
     *
     * @return The superiority.
     */
    public double getSuperiority() {
        return getAccuracy() / getHighestPrior();
    }

    /**
     * <p>
     * Get the precision for a given category. <code>precision = |TP| / (|TP| + |FP|)</code>.
     * </p>
     *
     * @param category The category.
     * @return The precision for a given category.
     */
    public double getPrecision(String category) {
        int correct = getCorrectlyClassifiedDocuments(category);
        int classified = getClassifiedDocuments(category);
        if (classified == 0) {
            return Double.NaN;
        }
        return (double) correct / classified;
    }

    /**
     * <p>
     * Get the recall for a given category. <code>recall = |TP| / (|TP| + |FN|)</code>.
     * </p>
     *
     * @param category The category.
     * @return The recall for a given category.
     */
    public double getRecall(String category) {
        int correct = getCorrectlyClassifiedDocuments(category);
        int real = getRealDocuments(category);
        if (real == 0) {
            return 1;
        }
        return (double) correct / real;
    }

    /**
     * <p>
     * Get the F measure for a given category.
     * </p>
     *
     * @param category The category.
     * @param alpha    A value between 0 and 1 to weight precision and recall (1.0 for F1). Use values of 2.0 for
     *                 F2 score and 0.5 for F0.5 score and so on.
     * @return The F measure for a given category.
     */
    public double getF(double alpha, String category) {
        double precision = getPrecision(category);
        double recall = getRecall(category);
        if (Double.isNaN(precision)) {
            return Double.NaN;
        }
        double alphaSquare = alpha * alpha;
        return (1. + alphaSquare) * (precision * recall / (alphaSquare * precision + recall));
    }

    /**
     * <p>
     * Calculate the sensitivity for a given category. <code>sensitivity = |TP| / (|TP| + |FN|)</code>. Sensitivity
     * specifies what percentage of actual category members were found. 100 % sensitivity means that all actual
     * documents belonging to the category were classified correctly.
     * </p>
     *
     * @param category The category.
     * @return The sensitivity for the given category.
     */
    public double getSensitivity(String category) {
        int truePositives = getCorrectlyClassifiedDocuments(category);
        int realPositives = getRealDocuments(category);
        int falseNegatives = realPositives - truePositives;
        if (truePositives + falseNegatives == 0) {
            return Double.NaN;
        }
        return (double) truePositives / (truePositives + falseNegatives);
    }

    /**
     * <p>
     * Calculate the specificity for a given category. <code>specificity = |TN| / (|TN| + |FP|)</code>. Specificity
     * specifies what percentage of not-category members were recognized as such. 100 % specificity means that there
     * were no documents classified as category member when they were actually not.
     * </p>
     *
     * @param category The category.
     * @return The specificity for the given category.
     */
    public double getSpecificity(String category) {
        int truePositives = getCorrectlyClassifiedDocuments(category);
        int realPositives = getRealDocuments(category);
        int classifiedPositives = getClassifiedDocuments(category);

        int falsePositives = classifiedPositives - truePositives;
        int falseNegatives = realPositives - truePositives;
        int trueNegatives = getTotalDocuments() - classifiedPositives - falseNegatives;

        if (trueNegatives + falsePositives == 0) {
            return Double.NaN;
        }

        return (double) trueNegatives / (trueNegatives + falsePositives);
    }

    /**
     * <p>
     * Calculate the accuracy for a given category. <code>accuracy = (|TP| + |TN|) / (|TP| + |TN| + |FP| + |FN|)</code>.
     * </p>
     *
     * @param category The category.
     * @return The accuracy for the given category.
     */
    public double getAccuracy(String category) {
        int truePositives = getCorrectlyClassifiedDocuments(category);
        int realPositives = getRealDocuments(category);
        int classifiedPositives = getClassifiedDocuments(category);

        int falsePositives = classifiedPositives - truePositives;
        int falseNegatives = realPositives - truePositives;
        int trueNegatives = getTotalDocuments() - classifiedPositives - falseNegatives;

        if (truePositives + trueNegatives + falsePositives + falseNegatives == 0) {
            return Double.NaN;
        }

        return (double) (truePositives + trueNegatives)
                / (truePositives + trueNegatives + falsePositives + falseNegatives);
    }

    /**
     * <p>
     * Calculate the prior for the given category. The prior is determined by calculating the frequency of the category
     * in the data set and dividing it by the total number of documents.
     * </p>
     *
     * @param category The category for which the prior should be determined.
     * @return The prior for the given category.
     */
    public double getPrior(String category) {
        int documentCount = getRealDocuments(category);
        int totalAssigned = getTotalDocuments();
        if (totalAssigned == 0) {
            return 0;
        }
        return (double) documentCount / totalAssigned;
    }

    /**
     * <p>
     * Get the average precision of all categories.
     * </p>
     *
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *                 each category equally.
     * @return The average precision of all categories.
     */
    public double getAveragePrecision(boolean weighted) {
        double precision = 0.0;
        for (String category : getCategories()) {
            double precisionForCategory = getPrecision(category);
            if (Double.isNaN(precisionForCategory)) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            precision += precisionForCategory * weight;
        }
        if (weighted) {
            return precision;
        }
        int count = getCategories().size();
        if (count == 0) {
            return Double.NaN;
        }
        return precision / count;
    }

    /**
     * <p>
     * Get the average recall of all categories.
     * </p>
     *
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *                 each category equally.
     * @return The average recall of all categories.
     */
    public double getAverageRecall(boolean weighted) {
        double recall = 0.0;
        for (String category : getCategories()) {
            double recallForCategory = getRecall(category);
            if (Double.isNaN(recallForCategory)) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            recall += recallForCategory * weight;
        }
        if (weighted) {
            return recall;
        }
        int count = getCategories().size();
        if (count == 0) {
            return Double.NaN;
        }
        return recall / count;
    }

    /**
     * <p>
     * Get the average F measure of all categories.
     * </p>
     *
     * @param alpha    A value between 0 and 1 to weight precision and recall (1.0 for F1). Use values of 2.0 for
     *                 F2 score and 0.5 for F0.5 score and so on.
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *                 each category equally.
     * @return The average F of all categories.
     */
    public double getAverageF(double alpha, boolean weighted) {
        double f = 0.0;
        for (String category : getCategories()) {
            double fForCategory = getF(alpha, category);
            if (Double.isNaN(fForCategory)) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            f += fForCategory * weight;
        }
        if (weighted) {
            return f;
        }
        int count = getCategories().size();
        if (count == 0) {
            return Double.NaN;
        }
        return f / count;
    }

    /**
     * <p>
     * Calculate the average sensitivity.
     * </p>
     *
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *                 each category equally.
     * @return The average sensitivity for all categories.
     */
    public double getAverageSensitivity(boolean weighted) {
        double sensitivity = 0.0;
        for (String category : getCategories()) {
            double sensitivityForCategory = getSensitivity(category);
            if (Double.isNaN(sensitivityForCategory)) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            sensitivity += sensitivityForCategory * weight;
        }
        if (weighted) {
            return sensitivity;
        }
        int count = getCategories().size();
        if (count == 0) {
            return Double.NaN;
        }
        return sensitivity / count;
    }

    /**
     * <p>
     * Calculate the average specificity.
     * </p>
     *
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *                 each category equally.
     * @return The average accuracy for all categories.
     */
    public double getAverageSpecificity(boolean weighted) {
        double specificity = 0.0;
        for (String category : getCategories()) {
            double specifityForCategory = getSpecificity(category);
            if (Double.isNaN(specifityForCategory)) {
                return Double.NaN;
            }
            double weight = weighted ? getPrior(category) : 1;
            specificity += specifityForCategory * weight;
        }
        if (weighted) {
            return specificity;
        }
        int count = getCategories().size();
        if (count == 0) {
            return Double.NaN;
        }
        return specificity / count;
    }

    /**
     * <p>
     * Calculate the average accuracy.
     * </p>
     *
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *                 each category equally.
     * @return The average accuracy for all categories.
     * @deprecated Why should one want to average this?
     */
    @Deprecated
    public double getAverageAccuracy(boolean weighted) {
        double accuracy = 0.0;
        for (String category : getCategories()) {
            double accuracyForCategory = getAccuracy(category);
            if (Double.isNaN(accuracyForCategory)) {
                return Double.NaN;
            }
            double weight = weighted ? getPrior(category) : 1;
            accuracy += accuracyForCategory * weight;
        }
        if (weighted) {
            return accuracy;
        }
        int count = getCategories().size();
        if (count == 0) {
            return Double.NaN;
        }
        return accuracy / count;
    }

    /**
     * <p>
     * Calculate the
     * <a href="https://en.wikipedia.org/wiki/Matthews_correlation_coefficient">
     * Matthews correlation coefficient</a>, in case this is a binary
     * classification problem (ie. {@link #getCategories()} has a size of two).
     * A coefficient of +1 represents a perfect prediction, 0 represents a
     * random prediction by prior, -1 represents worst prediction.
     * </p>
     *
     * @return The Matthews correlation coefficient in range [-1,+1].
     */
    public double getMatthewsCorrelationCoefficient() {
        if (getCategories().size() != 2) {
            throw new IllegalStateException("Matthews correlation coefficient only works for binary classifications");
        }
        Iterator<String> iterator = getCategories().iterator();
        // it doesn't matter, which class we consider positive or negative;
        // result is the same
        String positive = iterator.next();
        String negative = iterator.next();
        int tp = getConfusions(positive, positive);
        int tn = getConfusions(negative, negative);
        int fp = getConfusions(negative, positive);
        int fn = getConfusions(positive, negative);
        double denominator = sqrt(tp + fp) * sqrt(tp + fn) * sqrt(tn + fp) * sqrt(tn + fn);
        return denominator != 0 ? (tp * tn - fp * fn) / denominator : 0;
    }

    /**
     * FIXME this formatting is weird
     */
    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean tsv) {
        StringBuilder out = new StringBuilder("Confusion Matrix:\n\n");
        List<String> possibleClasses = new ArrayList<>(getCategories());
        StringBuilder headerBuilder = new StringBuilder();
        int maxClassNameLength = 0;
        for (String clazz : possibleClasses) {
            if (!tsv) {
                headerBuilder.append(clazz).append(" ");
            } else {
                headerBuilder.append(clazz).append("\t");
            }
            maxClassNameLength = clazz.length() > maxClassNameLength ? clazz.length() : maxClassNameLength;
        }
        headerBuilder.append("total");
        String classNameLengthSpace = CharBuffer.allocate(maxClassNameLength).toString().replace('\0', ' ');
        out.append(classNameLengthSpace).append("\t").append("classified as:\n");
        out.append(classNameLengthSpace).append("\t").append(headerBuilder);
        out.append("\n");

        for (String clazz : possibleClasses) {
            out.append(clazz);
            out.append(CharBuffer.allocate(maxClassNameLength - clazz.length()).toString().replace('\0', ' '));
            out.append("\t");
            for (String predictedClazz : possibleClasses) {
                Integer value = confusionMatrix.get(predictedClazz, clazz);
                value = value == null ? 0 : value;
                if (!tsv) {
                    int valueSize = value.toString().length();
                    int remainingLength = predictedClazz.length() - valueSize + 1;
                    int spacesInFrontOfValue = Math.max((int) Math.ceil((double) remainingLength / 2), 0);
                    out.append(CharBuffer.allocate(spacesInFrontOfValue).toString().replace('\0', ' '));
                    out.append(value);
                    int spacesAfterValue = Math.max(predictedClazz.length() - valueSize - spacesInFrontOfValue, 1);
                    out.append(CharBuffer.allocate(spacesAfterValue).toString().replace('\0', ' '));
                } else {
                    out.append(value);
                    out.append("\t");
                }
            }
            out.append(getRealDocuments(clazz));
            out.append("\n");
        }

        out.append("\n");
        out.append("\n");

        out.append("Strongest Confusions (Top 100):\n");
        List<Triple<String, String, Double>> strongConfusions = CollectionHelper.getSublist(getOrderedConfusions(),0, 100);
        for (Triple strongConfusion : strongConfusions) {
            out.append(strongConfusion.getLeft()).append(" => ").append(strongConfusion.getMiddle()).append("\t").append(strongConfusion.getRight()).append("\n");
        }
        out.append("\n");

        out.append("Most Confusing Categories (Top 100) :\n");
        List<String> mostConfusingCategories = CollectionHelper.getSublist(getMostConfusingCategories(), 0, 100);
        for (String mostConfusingCategory : mostConfusingCategories) {
            out.append(mostConfusingCategory).append("\n");
        }
        out.append("\n");

        if (!tsv) {
            out.append(classNameLengthSpace).append("  ").append("prior  precision recall f1-measure accuracy\n");
        } else {
            out.append("\tprior\tprecision\trecall\tf1-measure\taccuracy\n");
        }

        for (String clazz : possibleClasses) {

            if (!tsv) {
                out.append(clazz).append(": ");
                int missingSpaces = maxClassNameLength - clazz.length();
                if (missingSpaces > 0) {
                    out.append(CharBuffer.allocate(missingSpaces).toString().replace('\0', ' '));
                }
            } else {
                out.append(clazz).append(":\t");
            }

            double prior = MathHelper.round(getPrior(clazz), 4);
            double precision = MathHelper.round(getPrecision(clazz), 4);
            double recall = MathHelper.round(getRecall(clazz), 4);
            double accuracy = MathHelper.round(getAccuracy(clazz), 4);
            double f1measure = MathHelper.round(getF(1.0, clazz), 4);
            out.append(prior);
            if (!tsv) {
                int precisionSpaces = "prior  ".length() - String.valueOf(prior).length();
                out.append(CharBuffer.allocate(Math.max(precisionSpaces, 0)).toString().replace('\0', ' ')).append(
                        precision);
            } else {
                out.append("\t").append(precision);
            }
            if (!tsv) {
                int recallSpaces = "precision ".length() - String.valueOf(precision).length();
                out.append(CharBuffer.allocate(Math.max(recallSpaces, 0)).toString().replace('\0', ' ')).append(recall);
            } else {
                out.append("\t").append(recall);
            }
            if (!tsv) {
                int f1MeasureSpaces = "recall ".length() - String.valueOf(recall).length();
                out.append(CharBuffer.allocate(Math.max(f1MeasureSpaces, 0)).toString().replace('\0', ' ')).append(
                        f1measure);
            } else {
                out.append("\t").append(f1measure);
            }
            if (!tsv) {
                int accuracySpaces = "f1-measure ".length() - String.valueOf(f1measure).length();
                out.append(CharBuffer.allocate(Math.max(accuracySpaces, 0)).toString().replace('\0', ' ')).append(accuracy);
            } else {
                out.append("\t").append(accuracy);
            }
            out.append("\n");
        }

        out.append("\n");
        out.append("Accuracy:\t").append(MathHelper.round(getAccuracy(), 4)).append('\n');
        out.append("Highest Prior:\t").append(MathHelper.round(getHighestPrior(), 4)).append('\n');
        out.append("Superiority:\t").append(MathHelper.round(getSuperiority(), 4)).append('\n');
        if (getCategories().size() == 2) {
            out.append("Matthews Correlation Coefficient:\t").append(MathHelper.round(getMatthewsCorrelationCoefficient(), 4)).append('\n');
        }
        out.append("# Documents:\t").append(getTotalDocuments()).append('\n');
        out.append("# Correctly Classified:\t").append(getTotalCorrect()).append('\n');
        out.append("\n");
        out.append("Average Precision:\t").append(MathHelper.round(getAveragePrecision(true), 4)).append('\n');
        out.append("Average Recall:\t").append(MathHelper.round(getAverageRecall(true), 4)).append('\n');
        out.append("Average F1:\t").append(MathHelper.round(getAverageF(0.5, true), 4)).append('\n');
        out.append("Average Sensitivity:\t").append(MathHelper.round(getAverageSensitivity(true), 4)).append('\n');
        out.append("Average Specificity:\t").append(MathHelper.round(getAverageSpecificity(true), 4)).append('\n');
        // out.append("Average Accuracy:\t").append(MathHelper.round(getAverageAccuracy(true), 4)).append('\n');

        return out.toString();

    }


}
