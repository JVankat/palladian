/**
 * 
 */
package ws.palladian.extraction.entity;

import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.entity.tagger.PalladianNer;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;

/**
 * @author Klemens Muthmann
 * 
 */
public final class ProcessingApiTest {

	private String FIXTURE = "John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.";

	@Test
	public void testProcessing() throws Exception {
		String tudnerEnModel = ResourceHelper.getResourcePath("/ner/tudnerEn.model");
		PalladianNer nerProcessor = new PalladianNer(
				LanguageMode.English);
        
        nerProcessor.loadModel(tudnerEnModel);
		
		PipelineDocument<String> document = new PipelineDocument<String>(
				FIXTURE);
		nerProcessor.processDocument(document);
		
		AnnotationFeature extractedEntities = document.getFeature(NamedEntityRecognizer.PROVIDED_FEATURE_DESCRIPTOR);
		
		assertThat(extractedEntities.getValue().size(),Matchers.is(5));
		for(Annotation<String> nerAnnotation:extractedEntities.getValue()) {
			assertThat(nerAnnotation,Matchers.notNullValue());
		}
	}
}