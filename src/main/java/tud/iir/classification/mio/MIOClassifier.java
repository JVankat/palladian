/**
 * The MIOClassifier calculate scores for ranking of MIOs.
 * Attention: First train the Classifier, then save it.
 * For classifying the classifier must be loaded first.
 * 
 * @author Martin Werner
 */
package tud.iir.classification.mio;

import java.io.File;

import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.extraction.mio.MIO;

public class MIOClassifier extends Classifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MIOClassifier.class);


    /**
     * Instantiates a new mioClassifier.
     */
    public MIOClassifier() {
        super(Classifier.LINEAR_REGRESSION);
        // super(Classifier.NEURAL_NETWORK);
    }

    /**
     * Calculate the regression value for a given MIO.
     * 
     * @param mio the MIO
     * @return the float
     */
    public void classify(final MIO mio) {

        // use the MIOFeatures as FeatureObject
        final FeatureObject featObject = new FeatureObject(mio.getFeatures());

        final int featureCount = featObject.getFeatureNames().length;

        // creating WekaAttributes is necessary because of loading the Classifier from File
        super.createWekaAttributes(featureCount, featObject.getFeatureNames());

        final double[] probability = super.classifySoft(featObject);

        mio.setTrust(normalizeTrust(probability[0]));
    }

    /**
     * Normalize trust to %
     * 
     * @param trust the trust
     * @return the double
     */
    private double normalizeTrust(final double trust) {
        double normalizedTrust = trust;
        if (trust >= 1) {
            normalizedTrust = 1.;

        }
        if (trust <= 0) {
            normalizedTrust = 0.;
        }
        normalizedTrust = Math.round(normalizedTrust * 1000.) / 10.;
        return normalizedTrust;
    }

    /**
     * Check if an already trained MIOClassifier exists.
     * 
     * @return true, if successful
     */
    public boolean doesTrainedMIOClassifierExists() {
        boolean returnValue = false;
        final File trainedClassifierModel = new File("data/models/" + "MIOClassifier" + getChosenClassifierName() + ".model");
        if (trainedClassifierModel.exists()) {
            returnValue = true;
        }
        return returnValue;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(final String[] args) {
        final MIOClassifier mioClass = new MIOClassifier();
        mioClass.trainClassifier("data/miofeatures_scored.txt");
        // mioClass.saveTrainedClassifier();
        // mioClass.loadTrainedClassifier();
        // mioClass.testClassifier("f:/features_bewertet_august.txt");
    }
}
