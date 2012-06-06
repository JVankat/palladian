/**
 * Created on: 06.06.2012 21:15:21
 */
package ws.palladian.extraction.feature;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public abstract class AbstractDefaultPipelineProcessor extends AbstractPipelineProcessor<String> {
    /**
     * 
     */
    private static final long serialVersionUID = 5839622925654060268L;

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<String> document = getDefaultInput();
        processDocument(document);
        setDefaultOutput(document);
    }

    public abstract void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException;
}
