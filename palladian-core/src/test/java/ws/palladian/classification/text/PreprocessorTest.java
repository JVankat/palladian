package ws.palladian.classification.text;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class PreprocessorTest {

    // see: https://gitlab.com/palladian/palladian-knime/-/issues/38
    @Test
    public void testNGramAndStemming() {
        FeatureSetting featureSetting = FeatureSettingBuilder.words(2, 2).termLength(1, 50).stem().create();
        Preprocessor preprocessor = new Preprocessor(featureSetting);
        Iterator<String> tokens = preprocessor.apply("artificial intelligence");
        String token = tokens.next();
        assertEquals("artifici intellig", token);
    }

    // see: https://gitlab.com/palladian/palladian-knime/-/issues/38
    @Test
    public void testNGramStopWordRemoval() {
        FeatureSetting featureSetting = FeatureSettingBuilder.words(2, 2).termLength(1, 50).removeStopwords().create();
        Preprocessor preprocessor = new Preprocessor(featureSetting);
        Iterator<String> tokens = preprocessor.apply("the quick brown fox");
        assertEquals("quick brown", tokens.next());
        assertEquals("brown fox", tokens.next());
    }

    // https://gitlab.com/palladian/palladian-knime/-/issues/38
    @Test
    public void testMinMaxTermLength() {
        FeatureSetting featureSetting = FeatureSettingBuilder.words(2).termLength(2, 20).create();
        Preprocessor preprocessor = new Preprocessor(featureSetting);
        Iterator<String> tokens = preprocessor.apply("I am not a virus");
        assertEquals("am not", tokens.next());
    }

}
