package ws.palladian.semantics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Trie;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Spell checks and auto-corrects text using the Palladian spell checker which is based on Peter Norvigs implementation.
 * </p>
 * <p/>
 * <p>
 * Training data can be found on Palladian server under Datasets/SpellingCorrection
 * </p>
 *
 * @author David Urbansky
 * @see http://norvig.com/spell-correct.html
 */
public class PalladianSpellChecker {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianSpellChecker.class);
    private static final Pattern SPLIT = Pattern.compile("\\s");

    private boolean germanCompoundSupport = false;

    /**
     * Do not correct words that contain any of these characters.
     */
    private static final Pattern NO_CORRECTION_PATTERN = Pattern.compile("[0-9" + Pattern.quote("<>=-*'#/") + "]");

    private Trie<Integer> words = new Trie<>();

    public PalladianSpellChecker() {
    }

    public PalladianSpellChecker(String file) {

        StopWatch stopWatch = new StopWatch();

        // read the input file and create a P(w) model by counting the word occurrences
        final Set<String> uniqueWords = new HashSet<>();
        final Pattern p = Pattern.compile("[\\wöäüß-]+");
        LineAction lineAction = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                Matcher m = p.matcher(line.toLowerCase());
                while (m.find()) {
                    String match = m.group();
                    Integer count = words.get(match);
                    if (count == null) {
                        count = 0;
                    }
                    words.put(match, count + 1);
                    uniqueWords.add(match);
                }
            }

        };

        FileHelper.performActionOnEveryLine(file, lineAction);

        LOGGER.info("dictionary of " + uniqueWords.size() + " created in " + stopWatch.getElapsedTimeString());
    }

    /**
     * <p>
     * Compute all edit distance 1 words. This list can get rather long.
     * </p>
     * <ol>
     * <li>n deletions</li>
     * <li>n-1 transpositions</li>
     * <li>26n alternations (replaced letter)</li>
     * <li>26(n+1) insertions (letter added at arbirary position)</li>
     * </ol>
     *
     * @param word The word for which we create the edit distance words.
     * @return A list of possible variations.
     */
    private List<String> edits(String word) {
        List<String> result = new ArrayList<>();

        int n = word.length();

        // deletes, n
        for (int i = 0; i < n; ++i) {
            result.add(word.substring(0, i) + word.substring(i + 1));
        }

        // transpositions, n-1
        for (int i = 0; i < n - 1; ++i) {
            result.add(word.substring(0, i) + word.substring(i + 1, i + 2) + word.substring(i, i + 1)
                    + word.substring(i + 2));
        }

        // alternations, 26n
        for (int i = 0; i < n; ++i) {
            for (char c = 'a'; c <= 'z'; ++c) {
                result.add(word.substring(0, i) + c + word.substring(i + 1));
            }
            // umlauts
            result.add(word.substring(0, i) + 'ä' + word.substring(i + 1));
            result.add(word.substring(0, i) + 'ö' + word.substring(i + 1));
            result.add(word.substring(0, i) + 'ü' + word.substring(i + 1));
        }

        // insertions, 26(n+1)
        for (int i = 0; i <= n; ++i) {
            for (char c = 'a'; c <= 'z'; ++c) {
                result.add(word.substring(0, i) + c + word.substring(i));
            }
        }

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
        StringBuilder correctedText = new StringBuilder();

        String[] textWords = SPLIT.split(text);
        for (String word : textWords) {
            if (word.length() < 2 || StringHelper.getRegexpMatch(NO_CORRECTION_PATTERN, word).length() > 0) {
                correctedText.append(word).append(" ");
                continue;
            }
            char startOfWord = word.charAt(0);
            char endOfWord = word.charAt(word.length() - 1);
            word = StringHelper.trim(word);
            int type = Character.getType(startOfWord);
            if (type == Character.OTHER_PUNCTUATION) {
                correctedText.append(startOfWord);
            }
            correctedText.append(correctWord(word));
            type = Character.getType(endOfWord);
            if (type == Character.OTHER_PUNCTUATION) {
                correctedText.append(endOfWord);
            }
            correctedText.append(" ");
        }

        return correctedText.toString().trim();
    }

    /**
     * <p>
     * Automatically detect and correct spelling mistakes in a word.
     * </p>
     *
     * @param word The word to check for errors.
     * @return The auto-corrected word.
     */
    public String correctWord(String word) {

        int uppercaseCount = StringHelper.countUppercaseLetters(word);

        // don't correct words with uppercase letters in the middle
        if (uppercaseCount > 1) {
            return word;
        }

        // don't correct words with apostrophe
        if (word.contains("'")) {
            return word;
        }

        boolean uppercase = uppercaseCount == 1;
        word = word.toLowerCase();

        // correct words don't need to be corrected
        if (words.get(word) != null) {
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
            Integer count = words.get(s);
            if (count != null) {
                candidates.put(count, s);
            }
        }

        // German words can be compounds, e.g. "Goldkette", we most likely don't have all these words in the dictionary
        // and might cause incorrect corrections, we therefore split the compound and test its parts for misspellings
        boolean compoundCorrect = false;
        if (isGermanCompoundSupport()) {
            compoundCorrect = true;
            List<String> strings = WordTransformer.splitGermanCompoundWords(word);
            for (String string : strings) {
                if (words.get(string) == null) {
                    String key = WordTransformer.wordToSingularGermanCaseSensitive(string);
                    if (words.get(key) == null) {
                        compoundCorrect = false;
                    }
                }
            }
        }

        // check for edit distance 2 if we haven't found anything, the first character must not change
        if (candidates.isEmpty() && !compoundCorrect) {
            for (String s : list) {
                for (String w : edits(s)) {
                    Integer count = words.get(w);
                    if (count != null && firstCharacterSame(w, word)) {
                        candidates.put(count, w);
                    }
                }
            }
        }

        String corrected = word;
        if (!candidates.isEmpty() && !compoundCorrect) {
            corrected = candidates.get(Collections.max(candidates.keySet()));
        }

        if (uppercase) {
            corrected = StringHelper.upperCaseFirstLetter(corrected);
        }

        return corrected;
    }

    private boolean firstCharacterSame(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.charAt(0) == b.charAt(0);
    }

    public Trie<Integer> getWords() {
        return words;
    }

    public void setWords(Trie<Integer> words) {
        this.words = words;
    }

    public boolean isGermanCompoundSupport() {
        return germanCompoundSupport;
    }

    public void setGermanCompoundSupport(boolean germanCompoundSupport) {
        this.germanCompoundSupport = germanCompoundSupport;
    }

    public static void main(String[] args) throws IOException {
        // System.out.println(new PalladianSpellChecker("en.txt").autoCorrect("caar"));
        // System.out.println(new
        // PalladianSpellChecker("en.txt").autoCorrect("This ls hoow the etxt is supossed to be"));
        // System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("Ist das nichk enn schoner Tetx"));
        // System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("blauess hadny"));
        System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("orankes Hadny"));
    }
}