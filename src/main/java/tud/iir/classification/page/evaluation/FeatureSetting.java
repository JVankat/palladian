package tud.iir.classification.page.evaluation;

import java.util.HashSet;
import java.util.Set;

public class FeatureSetting {

    public static final int CHAR_NGRAMS = 1;

    public static final int WORD_NGRAMS = 2;

    private int textFeatureType = CHAR_NGRAMS;

    /** the maximum number of terms that should be used per document */
    private int maxTerms = 800;

    /** minimum n-gram length */
    private int minNGramLength = 4;

    /** maximum n-gram length */
    private int maxNGramLength = 7;

    /**
     * the maximum length of a single term, this only applies if {@link textFeatureType} is set to {@link WORD_NGRAMS}
     * and {@link maxNGramLength} is 1, that is, only unigrams will be used
     */
    private int maximumTermLength = 20;

    /**
     * the minimum length of a single term, this only applies if {@link textFeatureType} is set to {@link WORD_NGRAMS}
     * and {@link maxNGramLength} is 1, that is, only unigrams will be used
     */
    private int minimumTermLength = 3;

    public static Set<String> englishStopWords = new HashSet<String>();

    private Set<String> stopWords = new HashSet<String>();

    public FeatureSetting() {

        String[] englishStopWordsArray = { "I", "a", "about", "an", "and", "are", "as", "at", "be", "by", "com", "de",
                "en", "for", "from", "how", "in", "is", "he", "she", "it", "la", "of", "on", "or", "that", "the",
                "this", "to", "was", "what", "when", "where", "who", "will", "with", "und", "the", "www" };

        for (String stopWord : englishStopWordsArray) {
            englishStopWords.add(stopWord);
        }

    }

    public int getTextFeatureType() {
        return textFeatureType;
    }

    public void setTextFeatureType(int textFeatureType) {
        this.textFeatureType = textFeatureType;
    }

    public void setMaxTerms(int maxTerms) {
        this.maxTerms = maxTerms;
    }

    public int getMaxTerms() {
        return maxTerms;
    }

    public int getMinNGramLength() {
        return minNGramLength;
    }

    public void setMinNGramLength(int minNGramLength) {
        this.minNGramLength = minNGramLength;
    }

    public int getMaxNGramLength() {
        return maxNGramLength;
    }

    public void setMaxNGramLength(int maxNGramLength) {
        this.maxNGramLength = maxNGramLength;
    }

    /**
     * Set the maximum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    public void setMaximumTermLength(int maximumTermLength) {
        this.maximumTermLength = maximumTermLength;
    }

    public int getMaximumTermLength() {
        return maximumTermLength;
    }

    /**
     * Set the minimum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    public void setMinimumTermLength(int minimumTermLength) {
        this.minimumTermLength = minimumTermLength;
    }

    public int getMinimumTermLength() {
        return minimumTermLength;
    }

    public Set<String> getStopWords() {
        return stopWords;
    }

    public void setStopWords(Set<String> stopWords) {
        this.stopWords = stopWords;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureSetting [maxNGramLength=");
        builder.append(maxNGramLength);
        builder.append(", maxTerms=");
        builder.append(maxTerms);
        builder.append(", maximumTermLength=");
        builder.append(maximumTermLength);
        builder.append(", minNGramLength=");
        builder.append(minNGramLength);
        builder.append(", minimumTermLength=");
        builder.append(minimumTermLength);
        builder.append(", stopWords=");
        builder.append(stopWords);
        builder.append(", textFeatureType=");
        builder.append(textFeatureType);
        builder.append("]");
        return builder.toString();
    }

}