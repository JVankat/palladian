package ws.palladian.extraction.location;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EntityPreprocessingTaggerTest {

    private EntityPreprocessingTagger tagger;

    @Before
    public void setUp() throws FileNotFoundException {
        InputStream caseDictionaryStream = ResourceHelper.getResourceStream("caseDictionary.csv");
        tagger = new EntityPreprocessingTagger(caseDictionaryStream, 2, 0);
    }

    @After
    public void tearDown() {
        tagger = null;
    }

    @Test
    public void testEntityPreprocessor() throws IOException {
        String text = FileHelper.readFileToString(ResourceHelper.getResourcePath("testText.txt"));
        List<Annotation> annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);
        assertEquals(41, annotations.size());
        assertEquals("Clean Water Act", annotations.get(0).getValue());
        assertEquals(31, annotations.get(0).getStartPosition());
        assertEquals(46, annotations.get(0).getEndPosition());
        assertEquals("Cleveland", annotations.get(3).getValue());
        assertEquals("Bill Ruckelshaus", annotations.get(33).getValue());
    }

    @Test
    public void testEntityPreprocessor_shortPhrase_issue294() throws IOException {
        String text = "New York City";
        List<Annotation> annotations = tagger.getAnnotations(text);
        assertEquals(1, annotations.size());
        assertEquals("New York City", annotations.get(0).getValue());
    }

    @Test
    public void testCorrectCapitalization() {
        assertEquals("senior U.S. military official visits Georgia.", tagger.correctCapitalization("Senior U.S. Military Official Visits Georgia."));
        assertEquals("by RACHEL E. SHEELEY staff writer", tagger.correctCapitalization("BY RACHEL E. SHEELEY STAFF WRITER"));
        assertEquals("OSCE envoy condemns Dvani attack that killed one policeman.", tagger.correctCapitalization("OSCE Envoy Condemns Dvani Attack that Killed One Policeman."));
        assertEquals("competitive growth on imposition of special duties on importation of passenger cars.",
                tagger.correctCapitalization("Competitive Growth On Imposition Of Special Duties On Importation Of Passenger Cars."));
        assertEquals("at MAGAZAN BEACH resort", tagger.correctCapitalization("AT MAGAZAN BEACH RESORT"));
    }

    @Test
    public void testLongAnnotationSplit() {
        List<Annotation> annotations = StringTagger.INSTANCE.getAnnotations(
                "Rocky Hill Tax Credits Available. Jordan Elementary School Principal Stacy DeCorsey shows her students an oversized check made out to the school for $1,825.40. Former Bloomfield Town Councilman Richard Days Dead At 79. Platte County Attorney Sandra Allen Calls Tourism Australia Managing Director Andrew McEvoy.");
        List<Annotation> splitAnnotations = tagger.getLongAnnotationSplit(annotations, 3);

        assertEquals(9, splitAnnotations.size());
        assertEquals("Rocky Hill", splitAnnotations.get(0).getValue());
        assertEquals(0, splitAnnotations.get(0).getStartPosition());
        assertEquals(10, splitAnnotations.get(0).getEndPosition());

        assertEquals("Sandra Allen", splitAnnotations.get(6).getValue());
        assertEquals(242, splitAnnotations.get(6).getStartPosition());
        assertEquals(254, splitAnnotations.get(6).getEndPosition());

        annotations = tagger.getAnnotations("New York City-based");
        splitAnnotations = tagger.getLongAnnotationSplit(annotations, 3);
        assertEquals(2, splitAnnotations.size());
        assertEquals("New York City", splitAnnotations.get(1).getValue());
    }

}
