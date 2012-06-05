/**
 * Created on: 18.06.2011 15:32:57
 */
package ws.palladian.extraction;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Abstract base class for pipeline processors. Handles the mapping between input and output views.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.8
 * @version 2.0
 */
public abstract class AbstractPipelineProcessor<T> implements PipelineProcessor<T> {
    /**
     * <p>
     * Unique identifier to serialize and deserialize objects of this type to and from a file.
     * </p>
     */
    private static final long serialVersionUID = -7030337967596448903L;

    private List<Port<?>> inputPorts;
    private List<Port<?>> outputPorts;

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor} working on the default views. It maps the default
     * output view ("modifiedContent") from the previous component to the default input ("originalContent") of this
     * component.
     * </p>
     */
    public AbstractPipelineProcessor() {
        super();

        inputPorts = new ArrayList<Port<?>>();
        outputPorts = new ArrayList<Port<?>>();

        inputPorts.add(new Port<String>("defaultInput"));
        outputPorts.add(new Port<String>("defaultOutput"));
    }

    public AbstractPipelineProcessor(final List<Port<?>> inputPorts, final List<Port<?>> outputPorts) {
        super();

        this.inputPorts = new ArrayList<Port<?>>(inputPorts);
        this.outputPorts = new ArrayList<Port<?>>(outputPorts);
    }

    @Override
    public final void process() throws DocumentUnprocessableException {
        allInputPortsAvailable();
        processDocument();
        allOutputPortsAvailable();
    }

    //
    // @Override
    // public final void process(PipelineDocument<T> document) throws DocumentUnprocessableException {
    // Validate.notNull(document);
    // Validate.isTrue(inputPorts.size() == 1);
    // Validate.isTrue(outputPorts.size() == 1);
    // Validate.isTrue(inputPorts.get(0).getName().equals("defaultInput"));
    // Validate.isTrue(outputPorts.get(0).getName().equals("defaultOutput"));
    //
    // ((Port<T>)inputPorts.get(0)).setPipelineDocument(document);
    // allInputPortsAvailable();
    // processDocument();
    // allOutputPortsAvailable();
    // }

    /**
     * <p>
     * Apply the algorithm implemented by this {@code PipelineProcessor} to a {@code PipelineDocument}. This is the
     * central method of each {@code PipelineProcessor} providing the core functionality.
     * </p>
     * 
     * @throws DocumentUnprocessableException
     *             If the {@code document} could not be processed by this {@code PipelineProcessor}.
     */
    protected abstract void processDocument() throws DocumentUnprocessableException;

    /**
     * <p>
     * Checks whether all output views where created in a {@code PipelineDocument} and throws an
     * {@code DocumentUnprocessableException} if not.
     * </p>
     * 
     * @throws DocumentUnprocessableException
     *             In case the document does not provide the required output
     *             view.
     */
    private void allOutputPortsAvailable() throws DocumentUnprocessableException {
        for (Port<?> outputPort : getOutputPorts()) {
            if (outputPort.getPipelineDocument() == null) {
                throw new DocumentUnprocessableException("Output port: " + outputPort
                        + " does not provide required output.");
            }
        }
    }

    /**
     * <p>
     * Checks whether all input views where provided with a {@code PipelineDocument} and throws an
     * {@code DocumentUnprocessableException} if not.
     * </p>
     * 
     * @throws DocumentUnprocessableException
     *             In case the document does not provide the required input
     *             view.
     */
    private void allInputPortsAvailable() throws DocumentUnprocessableException {
        for (Port<?> inputPort : getInputPorts()) {
            if (inputPort.getPipelineDocument() == null) {
                throw new DocumentUnprocessableException("Input port: " + inputPort
                        + " does not provide required input.");
            }
        }
    }

    @Override
    public List<Port<?>> getInputPorts() {
        return inputPorts;
    }

    @Override
    public List<Port<?>> getOutputPorts() {
        return outputPorts;
    }

    @Override
    public Boolean isExecutable() {
        for (Port<?> inputPort : getInputPorts()) {
            if (inputPort.getPipelineDocument() == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    protected PipelineDocument<T> getDefaultInput() {
        return (PipelineDocument<T>)inputPorts.get(0).getPipelineDocument();
    }

    protected PipelineDocument<T> getDefaultOutput() {
        return (PipelineDocument<T>)outputPorts.get(0).getPipelineDocument();
    }
}
