package ws.palladian.helper.collection;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Philipp Katz
 */
public class FixedSizeQueueTest {

    @Test
    public void testFixedSizeQueue() {
        List<Integer> queue = FixedSizeQueue.create(5);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        queue.add(4);
        queue.add(5);
        assertEquals(5, queue.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), queue);
        queue.add(6);
        assertEquals(5, queue.size());
        assertEquals(Arrays.asList(2, 3, 4, 5, 6), queue);
        queue.add(0, 7);
        assertEquals(5, queue.size());
        assertEquals(Arrays.asList(7, 2, 3, 4, 5), queue);
    }

}
