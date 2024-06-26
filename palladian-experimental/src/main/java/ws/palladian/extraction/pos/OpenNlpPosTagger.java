package ws.palladian.extraction.pos;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import org.apache.commons.lang.Validate;
import ws.palladian.helper.Cache;
import ws.palladian.helper.io.FileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * <a href="http://opennlp.apache.org/">Apache OpenNLP</a> based POS tagger.
 * </p>
 *
 * @author Martin Wunderwald
 * @author Philipp Katz
 * @see <a href="http://opennlp.sourceforge.net/models-1.5/">Download</a> page for models.
 */
public final class OpenNlpPosTagger extends AbstractPosTagger {

    /** The name of this POS tagger. */
    private static final String TAGGER_NAME = "OpenNLP POS-Tagger";

    /** The actual OpenNLP POS tagger. */
    private final POSTagger tagger;

    public OpenNlpPosTagger(File modelFile) {
        Validate.notNull(modelFile, "The model file must not be null.");
        this.tagger = loadModel(modelFile);
    }

    private POSTagger loadModel(File modelFile) {
        String modelPath = modelFile.getAbsolutePath();
        POSTagger model = (POSTagger) Cache.getInstance().getDataObject(modelPath);
        if (model == null) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(modelFile);
                model = new POSTaggerME(new POSModel(inputStream));
                Cache.getInstance().putDataObject(modelPath, model);
            } catch (IOException e) {
                throw new IllegalStateException("Error initializing OpenNLP POS Tagger from \"" + modelPath + "\": " + e.getMessage());
            } finally {
                FileHelper.close(inputStream);
            }
        }
        return model;
    }

    @Override
    protected List<String> getTags(List<String> tokens) {
        return Arrays.asList(tagger.tag(tokens.toArray(new String[tokens.size()])));
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

}
