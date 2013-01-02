package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.Pair;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/09/26, 16:54
 *
 * @author Hiroki Itoh
 */
public class LinkedHashMapIndexTest {

    protected <K, V> Map.Entry<K, V> e(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<K, V>(key, value);
    }

    protected <K, V> Index<K, V> createInstance(
            int initialCapacity, int capacity, boolean accessOrder) {
        return new LinkedHashMapIndex<K, V>(initialCapacity, capacity, accessOrder);
    }

    @Test
    public void testPut() {
        Index<Integer, Integer> index = createInstance(16, Integer.MAX_VALUE, true);

        assertThat(index.get(0), is(nullValue()));

        index.put(0, 1);
        index.put(0, 2);
        assertThat(index.get(0), is(2));

        index.put(1, 3);
        assertThat(index.get(0), is(2));
        assertThat(index.get(1), is(3));
        assertThat(index.get(2), is(nullValue()));
        assertThat(index.size(), is(2));
    }

    @Test
    public void testRemove() {
        Index<Integer, Integer> index = createInstance(16, Integer.MAX_VALUE, true);

        index.put(0, 10);
        index.put(1, 11);
        index.put(2, 12);
        index.put(3, 13);
        index.put(4, 14);
        assertThat(index.size(), is(5));

        index.remove(2);
        assertThat(index.get(2), is(nullValue()));
        assertThat(index.size(), is(4));

        index.remove(new HashSet<Integer>(Arrays.asList(1, 2, 4)));
        assertThat(index.get(0), is(10));
        assertThat(index.get(1), is(nullValue()));
        assertThat(index.get(2), is(nullValue()));
        assertThat(index.get(3), is(13));
        assertThat(index.get(4), is(nullValue()));
        assertThat(index.size(), is(2));
    }

    @Test
    public void testContains() {
        Index<Integer, Integer> index = createInstance(16, Integer.MAX_VALUE, true);

        index.put(0, 10);
        index.put(1, 11);
        index.put(2, 12);
        assertThat(index.containsKey(0), is(true));
        assertThat(index.containsKey(1), is(true));
        assertThat(index.containsKey(2), is(true));
        assertThat(index.containsKey(-1), is(false));
        assertThat(index.containsKey(3), is(false));
        assertThat(index.containsAllKeys(new HashSet<Integer>(Arrays.asList(0, 1, 2))), is(true));
        assertThat(index.containsAllKeys(new HashSet<Integer>(Arrays.asList(0, 1, 2, 3))), is(false));
        assertThat(index.containsAllKeys(new HashSet<Integer>(Arrays.asList(1, 2))), is(true));
        assertThat(index.containsAllKeys(new HashSet<Integer>(Arrays.asList(-1, 1, 2))), is(false));
    }

    @Test
    public void testClear() {
        Index<Integer, Integer> index = createInstance(16, Integer.MAX_VALUE, true);

        index.put(0, 10);
        index.put(1, 11);
        index.put(2, 12);
        assertThat(index.size(), is(3));

        index.clear();
        assertThat(index.size(), is(0));
    }

    @Test
    public void testMaxSize() {
        Index<Integer, Integer> index = createInstance(16, Integer.MAX_VALUE, true);

        assertThat(index.maxSize(), is(Integer.MAX_VALUE));

        index = createInstance(3, 3, true);
        assertThat(index.maxSize(), is(3));
    }

    @Test
    public void testEventListenerOnPut() {
        Index<Integer, Integer> index = createInstance(5, 5, true);
        MockIndexEventListener<Integer, Integer> eventListener;

        eventListener = new MockIndexEventListener<Integer, Integer>();
        index.setEventListener(eventListener);
        index.put(0, 10);
        MockIndexEventListener.Args<Integer, Integer> args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.index, is(sameInstance(index)));
        assertThat(args.key, is(0));
        assertThat(args.value, is(10));

        eventListener.argsList.clear();
        index.setEventListener(null);
        index.put(1, 11);
        assertThat(eventListener.argsList.size(), is(0));
    }

    @Test
    public void testEventListenerOnPutMulti() {
        Index<Integer, Integer> index = createInstance(5, 5, true);
        MockIndexEventListener<Integer, Integer> eventListener;

        eventListener = new MockIndexEventListener<Integer, Integer>();
        Collection<Map.Entry<Integer, Integer>> list = Pair.newImmutableEntries(2, 12, 3, 13, 4, 14);
        index.setEventListener(eventListener);
        index.put(list);
        MockIndexEventListener.Args<Integer, Integer> args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.index, is(sameInstance(index)));
        assertThat(args.key, is(2));
        assertThat(args.value, is(12));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.index, is(sameInstance(index)));
        assertThat(args.key, is(3));
        assertThat(args.value, is(13));
        args = eventListener.argsList.get(2);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.index, is(sameInstance(index)));
        assertThat(args.key, is(4));
        assertThat(args.value, is(14));
        assertThat(eventListener.argsList.size(), is(3));

        eventListener.argsList.clear();
        list = Pair.newImmutableEntries(1, 21, 2, 22, 5, 25, 6, 26);
        index.put(list);
        args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.key, is(1));
        assertThat(args.value, is(21));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.key, is(2));
        assertThat(args.value, is(22));
        args = eventListener.argsList.get(2);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.key, is(5));
        assertThat(args.value, is(25));
        args = eventListener.argsList.get(3);
        assertThat(args.method, is(MockIndexEventListener.Method.CACHE_OUT));
        assertThat(args.key, is(3));
        assertThat(args.value, is(13));
        args = eventListener.argsList.get(4);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.key, is(6));
        assertThat(args.value, is(26));
        assertThat(eventListener.argsList.size(), is(5));
    }

    @Test
    public void testEventListenerOnRemove() {
        Index<Integer, Integer> index = createInstance(5, 5, true);
        MockIndexEventListener<Integer, Integer> eventListener;

        eventListener = new MockIndexEventListener<Integer, Integer>();
        index.setEventListener(eventListener);
        index.remove(3);
        assertThat(eventListener.argsList.size(), is(0));

        index.setEventListener(null);
        index.put(3, 13);
        index.setEventListener(eventListener);
        index.remove(3);
        MockIndexEventListener.Args<Integer, Integer> args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockIndexEventListener.Method.REMOVE));
        assertThat(args.key, is(3));
        assertThat(args.value, is(13));

        index.put(1, 11);
        index.put(2, 12);
        index.put(3, 13);
        index.put(4, 14);
        index.put(5, 15);
        eventListener.argsList.clear();
        index.put(6, 16); // サイズ規定により onDiscard(), その後に onPut
        args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockIndexEventListener.Method.CACHE_OUT));
        assertThat(args.key, is(1));
        assertThat(args.value, is(11));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockIndexEventListener.Method.PUT));
        assertThat(args.key, is(6));
        assertThat(args.value, is(16));
    }

    @Test
    public void testEventListenerOnRemoveMulti() {
        Index<Integer, Integer> index = createInstance(5, 5, true);
        MockIndexEventListener<Integer, Integer> eventListener;

        eventListener = new MockIndexEventListener<Integer, Integer>();
        index.setEventListener(eventListener);
        Set<Integer> keySet = new HashSet<Integer>(Arrays.asList(-1, 1, 4));
        index.remove(keySet);
        assertThat(eventListener.argsList.isEmpty(), is(true));


        eventListener.argsList.clear();
        index.setEventListener(null);
        Collection<Map.Entry<Integer, Integer>> list = Pair.newImmutableEntries(1, 11, 4, 14);
        index.put(list);
        index.setEventListener(eventListener);
        index.remove(keySet);
        MockIndexEventListener.Args<Integer, Integer> args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockIndexEventListener.Method.REMOVE));
        assertThat(args.key, is(1));
        assertThat(args.value, is(11));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockIndexEventListener.Method.REMOVE));
        assertThat(args.key, is(4));
        assertThat(args.value, is(14));
        assertThat(eventListener.argsList.size(), is(2));
    }

    @Test
    public void testEventListenerOnClear() {
        Index<Integer, Integer> index = createInstance(5, 5, true);
        MockIndexEventListener<Integer, Integer> eventListener;

        eventListener = new MockIndexEventListener<Integer, Integer>();
        index.put(0, 0);
        index.put(1, 1);
        index.setEventListener(eventListener);
        index.clear();
        MockIndexEventListener.Args<Integer, Integer> args = eventListener.argsList.get(0);
        args = eventListener.argsList.get(0);
        assertThat(args.method, is(MockIndexEventListener.Method.REMOVE));
        assertThat(args.key, is(0));
        assertThat(args.value, is(0));
        args = eventListener.argsList.get(1);
        assertThat(args.method, is(MockIndexEventListener.Method.REMOVE));
        assertThat(args.key, is(1));
        assertThat(args.value, is(1));
        assertThat(eventListener.argsList.size(), is(2));
        index.setEventListener(null);
    }

    @Test
    public void testPutIfAbsent() {
        Index<Integer, Integer> index = createInstance(5, 5, false);
        Integer result;

        result = index.putIfAbsent(1, 10);
        assertThat(result, is(nullValue()));
        assertThat(index.get(1), is(10));

        result = index.putIfAbsent(1, 20);
        assertThat(result, is(10));
        assertThat(index.get(1), is(10));
    }
}
