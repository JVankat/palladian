package ws.palladian.extraction.helper;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.feature.AbstractDefaultPipelineProcessor;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.NumericFeature;

public class WordCounter extends AbstractDefaultPipelineProcessor {

    private static final long serialVersionUID = 4592668328026315402L;

    @Override
    public void processDocument(PipelineDocument<String> document) {

        int wordCount = StringHelper.countWords(document.getContent());

        NumericFeature numericFeature = new NumericFeature("wordCount", (double)wordCount);

        document.getFeatureVector().add(numericFeature);

    }

}
