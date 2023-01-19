package ws.palladian.extraction.feature;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

import java.io.*;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>
 * A corpus with terms from documents. Used typically for IDF calculations.
 * </p>
 *
 * @author Philipp Katz
 */
public final class MapTermCorpus extends AbstractTermCorpus {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MapTermCorpus.class);

    private static final String SEPARATOR = "#";

    private int numDocs;
    private final Bag<String> terms;

    /**
     * <p>
     * Create a new, empty {@link MapTermCorpus}.
     * </p>
     */
    public MapTermCorpus() {
        this(new Bag<String>(), 0);
    }

    /**
     * <p>
     * Create a new {@link MapTermCorpus} with the specified terms and number of documents.
     * </p>
     *
     * @param terms   The terms to add.
     * @param numDocs The number of documents this corpus contains.
     */
    public MapTermCorpus(Bag<String> terms, int numDocs) {
        this.numDocs = numDocs;
        this.terms = terms;
    }

    /**
     * <p>
     * Add the terms from the specified document and increment the number of documents counter.
     * </p>
     *
     * @param terms The terms to add.
     */
    public void addTermsFromDocument(Set<String> terms) {
        this.terms.addAll(terms);
        numDocs++;
    }

    @Override
    public int getCount(String term) {
        return terms.count(term);
    }

    @Override
    public int getNumDocs() {
        return numDocs;
    }

    @Override
    public int getNumTerms() {
        return terms.size();
    }

    @Override
    public int getNumUniqueTerms() {
        return terms.unique().size();
    }

    /**
     * <p>
     * Load a serialized {@link MapTermCorpus} from the given path.
     * </p>
     *
     * @param filePath The path to the file with the corpus, not <code>null</code>.
     * @return A {@link MapTermCorpus} with the deserialized corpus.
     * @throws IOException In case the file could not be read.
     */
    public static MapTermCorpus load(File filePath) throws IOException {
        Validate.notNull(filePath, "filePath must not be null");
        InputStream inputStream = null;
        try {
            inputStream = new GZIPInputStream(new FileInputStream(filePath));
            return load(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
    }

    /**
     * <p>
     * Load a serialized {@link MapTermCorpus} from the given input stream.
     * </p>
     *
     * @param inputStream The input stream providing the serialized data, not <code>null</code>.
     * @return A {@link MapTermCorpus} with the deserialized corpus.
     */
    public static MapTermCorpus load(InputStream inputStream) {
        Validate.notNull(inputStream, "inputStream must not be null");
        final int[] numDocs = new int[1];
        final Bag<String> counts = new Bag<>();
        StopWatch stopWatch = new StopWatch();
        FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
            @Override
            public void performAction(String text, int number) {
                //                if (number != 0 && number % 100000 == 0) {
                //                    System.out.print('.');
                //                    if (number % 10000000 == 0) {
                //                        System.out.println();
                //                    }
                //                }
                String[] split = text.split(SEPARATOR);
                if (number > 1) {
                    if (split.length != 2) {
                        // System.err.println(text);
                        return;
                    }
                    counts.add(split[0], Integer.parseInt(split[1]));
                } else if (text.startsWith("numDocs" + SEPARATOR)) {
                    numDocs[0] = Integer.parseInt(split[1]);
                }
            }
        });
        //        System.out.println();
        LOGGER.debug("Loaded {} terms in {}", counts.unique().size(), stopWatch);
        return new MapTermCorpus(counts, numDocs[0]);
    }

    public void save(File file) throws IOException {
        OutputStream outputStream = null;
        PrintWriter printWriter = null;
        try {
            outputStream = new GZIPOutputStream(new FileOutputStream(file));
            printWriter = new PrintWriter(outputStream);
            printWriter.println("numDocs" + SEPARATOR + getNumDocs());
            printWriter.println();
            for (String term : terms.uniqueItems()) {
                int count = terms.count(term);
                String line = term + SEPARATOR + count;
                printWriter.println(line);
            }
        } finally {
            FileHelper.close(printWriter, outputStream);
        }
    }

    /**
     * <p>
     * Reset this {@link MapTermCorpus}, i.e. clear all terms and reset the number of documents to zero.
     * </p>
     */
    public void clear() {
        numDocs = 0;
        terms.clear();
    }

    /**
     * <p>
     * Get a new filtered {@link TermCorpus} with high frequency terms only.
     * </p>
     *
     * @param minOccurrenceCount The minimum occurrence count, greater/equal zero.
     * @return The filtered {@link TermCorpus}.
     */
    public MapTermCorpus getFilteredCorpus(int minOccurrenceCount) {
        Bag<String> resultTerms = new Bag<>();
        for (String term : terms.uniqueItems()) {
            int count = terms.count(term);
            if (count >= minOccurrenceCount) {
                resultTerms.add(term, count);
            }
        }
        return new MapTermCorpus(resultTerms, numDocs);
    }

    /**
     * Get a reduced version of the term corpus with the specified maximum size
     * (i.e. number of unique terms), by taking the highest frequency items.
     *
     * @param maxSize The size of the reduced corpus.
     * @return The reduced corpus.
     */
    public MapTermCorpus getReducedCorpus(int maxSize) {
        Validate.isTrue(maxSize > 0, "maxSize must be greater zero.");
        Bag<String> resultTerms = new Bag<>();
        Bag<String> sorted = terms.createSorted(Order.DESCENDING);
        int size = 0;
        for (String term : sorted.uniqueItems()) {
            if (++size > maxSize) {
                break;
            }
            int count = terms.count(term);
            resultTerms.add(term, count);
        }
        return new MapTermCorpus(resultTerms, numDocs);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TermCorpus");
        sb.append(" numDocs=").append(getNumDocs());
        sb.append(" numUniqueTerms=").append(terms.unique().size());
        sb.append(" numTerms=").append(terms.size());
        return sb.toString();
    }

    @Override
    public Iterator<String> iterator() {
        return terms.uniqueItems().iterator();
    }

}