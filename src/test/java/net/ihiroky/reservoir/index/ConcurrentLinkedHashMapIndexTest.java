package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.Pair;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/09/26, 16:54
 *
 * @author Hiroki Itoh
 */
public class ConcurrentLinkedHashMapIndexTest extends LinkedHashMapIndexTest {

    @Override
    protected <K, V> Index<K, V> createInstance(
            int initialCapacity, int capacity, boolean accessOrder) {
        return new ConcurrentLinkedHashMapIndex<K, V>(initialCapacity, capacity, 1, accessOrder);
    }

    @Override
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

}
