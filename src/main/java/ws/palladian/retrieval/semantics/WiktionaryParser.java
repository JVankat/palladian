package ws.palladian.retrieval.semantics;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * This class is a parser for the Wiktionary project dump files. The parser works for the German and English dumps which
 * can be found at <a href="http://dumps.wikimedia.org/dewiktionary/">German dumps</a> and <a
 * href="http://dumps.wikimedia.org/enwiktionary/">English dumps</a>. Use pages-articles.xml.bz2.
 * </p>
 * 
 * <p>
 * The German Word DB can be extended with data from openthesaurus.de. We need to download the SQL database
 * (http://www.openthesaurus.de/about/download) query the hypernyms (SELECT t1.word,t2.word FROM term t1, term t2,
 * synset s1, synset s2, synset_link sl WHERE t1.synset_id = s1.id AND t2.synset_id = s2.id AND sl.synset_id = s1.id AND
 * sl.target_synset_id = s2.id AND sl.link_type_id=1;), export this data to a csv file (word;hypernym), and tell the
 * parser to use this file for additional hypernyms.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class WiktionaryParser {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WiktionaryParser.class);

    /** The database where the dictionary is stored. */
    private final WordDB wordDB;

    /** The supported languages which the parser can handle. */
    public enum Language {
        GERMAN, ENGLISH
    };

    /** The language to use for the parsing. */
    private final Language corpusLanguage;

    /**
     * The path to an additional hypernym file which should be used for parsing. The file has to have one
     * hyponym;hypernym tuple per line.
     */
    private String additionalHypernymFile = "";

    public WiktionaryParser(String targetPath, Language language) {
        targetPath = FileHelper.addTrailingSlash(targetPath);
        this.corpusLanguage = language;
        wordDB = new WordDB(targetPath);
        wordDB.setInMemoryMode(true);
        wordDB.setup();
    }

    /**
     * 
     * @param wiktionaryXmlFilePath
     */
    public void parseAndCreateDB(String wiktionaryXmlFilePath) {

        final long bytesToProcess = new File(wiktionaryXmlFilePath).length();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {

                private long bytesProcessed = 0;
                private int elementsParsed = 0;
                private boolean isTitle = false;
                private boolean considerText = false;
                private boolean isText = false;

                private String currentWord = "";
                private StringBuilder text = new StringBuilder();
                private final StopWatch sw = new StopWatch();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {

                    // System.out.println("Start Element :" + qName);

                    if (qName.equalsIgnoreCase("text")) {
                        isText = true;
                        text = new StringBuilder();
                    }

                    if (qName.equalsIgnoreCase("title")) {
                        isTitle = true;
                    }

                    bytesProcessed += qName.length();
                }

                private void postProcess(String word, StringBuilder text) throws SQLException {

                    if (word.equalsIgnoreCase("ewusersonly")) {
                        return;
                    }

                    // if (word.equalsIgnoreCase("April")) {
                    // System.out.println("haus");
                    // }

                    String plural = "";
                    String language = "";
                    String wordType = "";
                    List<String> synonyms = new ArrayList<String>();
                    List<String> hypernyms = new ArrayList<String>();
                    List<String> hyponyms = new ArrayList<String>();

                    String textString = text.toString();

                    // get the language
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        language = StringHelper.getSubstringBetween(textString, " ({{Sprache|", "}}");
                    } else if (corpusLanguage.equals(Language.ENGLISH)) {
                        language = StringHelper.getSubstringBetween(textString, "==", "==");
                    }

                    // get the word type
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        wordType = StringHelper.getSubstringBetween(textString, "=== {{Wortart|", "|");
                        if (wordType.indexOf("}}") > -1) {
                            wordType = StringHelper.getSubstringBetween(textString, "=== {{Wortart|", "}}");
                        }
                    } else if (corpusLanguage.equals(Language.ENGLISH)) {
                        wordType = StringHelper.getSubstringBetween(textString, "Etymology 1===", "# ");
                        if (wordType.length() == 0) {
                            wordType = StringHelper.getSubstringBetween(textString, "Pronunciation===", "# ");
                        }
                        if (wordType.length() == 0) {
                            wordType = StringHelper.getSubstringBetween(textString, language + "==", "# ");
                        }
                        if (wordType.indexOf("Etymology==") > -1) {
                            wordType = StringHelper.getSubstringBetween(textString, "Etymology===", "# ");
                        }
                        if (wordType.indexOf("Pronunciation") > -1) {
                            wordType = StringHelper.getSubstringBetween(textString, "Pronunciation===", "# ");
                        }

                        if (wordType.length() > 0) {
                            wordType = StringHelper.getSubstringBetween(wordType, "===", "===");
                            wordType = StringHelper.trim(wordType);
                        }
                    }

                    // get the plural if noun
                    if (corpusLanguage.equals(Language.GERMAN) && wordType.equalsIgnoreCase("substantiv")) {
                        plural = StringHelper.getSubstringBetween(textString, "{{Silbentrennung}}\n", "\n");

                        if (plural.length() == 0) {
                            plural = StringHelper.getSubstringBetween(textString, "{{Silbentrennung}} \n", "\n");
                        }

                        int index = plural.indexOf("{{Pl.}}");
                        if (index > -1) {
                            plural = plural.substring(plural.indexOf("{{Pl.}}") + 7);
                        } else {
                            index = plural.indexOf("{{Pl.1}}");
                            if (index > -1) {
                                plural = plural.substring(plural.indexOf("{{Pl.1}}") + 8);
                                index = plural.indexOf(",");
                                if (index > -1) {
                                    plural = plural.substring(0, index);
                                } else {
                                    plural = "";
                                }
                            }
                        }
                        plural = StringHelper.trim(plural.replace("\n", "").replace("·", "").replaceAll("''.*?''", ""));
                    }
                    if (plural.length() > WordDB.MAX_WORD_LENGTH) {
                        plural = "";
                    }

                    String synonymString = "";

                    if (corpusLanguage.equals(Language.GERMAN)) {
                        synonymString = StringHelper.getSubstringBetween(textString, "{{Synonyme}}", "{{");

                        // take only the line starting with [1] because it is the most relevant, the others are too far
                        // off
                        synonymString = StringHelper.getSubstringBetween(synonymString, ":[1]", "\n");

                        synonyms = StringHelper.getRegexpMatches("(?<=\\[\\[)(.+?)(?=\\]\\])", synonymString);
                    } else if (corpusLanguage.equals(Language.ENGLISH)) {
                        synonymString = StringHelper.getSubstringBetween(textString, "====Synonyms====", "===");
                        synonyms = StringHelper.getRegexpMatches("(?<=\\[\\[)(.+?)(?=\\]\\])", synonymString);
                    }

                    // hypernyms are only available in German, strange though...
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        String hypernymString = StringHelper.getSubstringBetween(textString, "{{Oberbegriffe}}", "{{");
                        hypernymString = StringHelper.getSubstringBetween(hypernymString, ":[1]", "\n");
                        hypernyms = StringHelper.getRegexpMatches("(?<=\\[\\[)(.+?)(?=\\]\\])", hypernymString);
                    }

                    // get descending words (words from which the current one is the hypernym)
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        String hyponymString = StringHelper.getSubstringBetween(textString, "{{Unterbegriffe}}", "{{");
                        hyponymString = StringHelper.getSubstringBetween(hyponymString, ":[1]", "\n");
                        hyponyms = StringHelper.getRegexpMatches("(?<=\\[\\[)(.+?)(?=\\]\\])", hyponymString);
                    }

                    Word wordObject = wordDB.getWord(word);
                    if (wordObject == null) {
                        wordObject = new Word(-1, word, plural, wordType, language);
                        wordDB.addWord(wordObject);

                        // get it from the db again to get the correct id
                        wordObject = wordDB.getWord(word);

                    } else {
                        wordObject.setPlural(plural);
                        wordObject.setType(wordType);
                        wordObject.setLanguage(language);
                        wordDB.updateWord(wordObject);
                    }

                    if (wordObject != null) {
                        wordDB.addSynonyms(wordObject, synonyms);
                        wordDB.addHypernyms(wordObject, hypernyms);
                        wordDB.addHyponyms(wordObject, hyponyms);
                    }

                    if (elementsParsed++ % 100 == 0) {
                        System.out.println(">" + MathHelper.round(100 * bytesProcessed / bytesToProcess, 2) + "%, +"
                                + sw.getElapsedTimeString());
                        sw.start();
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {

                    if (qName.equalsIgnoreCase("text")) {
                        if (considerText) {
                            LOGGER.debug("Word: " + currentWord);
                            LOGGER.debug("Text: " + text);
                            try {
                                postProcess(currentWord, text);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        isText = false;
                        considerText = false;
                    }

                    if (qName.equalsIgnoreCase("title")) {
                        isTitle = false;
                    }

                    bytesProcessed += qName.length();
                }

                @Override
                public void characters(char ch[], int start, int length) throws SAXException {

                    if (isTitle) {
                        String titleText = new String(ch, start, length);

                        if (titleText.indexOf(":") == -1 && titleText.indexOf("Wiktionary") == -1) {
                            considerText = true;
                            currentWord = titleText;
                        }
                    }

                    if (isText && considerText) {
                        String textString = new String(ch, start, length);
                        text.append(textString);
                    }

                    bytesProcessed += length;
                }

            };

            saxParser.parse(wiktionaryXmlFilePath, handler);

            // if we have an additional hypernym file, parse it
            if (getAdditionalHypernymFile().length() > 0) {
                List<String> hypernymArray = FileHelper.readFileToArray(getAdditionalHypernymFile());

                String lastHyponym = "";
                List<String> hypernyms = new ArrayList<String>();

                int c = 0;
                for (String wordPair : hypernymArray) {

                    String[] words = wordPair.split(";");

                    if (words.length < 2) {
                        continue;
                    }

                    String hyponym = StringHelper.trim(StringHelper.removeBrackets(words[0]));
                    String hypernym = StringHelper.trim(StringHelper.removeBrackets(words[1]));

                    if (!hyponym.equals(lastHyponym)) {

                        if (lastHyponym.length() > 0) {
                            Word wordObject = wordDB.getWord(lastHyponym);
                            if (wordObject != null) {
                                wordDB.addHypernyms(wordObject, hypernyms);
                            }
                            hypernyms = new ArrayList<String>();
                            hypernyms.add(hypernym);
                        } else {
                            hypernyms.add(hypernym);
                        }
                        lastHyponym = hyponym;
                    } else {
                        hypernyms.add(hypernym);
                    }

                    if (c++ % 100 == 0) {
                        LOGGER.info(MathHelper.round(100 * c / hypernymArray.size(), 2)
                                + "% of additional hypernyms processed");
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        wordDB.writeToDisk();
    }

    public String getAdditionalHypernymFile() {
        return additionalHypernymFile;
    }

    public void setAdditionalHypernymFile(String additionalHypernymFile) {
        this.additionalHypernymFile = additionalHypernymFile;
    }

    /**
     * The main function.
     * 
     * @param args
     */
    public static void main(String[] args) {
        StopWatch sw = new StopWatch();

        // German
        WiktionaryParser wpG = new WiktionaryParser("data/temp/wordDatabaseGerman/", Language.GERMAN);
        // wpG.parseAndCreateDB("data/temp/dewiktionary-20110327-pages-meta-current.xml");
        wpG.setAdditionalHypernymFile("data/temp/openthesaurusHypernyms.csv");
        wpG.parseAndCreateDB("data/temp/dewiktionary-20110620-pages-articles.xml");
        // wpG.parseAndCreateDB("data/temp/disk1.xml");

        // English
        // WiktionaryParser wpE = new WiktionaryParser("data/temp/wordDatabaseEnglish/", Language.ENGLISH);
        // wpE.parseAndCreateDB("data/temp/enwiktionary-20110402-pages-meta-current.xml");

        LOGGER.info("created wiktionary DB in " + sw.getElapsedTimeString());
    }

}