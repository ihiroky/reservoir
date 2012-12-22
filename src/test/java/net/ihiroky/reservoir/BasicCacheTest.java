package net.ihiroky.reservoir;

import net.ihiroky.reservoir.accessor.HeapCacheAccessor;
import net.ihiroky.reservoir.coder.JSONCoder;
import net.ihiroky.reservoir.index.LRUIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/09/26, 16:49
 *
 * @author Hiroki Itoh
 */
public class BasicCacheTest {

    private Set<BasicCache<?, ?>> disposeInAfterSet;

    @Before
    public void before() {
        disposeInAfterSet = new HashSet<BasicCache<?, ?>>();
    }

    @After
    public void after() {
        for (BasicCache<?, ?> basicCache : disposeInAfterSet) {
            basicCache.dispose();
        }
    }

    private <K, V> BasicCache<K, V> createBasicCache(int initialSize, int maxSize) {
        Index<K, Ref<V>> index = new LRUIndex<K, Ref<V>>(initialSize, maxSize);
        CacheAccessor<K, V> cacheAccessor = new HeapCacheAccessor<K, V>();
        BasicCache<K, V> basicCache = new BasicCache<K, V>("test", index, cacheAccessor);
        disposeInAfterSet.add(basicCache);
        return basicCache;
    }

    @Test
    public void testPut() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(32, Integer.MAX_VALUE);

        basicCache.put(1, 11);
        assertThat(basicCache.get(1), is(11));
        assertThat(basicCache.size(), is(1));

        basicCache.put(2, 22);
        assertThat(basicCache.get(2), is(22));
        assertThat(basicCache.size(), is(2));

        basicCache.put(2, 222);
        assertThat(basicCache.get(2), is(222));
        assertThat(basicCache.size(), is(2));
    }

    @Test
    public void testPutMulti() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(32, Integer.MAX_VALUE);

        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(1, 11);
        map.put(2, 22);
        basicCache.putAll(map);
        assertThat(basicCache.get(map.keySet()), is(map));
        assertThat(basicCache.size(), is(2));

        map.clear();
        map.put(2, 222);
        map.put(3, 333);
        basicCache.putAll(map);
        map.put(1, 11);
        assertThat(basicCache.get(map.keySet()), is(map));
        assertThat(basicCache.size(), is(3));
    }

    @Test
    public void testDelete() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(32, Integer.MAX_VALUE);

        basicCache.delete(0);
        assertThat(basicCache.size(), is(0));

        basicCache.put(1, 11);
        basicCache.put(2, 22);
        basicCache.put(3, 33);
        basicCache.delete(2);
        assertThat(basicCache.get(2), is(nullValue()));
        assertThat(basicCache.size(), is(2));

        basicCache.delete(1);
        assertThat(basicCache.get(1), is(nullValue()));
        assertThat(basicCache.size(), is(1));
    }

    @Test
    public void testDeleteMulti() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(32, Integer.MAX_VALUE);

        basicCache.delete(new HashSet<Integer>(Arrays.asList(0, 1)));
        assertThat(basicCache.size(), is(0));

        basicCache.put(1, 11);
        basicCache.put(2, 22);
        basicCache.put(3, 33);
        basicCache.put(4, 44);
        basicCache.delete(new HashSet<Integer>(Arrays.asList(2)));
        assertThat(basicCache.get(2), is(nullValue()));
        assertThat(basicCache.size(), is(3));

        basicCache.delete(new HashSet<Integer>(Arrays.asList(1, 4)));
        assertThat(basicCache.get(1), is(nullValue()));
        assertThat(basicCache.get(4), is(nullValue()));
        assertThat(basicCache.size(), is(1));
    }

    @Test
    public void testRemove() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(32, Integer.MAX_VALUE);

        basicCache.remove(0);
        assertThat(basicCache.size(), is(0));

        basicCache.put(1, 11);
        basicCache.put(2, 22);
        basicCache.put(3, 33);
        Integer ret = basicCache.remove(2);
        assertThat(ret, is(22));
        assertThat(basicCache.get(2), is(nullValue()));
        assertThat(basicCache.size(), is(2));

        ret = basicCache.remove(1);
        assertThat(ret, is(11));
        assertThat(basicCache.get(1), is(nullValue()));
        assertThat(basicCache.size(), is(1));
    }

    @Test
    public void testRemoveMulti() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(32, Integer.MAX_VALUE);

        basicCache.remove(new HashSet<Integer>(Arrays.asList(0, 1)));
        assertThat(basicCache.size(), is(0));

        basicCache.put(1, 11);
        basicCache.put(2, 22);
        basicCache.put(3, 33);
        basicCache.put(4, 44);
        Map<Integer, Integer> ret = basicCache.remove(new HashSet<Integer>(Arrays.asList(2)));
        assertThat(ret.get(2), is(22));
        assertThat(ret.size(), is(1));
        assertThat(basicCache.get(2), is(nullValue()));
        assertThat(basicCache.size(), is(3));

        ret = basicCache.remove(new HashSet<Integer>(Arrays.asList(1, 4)));
        assertThat(ret.get(1), is(11));
        assertThat(ret.get(4), is(44));
        assertThat(ret.size(), is(2));
        assertThat(basicCache.get(1), is(nullValue()));
        assertThat(basicCache.get(4), is(nullValue()));
        assertThat(basicCache.size(), is(1));
    }

    @Test
    public void testClear() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(32, Integer.MAX_VALUE);

        basicCache.clear();
        assertThat(basicCache.size(), is(0));

        basicCache.put(1, 11);
        basicCache.put(2, 22);
        basicCache.put(3, 33);
        basicCache.clear();
        assertThat(basicCache.size(), is(0));
    }

    @Test(timeout = 3000)
    public void testEventListener() throws Exception {
        BasicCache<Integer, Integer> basicCache = createBasicCache(3, 3);

        MockCacheEventListener<Integer, Integer> eventListener = new MockCacheEventListener<Integer, Integer>();
        basicCache.addEventListener(eventListener);
        basicCache.put(1, 11);
        basicCache.put(2, 22);
        basicCache.remove(1);
        basicCache.put(3, 33);
        basicCache.put(1, 111);
        while (eventListener.argsList.size() != 5) {
            Thread.sleep(10);
        }
        MockCacheEventListener.Args<Integer, Integer> args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(1, 11)));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(2, 22)));
        args = eventListener.argsList.get(2);
        assertThat(args.method, is(MockCacheEventListener.Method.REMOVE));
        assertThat(args.entry, is(Pair.newImmutableEntry(1, 11)));
        args = eventListener.argsList.get(3);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(3, 33)));
        args = eventListener.argsList.get(4);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(1, 111)));

        eventListener.argsList.clear();
        basicCache.put(4, 444);
        while (eventListener.argsList.size() != 2) {
            Thread.sleep(10);
        }
        args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockCacheEventListener.Method.CACHE_OUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(2, 22)));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(4, 444)));
    }

    @Test(timeout = 3000)
    public void testEventListenerMulti() throws Exception {
        BasicCache<Integer, Integer> basicCache = createBasicCache(3, 3);

        MockCacheEventListener<Integer, Integer> eventListener = new MockCacheEventListener<Integer, Integer>();
        basicCache.addEventListener(eventListener);
        Map<Integer, Integer> put0 = new HashMap<Integer, Integer>();
        put0.put(1, 11);
        put0.put(2, 22);
        put0.put(3, 33);
        Set<Integer> remove0 = new HashSet<Integer>(Arrays.asList(1, 2));
        basicCache.putAll(put0);
        basicCache.remove(remove0);
        while (eventListener.argsList.size() != 5) {
            Thread.sleep(10);
        }
        MockCacheEventListener.Args<Integer, Integer> args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(1, 11)));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(2, 22)));
        args = eventListener.argsList.get(2);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(3, 33)));
        args = eventListener.argsList.get(3);
        assertThat(args.method, is(MockCacheEventListener.Method.REMOVE));
        assertThat(args.entry, is(Pair.newImmutableEntry(1, 11)));
        args = eventListener.argsList.get(4);
        assertThat(args.method, is(MockCacheEventListener.Method.REMOVE));
        assertThat(args.entry, is(Pair.newImmutableEntry(2, 22)));
        assertThat(eventListener.argsList.size(), is(5));

        basicCache.clear();
        while (eventListener.argsList.size() != 6) {
            Thread.sleep(10);
        }
        eventListener.argsList.clear();
        Map<Integer, Integer> put1 = new HashMap<Integer, Integer>();
        put1.put(1, 111);
        put1.put(2, 222);
        put1.put(3, 333);
        put1.put(4, 444);
        put1.put(5, 555);
        basicCache.putAll(put1);
        while (eventListener.argsList.size() != 7) {
            Thread.sleep(10);
        }
        args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(1, 111)));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(2, 222)));
        args = eventListener.argsList.get(2);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(3, 333)));
        args = eventListener.argsList.get(3);
        assertThat(args.method, is(MockCacheEventListener.Method.CACHE_OUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(1, 111)));
        args = eventListener.argsList.get(4);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(4, 444)));
        args = eventListener.argsList.get(5);
        assertThat(args.method, is(MockCacheEventListener.Method.CACHE_OUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(2, 222)));
        args = eventListener.argsList.get(6);
        assertThat(args.method, is(MockCacheEventListener.Method.PUT));
        assertThat(args.entry, is(Pair.newImmutableEntry(5, 555)));
        assertThat(eventListener.argsList.size(), is(7));
    }

    @Test
    public void testIterator() {
        BasicCache<Integer, Integer> basicCache = createBasicCache(3, 3);
        basicCache.put(0, 10);
        basicCache.put(1, 11);
        basicCache.put(2, 12);
        basicCache.put(3, 13);
        Iterator<Map.Entry<Integer, Integer>> iterator = basicCache.iterator();
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
        assertThat(resultList.get(0), is(Pair.newImmutableEntry(1, 11)));
        assertThat(resultList.get(1), is(Pair.newImmutableEntry(2, 12)));
        assertThat(resultList.get(2), is(Pair.newImmutableEntry(3, 13)));
        assertThat(resultList.size(), is(3));
    }

    private String json(int key, int value) {
        return "{\"k\":" + key + ",\"v\":" + value + "}";
    }

    @Test
    public void testWriteTo() throws Exception {
        BasicCache<Integer, Integer> basicCache = createBasicCache(16, 16);
        for (int i = 0; i < 3; i++) {
            basicCache.put(i, i * i + i);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        basicCache.writeTo(out, new IntegerJSONCoder());
        assertThat(out.toString("UTF-8"), is("[" + json(0, 0) + "," + json(1, 2) + "," + json(2, 6) + "]"));
    }

    @Test
    public void testReadFrom() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(
                ("[" + json(0, 0) + "," + json(1, 2) + "," + json(2, 6) + "]").getBytes("UTF-8"));
        BasicCache<Integer, Integer> basicCache = createBasicCache(16, 16);
        basicCache.readFrom(in, new IntegerJSONCoder());
        assertThat(basicCache.get(0), is(0));
        assertThat(basicCache.get(1), is(2));
        assertThat(basicCache.get(2), is(6));
        assertThat(basicCache.size(), is(3));
    }
}

class IntegerJSONCoder extends JSONCoder<Integer, Integer> {
    @Override
    protected void writeKey(JsonWriter writer, Integer key) throws Exception {
        writer.setNumber(key);
    }

    @Override
    protected void writeValue(JsonWriter writer, Integer value) throws Exception {
        writer.setNumber(value);
    }

    @Override
    protected Integer readKey(JsonReader reader) throws Exception {
        return reader.getInt();
    }

    @Override
    protected Integer readValue(JsonReader reader) throws Exception {
        return reader.getInt();
    }

    @Override
    protected String toKeyString(Integer key) throws Exception {
        return String.valueOf(key);
    }

    @Override
    protected Integer toKey(String key) throws Exception {
        return Integer.parseInt(key);
    }
}
