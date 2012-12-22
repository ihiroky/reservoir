package net.ihiroky.reservoir;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/03, 17:33
 *
 * @author Hiroki Itoh
 */
public class CompoundCacheTest {

    private CompoundCache<Integer, Integer> cache;
    private BasicCache<Integer, Integer> main;
    private BasicCache<Integer, Integer> sub;

    @Before
    public void before() {
        main = Reservoir.newCacheBuilder().name("main").maxCacheSize(5)
                .indexType(Reservoir.IndexType.LRU)
                .cacheAccessorType(Reservoir.CacheAccessorType.HEAP)
                .build();
        sub = Reservoir.newCacheBuilder().name("sub")
                .indexType(Reservoir.IndexType.LRU)
                .cacheAccessorType(Reservoir.CacheAccessorType.BYTE_BUFFER)
                .property("reservoir.ByteBufferCacheAccessor.direct", "true")
                .property("reservoir.ByteBufferCacheAccessor.size", "8192")
                .property("reservoir.ByteBufferCacheAccessor.blockSize", "256")
                .property("reservoir.ByteBufferCacheAccessor.partitions", "8")
                .property("reservoir.ByteBufferCacheAccessor.coder", "net.ihiroky.reservoir.coder.SerializableCoder")
                .build();
        cache = null;
    }

    @After
    public void after() {
        if (cache != null) {
            cache.dispose();
        }
    }

    @Test
    public void testPut() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        assertThat(cache.get(0), is(10));
        assertThat(cache.get(1), is(11));
        assertThat(cache.get(2), is(12));
        assertThat(cache.get(3), is(13));
        assertThat(cache.get(4), is(14));
        assertThat(cache.size(), is(5));
        assertThat(main.size(), is(5));
        assertThat(sub.size(), is(0));

        cache.put(5, 15);
        assertThat(cache.get(0), is(10));
        assertThat(cache.get(1), is(11));
        assertThat(cache.get(2), is(12));
        assertThat(cache.get(3), is(13));
        assertThat(cache.get(4), is(14));
        assertThat(cache.get(5), is(15));
        assertThat(cache.size(), is(6));
        assertThat(main.get(0), is(nullValue()));
        assertThat(main.size(), is(5));
        assertThat(sub.size(), is(1));
    }

    @Test
    public void testPutMulti() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub);

        Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        map.put(3, 13);
        map.put(4, 14);
        cache.putAll(map);
        assertThat(cache.size(), is(5));
        assertThat(main.size(), is(5));
        assertThat(sub.size(), is(0));

        map.put(3, 23);
        map.put(4, 24);
        map.put(5, 25);
        cache.putAll(map);
        Map<Integer, Integer> kv = cache.get(Arrays.asList(0, 1, 2, 3, 4, 5, -1));
        assertThat(kv.get(0), is(10));
        assertThat(kv.get(1), is(11));
        assertThat(kv.get(2), is(12));
        assertThat(kv.get(3), is(23));
        assertThat(kv.get(4), is(24));
        assertThat(kv.get(5), is(25));
        assertThat(kv.size(), is(6));
        assertThat(cache.size(), is(6));
        assertThat(main.get(0), is(nullValue()));
        assertThat(main.size(), is(5));
        assertThat(sub.size(), is(1));
    }

    @Test
    public void testGetPromote() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub, true, false);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        assertThat(cache.get(0), is(10));
        assertThat(cache.get(1), is(11));
        assertThat(cache.get(2), is(12));
        assertThat(cache.get(3), is(13));
        assertThat(cache.get(4), is(14));
        assertThat(cache.size(), is(5));
        assertThat(main.size(), is(5));
        assertThat(sub.size(), is(0));

        cache.put(5, 15); // demote 0
        assertThat(cache.get(0), is(10));
        assertThat(main.get(1), is(nullValue()));
        assertThat(cache.get(1), is(11));
        assertThat(main.get(2), is(nullValue()));
        assertThat(cache.get(2), is(12));
        assertThat(main.get(3), is(nullValue()));
        assertThat(cache.get(3), is(13));
        assertThat(main.get(4), is(nullValue()));
        assertThat(cache.get(4), is(14));
        assertThat(main.get(5), is(nullValue()));
        assertThat(cache.get(5), is(15));
        assertThat(main.get(0), is(nullValue()));
        assertThat(cache.size(), is(6));
        assertThat(main.size(), is(5));
        assertThat(sub.size(), is(1));
    }

    @Test
    public void testGetPromoteMulti() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub, true, false);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        cache.put(5, 15);
        Map<Integer, Integer> kv = cache.get(Arrays.asList(0, 1, 2, 3, 4, -1));
        assertThat(kv.get(0), is(10));
        assertThat(kv.get(1), is(11));
        assertThat(kv.get(2), is(12));
        assertThat(kv.get(3), is(13));
        assertThat(kv.get(4), is(14));
        assertThat(cache.size(), is(6));
        assertThat(main.get(5), is(nullValue()));
        assertThat(sub.get(5), is(15));
        assertThat(sub.size(), is(1));
    }

    @Test
    public void testPutDemoteBehind() throws Exception {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub, false, true);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        cache.put(5, 15);
        cache.put(6, 16);
        while (main.size() != 5 || sub.size() != 2) {
            Thread.sleep(10);
        }

        assertThat(cache.size(), is(7));
        assertThat(main.get(0), is(nullValue()));
        assertThat(main.get(1), is(nullValue()));
        assertThat(main.size(), is(5));
        assertThat(sub.get(0), is(10));
        assertThat(sub.get(1), is(11));
        assertThat(sub.size(), is(2));
    }

    // @Test(timeout = 3000)
    @Test
    public void testPutMultiDemoteBehind() throws Exception {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub, false, true);

        Map<Integer, Integer> kv = new HashMap<Integer, Integer>();
        kv.put(0, 10);
        kv.put(1, 11);
        kv.put(2, 12);
        kv.put(3, 13);
        kv.put(4, 14);
        kv.put(5, 15);
        kv.put(6, 16);
        cache.putAll(kv);
        while (main.size() != 5 || sub.size() != 2) {
            Thread.sleep(10);
        }

        assertThat(cache.size(), is(7));
        assertThat(main.get(0), is(nullValue()));
        assertThat(main.get(1), is(nullValue()));
        assertThat(main.size(), is(5));
        assertThat(sub.get(0), is(10));
        assertThat(sub.get(1), is(11));
        assertThat(sub.size(), is(2));
    }

    @Test
    public void testDelete() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        cache.put(5, 15);
        cache.put(6, 16);
        cache.delete(0);
        assertThat(cache.get(0), is(nullValue()));
        cache.delete(6);
        assertThat(cache.get(6), is(nullValue()));
        assertThat(main.get(5), is(15));
        assertThat(main.get(4), is(14));
        assertThat(main.get(3), is(13));
        assertThat(main.get(2), is(12));
        assertThat(sub.get(1), is(11));
        assertThat(cache.size(), is(5));
    }

    @Test
    public void testDeleteMulti() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        cache.put(5, 15);
        cache.put(6, 16);
        Collection<Integer> keys = Arrays.asList(0, 6);
        cache.delete(keys);
        assertThat(cache.get(0), is(nullValue()));
        assertThat(cache.get(6), is(nullValue()));
        assertThat(main.get(5), is(15));
        assertThat(main.get(4), is(14));
        assertThat(main.get(3), is(13));
        assertThat(main.get(2), is(12));
        assertThat(sub.get(1), is(11));
        assertThat(cache.size(), is(5));
    }

    @Test
    public void testRemove() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub);

        Integer ret = cache.remove(-1);
        assertThat(ret, is(nullValue()));

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        cache.put(5, 15);
        cache.put(6, 16);
        ret = cache.remove(0);
        assertThat(ret, is(10));
        assertThat(cache.get(0), is(nullValue()));
        ret = cache.remove(6);
        assertThat(ret, is(16));
        assertThat(cache.get(6), is(nullValue()));
        assertThat(main.get(5), is(15));
        assertThat(main.get(4), is(14));
        assertThat(main.get(3), is(13));
        assertThat(main.get(2), is(12));
        assertThat(sub.get(1), is(11));
        assertThat(cache.size(), is(5));
    }

    @Test
    public void testRemoveMulti() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        cache.put(5, 15);
        cache.put(6, 16);
        Collection<Integer> keys = Arrays.asList(0, 6);
        Map<Integer, Integer> ret = cache.remove(keys);
        assertThat(ret.get(0), is(10));
        assertThat(ret.get(6), is(16));
        assertThat(ret.size(), is(2));
        assertThat(cache.get(0), is(nullValue()));
        assertThat(cache.get(6), is(nullValue()));
        assertThat(main.get(5), is(15));
        assertThat(main.get(4), is(14));
        assertThat(main.get(3), is(13));
        assertThat(main.get(2), is(12));
        assertThat(sub.get(1), is(11));
        assertThat(cache.size(), is(5));
    }

    @Test
    public void testIterator() {
        cache = new CompoundCache<Integer, Integer>("compound", main, sub);

        cache.put(0, 10);
        cache.put(1, 11);
        cache.put(2, 12);
        cache.put(3, 13);
        cache.put(4, 14);
        cache.put(5, 15);
        cache.put(6, 16);
        Iterator<Map.Entry<Integer, Integer>> iterator = cache.iterator();
        List<Map.Entry<Integer, Integer>> resultList = new ArrayList<Map.Entry<Integer, Integer>>();
        while (iterator.hasNext()) {
            resultList.add(iterator.next());
        }
        Collections.sort(resultList, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o1.getKey() - o2.getKey();
            }
        });
        assertThat(resultList.get(0), is(Pair.newImmutableEntry(0, 10)));
        assertThat(resultList.get(1), is(Pair.newImmutableEntry(1, 11)));
        assertThat(resultList.get(2), is(Pair.newImmutableEntry(2, 12)));
        assertThat(resultList.get(3), is(Pair.newImmutableEntry(3, 13)));
        assertThat(resultList.get(4), is(Pair.newImmutableEntry(4, 14)));
        assertThat(resultList.get(5), is(Pair.newImmutableEntry(5, 15)));
        assertThat(resultList.get(6), is(Pair.newImmutableEntry(6, 16)));
        assertThat(resultList.size(), is(7));
    }
}
