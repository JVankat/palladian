package ws.palladian.classification.featureselection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.helper.functional.Filters.regex;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.featureselection.BackwardFeatureEliminationConfig.Builder;
import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesLearner;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.Instance;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.ResourceHelper;

/**
 * @author Philipp Katz
 */
public class BackwardFeatureEliminationTest {

	private static List<Instance> instances;

	@BeforeClass
	public static void getData() throws FileNotFoundException {
		File testFile = ResourceHelper.getResourceFile("/classifier/diabetes2.csv");
		instances = CsvDatasetReaderConfig.filePath(testFile).readHeader(true).create().readAll();
	}
	
    @Test
    public void testElimination() throws FileNotFoundException {
    	Builder<NaiveBayesModel> builder = BackwardFeatureEliminationConfig.with(
    			new NaiveBayesLearner(), new NaiveBayesClassifier());
    	BackwardFeatureElimination elimination = builder.create();
        FeatureRanking ranking = elimination.rankFeatures(instances, NoProgress.INSTANCE);
        String bestFeatureValue = ranking.getAll().get(0).getName();

        // this is not really a good test, as the BackwardFeatureElimination shuffles the dataset; the top ranked
        // features are as below, but I cannot exclude the case, the in one of 349834983 cases this might fail.
        assertTrue(Arrays.asList("plasma", "bmi", "bloodPressure", "triceps", "pedigree").contains(bestFeatureValue));
    }
    
    @Test
    public void testElimination_FeatureGroups() throws FileNotFoundException {
    	Builder<NaiveBayesModel> builder = BackwardFeatureEliminationConfig.with(
    			new NaiveBayesLearner(), new NaiveBayesClassifier());
    	builder.addFeatureGroup(regex("plasma|bmi|pedigree"));
    	BackwardFeatureElimination elimination = builder.create();
    	FeatureRanking ranking = elimination.rankFeatures(instances, NoProgress.INSTANCE);
    	CollectionHelper.print(ranking.getAll());
    	String bestFeatureValue = ranking.getAll().get(0).getName();
    	assertEquals("plasma|bmi|pedigree", bestFeatureValue);
    }

}
