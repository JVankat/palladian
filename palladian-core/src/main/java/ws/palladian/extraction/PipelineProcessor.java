package ws.palladian.extraction;

import java.io.Serializable;
import java.util.List;

import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * The interface for components processed by processing pipelines. Each component needs to implement this interface
 * before it can be processed by a pipeline.
 * </p>
 * <p>
 * Components can handle any type of information processing task and modify the document given to them. In addition the
 * document may be extended by features that may be retrieved using the components feature identifier. See the
 * {@link FeatureVector} class for more information.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @version 3.0
 * @since 0.0.8
 */
public interface PipelineProcessor<T> extends Serializable {

    static final String DEFAULT_INPUT_PORT_IDENTIFIER = "ws.palladian.inputport";
    static final String DEFAULT_OUTPUT_PORT_IDENTIFIER = "ws.palladian.outputport";

    void process() throws DocumentUnprocessableException;

    List<Port<?>> getInputPorts();

    List<Port<?>> getOutputPorts();

    Port<?> getOutputPort(final String name);

    Boolean isExecutable();

    /**
     * <p>
     * Sets the input at the input port with index {@code inputPortIndex}.
     * </p>
     * 
     * @param inputPortIndex The index of the input port to set the document at.
     * @param document The document to set at the port specified by {@code inputPortIndex}.
     */
    void setInput(final Integer inputPortIndex, final PipelineDocument<?> document);

    void setInput(final String inputPortIdentifier, final PipelineDocument<?> document);

    Port<?> getInputPort(final String name);
}
