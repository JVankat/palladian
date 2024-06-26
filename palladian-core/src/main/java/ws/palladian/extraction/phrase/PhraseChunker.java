package ws.palladian.extraction.phrase;

import ws.palladian.core.Annotation;

import java.util.List;

/**
 * @author Martin Wunderwald
 * @author Philipp Katz
 */
public interface PhraseChunker {

    /**
     * <p>Chunks a sentence and returns parts in {@link TagAnnotations}.</p>
     *
     * @param sentence The sentence to chunk.
     */
    List<Annotation> chunk(String sentence);

    /**
     * <p>Get the name of this chunker.</p>
     *
     * @return
     */
    String getName();

    /**
     * @param args
     */
    //    public static void main(String[] args) {
    //
    //        final OpenNlpPhraseChunker onlppc = new OpenNlpPhraseChunker();
    //        onlppc.loadDefaultModel();
    //
    //        final StopWatch stopWatch = new StopWatch();
    //        stopWatch.start();
    //
    //        onlppc.chunk("Death toll rises after Indonesia tsunami.");
    //        LOGGER.info(onlppc.getTagAnnotations().getTaggedString());
    //
    //        stopWatch.stop();
    //        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());
    //
    //    }
}
