/**
 * Created on: 05.06.2012 21:48:01
 */
package ws.palladian.extraction;

import java.util.ArrayList;

import org.apache.commons.lang.Validate;

import scala.actors.threadpool.Arrays;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DocumentCreationProcessor<T> extends AbstractPipelineProcessor<T> {

    /**
     * 
     */
    private static final long serialVersionUID = 95543851870074183L;
    private PipelineDocument<T> document;

    /**
     * <p>
     * Creates a new {@code DocumentCreationProcessor} with no input ports and just one output port called
     * <tt>newDocument</tt>.
     * </p>
     * 
     * @param document The {@code PipelineDocument} this {@code PipelineProcessor} should output.
     */
    public DocumentCreationProcessor(final PipelineDocument<T> document) {
        super(new ArrayList<Port<?>>(), Arrays.asList(new Port<?>[] {new Port<T>("newDocument")}));
        Validate.notNull(document);

        this.document = document;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        ((Port<T>)getOutputPorts().get(0)).setPipelineDocument(document);
    }

}
