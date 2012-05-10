package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.model.features.Annotation;

public class DuplicateTokenConsolidatorTest {
    
    private static final String SAMPLE_TEXT = "Das Reh springt hoch, das Reh springt weit. Warum auch nicht - es hat ja Zeit!";
    
    @Test
    public void testDuplicateTokenConsolidator() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new DuplicateTokenConsolidator());
        PipelineDocument document = pipeline.process(new PipelineDocument(SAMPLE_TEXT));
        
        List<Annotation> tokenAnnotations = BaseTokenizer.getTokenAnnotations(document);
        Annotation token1 = tokenAnnotations.get(0);
        assertEquals("Das", token1.getValue());
        List<Annotation> duplicates1 = DuplicateTokenConsolidator.getDuplicateAnnotations(token1);
        assertEquals(1, duplicates1.size());
        assertEquals((Integer) 22, duplicates1.get(0).getStartPosition());
        assertEquals((Integer) 25, duplicates1.get(0).getEndPosition());
        assertEquals("das", duplicates1.get(0).getValue());
    }

}
