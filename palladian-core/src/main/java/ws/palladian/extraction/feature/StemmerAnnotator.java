package ws.palladian.extraction.feature;

import org.apache.commons.lang3.Validate;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.germanStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.NominalFeature;

/**
 * <p>
 * A {@link PipelineProcessor} for stemming a pre-tokenized text. This means, the documents to be processed by this
 * class must be processed by a {@link BaseTokenizer} in advance, supplying {@link BaseTokenizer#PROVIDED_FEATURE}
 * annotations. The stemmer is based on the <a href="http://snowball.tartarus.org/">Snowball</a> algorithm.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class StemmerAnnotator extends AbstractTokenProcessor {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * The mode in which this {@link StemmerAnnotator} operates.
     * </p>
     */
    // TODO re-think whether we really need this feature, as it bloats the API.
    public static enum Mode {
        /**
         * <p>
         * Provide an annotation of the stemmed value to each token which can be retrieved by {@link #STEM}.
         * </p>
         */
        ANNOTATE,
        /**
         * <p>
         * Change each token's value to the stemmed value and provide an annotation with the original value, which can
         * be retrieved by {@link #UNSTEM}.
         * </p>
         */
        MODIFY
    };

    /**
     * <p>
     * The descriptor of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final FeatureDescriptor<NominalFeature> STEM = FeatureDescriptorBuilder.build(
            "ws.palladian.features.stem", NominalFeature.class);
    public static final FeatureDescriptor<NominalFeature> UNSTEM = FeatureDescriptorBuilder.build(
            "ws.palladian.features.unstem", NominalFeature.class);

    private final Mode mode;
    private final SnowballStemmer stemmer;

    /**
     * <p>
     * Initialize a new StemmerAnnotator using a Porter Stemmer.
     * </p>
     */
    public StemmerAnnotator() {
        this(new porterStemmer(), Mode.ANNOTATE);
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator using the specified {@link SnowballStemmer} instance.
     * </p>
     * 
     * @param stemmer
     * @param mode
     */
    public StemmerAnnotator(SnowballStemmer stemmer, Mode mode) {
        Validate.notNull(stemmer, "stemmer must not be null");
        Validate.notNull(mode, "mode must not be null");
        this.stemmer = stemmer;
        this.mode = mode;
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator with a Snowball Stemmer for the specified {@link Language} and in
     * {@link Mode#ANNOTATE}.
     * </p>
     * 
     * @param language
     */
    public StemmerAnnotator(Language language) {
        this(language, Mode.ANNOTATE);
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator with a Snowball Stemmer for the specified {@link Language}.
     * </p>
     * 
     * @param language
     * @param mode
     */
    public StemmerAnnotator(Language language, Mode mode) {
        // TODO support all available languages by SnowballStemmer
        Validate.notNull(language, "language must not be null");
        Validate.notNull(mode, "mode must not be null");
        switch (language) {
            case ENGLISH:
                this.stemmer = new englishStemmer();
                break;
            case GERMAN:
                this.stemmer = new germanStemmer();
                break;
            default:
                this.stemmer = new porterStemmer();
                break;
        }
        this.mode = mode;
    }

    @Override
    protected void processToken(Annotation annotation) throws DocumentUnprocessableException {
        String unstem = annotation.getValue();
        String stem = stem(unstem);
        switch (mode) {
            case ANNOTATE:
                annotation.getFeatureVector().add(new NominalFeature(STEM, stem));
                break;
            case MODIFY:
                annotation.getFeatureVector().add(new NominalFeature(UNSTEM, unstem));
                annotation.setValue(stem);
                break;
        }
    }

    /**
     * <p>
     * Stem the supplied word.
     * </p>
     * 
     * @param word The word to stem.
     * @return The stemmed word.
     */
    public String stem(String word) {
        stemmer.setCurrent(word);
        stemmer.stem();
        return stemmer.getCurrent();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StemmerAnnotator [mode=");
        builder.append(mode);
        builder.append(", stemmer=");
        builder.append(stemmer.getClass().getSimpleName());
        builder.append("]");
        return builder.toString();
    }

}
