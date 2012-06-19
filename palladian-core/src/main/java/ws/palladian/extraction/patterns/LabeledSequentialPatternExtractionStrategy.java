/**
 * Created on: 19.06.2012 19:35:00
 */
package ws.palladian.extraction.patterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ws.palladian.model.features.FeatureDescriptor;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class LabeledSequentialPatternExtractionStrategy implements SpanExtractionStrategy {

    @Override
    public List<SequentialPattern> extract(final String[] tokenList, final Integer minPatternSize,
            final Integer maxPatternSize, final FeatureDescriptor<SequentialPattern> descriptor) {
        List<SequentialPattern> extractedPatterns = new ArrayList<SequentialPattern>();
        Collection<List<String>> patterns = ws.palladian.extraction.token.Tokenizer.getAllSpans(tokenList,
                maxPatternSize);

        for (List<String> pattern : patterns) {
            SequentialPattern lspFeature = new SequentialPattern(descriptor, pattern);
            extractedPatterns.add(lspFeature);
        }

        return extractedPatterns;
    }

}
