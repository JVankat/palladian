/**
 * Created on: 16.06.2012 11:32:26
 */
package ws.palladian.extraction.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import scala.actors.threadpool.Arrays;
import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.Port;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * Calculates the Jaccard similarity (see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Jaccard index</a>) as a
 * measure of overlap between two sets of the same {@link Annotation} from two {@link PipelineDocument}s. The processor
 * provides two input ports identified by {@link #INPUT_PORT_ONE_IDENTIFIER} and
 * {@link TokenOverlapCalculator#INPUT_PORT_TWO_IDENTIFIER}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class TokenOverlapCalculator extends AbstractPipelineProcessor {

    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set changes.
     * </p>
     */
    private static final long serialVersionUID = 3094845412635456119L;

    public static final String INPUT_PORT_ONE_IDENTIFIER = "input1";
    public static final String INPUT_PORT_TWO_IDENTIFIER = "input2";

    private final FeatureDescriptor<AnnotationFeature> input1FeatureDescriptor;
    private final FeatureDescriptor<AnnotationFeature> input2FeatureDescriptor;
    private final FeatureDescriptor<NumericFeature> featureDescriptor;

    /**
     * <p>
     * Creates a new {@code TokenOverlapCalculator} which takes the {@code Feature}s from the {@code PipelineDocument}
     * at the {@code Port} identified by {@link #INPUT_PORT_ONE_IDENTIFIER} and calculates the Jaccard similarity with
     * the {@code Feature}s from the {@code PipelineDocument} at the {@code Port} identified by
     * {@link #INPUT_PORT_TWO_IDENTIFIER}. The {@code Feature}s from the first {@code PipelineDocument} are identified
     * by {@code input1FeatureDescriptor} while the {@code Feature}s from the second {@code PipelineDocument} are
     * identified by {@code input2FeatureDescriptor}.
     * </p>
     * 
     * @param featureDescriptor The descriptor for the result {@code Feature}.
     * @param input1FeatureDescriptor The descriptor for the first input {@code Feature}.
     * @param input2FeatureDescriptor The descriptor for the second input {@code Feature}.
     */
    public TokenOverlapCalculator(final FeatureDescriptor<NumericFeature> featureDescriptor,
            final FeatureDescriptor<AnnotationFeature> input1FeatureDescriptor,
            final FeatureDescriptor<AnnotationFeature> input2FeatureDescriptor) {
        // Ports parameterized with Objects since it does not matter which type they have, because the Calculator only
        // uses the feature vector.
        super(Arrays.asList(new Port[] {new Port<Object>(INPUT_PORT_ONE_IDENTIFIER),
                new Port<Object>(INPUT_PORT_TWO_IDENTIFIER)}), Arrays.asList(new Port[] {new Port<Object>(
                PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER)}));

        this.featureDescriptor = featureDescriptor;
        this.input1FeatureDescriptor = input1FeatureDescriptor;
        this.input2FeatureDescriptor = input2FeatureDescriptor;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<?> document1 = getInputPort(INPUT_PORT_ONE_IDENTIFIER).getPipelineDocument();
        PipelineDocument<?> document2 = getInputPort(INPUT_PORT_TWO_IDENTIFIER).getPipelineDocument();

        final List<Annotation> input1Annotations = document1.getFeature(input1FeatureDescriptor).getValue();
        final List<Annotation> input2Annotations = document2.getFeature(input2FeatureDescriptor).getValue();

        Set<String> setOfInput1 = new HashSet<String>();
        Set<String> setOfInput2 = new HashSet<String>();
        for (Annotation annotation : input1Annotations) {
            setOfInput1.add(annotation.getValue());
        }
        for (Annotation annotation : input2Annotations) {
            setOfInput2.add(annotation.getValue());
        }
        final Set<String> overlap = new HashSet<String>();
        overlap.addAll(setOfInput1);
        overlap.retainAll(setOfInput2);
        final Set<String> union = new HashSet<String>();
        union.addAll(setOfInput1);
        union.addAll(setOfInput2);

        Double jaccardSimilarity = Integer.valueOf(overlap.size()).doubleValue()
                / Integer.valueOf(union.size()).doubleValue();
        document1.addFeature(new NumericFeature(featureDescriptor, jaccardSimilarity));
        setOutput(DEFAULT_OUTPUT_PORT_IDENTIFIER, document1);
    }
}
