/**
 *
 */
package ws.palladian.extraction.sentence;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.PositionAnnotation;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * A sentence detector based on the implementation provided by the <a href="http://alias-i.com/lingpipe">Lingpipe</a>
 * framework.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.0.1
 */
public final class LingPipeSentenceDetector extends AbstractSentenceDetector {

    /**
     * <p>
     * The {@code SentenceChunker} instance used and containing the core implementation for splitting a processed text
     * into sentences.
     * </p>
     */
    private final SentenceChunker sentenceChunker;

    /**
     * <p>
     * Creates a new completely initialized sentence detector without any parameters. The state of the new object is set
     * to default values.
     * </p>
     */
    public LingPipeSentenceDetector() {
        super();

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        SentenceModel sentenceModel = new IndoEuropeanSentenceModel();
        sentenceChunker = new SentenceChunker(tokenizerFactory, sentenceModel);
    }

    /**
     * <p>
     * Creates a new {@code LingPipeSentenceDetector} annotating sentences and saving those {@link PositionAnnotationn}s as a
     * {@link Feature} described by the provided feature identifiers.
     * </p>
     * 
     * @param featureDescriptor The identifier for the created {@code Feature}.
     */
    public LingPipeSentenceDetector(String featureIdentifier) {
        super(featureIdentifier);

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        SentenceModel sentenceModel = new IndoEuropeanSentenceModel();
        sentenceChunker = new SentenceChunker(tokenizerFactory, sentenceModel);
    }

    @Override
    public LingPipeSentenceDetector detect(String text) {
        Validate.notNull(text, "text must not be null");

        Chunking chunking = sentenceChunker.chunk(text);
        PositionAnnotation[] sentences = new PositionAnnotation[chunking.chunkSet().size()];
        TextDocument document = new TextDocument(text);
        int ite = 0;
        for (final Chunk chunk : chunking.chunkSet()) {
            String sentence = text.substring(chunk.start(), chunk.end());
            sentences[ite] = new PositionAnnotation(providedFeature, document, chunk.start(), chunk.end(), sentence);
            ite++;
        }
        setSentences(sentences);
        return this;
    }
}
