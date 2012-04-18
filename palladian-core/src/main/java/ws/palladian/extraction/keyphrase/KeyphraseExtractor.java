package ws.palladian.extraction.keyphrase;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import scala.actors.threadpool.Arrays;

import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.extraction.keyphrase.temp.Dataset2;
import ws.palladian.extraction.keyphrase.temp.DatasetItem;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public abstract class KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractor.class);

    /** Maximum number of keyphrases to assign. */
    private int keyphraseCount = 10;

    public final void train(final Dataset dataset) {

        LOGGER.info("training");

        startTraining();

        FileHelper.performActionOnEveryLine(dataset.getPath(), new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] split = line.split(dataset.getSeparationString());

                if (split.length < 2) {
                    return;
                }

                String inputText = split[0];
                if (dataset.isFirstFieldLink()) {
                    inputText = FileHelper.readFileToString(dataset.getRootPath() + "/" + split[0]);
                }

                // the manually assigned keyphrases
                Set<String> keyphrases = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    keyphrases.add(split[i]);
                }

                train(inputText, keyphrases);
                
                if (lineNumber % 10 == 0) {
                    LOGGER.info(lineNumber);
                }

            }
        });

        endTraining();

    }
    
    public final void train(Dataset2 dataset) {
        startTraining();
        int i = 0;
        for (DatasetItem item : dataset) {
            System.out.println(i++ + "// " + dataset.size());
            String[] categories = item.getCategories();
            String text;
            try {
                text = FileUtils.readFileToString(item.getFile());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            train(text, new HashSet<String>(Arrays.asList(categories)));
        }
        endTraining();
    }

    /**
     * Hook method which is called before the training begins. Can be overridden by subclasses as necessary.
     */
    public void startTraining() {
        // override if necessary
    }

    /**
     * Train the keyphrase extractor with the specified text and the assigned keyphrases.
     * 
     * @param inputText
     * @param keyphrases
     * @param index
     */
    public void train(String inputText, Set<String> keyphrases) {
        // override if this extractor needs training
    }

    /**
     * Hook method which is called after the training is finished. Can be overridden by subclasses as necessary.
     */
    public void endTraining() {
        // override if necessary
    }

    /**
     * Indicate whether this KeyphraseExtractor needs to be trained.
     * 
     * @return
     */
    public abstract boolean needsTraining();

    public abstract List<Keyphrase> extract(String inputText);

    /**
     * @return the keyphraseCount
     */
    public int getKeyphraseCount() {
        return keyphraseCount;
    }

    /**
     * Specify the maximum number of keyphrases to extract.
     * 
     * @param keyphraseCount the keyphraseCount to set
     */
    public void setKeyphraseCount(int keyphraseCount) {
        this.keyphraseCount = keyphraseCount;
    }

    /**
     * Hook method which is called before extraction. Can be overridden by subclasses as necessary.
     */
    public void startExtraction() {

    }
    
    public abstract String getExtractorName();

    public void reset() {
        // TODO Auto-generated method stub
        
    }

}