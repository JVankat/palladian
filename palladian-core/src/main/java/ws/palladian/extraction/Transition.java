/**
 * Created on: 05.06.2012 21:00:56
 */
package ws.palladian.extraction;

import org.apache.commons.lang.Validate;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class Transition<T> {
    private final Port<T> inputPort;
    private final Port<T> outputPort;

    public Transition(final Port<T> inputPort, final Port<T> outputPort) {
        super();
        Validate.notNull(inputPort);
        Validate.notNull(outputPort);

        this.inputPort = inputPort;
        this.outputPort = outputPort;
    }

    public void transit() {
        Validate.notNull(inputPort.getPipelineDocument());
        outputPort.setPipelineDocument(inputPort.getPipelineDocument());
    }

    public Boolean canFire() {
        if (inputPort.getPipelineDocument() != null) {
            return true;
        } else {
            return false;
        }
    }
}
