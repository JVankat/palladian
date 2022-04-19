package ws.palladian.semantics;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Spell checks and auto-corrects text using the Palladian spell checker which is based on Peter Norvigs implementation.
 * </p>
 *
 * <p>
 * Training data can be found on Palladian server under Datasets/SpellingCorrection
 * </p>
 *
 * @author David Urbansky
 * https://norvig.com/spell-correct.html
 */
public class PalladianSpellChecker {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianSpellChecker.class);
    private static final Pattern SPLIT = Pattern.compile("\\s");
    private static final Collection<String> EMPTY_SET = new HashSet<>();

    /**
     * Support for correcting German compounds.
     */
    private boolean germanCompoundSupport = false;

    /**
     * The longer the words, the longer it takes to create the variations (edits). This is the maxium word length we
     * allow for correction.
     */
    private int maxWordLength = 20;
    private int maxWordLengthDistanceTwo = 10;
    private int minWordLength = 2;

    /**
     * The number of occurrences for a candidate before we skip breaking German compounds apart.
     */
    private int germanCompoundStopCount = 50;

    /**
     * Manual spelling mappings. Word, e.g. "cov" => "cow" and phrase, e.g. "i pad" => "ipad"
     */
    private Map<String, String> manualWordMappings = new HashMap<>();
    private Map<String, String> manualPhraseMappings = new HashMap<>();

    /**
     * Keep track of the context around words and use it to improve decision when correcting words.
     */
    private boolean useContext;
    private final Bag<String> contextCounter = new Bag<>();

    /**
     * Do not correct words that contain any of these characters.
     */
    private static final Pattern NO_CORRECTION_PATTERN = Pattern.compile("[0-9" + Pattern.quote("<>=-*'#/+'&.") + "]");

    //    private Trie<Integer> words = new Trie<>();
    private Map<String, Integer> words = new HashMap<>();

    public PalladianSpellChecker() {
        this.useContext = true;
    }

    public PalladianSpellChecker(String file) {
        this(file, false);
    }

    public PalladianSpellChecker(String file, boolean ignoreDiacritics) {
        this(file, ignoreDiacritics, true);
    }

    /**
     * Create the object.
     *
     * @param file             The text file from which to create a dictionary.
     * @param ignoreDiacritics If true, diacritics will be ignored, e.g. "uber" will not try to be corrected to "über".
     * @param useContext       Takes more space but makes corrections more precise.
     */
    public PalladianSpellChecker(String file, boolean ignoreDiacritics, boolean useContext) {
        StopWatch stopWatch = new StopWatch();

        this.useContext = useContext;
        int lines = FileHelper.getNumberOfLines(file);
        final ProgressMonitor progressMonitor = new ProgressMonitor(lines, 1, "Spell Checker Loading Dictionary");

        // read the input file and create a P(w) model by counting the word occurrences
        final Set<String> uniqueWords = new HashSet<>();
        final Pattern p = Pattern.compile("[\\w\\p{L}-]+");
        LineAction lineAction = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                if (ignoreDiacritics) {
                    line = StringUtils.stripAccents(line);
                }
                Matcher m = p.matcher(line.toLowerCase());
                String lastMatch = null;
                while (m.find()) {
                    String match = m.group();
                    Integer count = getWordCount(match);
                    if (count == null) {
                        count = 0;
                    }
                    words.put(match, count + 1);
                    uniqueWords.add(match);
                    if (lastMatch != null && useContext) {
                        contextCounter.add(lastMatch + "_" + match);
                    }
                    lastMatch = match;
                }

                progressMonitor.incrementAndPrintProgress();
            }

        };

        FileHelper.performActionOnEveryLine(file, lineAction);

        LOGGER.info("dictionary of " + uniqueWords.size() + " created in " + stopWatch.getElapsedTimeString());
    }

    /**
     * <p>
     * Set manual mappings by providing a mapping file. Each line must follow the following format:
     *
     * <pre>
     * wrongword = correctword
     * </pre>
     *
     * </p>
     *
     * @param mappingFile The file with mappings.
     */
    public void setManualMappings(File mappingFile) {
        List<String> strings = FileHelper.readFileToArray(mappingFile);
        for (String string : strings) {
            String[] split = string.split("=");
            if (split.length != 2) {
                continue;
            }
            if (split[0].trim().contains(" ")) {
                manualPhraseMappings.put(split[0].toLowerCase(), split[1]);
            } else {
                manualWordMappings.put(split[0].toLowerCase(), split[1]);
            }
        }
    }

    public void setManualMappings(Map<String, String> manualWordMappings) {
        this.manualWordMappings = manualWordMappings;
    }

    public void setManualPhraseMappings(Map<String, String> manualPhraseMappings) {
        this.manualPhraseMappings = manualPhraseMappings;
    }

    public Map<String, String> getManualWordMappings() {
        return manualWordMappings;
    }

    public void setManualWordMappings(Map<String, String> manualWordMappings) {
        this.manualWordMappings = manualWordMappings;
    }

    public Map<String, String> getManualPhraseMappings() {
        return manualPhraseMappings;
    }

    public void setUseContext(boolean useContext) {
        this.useContext = useContext;
    }

    public boolean isUseContext() {
        return useContext;
    }

    public void addManualMapping(String source, String target) {
        if (source.contains(" ")) {
            manualPhraseMappings.put(source.toLowerCase(), target);
        } else {
            manualWordMappings.put(source.toLowerCase(), target);
        }
    }

    /**
     * <p>
     * Compute all edit distance 1 words. This list can get rather long.
     * </p>
     * <ol>
     * <li>n deletions (deleted letter)</li>
     * <li>n-1 transpositions</li>
     * <li>26n alternations (replaced letter)</li>
     * <li>26(n+1) insertions (letter added at arbitrary position)</li>
     * </ol>
     *
     * @param word The word for which we create the edit distance words.
     * @return A list of possible variations.
     */
    private List<String> edits(String word) {
        List<String> result = new ArrayList<>();

        int n = word.length();

        if (n == 0) {
            return result;
        }

        // caching substrings is about 2x performance boost
        Map<Integer, String> zeroToNSubstrings = new HashMap<>();
        zeroToNSubstrings.put(n, word);
        Map<Integer, String> i1ToEndSubstrings = new HashMap<>();

        // deletes, n
        for (int i = 0; i < n; ++i) {
            String substring = word.substring(0, i);
            zeroToNSubstrings.put(i, substring);
            String substring1 = word.substring(i + 1);
            i1ToEndSubstrings.put(i, substring1);
            result.add(substring + substring1);
        }

        // transpositions, n-1
        for (int i = 0; i < n - 1; ++i) {
            result.add(zeroToNSubstrings.get(i) + word.charAt(i + 1) + word.charAt(i) + word.substring(i + 2));
        }

        // alternations, 29n
        for (int i = 0; i < n; ++i) {
            String substring0i = zeroToNSubstrings.get(i);
            String substringi1 = i1ToEndSubstrings.get(i);

            for (char c = 'a'; c <= 'z'; ++c) {
                result.add(substring0i + c + substringi1);
            }

            result.add(substring0i + 'ä' + substringi1);
            result.add(substring0i + 'ö' + substringi1);
            result.add(substring0i + 'ü' + substringi1);
        }

        // insertions, 26(n+1)
        for (int i = 0; i <= n; ++i) {
            String substringI = word.substring(i);
            String substringI2 = zeroToNSubstrings.get(i);
            for (char c = 'a'; c <= 'z'; ++c) {
                result.add(substringI2 + c + substringI);
            }

            // umlauts
            result.add(substringI2 + 'ä' + substringI);
            result.add(substringI2 + 'ö' + substringI);
            result.add(substringI2 + 'ü' + substringI);
        }

        result.removeIf(StringUtils::isEmpty);
        return result;
    }

    /**
     * <p>
     * Automatically detect and correct spelling mistakes.
     * </p>
     *
     * @param text The text to check for errors.
     * @return The auto-corrected text.
     */
    public String autoCorrect(String text) {
        return autoCorrect(text, false);
    }

    public String autoCorrect(String text, Collection<String> ignoreWords) {
        return autoCorrect(text, false, ignoreWords);
    }

    /**
     * <p>
     * Automatically detect and correct spelling mistakes.
     * </p>
     *
     * @param text The text to check for errors.
     * @return The auto-corrected text.
     */
    public String autoCorrect(String text, boolean caseSensitive) {
        return autoCorrect(text, caseSensitive, EMPTY_SET);
    }

    public CorrectedText autoCorrectText(String text) {
        return autoCorrectText(text, false, EMPTY_SET);

    }

    public CorrectedText autoCorrectText(String text, boolean caseSensitive, Collection<String> ignoreWords) {
        CorrectedText correctedText = new CorrectedText();
        StringBuilder correctedTextBuilder = new StringBuilder();

        String s = StringHelper.containsWhichWord(manualPhraseMappings.keySet(), text);
        if (s != null) {
            correctedText.setCorrected(true);
            text = text.replace(s, manualPhraseMappings.get(s));
        }

        boolean allWordsKnown = true;
        String[] textWords = SPLIT.split(text);
        for (int i = 0; i < textWords.length; i++) {
            String word = textWords[i];
            String leftContext = null;
            String rightContext = null;
            if (i > 0) {
                leftContext = textWords[i - 1];
            }
            if (i < textWords.length - 1) {
                rightContext = textWords[i + 1];
            }

            int length = word.length();
            if (length < minWordLength || length > maxWordLength || ignoreWords.contains(word) || !StringHelper.getRegexpMatch(NO_CORRECTION_PATTERN, word).isEmpty()) {
                correctedTextBuilder.append(word).append(" ");
                continue;
            }
            char startOfWord = word.charAt(0);
            char endOfWord = word.charAt(word.length() - 1);
            word = StringHelper.trim(word);
            int type = Character.getType(startOfWord);
            if (type == Character.OTHER_PUNCTUATION) {
                correctedTextBuilder.append(startOfWord);
            }
            AtomicBoolean corrected = new AtomicBoolean(false);
            AtomicBoolean wordKnown = new AtomicBoolean(false);
            correctedTextBuilder.append(correctWord(word, caseSensitive, leftContext, rightContext, corrected, wordKnown));

            if (!wordKnown.get()) {
                allWordsKnown = false;
            }
            if (corrected.get()) {
                correctedText.setCorrected(true);
            }
            type = Character.getType(endOfWord);
            if (type == Character.OTHER_PUNCTUATION) {
                correctedTextBuilder.append(endOfWord);
            }
            correctedTextBuilder.append(" ");
        }

        correctedText.setAllWordsKnown(allWordsKnown);
        correctedText.setCorrectedText(correctedTextBuilder.toString().trim());

        return correctedText;
    }

    public String autoCorrect(String text, boolean caseSensitive, Collection<String> ignoreWords) {
        return autoCorrectText(text, caseSensitive, ignoreWords).getCorrectedText();
    }

    /**
     * <p>
     * Automatically detect and correct spelling mistakes in a word.
     * </p>
     * <p/>
     * <p>
     * NOTE: The given word must be lowercase. This saves time in the process.
     * </p>
     *
     * @param word The word to check for errors.
     * @return The auto-corrected word.
     */
    public String correctWordCaseSensitive(String word, String leftContext, String rightContext) {
        return correctWord(word, true, leftContext, rightContext);
    }

    /**
     * <p>
     * Automatically detect and correct spelling mistakes in a word.
     * </p>
     *
     * @param word The word to check for errors.
     * @return The auto-corrected word.
     */
    public String correctWord(String word, String leftContext, String rightContext) {
        return correctWord(word, false, leftContext, rightContext);
    }

    public String correctWord(String word, boolean caseSensitive, String leftContext, String rightContext) {
        return correctWord(word, caseSensitive, leftContext, rightContext, new AtomicBoolean(), new AtomicBoolean());
    }

    private String correctWord(String word, boolean caseSensitive, String leftContext, String rightContext, AtomicBoolean correction, AtomicBoolean wordKnown) {
        correction.set(false);
        boolean uppercase = false;
        int uppercaseCount = 0;
        if (!caseSensitive) {
            uppercaseCount = StringHelper.countUppercaseLetters(word);

            uppercase = uppercaseCount == 1;
            word = word.toLowerCase();
        }

        // check whether a manual mapping exists
        String s1 = manualWordMappings.get(word);
        if (s1 != null) {
            wordKnown.set(true);
            correction.set(true);
            if (uppercase) {
                return StringHelper.upperCaseFirstLetter(s1);
            }
            return s1;
        }

        if (word.length() > maxWordLength) {
            return word;
        }

        // don't correct words with uppercase letters in the middle
        if (!caseSensitive && uppercaseCount > 1) {
            wordKnown.set(true);
            return word;
        }

        // correct words don't need to be corrected
        if (word.isEmpty()) {
            wordKnown.set(true);
            return word;
        }
        if (getWordCount(word) != null) {
            wordKnown.set(true);
            if (uppercase) {
                return StringHelper.upperCaseFirstLetter(word);
            }
            return word;
        }

        List<String> list = edits(word);
        Map<Integer, String> candidates = new HashMap<>();
        for (String s : list) {
            if (s.isEmpty()) {
                continue;
            }
            Integer count = getWordCount(s);
            if (count != null) {
                // look at the context
                if (leftContext != null && useContext) {
                    count += 100 * contextCounter.count(leftContext + "_" + s);
                }
                if (rightContext != null && useContext) {
                    count += 100 * contextCounter.count(s + "_" + rightContext);
                }
                candidates.put(count, s);
            }
        }

        // German words can be compounds, e.g. "Goldkette", we most likely don't have all these words in the dictionary
        // and might cause incorrect corrections, we therefore split the compound and test its parts for misspellings
        boolean compoundCorrect = false;
        if (isGermanCompoundSupport()) {
            if (candidates.keySet().isEmpty() || Collections.max(candidates.keySet()) < germanCompoundStopCount) {
                compoundCorrect = true;
                List<String> strings = WordTransformer.splitGermanCompoundWords(word);
                for (String string : strings) {
                    if (string.length() < 2) {
                        compoundCorrect = false;
                        break;
                    }
                    if (getWordCount(string) == null) {
                        String key = WordTransformer.wordToSingularGermanCaseSensitive(string);
                        // if (words.get(key) == null && strings.size() > 1) {
                        // key = autoCorrect(key, true);
                        // }
                        if (getWordCount(key) == null) {
                            compoundCorrect = false;
                            break;
                        }
                    }
                }
                if (compoundCorrect) {
                    wordKnown.set(true);
                }
            }
        }

        // check for edit distance 2 if we haven't found anything, the first character must not change
        if (candidates.isEmpty() && !compoundCorrect) {
            for (String s : list) {
                if (s.length() > maxWordLengthDistanceTwo) {
                    continue;
                }
                for (String w : edits(s)) {
                    Integer count = getWordCount(w);
                    if (count != null && firstCharacterSame(w, word)) {
                        candidates.put(count, w);
                    }
                }
            }
        }

        String corrected = word;
        if (!candidates.isEmpty() && !compoundCorrect) {
            corrected = candidates.get(Collections.max(candidates.keySet()));
            correction.set(true);
        }

        if (uppercase) {
            corrected = StringHelper.upperCaseFirstLetter(corrected);
        }

        return corrected;
    }

    private boolean firstCharacterSame(String a, String b) {
        return !(a.isEmpty() || b.isEmpty()) && a.charAt(0) == b.charAt(0);
    }

    public Map<String, Integer> getWords() {
        return words;
    }

    public void setWords(Map<String, Integer> words) {
        this.words = words;
    }

    public boolean isGermanCompoundSupport() {
        return germanCompoundSupport;
    }

    public void setGermanCompoundSupport(boolean germanCompoundSupport) {
        this.germanCompoundSupport = germanCompoundSupport;
    }

    public int getMaxWordLength() {
        return maxWordLength;
    }

    public void setMaxWordLength(int maxWordLength) {
        this.maxWordLength = maxWordLength;
    }

    public int getMaxWordLengthDistanceTwo() {
        return maxWordLengthDistanceTwo;
    }

    public void setMaxWordLengthDistanceTwo(int maxWordLengthDistanceTwo) {
        this.maxWordLengthDistanceTwo = maxWordLengthDistanceTwo;
    }

    public int getMinWordLength() {
        return minWordLength;
    }

    public void setMinWordLength(int minWordLength) {
        this.minWordLength = minWordLength;
    }

    public int getGermanCompoundStopCount() {
        return germanCompoundStopCount;
    }

    public void setGermanCompoundStopCount(int germanCompoundStopCount) {
        this.germanCompoundStopCount = germanCompoundStopCount;
    }

    protected Integer getWordCount(String word) {
        return words.get(word);
    }

    public static void main(String[] args) throws IOException {
        // System.out.println(new PalladianSpellChecker("en.txt").autoCorrect("caar"));
        // System.out.println(new
        // PalladianSpellChecker("en.txt").autoCorrect("This ls hoow the etxt is supossed to be"));
        // System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("Ist das nichk enn schoner Tetx"));
        // System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("blauess hadny"));
        System.out.println(ProcessHelper.getHeapUtilization());
        System.out.println(new PalladianSpellChecker("data/models/de.txt").autoCorrect("orankes Hadny"));
        System.out.println(ProcessHelper.getHeapUtilization());
//        System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("orankes Hadny"));
    }
}