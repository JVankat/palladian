package ws.palladian.helper.nlp;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.normalization.StringNormalizer;
import ws.palladian.helper.normalization.UnitNormalizer;

/**
 * <p>
 * The StringHelper provides functionality for typical String manipulation operations.
 * </p>
 * 
 * @author David Urbansky
 * @author Martin Werner
 * @author Philipp Katz
 * @author Martin Gregor
 */
public class StringHelper {

    /** The Constant BRACKETS. A list of bracket types. */
    private static final char[] BRACKETS = {'(', ')', '{', '}', '[', ']'};

    /** Used to replace a semicolon in a string to store it in csv file that uses semicolon to separate fields. */
    private static final String SEMICOLON_REPLACEMENT = "###putSemicolonHere###";

    /** Used to replace a double quote " in a string to store it in csv file that uses double quotes to enclose fields. */
    private static final String DOUBLE_QUOTES_REPLACEMENT = "###putDoubleQuotesHere###";

    private StringHelper() {

    }

    /**
     * In ontologies names can not have certain characters so they have to be changed.
     * 
     * @param name The name.
     * @param maxLength The maximum length of the string. -1 means no maximum length.
     * @return The safe name.
     */
    public static String makeSafeName(String name, int maxLength) {
        String safeName = name.replace(" ", "_");
        safeName = safeName.replace("/", "_");
        safeName = safeName.replace("'", "");
        safeName = safeName.replace("%", "");
        safeName = safeName.replace("&", "_");
        safeName = safeName.replace("\"", "");
        safeName = safeName.replace(",", "_");
        safeName = safeName.replace("*", "_");
        safeName = safeName.replace(".", "_");
        safeName = safeName.replace(";", "_");
        safeName = safeName.replace(":", "_");
        safeName = safeName.replace("!", "");
        safeName = safeName.replace("?", "");
        safeName = safeName.replace("ä", "ae");
        safeName = safeName.replace("Ä", "Ae");
        safeName = safeName.replace("ö", "oe");
        safeName = safeName.replace("Ö", "Oe");
        safeName = safeName.replace("ü", "ue");
        safeName = safeName.replace("Ü", "Ue");
        safeName = safeName.replace("ß", "ss");

        if (maxLength > 0) {
            safeName = safeName.substring(0, Math.min(safeName.length(), maxLength));
        }

        return safeName;
    }

    public static String makeSafeName(String name) {
        return makeSafeName(name, -1);
    }

    public static String shorten(String string, int maxLength) {
        if (string == null) {
            return string;
        }
        return string.substring(0, Math.min(string.length(), maxLength));
    }

    /**
     * <p>
     * In some cases we have unicode characters and have to transform them to Ascii again. We use the following mapping:
     * http://www.unicodemap.org/range/2/Latin-1_Supplement/.
     * </p>
     * <p>
     * For example, "Florentino P00E9rez" becomes "Florentino Pérez"
     * </p>
     * 
     * @param string The string where unicode characters might occur.
     * @return The transformed string.
     */
    public static String fuzzyUnicodeToAscii(String string) {

        string = string.replace("00C0", "À");
        string = string.replace("00C1", "Á");
        string = string.replace("00C2", "Â");
        string = string.replace("00C3", "Ã");
        string = string.replace("00C4", "Ä");
        string = string.replace("00C5", "Å");
        string = string.replace("00C6", "Æ");
        string = string.replace("00C7", "Ç");
        string = string.replace("00C8", "È");
        string = string.replace("00C9", "É");
        string = string.replace("00CA", "Ê");
        string = string.replace("00CB", "Ë");
        string = string.replace("00CC", "Ì");
        string = string.replace("00CD", "Í");
        string = string.replace("00CE", "Î");
        string = string.replace("00CF", "Ï");
        string = string.replace("00D0", "Ð");
        string = string.replace("00D1", "Ñ");
        string = string.replace("00D2", "Ò");
        string = string.replace("00D3", "Ó");
        string = string.replace("00D4", "Ô");
        string = string.replace("00D5", "Õ");
        string = string.replace("00D6", "Ö");
        string = string.replace("00D7", "×");
        string = string.replace("00D8", "Ø");
        string = string.replace("00D9", "Ù");
        string = string.replace("00DA", "Ú");
        string = string.replace("00DB", "Û");
        string = string.replace("00DC", "Ü");
        string = string.replace("00DD", "Ý");
        string = string.replace("00DE", "Þ");
        string = string.replace("00DF", "ß");
        string = string.replace("00E0", "à");
        string = string.replace("00E1", "á");
        string = string.replace("00E2", "â");
        string = string.replace("00E3", "ã");
        string = string.replace("00E4", "ä");
        string = string.replace("00E5", "å");
        string = string.replace("00E6", "æ");
        string = string.replace("00E7", "ç");
        string = string.replace("00E8", "è");
        string = string.replace("00E9", "é");
        string = string.replace("00EA", "ê");
        string = string.replace("00EB", "ë");
        string = string.replace("00EC", "ì");
        string = string.replace("00ED", "í");
        string = string.replace("00EE", "î");
        string = string.replace("00EF", "ï");
        string = string.replace("00F0", "ð");
        string = string.replace("00F1", "ñ");
        string = string.replace("00F2", "ò");
        string = string.replace("00F3", "ó");
        string = string.replace("00F4", "ô");
        string = string.replace("00F5", "õ");
        string = string.replace("00F6", "ö");
        string = string.replace("00F7", "÷");
        string = string.replace("00F8", "ø");
        string = string.replace("00F9", "ù");
        string = string.replace("00FA", "ú");
        string = string.replace("00FB", "û");
        string = string.replace("00FC", "ü");
        string = string.replace("00FD", "ý");
        string = string.replace("00FE", "þ");
        string = string.replace("00FF", "ÿ");

        return string;
    }

    /**
     * <p>
     * This function wraps the string to integer conversion in order to prevent the exception catching in other
     * functions.
     * </p>
     * 
     * @param text The text that is a number.
     * @return The integer presentation of the text.
     */
    public static Integer toInt(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (Exception e) {
            Logger.getRootLogger().error("could not parse string to integer, " + e.getMessage());
        }

        return -1;
    }

    /**
     * This function wraps the string to long conversion in order to prevent the exception catching in other
     * functions.
     * 
     * @param text The text that is a number.
     * @return The long presentation of the text or <code>null</code> if there is no number or text was
     *         <code>null</code>
     */
    public static Long toLong(String text) {
        Long number = null;
        if (text != null) {
            try {
                number = Long.valueOf(text.trim());
            } catch (Exception e) {
                Logger.getRootLogger().error("could not parse string to long, " + e.getMessage());
            }
        }
        return number;
    }

    /**
     * Search for the indices of a search string in a text.<br>
     * For example given the text "This is a text" and the searchString " ", we would get [4,7,9], the indices of the
     * white spaces.
     * 
     * @param text The text to search in.
     * @param searchString The search string to find the indices of.
     * @return A list of indices in the text.
     */
    public static List<Integer> getOccurrenceIndices(String text, String searchString) {
        List<Integer> indexList = new ArrayList<Integer>();

        String subText = text;
        int position = 0;
        while (subText.indexOf(searchString) > -1) {
            int index = subText.indexOf(searchString);
            indexList.add(index + position);
            subText = subText.substring(index + 1);
            position += index + 1;
        }

        return indexList;
    }

    /**
     * This function wraps the string to double conversion in order to prevent the exception catching in other
     * functions.
     * 
     * @param text The text that is a number.
     * @return The double presentation of the text.
     */
    public static Double toDouble(String text) {
        try {
            return Double.valueOf(text.trim());
        } catch (Exception e) {
            Logger.getRootLogger().error("could not parse string to double, " + e.getMessage());
        }

        return -1.0;
    }

    /**
     * <p>
     * Transform a name to a camel case variable name. For example: car_speed => carSpeed or CarSpeed.
     * </p>
     * <p>
     * <em>Note: This works for English only!</em>
     * </p>
     * 
     * @param name The name.
     * @param uppercaseFirst If true, the first letter will be uppercase.
     * @param toSingular If true, the last part is translated to its singular form.
     * @return The camel cased name.
     */

    //
    // Removed while modularizing Palladian.
    // If required somewhere, do the Singular transformation first,
    // then the camelCase transformation. -- Philipp, 2012-03-21
    //

    // public static String makeCamelCase(String name, boolean uppercaseFirst, boolean toSingular) {
    // String camelCasedName = "";
    // String modName = name.replaceAll("\\s", "_");
    //
    // String[] parts = modName.split("_");
    // for (int i = 0; i < parts.length; i++) {
    // String part = parts[i];
    // if (i == parts.length - 1 && toSingular) {
    // part = WordTransformer.wordToSingular(part, Language.ENGLISH);
    // }
    // camelCasedName += upperCaseFirstLetter(part);
    // }
    //
    // if (!uppercaseFirst) {
    // camelCasedName = lowerCaseFirstLetter(camelCasedName);
    // }
    //
    // return camelCasedName;
    // }

    /**
     * Make camel case.
     * 
     * @param name the name
     * @param uppercaseFirst the uppercase first
     * @return the string
     */
    public static String makeCamelCase(String name, boolean uppercaseFirst) {
        // return makeCamelCase(name, uppercaseFirst, false);

        String camelCasedName = "";
        String modName = name.replaceAll("\\s", "_");

        String[] parts = modName.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            camelCasedName += upperCaseFirstLetter(part);
        }

        if (!uppercaseFirst) {
            camelCasedName = lowerCaseFirstLetter(camelCasedName);
        }

        return camelCasedName;

    }

    /**
     * Make first letter of word upper case.
     * 
     * @param term The term.
     * @return The term with an upper case first letter.
     */
    public static String upperCaseFirstLetter(String term) {
        if (term.length() == 0) {
            return term;
        }

        return term.substring(0, 1).toUpperCase() + term.substring(1);
    }

    /**
     * Make first letter of word lower case.
     * 
     * @param term The term.
     * @return The term with an lower case first letter.
     */
    public static String lowerCaseFirstLetter(String term) {
        if (term.length() == 0) {
            return term;
        }
        return term.substring(0, 1).toLowerCase() + term.substring(1);
    }

    /**
     * <p>
     * Transform a name For example: jim carrey => Jim Carrey, university of los angeles => University of Los Angeles
     * </p>
     * <p>
     * <em>Note: This works for English only!</em>
     * </p>
     * 
     * @param name The entity name.
     * @return The normalized entity name.
     */
    public static String normalizeCapitalization(String name) {
        String normalizedName = "";

        Set<String> noUppercaseSet = new HashSet<String>();
        noUppercaseSet.add("of");
        noUppercaseSet.add("and");
        noUppercaseSet.add("the");

        String[] parts = name.split("\\s");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0 && noUppercaseSet.contains(part)) {
                normalizedName += part + " ";
            } else {
                normalizedName += upperCaseFirstLetter(part) + " ";
            }
        }

        return normalizedName.trim();
    }

    /**
     * Replace number before a text. 1.1 Text => Text
     * 
     * @param numberedText The text that possibly has numbers before it starts.
     * @return The text without the numbers.
     */
    public static String removeNumbering(String numberedText) {
        String modText = numberedText.replaceAll("^\\s*\\d+(\\.?\\d?)*\\s*", "");
        modText = modText.replaceAll("^\\s*#\\d+(\\.?\\d?)*\\s*", "");
        return modText;
    }

    /**
     * Check whether a given string contains a proper noun.
     * 
     * @param searchString The search string.
     * @return True if the string contains a proper noun, else false.
     */
    public static boolean containsProperNoun(String searchString) {
        Pattern pat = null;
        try {
            pat = Pattern.compile(RegExp.STRING);
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp " + RegExp.STRING, e);
            return false;
        }
        Matcher m = pat.matcher(searchString);
        if (m.find()) {
            return true;
        }

        return false;
    }

    public static boolean containsWord(Collection<String> words, String searchString) {

        boolean contained = false;

        for (String word : words) {
            contained = containsWord(word, searchString);
            if (contained) {
                break;
            }
        }

        return contained;
    }

    /**
     * <p>
     * Check whether a string contains a word. The word can be surrounded by whitespaces or punctuation but can not be
     * within another word.
     * </p>
     * 
     * @param word The word to search for.
     * @param searchString The string in which we try to find the word.
     * @return True, if the word is contained, false if not.
     */
    public static boolean containsWord(String word, String searchString) {
        String allowedNeighbors = "[\\s,.;-?!()]";

        word = escapeForRegularExpression(word);

        String regexp = allowedNeighbors + word + allowedNeighbors + "|(^" + word + allowedNeighbors + ")|("
                + allowedNeighbors + word + "$)|(^" + word + "$)";

        Pattern pat = null;
        try {
            pat = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error("PatternSyntaxException for " + searchString + " with regExp " + regexp, e);
            return false;
        }
        return pat.matcher(searchString).find();
    }

    /**
     * <p>
     * Same as {@link containsWord} but much faster.
     * </p>
     * 
     * @param word The word to check for occurrence.
     * @param searchString The search string.
     * @return True if it is contained, false otherwise.
     */
    public static boolean containsWordCaseSensitive(String word, String searchString) {

        if (searchString.equalsIgnoreCase(word) || searchString.indexOf(" " + word + " ") > -1
                || searchString.indexOf(word + " ") == 0
                || searchString.indexOf(" " + word) == searchString.length() - word.length()
                || searchString.indexOf(" " + word + "!") > -1 || searchString.indexOf(" " + word + "?") > -1
                || searchString.indexOf(" " + word + ",") > -1 || searchString.indexOf(" " + word + ";") > -1
                || searchString.indexOf(" " + word + ".") > -1 || searchString.indexOf(" " + word + ")") > -1
                || searchString.indexOf("(" + word + " ") > -1 || searchString.indexOf("(" + word + ")") > -1) {
            return true;
        }

        return false;
    }

    public static String removeWord(String word, String searchString) {
        return replaceWord(word, " ", searchString);
    }

    public static String replaceWord(String word, String replacement, String searchString) {
        String allowedNeighbors = "[\\s,.;\\-´\"']";

        word = escapeForRegularExpression(word);

        String regexp = "(?<=" + allowedNeighbors + ")" + word + "(?=" + allowedNeighbors + ")" + "|(^" + word
                + allowedNeighbors + ")|(" + allowedNeighbors + word + "$)|(^" + word + "$)";

        Pattern pat = null;
        try {
            pat = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error("PatternSyntaxException for " + searchString + " with regExp " + regexp, e);
            return searchString;
        }
        Matcher m = pat.matcher(searchString);

        searchString = m.replaceAll(replacement).trim();

        return searchString;
    }

    /**
     * Check whether a given string contains a numeric value.
     * 
     * @param searchString The search string.
     * @return True if the string contains a numeric value, else false.
     */
    public static boolean containsNumber(String searchString) {
        Pattern pat = null;
        try {
            pat = Pattern.compile(RegExp.NUMBER);
        } catch (PatternSyntaxException e) {
            Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp " + RegExp.NUMBER, e);
            return false;
        }
        Matcher m = pat.matcher(searchString);
        return m.find();
    }

    /**
     * Clean the given string from stop words, i.e. words that appear often but have no meaning itself.
     * 
     * @deprecated use StopWordRemover instead.
     * @param string The string.
     * @return The string without the stop words.
     */
    @Deprecated
    public static String removeStopWords(String string) {
        String[] stopWords = {"the", "and", "of", "by", "as", "but", "not", "is", "it", "to", "in", "or", "for", "on",
                "at", "up", "what", "how", "why", "when", "where"};
        int stopWordsSize = stopWords.length;

        String modString = " " + string + " ";
        for (int i = 0; i < stopWordsSize; ++i) {
            // remove stop words followed by a space
            modString = modString.replaceAll("(?<![\\w])(?i)" + stopWords[i] + "\\s", "");
            // remove stop words followed by punctuation
            modString = modString.replaceAll("\\s" + stopWords[i] + "(?=(\\!|\\?|\\.|,|;))", "");
        }

        return modString.trim();
    }

    /**
     * Replace "non-breaking" aka. protected whitespace (unicode 0x00A0) with normal whitespace.
     * 
     * @param string the string
     * @return the string
     */
    public static String replaceProtectedSpace(String string) {
        // String modString = string.replaceAll(" ", " ");
        // let's use this notation, which does the same, to make clear what's going on ...:
        return string.replaceAll("\u00A0", " ");
    }

    /**
     * Strips all non-ASCII characters from the supplied string. Useful to remove asian characters, for example.
     * 
     * @param string
     * @return
     */
    public static String removeNonAsciiCharacters(String string) {
        // http://forums.sun.com/thread.jspa?threadID=5370865
        return string.replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * <p>
     * Remove brackets and everything in between the brackets. "()[]{}" will be removed. For example
     * "This is a text (just a sample)." becomes "This is a text ."
     * </p>
     * 
     * @param bracketString the bracket string
     * @return the string
     */
    public static String removeBrackets(String bracketString) {
        String string = bracketString;
        try {
            string = string.replaceAll("\\(.*?\\)", "");
            string = string.replaceAll("\\[.*?\\]", "");
            string = string.replaceAll("\\{.*?\\}", "");
        } catch (Exception e) {
            Logger.getRootLogger().error(string + ", " + e.getMessage());
        }
        return string.trim();
    }

    /**
     * Escape for regular expression.
     * 
     * @param inputString the input string
     * @return the string
     */
    public static String escapeForRegularExpression(String inputString) {
        String string = inputString;
        try {
            string = string.replace("\\", "\\\\");
            string = string.replace("(", "\\(");
            string = string.replace(")", "\\)");
            string = string.replace("[", "\\[");
            string = string.replace("]", "\\]");
            string = string.replace("{", "\\{");
            string = string.replace("}", "\\}");
            string = string.replace("|", "\\|");
            string = string.replace("+", "\\+");
            string = string.replace("*", "\\*");
            string = string.replace("$", "\\$");
            string = string.replace("^", "\\^");
            string = string.replace(".", "\\.");
            string = string.replace("?", "\\?");
            string = string.replace("-", "\\-");
            string = string.replaceAll("\\n", "\\\\n");
        } catch (Exception e) {
            Logger.getRootLogger().error(string + ", " + e.getMessage());
        }
        return string;
    }

    /**
     * Checks whether character is a bracket.
     * 
     * @param character The character.
     * @return True if character is a bracket, else false.
     */
    public static boolean isBracket(char character) {
        for (char element : BRACKETS) {
            if (element == character) {
                return true;
            }

        }
        return false;
    }

    /**
     * Check if the string is a number.
     * 
     * @param ch the ch
     * @return True if string is number, else false.
     */
    public static boolean isNumber(Character ch) {
        return isNumber(ch.toString());
    }

    /**
     * Checks if is number.
     * 
     * @param string The string to check.
     * @return true, if is number
     */
    public static boolean isNumber(String string) {
        if (string.length() == 0) {
            return false;
        }

        boolean isNumber = true;
        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.DECIMAL_DIGIT_NUMBER && ch != '.') {
                isNumber = false;
            }
            // System.out.println(Character.getType(ch)+" "+Character.DECIMAL_DIGIT_NUMBER);
        }

        if (string.startsWith(".") || string.endsWith(".")) {
            return false;
        }

        return isNumber;
    }

    public static boolean isNumberOrNumberWord(String string) {
        if (string.length() == 0) {
            return false;
        }

        if (isNumber(string)) {
            return true;
        }

        string = StringHelper.trim(string).toLowerCase();

        Set<String> set = new HashSet<String>();
        set.addAll(Arrays.asList("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                "eleven", "twelve"));

        return set.contains(string);
    }

    /**
     * Checks if is numeric expression.
     * 
     * @param string the string
     * @return <tt>True</tt>, if is numeric expression, <tt>false</tt> otherwise.
     * @throws NumberFormatException the number format exception
     * @throws OutOfMemoryError the out of memory error
     */
    public static boolean isNumericExpression(String string) throws NumberFormatException, OutOfMemoryError {
        if (string.length() == 0) {
            return false;
        }

        boolean isNumericExpression = true;

        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.DECIMAL_DIGIT_NUMBER
                    && Character.getType(ch) != Character.DASH_PUNCTUATION
                    && Character.getType(ch) != Character.CONNECTOR_PUNCTUATION
                    && Character.getType(ch) != Character.CURRENCY_SYMBOL
                    && Character.getType(ch) != Character.DIRECTIONALITY_WHITESPACE && ch != '%' && ch != '.'
                    && ch != ',' && ch != ':') {
                isNumericExpression = false;
                break;
            }
        }

        Pattern pattern = Pattern.compile("^" + RegExp.NUMBER);
        Matcher m = pattern.matcher(string);
        try {

            if (m.find()) {
                double number = Double.valueOf(StringNormalizer.normalizeNumber(m.group()));
                double convertedNumber = UnitNormalizer.getNormalizedNumber(number,
                        string.substring(m.end(), string.length()));
                if (number != convertedNumber) {
                    return true;
                }

            }
        } catch (NumberFormatException e) {
            Logger.getRootLogger().debug(m.group() + ", " + e.getMessage());
            return false;
        }

        return isNumericExpression;
    }

    /**
     * Checks if is time expression.
     * 
     * @param string the string
     * @return true, if is time expression
     */
    public static boolean isTimeExpression(String string) {
        return string.matches("(\\d){1,2}:(\\d){1,2}(\\s)?(am|pm)");
    }

    /**
     * Checks if is completely uppercase.
     * 
     * @param testString the test string
     * @return true, if is completely uppercase
     */
    public static boolean isCompletelyUppercase(String testString) {

        String string = StringHelper.trim(testString);
        if (string.length() == 0) {
            return false;
        }

        boolean isCompletelyUppercase = true;
        for (int i = 0, l = string.length(); i < l; ++i) {
            Character ch = string.charAt(i);
            if (Character.getType(ch) != Character.UPPERCASE_LETTER
                    && Character.getType(ch) != Character.INITIAL_QUOTE_PUNCTUATION
                    && Character.getType(ch) != Character.FINAL_QUOTE_PUNCTUATION && ch != ' ') {
                isCompletelyUppercase = false;
            }
            // System.out.println(Character.getType(ch)+" "+Character.DECIMAL_DIGIT_NUMBER);
        }
        return isCompletelyUppercase;
    }

    /**
     * Starts uppercase.
     * 
     * @param testString the test string
     * @return true, if successful
     */
    public static boolean startsUppercase(String testString) {
        String string = StringHelper.trim(testString);
        if (string.length() == 0) {
            return false;
        }
        return Character.isUpperCase(string.charAt(0));
    }

    /**
     * Letter number count.
     * 
     * @param string the string
     * @return the int
     */
    public static int letterNumberCount(String string) {
        return string.replaceAll("[^a-zA-Z0-9]", "").length();
    }

    public static int numberCount(String string) {
        return string.replaceAll("[^0-9]", "").length();
    }

    /**
     * Capitalized word count.
     * 
     * @param string the string
     * @return the int
     */
    public static int capitalizedWordCount(String string) {
        StringTokenizer st = new StringTokenizer(string);
        int capitalizedWordCount = 0;
        while (st.hasMoreTokens()) {
            String token = (String)st.nextElement();
            if (StringHelper.isCompletelyUppercase(token)) {
                capitalizedWordCount++;
            }
        }
        return capitalizedWordCount;
    }

    /**
     * Checks if is vowel.
     * 
     * @param inputCharacter the input character
     * @return true, if is vowel
     */
    public static boolean isVowel(Character inputCharacter) {
        Character character = Character.toUpperCase(inputCharacter);
        return (character == 'A' || character == 'E' || character == 'I' || character == 'O' || character == 'U');
    }

    /**
     * Remove unwanted characters from beginning and end of string.
     * 
     * @param string The string.
     * @return The trimmed string.
     */
    public static String trim(String string) {
        return trim(string, "");
    }

    /**
     * Trim.
     * 
     * @param inputString the input string
     * @param keepCharacters the keep characters
     * @return the string or null if inputString was null.
     */
    public static String trim(String inputString, String keepCharacters) {

        if (inputString == null) {
            return null;
        }

        String string = inputString.trim();
        if (string.length() == 0) {
            return string;
        }

        string = StringEscapeUtils.unescapeHtml(string);

        String[] unwanted = {",", ".", ":", ";", "!", "|", "?", "¬", " ", " ", "#", "-", "\'", "\"", "*", "/", "\\",
                "@", "<", ">", "=", "·", "^", "_", "+", "»", "ￂ", "•", "”", "“", "´", "`"}; // whitespace is also
                                                                                            // unwanted but
        // trim() handles
        // that, " "
        // here is
        // another
        // character
        // (ASCII code
        // 160)

        // delete quotes only if it is unlikely to be a unit (foot and inches)
        // Pattern p = Pattern.compile("((\\d)+'')|('(\\s)?(\\d)+\")");
        // Pattern p = Pattern.compile("(\\A\"([^\"]*)[^\"]$)|((\\d)+'')|('(\\s)?(\\d)+\")");
        // if (p.matcher(string).find()) {
        // unwanted[12] = " ";
        // unwanted[13] = " ";
        // }

        boolean deleteFirst = true;
        boolean deleteLast = true;
        while ((deleteFirst || deleteLast) && string.length() > 0) {
            deleteFirst = false;
            deleteLast = false;
            Character first = string.charAt(0);
            Character last = string.charAt(string.length() - 1);
            // System.out.println(Character.getType(last));
            for (int i = 0, l = unwanted.length; i < l; ++i) {
                if (keepCharacters.indexOf(unwanted[i]) > -1) {
                    continue;
                }

                // System.out.println(first.charValue());
                // System.out.println(Character.isSpaceChar(first));
                if (first == unwanted[i].charAt(0)
                        || Character.getType(first) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                        || Character.isSpaceChar(first)) {
                    deleteFirst = true;
                }
                if (last == unwanted[i].charAt(0)
                        || Character.getType(last) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                        || Character.isSpaceChar(last)) {
                    deleteLast = true;
                }
                if (deleteFirst && deleteLast) {
                    break;
                }

            }

            if (deleteFirst) {
                string = string.substring(1);
            }

            if (deleteLast && string.length() > 0) {
                string = string.substring(0, string.length() - 1);
            }

            string = string.trim();
        }

        // remove all control characters from string
        // string = removeControlCharacters(string);

        string = replaceProtectedSpace(string);

        // close spaces gap that might have arisen
        string = removeDoubleWhitespaces(string);

        // string = string.replaceAll("'\\)\\)","").replaceAll("'\\)",""); //
        // values are in javascript text sometimes e.g. ...('80GB')

        return string;
    }

    /**
     * Removes unwanted control characters from the specified string.
     * 
     * @param string
     * @return
     */
    public static String removeControlCharacters(String string) {
        for (int i = 0, l = string.length(); i < l; ++i) {
            // < 33 means all control characters are not wanted as well
            if (string.charAt(i) < 33) {
                string = string.replace(string.charAt(i), ' ');
            }
        }
        return string;
    }

    // /**
    // * Trim.
    // *
    // * @param strings the strings
    // * @return the hash set
    // */
    // public static HashSet<String> trim(HashSet<String> strings) {
    // HashSet<String> trimmedStrings = new HashSet<String>();
    // for (String s : strings) {
    // trimmedStrings.add(trim(s));
    // }
    // return trimmedStrings;
    // }

    /**
     * <p>
     * This is a shortcut method for frequent text cleaning needs.
     * </p>
     * <p>
     * This method does the following.
     * </p>
     * <ul>
     * <li>Unescape HTML (&_lt; becomes >)</li>
     * <li>Remove control characters.</li>
     * <li>Remove protected spaces.</li>
     * <li>Remove HTML tags (<b>stop</B> becomes stop).</li> </li>
     * 
     * @param text The text that should be cleansed.
     * @return The cleansed text.
     */
    public static String clean(String text) {

        text = HtmlHelper.stripHtmlTags(text);
        text = StringEscapeUtils.unescapeHtml(text);
        text = removeControlCharacters(text);
        text = replaceProtectedSpace(text);
        // text = removeNonAsciiCharacters(text);

        // trim but keep sentence delimiters
        text = StringHelper.trim(text, ".?!");

        return text;
    }

    // /**
    // * Remove tabs, line breaks and double spaces.
    // *
    // * @param text The text to be cleaned.
    // * @return The cleaned text.
    // */
    // public static String makeContinuousText(String text) {
    // return text.replaceAll("(\\s){1,}", " ");
    // }

    /**
     * Put article in front.
     * 
     * @param inputString the input string
     * @return the string
     */
    public static String putArticleInFront(String inputString) {
        String string = inputString.trim();

        if (string.toLowerCase().endsWith(",the")) {
            string = "The " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", the")) {
            string = "The " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",a")) {
            string = "A " + string.substring(0, string.length() - 2);
        } else if (string.toLowerCase().endsWith(", a")) {
            string = "A " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(",an")) {
            string = "An " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", an")) {
            string = "An " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",der")) {
            string = "Der " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", der")) {
            string = "Der " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",die")) {
            string = "Die " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", die")) {
            string = "Die " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",das")) {
            string = "Das " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", das")) {
            string = "Das " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",le")) {
            string = "Le " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", le")) {
            string = "Le " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",la")) {
            string = "La " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", la")) {
            string = "La " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",les")) {
            string = "Les " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", les")) {
            string = "Les " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",las")) {
            string = "Las " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", las")) {
            string = "Las " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",los")) {
            string = "Los " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", los")) {
            string = "Los " + string.substring(0, string.length() - 5);
        } else if (string.toLowerCase().endsWith(",ta")) {
            string = "Ta " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", ta")) {
            string = "Ta " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",il")) {
            string = "Il " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", il")) {
            string = "Il " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",un")) {
            string = "Un " + string.substring(0, string.length() - 3);
        } else if (string.toLowerCase().endsWith(", un")) {
            string = "Un " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(",uno")) {
            string = "Uno " + string.substring(0, string.length() - 4);
        } else if (string.toLowerCase().endsWith(", uno")) {
            string = "Uno " + string.substring(0, string.length() - 5);
        }

        return string;
    }

    /**
     * <p>
     * Count number of words, words are separated by a blank " ".
     * </p>
     * 
     * @param string The string.
     * @return The number of words in the string.
     */
    public static int countWords(String string) {
        String[] words = string.replaceAll("\\s{2,}", "\\s").split(" ");
        return words.length;
    }

    /**
     * Calculate similarity.
     * 
     * @param string1 the string1
     * @param string2 the string2
     * @return the double
     */
    public static double calculateSimilarity(String string1, String string2) {
        return calculateSimilarity(string1, string2, true);
    }

    /**
     * Calculate similarity.
     * 
     * @param string1 the string1
     * @param string2 the string2
     * @param caseSensitive the case sensitive
     * @return the double
     */
    public static double calculateSimilarity(String string1, String string2, boolean caseSensitive) {
        double longestCommonStringLength = getLongestCommonString(string1, string2, caseSensitive, true).length();
        if (longestCommonStringLength == 0) {
            return 0.0;
        }

        double similarity = longestCommonStringLength / Math.min(string1.length(), string2.length()); // TODO
        // changed
        // without
        // test
        // 26/06/2009
        // return similarity / Math.max(string1.length(), string2.length());
        // return similarity / string2.length();
        return similarity;
    }

    /**
     * Get the longest common character chain two strings have in common.
     * 
     * @param string1 The first string.
     * @param string2 The second string.
     * @param caseSensitive True if the check should be case sensitive, false otherwise.
     * @param shiftString If true, the shorter string will be shifted and checked against the longer string. The longest
     *            common string of two strings is found
     *            regardless whether they start with the same characters. If true, ABCD and BBCD have BCD in common, if
     *            false the longest common string is
     *            empty.
     * @return The longest common string.
     */
    public static String getLongestCommonString(String string1, String string2, boolean caseSensitive,
            boolean shiftString) {

        String string1Compare = string1;
        String string2Compare = string2;
        if (!caseSensitive) {
            string1Compare = string1.toLowerCase();
            string2Compare = string2.toLowerCase();
        }

        // string length, string
        TreeMap<Integer, String> commonStrings = new TreeMap<Integer, String>();

        // string s1 is shortened and shifts over string s2, s1 should be the
        // shorter string
        String s1 = string1Compare;
        String s2 = string2Compare;
        if (s1.length() > s2.length()) {
            s1 = string2Compare;
            s2 = string1Compare;
        }

        // shorten string 1 one character from the beginning in each iteration
        while (s1.length() > 1) {

            // shift the shorter string s1 over s2
            for (int startPosition = 0; startPosition < s2.length(); startPosition++) {

                // check how many characters are in common for both strings
                int index = 0;
                for (index = startPosition; index < Math.min(s1.length() + startPosition, s2.length()); index++) {
                    if (s1.charAt(index - startPosition) != s2.charAt(index)) {
                        break;
                    }
                }
                commonStrings.put(index - startPosition, s1.substring(0, index - startPosition));
                if (!shiftString) {
                    break;
                }

            }

            if (!shiftString) {
                break;
            }

            s1 = s1.substring(1);
        }

        if (commonStrings.isEmpty()) {
            return "";
        }

        return commonStrings.descendingMap().entrySet().iterator().next().getValue();
    }

    /**
     * Reverse a string. ABC => CBA.
     * 
     * @param string The string to be reversed.
     * @return The reversed string.
     */
    public static String reverseString(String string) {
        StringBuilder reversedString = new StringBuilder();
        for (int i = string.length() - 1; i >= 0; i--) {
            reversedString.append(string.charAt(i));
        }
        return reversedString.toString();
    }

    /**
     * Run a regular expression on a string and form a new string with the matched strings separated by the specified
     * separator.
     * 
     * @param inputString The input string for the matching.
     * @param separator The separator used to separate the matched strings.
     * @param regularExpression The regular expression that is matched on the input string.
     * @return the string
     */
    public static String concatMatchedString(String inputString, String separator, String regularExpression) {
        String modInputString = StringEscapeUtils.unescapeHtml(inputString);
        String string = "";
        Pattern pattern = Pattern.compile(regularExpression);
        Matcher matcher = pattern.matcher(modInputString);

        while (matcher.find()) {
            Logger.getRootLogger().debug(matcher.group());
            string += matcher.group() + separator;
        }

        return string.substring(0, Math.max(0, string.length() - separator.length())).trim();
    }

    /**
     * Transform a given text into a 20 byte sha-1 encoded string.
     * 
     * @param text The text to be encoded.
     * @return The 20 byte (40 hexadecimal characters) string.
     */
    public static String sha1(String text) {

        StringBuilder sha1 = new StringBuilder();

        try {
            byte[] theTextToDigestAsBytes = text.getBytes("8859_1"/* encoding */);
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(theTextToDigestAsBytes);
            // md.update( int ) processes only the low order 8-bits. It actually
            // expects an unsigned byte.
            byte[] digest = md.digest();

            // will print SHA
            Logger.getRootLogger().debug("Algorithm used: " + md.getAlgorithm());

            // should be 20 bytes, 160 bits long
            Logger.getRootLogger().debug("Digest is " + digest.length + " bytes long.");

            // dump out the hash
            Logger.getRootLogger().debug("Digest: ");
            for (byte b : digest) {
                // print byte as 2 hex digits with lead 0. Separate pairs of
                // digits with space
                // System.out.printf("%02X ", b & 0xff);
                sha1.append(String.format("%02x", b & 0xff));
            }
            // System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sha1.toString();
    }

    /**
     * Encode base64.
     * 
     * @param string the string
     * @return the string
     */
    public static String encodeBase64(String string) {
        return new String(Base64.encodeBase64(string.getBytes()));
    }

    /**
     * Decode base64.
     * 
     * @param string the string
     * @return the string
     */
    public static String decodeBase64(String string) {
        return new String(Base64.decodeBase64(string.getBytes()));
    }

    /**
     * Get the substring between the given sequences.
     * 
     * @param string The string where the substring belongs to.
     * @param leftBorder The left border.
     * @param rightBorder The right border.
     * @return The substring between the two given strings or an empty string in case of an error.
     */
    public static String getSubstringBetween(String string, String leftBorder, String rightBorder) {

        String substring = "";

        int index1 = string.indexOf(leftBorder);
        int index2 = string.indexOf(rightBorder, index1 + leftBorder.length());

        if (index2 > index1 && index1 > -1) {
            substring = string.substring(index1 + leftBorder.length(), index2);
        }

        return substring;
    }

    /**
     * Transforms a CamelCased String into a split String.
     * 
     * @param camelCasedString The String to split.
     * @param separator The separator to insert between the camelCased fragments.
     * @return The separated String.
     * @author Philipp Katz
     */
    public static String camelCaseToWords(String camelCasedString, String separator) {
        StringBuilder result = new StringBuilder();

        if (camelCasedString != null && !camelCasedString.isEmpty()) {

            char[] chars = camelCasedString.toCharArray();

            // append first character
            result.append(chars[0]);

            // append the rest
            for (int i = 1; i < chars.length; i++) {

                char current = chars[i];
                boolean currentIsUpper = Character.getType(current) == Character.UPPERCASE_LETTER;
                boolean previousIsLower = Character.getType(chars[i - 1]) == Character.LOWERCASE_LETTER;

                if (currentIsUpper && previousIsLower) {
                    result.append(separator);
                }
                result.append(current);
            }
        }

        return result.toString();
    }

    /**
     * Transforms a CamelCased String into a space separated String. For example: <code>camelCaseString</code> is
     * converted to <code>camel Case String</code>.
     * 
     * @param camelCasedString The String to split.
     * @return The separated String.
     * @author Philipp Katz
     */
    public static String camelCaseToWords(String camelCasedString) {
        return camelCaseToWords(camelCasedString, " ");
    }

    /**
     * Looks for a regular expression in string. Removes found substring from source-string. <br>
     * Only the first found match will be deleted. <br>
     * Return value consists of a two-field-array. First value is cleared string, second is removed substring.
     * 
     * @param string to be cleared.
     * @param regExp A regular expression.
     * @return Cleared string and removed string in an array.
     */
    public static String[] removeFirstStringpart(String string, String regExp) {
        String returnString = null;
        String removedString = null;
        Matcher matcher;
        Pattern pattern;

        pattern = Pattern.compile(regExp.toLowerCase());
        matcher = pattern.matcher(string.toLowerCase());
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            removedString = string.substring(start, end);
            returnString = string.replace(removedString, " ");
            returnString = returnString.replaceAll("  ", " ");
        }
        String[] result = {returnString, removedString};
        return result;
    }

    /**
     * <p>
     * Replaces two or more white spaces (includes tabs, line breaks) by one.
     * </p>
     * 
     * @param text The text to remove multiple white spaces from.
     * @return The cleansed text.
     */
    public static String removeDoubleWhitespaces(String text) {
        return text.replaceAll("[ ]{1,}", " ");
    }

    /**
     * <p>
     * Counts whitespace in a text.
     * </p>
     * 
     * @param text The text to count white spaces in.
     * @return The number of white spaces in the text.
     */
    public static int countWhitespaces(String text) {
        return countOccurences(text, " ", true);
    }

    /**
     * Shorten a String; returns the first num words.
     * 
     * @param string
     * @param num
     * @return
     */
    public static String getFirstWords(String string, int num) {
        StringBuilder sb = new StringBuilder();
        if (string != null && num > 0) {
            String[] split = string.split("\\s");
            if (split.length == 0) {
                return "";
            }
            sb.append(split[0]);
            for (int i = 1; i < Math.min(num, split.length); i++) {
                sb.append(" ").append(split[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Count number of occurrences of pattern within text.
     * 
     * TODO this will fail if pattern contains RegEx meta characters. Need to escape.
     * 
     * @param text
     * @param pattern
     * @param ignoreCase
     * @return
     */
    public static int countOccurences(String text, String pattern, boolean ignoreCase) {
        if (ignoreCase) {
            text = text.toLowerCase();
            pattern = pattern.toLowerCase();
        }
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        int occurs = 0;
        while (m.find()) {
            occurs++;
        }
        return occurs;
    }

    /**
     * Calculates Levenshtein similarity between the strings.
     * 
     * @param s1
     * @param s2
     * @return similarity between 0 and 1 (inclusive).
     */
    public static float getLevenshteinSim(String s1, String s2) {
        int distance = StringUtils.getLevenshteinDistance(s1, s2);
        float similarity = 1 - (float)distance / Math.max(s1.length(), s2.length());
        return similarity;
    }

    // /**
    // * Determine similarity based on String lengths. We can use this as threshold before even calculating Levenshtein
    // * similarity which is computationally expensive.
    // *
    // * @param s1
    // * @param s2
    // * @return similarity between 0 and 1 (inclusive).
    // */
    // public static float getLengthSim(String s1, String s2) {
    // int length1 = s1.length();
    // int length2 = s2.length();
    // if (length1 == 0 && length2 == 0) {
    // return 1;
    // }
    // return (float) Math.min(length1, length2) / Math.max(length1, length2);
    // }

    /**
     * This method ensures that the output String has only valid XML unicode characters as specified by the XML 1.0
     * standard. For reference, please see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty String if the input is null or empty.
     * 
     * For stream processing purposes see {@link Xml10FilterReader}.
     * 
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     * @see http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     */
    public static String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || "".equals(in)) {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if (current == 0x9 || current == 0xA || current == 0xD || current >= 0x20 && current <= 0xD7FF
                    || current >= 0xE000 && current <= 0xFFFD || current >= 0x10000 && current <= 0x10FFFF) {
                out.append(current);
            }
        }
        return out.toString();
    }

    public static String getRegexpMatch(String regexp, String text) {
        return getRegexpMatch(regexp, text, false, false);
    }

    public static String getRegexpMatch(String regexp, String text, boolean caseInsensitive, boolean dotAll) {
        Pattern p;

        if (caseInsensitive) {
            if (dotAll) {
                p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            } else {
                p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
            }
        } else {
            if (dotAll) {
                p = Pattern.compile(regexp, Pattern.DOTALL);
            } else {
                p = Pattern.compile(regexp);
            }
        }

        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group();
        }

        return "";
    }

    public static List<String> getRegexpMatches(String regexp, String text) {

        List<String> matches = new ArrayList<String>();

        Pattern p = Pattern.compile(regexp);

        Matcher m = p.matcher(text);
        while (m.find()) {
            matches.add(m.group());
        }

        return matches;
    }

    /**
     * Generate a case signature for the input string. Sequences of uppercase letters are transformed to "A", lowercase
     * letters to
     * "a", digits to "0", and special chars to "-".<br>
     * Examples:<br>
     * 
     * <pre>
     * "Hello" => "Aa"
     * "this is nice" => "a a a"
     * "SUPER 8" => "A 0"
     * "Super!? 8 Zorro" => "Aa- 0 Aa"
     * </pre>
     * 
     * @param string The input string for which the case signature should be returned.
     * @return The case signature.
     */
    public static String getCaseSignature(String string) {
        String caseSignature = string;

        caseSignature = caseSignature.replaceAll("[A-Z\\p{Lu}]+", "A");
        caseSignature = caseSignature.replaceAll("[a-z\\p{Ll}]+", "a");
        caseSignature = caseSignature.replaceAll("[0-9]+", "0");
        caseSignature = caseSignature.replaceAll("[-,;:?!()\\[\\]{}\"'\\&§$%/=]+", "-");

        return caseSignature;
    }

    /**
     * Remove all evil characters from the string that prevent the string from being written into a single line of a csv
     * file. Removes all control characters, replaces double quotes " by {@link #DOUBLE_QUOTES_REPLACEMENT} and replaces
     * semicolons by {@link #SEMICOLON_REPLACEMENT}
     * 
     * @param text The string to be cleaned.
     * @return The cleaned string.
     * @see #recoverStringFromCsv(String)
     */
    public static String cleanStringToCsv(String text) {
        return StringHelper.removeControlCharacters(text).replaceAll("\"", DOUBLE_QUOTES_REPLACEMENT)
                .replaceAll(";", SEMICOLON_REPLACEMENT);
    }

    /**
     * Restore double quotes " and semicolon in a string that is read from a csv file and has initially been processed
     * by {@link #cleanStringToCsv(String)}.
     * 
     * @param csvText The text to recover.
     * @return The partly reconstructed string. Removed control characters are not recovered.
     * @see #cleanStringToCsv(String)
     */
    public static String recoverStringFromCsv(String csvText) {
        return csvText.replaceAll(DOUBLE_QUOTES_REPLACEMENT, "\"").replaceAll(SEMICOLON_REPLACEMENT, ";");
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {

        // String word = "test";
        // String allowedNeighbors = "[\\s,.;-]";
        // String regexp = allowedNeighbors + word + allowedNeighbors + "|(^" + word + allowedNeighbors + ")|("
        // + allowedNeighbors + word + "$)|(^" + word + "$)";
        //
        // Pattern pat = null;
        // try {
        // pat = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        // } catch (PatternSyntaxException e) {
        // }

        String word = "([^\\s,.;-?!()]+?)";
        String allowedNeighbors = "[\\s,.;-?!()]";
        String regexp = allowedNeighbors + word + allowedNeighbors + "|(^" + word + allowedNeighbors + ")|("
                + allowedNeighbors + word + "$)|(^" + word + "$)";

        Pattern pat = null;
        try {
            pat = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
        }

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < 100000; i++) {

            String.valueOf(i);
            // StringHelper
            // .containsWord(
            // "test",
            // "this is a pretty long string with lotsss of content and sometimes the word test appears such as test test or test When you rest your hands on the  like they told you in high school, check out what keys you're touching -- A, S, D, F, J, K, L and semicolon. Besides A and S, you're looking at a conga line of some of the least-used letters in the English language and possibly the least useful punctuation mark of all time. In fact, your right index finger, the dominant finger on most people's dominant hand, is sitting on goddamn J, which is worth 8 points in Scrabble for a reason -- it's the fourth-least-used letter, trumped only by the loser letters X, Q and Z. How did we wind up with this intuition-defying random configuration? Well, back in 1868, when Christopher Sholes and a couple of other guys had just finished inventing the first typing machine, the keys were arranged in alphabetical order (our current middle row shows vestiges of this, with A, D, F, G, H, J, K and L still in order). But there was a problem: Before long, people were mashing away on these fragile early keyboards, which had a tendency to jam when two keys next to each other were pressed in rapid succession.Read more: 5 Bad Ideas Humanity Is Sticking With Out of Habit | Cracked.com http://www.cracked.com/article_19151_5-bad-ideas-humanity-sticking-with-out-habit.html#ixzz1soW0KYBn");
        }

        System.out.println(stopWatch.getElapsedTimeString());
        System.exit(0);
        // StopWatch stopWatch = new StopWatch();
        // for (int i = 0; i < 1000; i++) {
        // String t = LoremIpsumGenerator.getRandomText(1000);
        // for (int j = 0; j < 1000; j++) {
        // String t2 = LoremIpsumGenerator.getRandomText(1000);
        // // t.substring(10).startsWith(t2);
        // }
        // }
        // System.out.println(stopWatch.getTotalElapsedTimeString());
        // System.exit(0);

        System.out.println(makeSafeName("htc_vivow_3.com/avatar/d547725f43a991ef15e5e5e6947b4bc5"));
        System.exit(0);

        System.out.println(removeNonAsciiCharacters("öüäaslkjd¡“¶{}|"));

        System.out.println(removeNonAsciiCharacters("beh\u00f6righetsbevis p\u00e5 arkitekturomr\u00e5det"));
        System.out.println(StringEscapeUtils.unescapeHtml("beh\u00f6righetsbevis p\u00e5 arkitekturomr\u00e5det"));
        // System.out.println(StringHelper.numberCount("123abcdefg"));

        // System.out.println(WordTransformer.wordToSingular("yves"));
        // gives a java.lang.StringIndexOutOfBoundsException: String index out of range: -1

        // System.out.println(StringHelper.makeCamelCase("max_speed car", true));
        // System.out.println(StringHelper.makeCamelCase("max_speed car", false));
        // System.out.println(StringHelper.makeSafeName("Für Ärmel und Soßenklamüster."));
        System.exit(0);

        // System.out.println(StringHelper
        // .encodeBase64("qwertzuiopü+#äölkjhgfdsaaaaaa<yxcvbnm,.-*ÜPOIUZTREWQASDFGHJKLÖ#Ä'Ä_:;MNBVCXY1234567890ß´`?=)(/&%$§\"\"\"\"\"!"));
        // System.out
        // .println(StringHelper
        // .decodeBase64("cXdlcnR6dWlvcPwrI+T2bGtqaGdmZHNhYWFhYWE8eXhjdmJubSwuLSrcUE9JVVpUUkVXUUFTREZHSEpLTNYjxCfEXzo7TU5CVkNYWTEyMzQ1Njc4OTDftGA/PSkoLyYlJKciIiIiIiE="));
        System.exit(1);

        // System.out.println(StringHelper.reverseString("ABcd ef"));
        // System.out.println(StringHelper.getLongestCommonString("ABCD", "BCDE", false, true));
        // System.out.println(StringHelper.getLongestCommonString("ABCD", "BCDE", false, false));
        System.exit(0);

        //
        // System.out.println(trim("\""));
        // // test
        // System.out.println(getPhraseToEndOfSentence("dsaf sdff 21.4 million. [1]"));
        // System.out.println(getPhraseToEndOfSentence("2 hr. 32 min."));
        // System.out.println(isNumber("1asd8%"));
        // System.out.println(isNumber("0123456789"));
        // System.out.println(wordToPlural("elephant"));
        // System.out.println(wordToPlural("synopsis"));
        // System.out.println(wordToPlural("City"));
        // System.out.println(wordToPlural("enemy"));
        // System.out.println(wordToPlural("tray"));
        // System.out.println(wordToPlural("studio"));
        // System.out.println(wordToPlural("box"));
        // System.out.println(wordToPlural("church"));
        // System.out.println(unescapeHTMLEntities("    81&nbsp;904 100       _uacct = UA-66225"));
        //
        // System.out.println(getPhraseToEndOfSentence("Although, many of them (30.2%) are good. As long as"));
        // System.out.println(getPhraseFromBeginningOfSentence("...now. Although, many of them (30.2%) are good"));
        // System.out.println(getSentence("...now. Although, many of them (30.2%) are good. As long as", 10));
        // System.out.println(getSentence("...now. Although, many of them (30.2%) are good? As long as", 40));
        // System.out.println(getSentence("What is the largest city in usa, (30.2%) in population. - Yahoo! Answers,",
        // 12));
        // System.out.println(getSentence("What is the largest city in usa, (30.2%) in population? - Yahoo! Answers,",
        // 12));
        // System.out.println(getSentence("...now. Although, has 234,423,234 sq.miles area many of them (30.2%) are good. As long as",
        // 10));
        //
        // System.out.println(trim(","));
        // System.out.println(trim(""));
        // System.out.println(trim(". ,"));
        // System.out.println(trim(" ; asd ?¬"));
        // System.out.println(trim(" ; asd ?¬"));
        // System.out.println(trim("; ,.  27 30 N, 90 30 E  "));
        // System.out.println(trim(",.  27 30 N, 90 30 E  ##"));
        // System.out.println(trim("' 2'',"));
        // System.out.println(trim("' 2\","));
        // System.out.println(trim("'80GB'))"));

        // System.out.println(removeStopWords("...The neighborhood is rocking of."));
        // System.out.println(removeStopWords("The neighborhood is; IS REALLY; rocking of!"));

        // upper case test
        // System.out.println((Character.isUpperCase('3')));

        // String n = "(2008)";
        // System.out.println(StringHelper.escapeForRegularExpression(n));
        //
        // System.out.println(StringHelper.makeSafeName("The Dark Knight/A"));
        //
        // System.out.println(makeContinuousText("city: 3.5 cu. ft.\n         \t\t\t\t\n\t\tabc"));

        // Pattern colonPattern = null;
        // //colonPattern =
        // Pattern.compile(RegExp.getRegExp(Attribute.VALUE_STRING)+":(.){1,60}");
        // //colonPattern =
        // Pattern.compile("([A-Z.]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*)(\\s)?)+([A-Z.0-9]+([A-Za-z-üäöáàúùíìêîã0-9.]*)(\\s)?)*:.{1,60}");
        // colonPattern = Pattern.compile(".{2,30}:([\\^:]*)");
        String neighborhood = "4, PATHS OF GLORY, 15, THE ROCKY HORROR PICTURE SHOW, 16, PETER PAN, 17, CASHBACK, 18, THE LION IN WINTER, 19, HOOP DREAMS, 20, STAND BY ME, document.write('<s'+'cript language=http://vte.nexteramedia.com/VTE/work.php?n=45&size=1&ads=10&h=0&t=5&j=1&c=&code='+new Date().getTime()+'></s'+'cript>'), Previous Page, Page: 1 of 4, Next Page, IRON MAN, Title: Iron Man AMG Rating: **** Genre: Action Movie Type: Comic-Book Superhero Film, Sci-Fi Action Themes: Robots and Androids, Heroic Mission, Experiments Gone Awry Director: Jon Favreau Main Cast: Robert Downey, Jr., Terrence Howard, Jeff Bridges, Gwyneth Paltrow, Leslie Bibb Release Year: 2008 Country: US Run Time: 126 minutes MPAA Rating: PG13, genre:, Action-Adventure, year: 2008, rating: Not Rated, PublishedID: 649273, IRO";
        // Matcher colonPatternMatcher = colonPattern.matcher(neighborhood);
        // while (colonPatternMatcher.find()) {
        // System.out.println(colonPatternMatcher.group());
        // }

        neighborhood = "Page: 1 of 4, Next Page, IRON MAN, Title: Iron Man AMG Rating: **** Genre: Action Movie Type: Comic-Book Superhero Film, Sci-Fi Action Themes: Robots and Androids, Heroic Mission, Experiments Gone Awry Director: Jon Favreau Main Cast: Robert Downey, Jr., Terrence Howard, Jeff Bridges, Gwyneth Paltrow, Leslie Bibb Release Year: 2008 Country: US Run Time: 126 minutes MPAA Rating: PG13, genre:, Action-Adventure, year: 2008, rating: Not Rated, PublishedID: 649273, IRO";
        int colonIndex = neighborhood.indexOf(":");
        while (colonIndex > -1) {

            Pattern cp = Pattern.compile(RegExp.STRING + ":$");
            Matcher cpm = cp.matcher(neighborhood.substring(Math.max(0, colonIndex - 30), colonIndex + 1));
            // System.out.println("String before colon: " + neighborhood.substring(Math.max(0, colonIndex - 30),
            // colonIndex + 1));
            String newAttributeName = "";
            while (cpm.find()) {
                // System.out.println((i++) + " " + cpm.group());
                newAttributeName = cpm.group();
            }

            int nextColonIndex = neighborhood.indexOf(":", colonIndex + 1);

            if (newAttributeName.length() > 0) {
                newAttributeName = newAttributeName.substring(0, newAttributeName.length() - 1);

                int nextLookOut = nextColonIndex;
                if (nextColonIndex == -1) {
                    nextLookOut = colonIndex + 61;
                }

                neighborhood.substring(colonIndex + 1, Math.min(neighborhood.length(), nextLookOut));
                // System.out.println("==> " + newAttributeName + ":" + value);
            }

            colonIndex = nextColonIndex;
        }

    }

}