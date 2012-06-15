/**
 * Created on: 09.06.2012 19:47:11
 */
package ws.palladian.extraction.pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.LingPipeTokenizer;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.model.features.Annotation;

/**
 * <p>
 * Tests the correct working of the LinPipe POS Tagger implementation in Palladian.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
@RunWith(value = Parameterized.class)
public class LingPipePosTaggerTest {

    private final PipelineDocument<String> document;

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { {"The quick brown fox jumps over the lazy dog."}, {"I like my cake."},
                {"Your gun is the best friend you have."}};
        return Arrays.asList(data);
    }

    public LingPipePosTaggerTest(String document) {
        super();

        this.document = new PipelineDocument<String>(document);
    }

    @Test
    public void test() throws FileNotFoundException, DocumentUnprocessableException {
        File modelFile = ResourceHelper.getResourceFile("/model/pos-en-general-brown.HiddenMarkovModel");
        PipelineProcessor<String> tokenizer = new LingPipeTokenizer();
        PipelineProcessor<String> objectOfClassUnderTest = new LingPipePosTagger(modelFile);

        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(tokenizer);
        pipeline.add(objectOfClassUnderTest);

        pipeline.process(document);
        System.out.println(document.getContent());
        for (Annotation token : document.getFeatureVector().get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR).getValue()) {
            System.out.print(" "
                    + token.getFeatureVector().get(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue());
        }
        System.out.println();
    }
}
