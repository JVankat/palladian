package ws.palladian.kaggle.fisheries.utils.hash;

import org.junit.Test;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.io.ResourceHelper;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AverageHashTest {
    @Test
    public void testAverageHash() throws FileNotFoundException, IOException {
        BufferedImage image = ImageHandler.load(ResourceHelper.getResourceFile("medium_5530248040.jpg"));
        String hash = new AverageHash().hash(image);
        assertEquals("00010638608ffffe", hash);
    }
}
