package net.ihiroky.reservoir.index;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/09/26, 17:35
 *
 * @author Hiroki Itoh
 */
public class LRUIndexTest {

    @Test
    public void testLRU() {
        LRUIndex<Integer, Integer> index = new LRUIndex<Integer, Integer>(5, 5);

        index.put(0, 0);
        index.put(1, 1);
        index.put(2, 2);
        index.put(3, 3);
        index.put(4, 4);
        assertThat(index.size(), is(5));

        index.get(0);
        index.put(5, 5);
        assertThat(index.size(), is(5));
        assertThat(index.containsKey(0), is(true));
        assertThat(index.containsKey(1), is(false));
        assertThat(index.containsKey(2), is(true));
        assertThat(index.containsKey(3), is(true));
        assertThat(index.containsKey(4), is(true));
        assertThat(index.containsKey(5), is(true));
    }
}
