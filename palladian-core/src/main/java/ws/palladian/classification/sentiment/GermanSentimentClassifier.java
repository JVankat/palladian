package ws.palladian.classification.sentiment;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p>
 * German Sentiment Classifier that uses <a
 * href="http://wortschatz.informatik.uni-leipzig.de/download/sentiws.html">SentiWS</a>. The model uses this dictionary
 * and imports it from the file. Texts can be classified into positive and negative.
 * </p>
 *
 * @author David Urbansky
 */
public class GermanSentimentClassifier extends AbstractSentimentClassifier implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GermanSentimentClassifier.class);

    /** Serial Version UID. */
    private static final long serialVersionUID = 3611658830894765273L;

    /** Sentiment Map. It contains the word and the sentiment between [-1,1] (-1 is negative, 1 is positive). */
    private Object2FloatMap<String> sentimentMap = new Object2FloatOpenHashMap<>();

    /** Some words can emphasize the sentiment. In this map we store these words and their multiplier. */
    private static final Map<String, Double> emphasizeMap = new HashMap<>();

    static {
        emphasizeMap.put("bisschen", 0.9);
        emphasizeMap.put("sehr", 2.0);
        emphasizeMap.put("deutlich", 2.0);
        emphasizeMap.put("unheimlich", 3.0);
        emphasizeMap.put("absolut", 3.0);
        emphasizeMap.put("vollkommen", 3.0);
        emphasizeMap.put("extrem", 3.0);
    }

    public GermanSentimentClassifier(String modelPath) {
        try {
            GermanSentimentClassifier gsc = FileHelper.deserialize(modelPath);
            this.sentimentMap = gsc.sentimentMap;
        } catch (IOException e) {
            throw new IllegalStateException("Could not deserialize model from \"" + modelPath + "\"", e);
        }
    }

    /**
     * <p>
     * Use this constructor only to build a model.
     * </p>
     */
    public GermanSentimentClassifier() {
    }

    public void buildModel(String dictionaryFilePath, String modelPath) throws IOException {
        loadDictionary(dictionaryFilePath);
        saveDictionary(modelPath);
    }

    /**
     * <p>
     * Import the dictionary from the SentiWS file.
     * </p>
     *
     * @param dictionaryFilePath The path should be the prefix of the positive and negative file.
     */
    private void loadDictionary(String dictionaryFilePath) {
        List<String> positiveLines = FileHelper.readFileToArray(dictionaryFilePath + "Positive.txt");
        List<String> negativeLines = FileHelper.readFileToArray(dictionaryFilePath + "Negative.txt");

        List<String> sentimentLines = new ArrayList<>();
        sentimentLines.addAll(positiveLines);
        sentimentLines.addAll(negativeLines);

        for (String sentimentLine : sentimentLines) {
            String[] parts = sentimentLine.toLowerCase().split("\t");

            String mainWord = parts[0];

            // if (sentimentLine.indexOf("unschön") > -1) {
            // System.out.println("stop");
            // }

            float sentimentValue = Float.parseFloat(parts[1]);

            // remove POS tag
            mainWord = mainWord.replaceAll("\\|.*", "");

            // get synonyms for the main word
            if (parts.length > 2) {
                String[] words = parts[2].split(",");
                for (String word : words) {
                    sentimentMap.put(word, sentimentValue);
                }
            }

            sentimentMap.put(mainWord, sentimentValue);
        }
    }

    private void saveDictionary(String modelPath) throws IOException {
        FileHelper.serialize(this, modelPath);
    }

    @Override
    public Category getPolarity(String text, String query) {
        String positiveCategory = "positive";
        String negativeCategory = "negative";

        if (query != null) {
            query = query.toLowerCase();
        }

        // total sum of positive and negative sentiments (on word level) in the text
        // double positiveSentimentSum = 0;
        // double negativeSentimentSum = 0;

        List<String> sentences = Tokenizer.getSentences(text);

        for (String sentence : sentences) {
            String lcSentence = sentence.toLowerCase();

            double positiveSentimentSumSentence = 0;
            double negativeSentimentSumSentence = 0;

            // if a query is given, we only consider sentences that contain the query term(s)
            if (query != null && !lcSentence.contains(query)) {
                continue;
            }

            String[] tokens = lcSentence.split("\\s");
            String beforeLastToken = "";
            String lastToken = "";
            for (String token : tokens) {
                token = StringHelper.trim(token);

                // check whether we should emphasize the sentiment
                double emphasizeWeight = 1.0;
                if (emphasizeMap.get(lastToken) != null) {
                    emphasizeWeight = emphasizeMap.get(lastToken);
                }

                // check whether we need to negate the sentiment
                if (lastToken.equalsIgnoreCase("nicht") || beforeLastToken.equalsIgnoreCase("nicht") || lastToken.equalsIgnoreCase("ohne") || lastToken.equalsIgnoreCase("kein")
                        || lastToken.equalsIgnoreCase("keine")) {
                    emphasizeWeight *= -1;
                }

                float sentiment = sentimentMap.getOrDefault(token, -10101);
                if (sentiment > -10101) {
                    sentiment *= emphasizeWeight;
                    if (sentiment > 0) {
                        //                        LOGGER.debug("positive word: " + token + " (" + sentiment + ")");
                        // positiveSentimentSum += sentiment;
                        positiveSentimentSumSentence += sentiment;
                    } else {
                        //                        LOGGER.debug("negative word: " + token + " (" + sentiment + ")");
                        // negativeSentimentSum += Math.abs(sentiment);
                        negativeSentimentSumSentence += Math.abs(sentiment);
                    }
                }

                beforeLastToken = lastToken;
                lastToken = token;
            }

            CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
            builder.add(positiveCategory, positiveSentimentSumSentence);
            builder.add(negativeCategory, negativeSentimentSumSentence);
            CategoryEntries categoryEntries = builder.create();

            double probabilityMostLikelySentiment = categoryEntries.getProbability(categoryEntries.getMostLikelyCategory());
            if (probabilityMostLikelySentiment > confidenceThreshold && (positiveSentimentSumSentence > 2 * negativeSentimentSumSentence
                    || negativeSentimentSumSentence > 2 * positiveSentimentSumSentence) && (positiveSentimentSumSentence >= 0.008 || negativeSentimentSumSentence > 0.008)) {
                addOpinionatedSentence(categoryEntries.getMostLikelyCategory(), sentence);
            }
        }

        // CategoryEntries categoryEntries = new CategoryEntries();
        // CategoryEntry positiveCategoryEntry = new CategoryEntry(categoryEntries, positiveCategory,
        // positiveSentimentSum);
        // CategoryEntry negativeCategoryEntry = new CategoryEntry(categoryEntries, negativeCategory,
        // negativeSentimentSum);
        // categoryEntries.add(positiveCategoryEntry);
        // categoryEntries.add(negativeCategoryEntry);

        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        int positiveSentences = 0;
        if (getOpinionatedSentences().get("positive") != null) {
            positiveSentences = getOpinionatedSentences().get("positive").size();
        }
        int negativeSentences = 0;
        if (getOpinionatedSentences().get("negative") != null) {
            negativeSentences = getOpinionatedSentences().get("negative").size();
        }

        builder.add(positiveCategory, positiveSentences);
        builder.add(negativeCategory, negativeSentences);

        return builder.create().getMostLikely();
    }

    public static void main(String[] args) throws IOException {
        GermanSentimentClassifier gsc;

        // build the model
        gsc = new GermanSentimentClassifier();
        // gsc.buildModel("data/temp/SentiWS_v1.8c-added_", "data/temp/gsc.gz");
        // System.exit(0);

        gsc = new GermanSentimentClassifier("data/temp/gsc.gz");
        gsc.setConfidenceThreshold(0.6);
        Category result = gsc.getPolarity("Das finde ich nicht so toll aber manchmal ist das unschön.");
        result = gsc.getPolarity("Die DAK hat Versäumt die Krankenkasse zu benachrichtigen und das ist auch gut so.");
        result = gsc.getPolarity("Die Deutsche-Bahn ist scheisse!!!");
        // result =
        // gsc.getPolarity("Angaben zu rechtlichen und/oder wirtschaftlichen Verknüpfungen zu anderen Büros oder
        // Unternehmen");
        // result = gsc.getPolarity(FileHelper.readFileToString("data/temp/opiniontext.TXT"));

        Map<String, List<String>> opinionatedSentences = gsc.getOpinionatedSentences();
        for (Entry<String, List<String>> entry : opinionatedSentences.entrySet()) {
            System.out.println(entry.getKey());
            CollectionHelper.print(entry.getValue());
        }

        System.out.println(result);
    }

}