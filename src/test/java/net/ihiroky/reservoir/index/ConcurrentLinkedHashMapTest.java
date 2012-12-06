package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.Pair;
import org.hamcrest.CoreMatchers;
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
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/10, 15:11
 *
 * @author Hiroki Itoh
 */
public class ConcurrentLinkedHashMapTest {

    @Test
    public void testPutIfAbsent() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        Integer result = map.putIfAbsent(0, 10);
        assertThat(result, is(nullValue()));
        assertThat(map.get(0), is(10));
        assertThat(map.size(), is(1));

        result = map.putIfAbsent(0, 100);
        assertThat(result, is(10));
        assertThat(map.get(0), is(10));
        assertThat(map.size(), is(1));
    }

    @Test
    public void testRemove() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        assertThat(map.orderQueue().size(), is(3));

        Integer result = map.remove(1);
        assertThat(result, is(11));
        assertThat(map.get(1), is(nullValue()));
        assertThat(map.size(), is(2));
        assertThat(map.orderQueue().size(), is(2));
        result = map.remove(-1);
        assertThat(result, is(nullValue()));
        assertThat(map.size(), is(2));
        assertThat(map.orderQueue().size(), is(2));
        result = map.put(1, 11);
        assertThat(result, is(nullValue()));
        assertThat(map.size(), is(3));
        assertThat(map.orderQueue().size(), is(3));
    }

    @Test
    public void testReplace() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        Integer result = map.replace(1, 111);
        assertThat(result, is(11));
        assertThat(map.get(1), is(111));
        assertThat(map.size(), is(3));
    }

    @Test
    public void testReplace2() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        boolean result = map.replace(1, 12, 111);
        assertThat(result, is(false));
        assertThat(map.get(1), is(11));
        result = map.replace(1, 11, 111);
        assertThat(result, is(true));
        assertThat(map.get(1), is(111));
        assertThat(map.size(), is(3));
    }

    @Test
    public void testIsEmpty() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        assertThat(map.isEmpty(), is(true));
        map.put(0, 0);
        assertThat(map.isEmpty(), is(false));
        map.remove(0);
        assertThat(map.isEmpty(), is(true));
        map.put(0, 0);
        map.clear();
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    public void testContainsKey() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        assertThat(map.containsKey(0), is(true));
        assertThat(map.containsKey(1), is(true));
        assertThat(map.containsKey(2), is(true));
        assertThat(map.containsKey(-1), is(false));
    }

    @Test
    public void testContainsValue() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        assertThat(map.containsValue(0), is(false));
        assertThat(map.containsValue(1), is(false));
        assertThat(map.containsValue(2), is(false));
        assertThat(map.containsValue(10), is(true));
        assertThat(map.containsValue(11), is(true));
        assertThat(map.containsValue(12), is(true));
    }

    @Test
    public void testRemove2() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        boolean result = map.remove(1, 111);
        assertThat(result, is(false));
        assertThat(map.size(), is(3));
        result = map.remove(1, 11);
        assertThat(result, is(true));
        assertThat(map.get(1), is(nullValue()));
        assertThat(map.size(), is(2));
        result = map.remove(0, 0);
        assertThat(result, is(false));
        assertThat(map.size(), is(2));
    }

    @Test
    public void testPutAll() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        Map<Integer, Integer> put = new HashMap<Integer, Integer>();
        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        map.putAll(put);
        assertThat(map.get(0), is(10));
        assertThat(map.get(1), is(11));
        assertThat(map.get(2), is(12));
        assertThat(map.size(), is(3));
    }

    @Test
    public void testClear() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        map.clear();
        assertThat(map.size(), is(0));
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    public void testKeySet() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        Set<Integer> keySet = map.keySet();
        assertThat(keySet.contains(0), is(true));
        assertThat(keySet.contains(1), is(true));
        assertThat(keySet.contains(2), is(true));
        assertThat(keySet.contains(-1), is(false));
        assertThat(keySet.size(), is(3));
        Iterator<Integer> iterator = keySet.iterator();
        List<Integer> resultList = new ArrayList<Integer>();
        assertThat(iterator.hasNext(), is(true));
        resultList.add(iterator.next());
        assertThat(iterator.hasNext(), is(true));
        resultList.add(iterator.next());
        assertThat(iterator.hasNext(), is(true));
        resultList.add(iterator.next());
        assertThat(iterator.hasNext(), is(false));
        Collections.sort(resultList);
        assertThat(resultList, is(Arrays.asList(0, 1, 2)));
    }

    @Test
    public void testValues() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        Collection<Integer> values = map.values();
        assertThat(values.size(), is(3));
        Iterator<Integer> iterator = values.iterator();
        List<Integer> resultList = new ArrayList<Integer>();
        assertThat(iterator.hasNext(), is(true));
        resultList.add(iterator.next());
        assertThat(iterator.hasNext(), is(true));
        resultList.add(iterator.next());
        assertThat(iterator.hasNext(), is(true));
        resultList.add(iterator.next());
        assertThat(iterator.hasNext(), is(false));
        Collections.sort(resultList);
        assertThat(resultList, is(Arrays.asList(10, 11, 12)));
    }

    @Test
    public void testEntrySet() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        map.put(3, 13);
        map.put(4, 14);
        map.put(5, 15);
        map.put(6, 16);
        Set<Map.Entry<Integer, Integer>> entrySet = map.entrySet();
        assertThat(entrySet.size(), is(7));
        Iterator<Map.Entry<Integer, Integer>> iterator = entrySet.iterator();
        List<Map.Entry<Integer, Integer>> resultList = new ArrayList<Map.Entry<Integer, Integer>>();
        while (iterator.hasNext()) {
            resultList.add(Pair.newImmutableEntry(iterator.next())); // for assertion
        }
        Collections.sort(resultList, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o1.getKey() - o2.getKey();
            }
        });
        assertThat(resultList, CoreMatchers.is(
                Pair.<Integer, Integer>newImmutableEntries(0, 10, 1, 11, 2, 12, 3, 13, 4, 14, 5, 15, 6, 16)));
        assertThat(resultList.size(), is(7));
    }

    @Test
    public void testEntrySetSizeLimited() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>() {
            @Override
            protected RemoveEldestPolicy removeEldestEntry(Entry<Integer, Integer> eldestEntry) {
                return (size() <= 5) ? RemoveEldestPolicy.DO_NOTHING : RemoveEldestPolicy.REMOVE;
            }
        };

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        map.put(3, 13);
        map.put(4, 14);
        map.put(5, 15);
        map.put(6, 16);
        Set<Map.Entry<Integer, Integer>> entrySet = map.entrySet();
        assertThat(entrySet.size(), is(5));
        Iterator<Map.Entry<Integer, Integer>> iterator = entrySet.iterator();
        List<Map.Entry<Integer, Integer>> resultList = new ArrayList<Map.Entry<Integer, Integer>>();
        while (iterator.hasNext()) {
            resultList.add(Pair.newImmutableEntry(iterator.next()));
        }
        Collections.sort(resultList, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o1.getKey() - o2.getKey();
            }
        });
        assertThat(resultList, CoreMatchers.is(
                Pair.<Integer, Integer>newImmutableEntries(2, 12, 3, 13, 4, 14, 5, 15, 6, 16)));
        assertThat(resultList.size(), is(5));
    }

    @Test
    public void test() throws Exception {
        ConcurrentLinkedHashMap<Integer, Integer> map =
                new ConcurrentLinkedHashMap<Integer, Integer>(32, 0.75f, 1, true);

        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        map.get(1);
        Iterator<Map.Entry<Integer, Integer>> iterator = map.orderQueue().iterator();
        List<Map.Entry<Integer, Integer>> resultList = new ArrayList<Map.Entry<Integer, Integer>>();
        assertThat(iterator.hasNext(), is(true));
        resultList.add(Pair.newImmutableEntry(iterator.next()));
        assertThat(iterator.hasNext(), is(true));
        resultList.add(Pair.newImmutableEntry(iterator.next()));
        assertThat(iterator.hasNext(), is(true));
        resultList.add(Pair.newImmutableEntry(iterator.next()));
        assertThat(iterator.hasNext(), is(false));
        assertThat(resultList, is(Pair.newImmutableEntries(0, 10, 2, 12, 1, 11)));

        map = new ConcurrentLinkedHashMap<Integer, Integer>(32, 0.75f, 32, false);
        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        map.get(1);
        iterator = map.orderQueue().iterator();
        resultList = new ArrayList<Map.Entry<Integer, Integer>>();
        assertThat(iterator.hasNext(), is(true));
        resultList.add(Pair.newImmutableEntry(iterator.next()));
        assertThat(iterator.hasNext(), is(true));
        resultList.add(Pair.newImmutableEntry(iterator.next()));
        assertThat(iterator.hasNext(), is(true));
        resultList.add(Pair.newImmutableEntry(iterator.next()));
        assertThat(iterator.hasNext(), is(false));
        assertThat(resultList, is(Pair.newImmutableEntries(0, 10, 1, 11, 2, 12)));
    }

    @Test
    public void testToString() {
        ConcurrentLinkedHashMap<Integer, Integer> map =
                new ConcurrentLinkedHashMap<Integer, Integer>(32, 0.75f, 1, true);

        assertThat(map.toString(), is("[]"));
        map.put(0, 10);
        map.put(1, 11);
        map.put(2, 12);
        assertThat(map.toString(), is("[0=10, 1=11, 2=12]"));
    }
}
