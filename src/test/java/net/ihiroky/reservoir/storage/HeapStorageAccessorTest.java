package net.ihiroky.reservoir.storage;

import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.Ref;
import net.ihiroky.reservoir.index.SimpleIndex;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/09/27, 9:41
 *
 * @author Hiroki Itoh
 */
public class HeapStorageAccessorTest {

    private HeapStorageAccessor<Integer, Integer> heapCacheAccessor;

    @Before
    public void before() {
        heapCacheAccessor = new HeapStorageAccessor<Integer, Integer>();
    }

    @Test
    public void testUpdate() {
        Index<Integer, Ref<Integer>> index = new SimpleIndex<Integer, Ref<Integer>>();
        heapCacheAccessor.update(1, 11, index);
        assertThat(index.get(1).value(), is(11));
        heapCacheAccessor.update(1, 111, index);
        assertThat(index.get(1).value(), is(111));

        Map<Integer, Integer> keyValues = new HashMap<Integer, Integer>();
        keyValues.put(1, 11);
        keyValues.put(2, 12);
        keyValues.put(3, 13);
        heapCacheAccessor.update(keyValues, index);
        assertThat(index.get(1).value(), is(11));
        assertThat(index.get(2).value(), is(12));
        assertThat(index.get(3).value(), is(13));
        keyValues.put(1, 111);
        keyValues.put(2, 122);
        keyValues.put(3, 133);
        assertThat(index.size(), is(3));
        heapCacheAccessor.update(keyValues, index);
        assertThat(index.get(1).value(), is(111));
        assertThat(index.get(2).value(), is(122));
        assertThat(index.get(3).value(), is(133));
        assertThat(index.size(), is(3));
    }
}
