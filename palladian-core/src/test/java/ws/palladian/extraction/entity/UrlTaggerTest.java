package ws.palladian.extraction.entity;

import org.junit.Test;
import ws.palladian.core.Annotation;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class UrlTaggerTest {

    @Test
    public void testUrlTagging() {
        List<Annotation> annotations = UrlTagger.INSTANCE.getAnnotations("You can download it here: cinefreaks.com/coolstuff.zip but be aware of the size.");
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getStartPosition());
        assertEquals(28, annotations.get(0).getValue().length());
    }
}
