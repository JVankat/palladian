package ws.palladian.extraction.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.constants.Language;
import java.util.function.Predicate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Remove stop words from a text.
 * </p>
 */
public class StopWordRemover implements Predicate<String> {
    private static final Map<String, Set<String>> CACHE = new HashMap<>();

    private final Set<String> stopwords;

    /**
     * <p>
     * Default constructor for English texts.
     * </p>
     */
    public StopWordRemover() {
        this(Language.ENGLISH);
    }

    /**
     * <p>
     * Create a new {@link StopTokenRemover} with stop words from the specified {@link File}.
     * </p>
     * 
     * @param file The file which contains the stop words. Each line is treated as one stop word, lines starting with #
     *            are treated as comments and are therefore ignored.
     * @throws IllegalArgumentException If the supplied file cannot be found.
     */
    public StopWordRemover(File file) {
        Validate.notNull(file, "file must not be null");
        try {
            stopwords = loadStopwords(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File \"" + file + "\" not found.");
        }
    }

    /**
     * <p>
     * Choose a language (English or German).
     * </p>
     * 
     * @param language The language for which the stop words should be removed.
     */
    public StopWordRemover(Language language) {
        this(language, false);
    }

    public StopWordRemover(Language language, boolean smallVersion) {
        Validate.notNull(language, "language must not be null");
        switch (language) {
            case ENGLISH:
            case GERMAN:
            case SPANISH:
            case DUTCH:
            case ITALIAN:
            case PORTUGUESE:
            case RUSSIAN:
            case DANISH:
            case FINNISH:
            case HUNGARIAN:
            case NORWEGIAN:
            case ROMANIAN:
            case SWEDISH:
            case TURKISH:
            case CHINESE:
            case JAPANESE:
            case VIETNAMESE:
            case FRENCH:
                String resourcePath = "/stopwords_" + language.getIso6391();
                if (smallVersion) {
                    resourcePath += "_small";
                }
                resourcePath += ".txt";
                stopwords = loadStopwordsResourceCached(resourcePath);
                break;
            default:
                stopwords = Collections.emptySet();
                break;
        }
    }

    private static final Set<String> loadStopwordsResourceCached(String resourcePath) {
        Set<String> stopwords = CACHE.get(resourcePath);
        if (stopwords != null) {
            return stopwords;
        }
        synchronized (CACHE) {
            stopwords = CACHE.get(resourcePath);
            if (stopwords == null) {
                stopwords = loadStopwordsResource(resourcePath);
                CACHE.put(resourcePath, stopwords);
            }
        }
        return stopwords;
    }

    private static Set<String> loadStopwordsResource(String resourcePath) {
        InputStream inputStream = StopWordRemover.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Resource \"" + resourcePath + "\" not found.");
        }
        try {
            return loadStopwords(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
    }

    private static Set<String> loadStopwords(InputStream fileInputStream) {
        final Set<String> result = new HashSet<>();
        FileHelper.performActionOnEveryLine(fileInputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String lineString = line.trim();
                // ignore comments and empty lines ...
                if (!lineString.startsWith("#") && !lineString.isEmpty()) {
                    result.add(line.toLowerCase());
                }
            }
        });
        return result;
    }

    public String removeStopWords(String text) {

        for (String stopWord : stopwords) {

            // skip comment lines
            if (stopWord.startsWith("#")) {
                continue;
            }

            text = StringHelper.removeWord(stopWord, text);
        }

        return text;
    }

    @Override
    public boolean test(String item) {
        return !isStopWord(item);
    }

    public boolean isStopWord(String word) {
        return stopwords.contains(word);
    }

    public void addStopWord(String word) {
        stopwords.add(word);
    }

    public void removeStopWord(String word) {
        stopwords.remove(word);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StopWordRemover [#stopwords=");
        builder.append(stopwords.size());
        builder.append("]");
        return builder.toString();
    }

    public static void main(String[] args) {
        StopWordRemover stopWordRemover = new StopWordRemover();
        System.out.println(stopWordRemover.removeStopWords("is the"));
    }

}
