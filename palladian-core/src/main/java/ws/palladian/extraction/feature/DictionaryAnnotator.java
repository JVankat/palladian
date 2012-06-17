/**
 * Created on: 16.06.2012 18:44:37
 */
package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.PositionAnnotation;

/**
 * <p>
 * Takes a tokenized (see {@link BaseTokenizer}) as input and annotates all token matching one item from a dictionary.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DictionaryAnnotator extends AbstractFeatureProvider<String, AnnotationFeature> {

    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = 2190396926017567035L;

    /**
     * <p>
     * The dictionary to match token agains.
     * </p>
     */
    private final Set<String> dictionary;

    /**
     * <p>
     * Creates a new {@code DictionaryAnnotator} saving all {@link Annotation}s to a new {@link AnnotationFeature}
     * identified by {@code featureDescriptor}. The new {@code DictionaryAnnotator} also uses the provided
     * {@code dictionary} to match token.
     * </p>
     * 
     * @param featureDescriptor The {@link FeatureDescriptor} used to save matching tokens as new {@code Annotation}s.
     * @param dictionary The dictionary to match token agains.
     */
    public DictionaryAnnotator(final FeatureDescriptor<AnnotationFeature> featureDescriptor, final String[] dictionary) {
        super(featureDescriptor);

        Validate.notNull(dictionary, "dictionary must not be null");

        this.dictionary = new HashSet<String>(Arrays.asList(dictionary));
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<String> document = getDefaultInput();

        AnnotationFeature annotationFeature = document.getFeature(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> matchingToken = new ArrayList<Annotation>();
        for (Annotation tokenAnnotation : annotationFeature.getValue()) {
            String token = tokenAnnotation.getValue();
            if (dictionary.contains(token)) {
                int startPosition = tokenAnnotation.getStartPosition();
                int endPosition = tokenAnnotation.getEndPosition();
                Annotation match = new PositionAnnotation(document, startPosition, endPosition, token);
                matchingToken.add(match);
            }
        }

        document.addFeature(new AnnotationFeature(getDescriptor(), matchingToken));
        setDefaultOutput(document);
    }

}
