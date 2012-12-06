package net.ihiroky.reservoir;

import net.ihiroky.reservoir.coder.SerializableCoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/05, 11:37
 *
 * @author Hiroki Itoh
 */
public class ReservoirTest {

    List<Cache<?, ?>> disposeList;
    List<BasicQueue<?>> disposeQueueList;

    @Before
    public void before() {
        disposeList = new ArrayList<Cache<?, ?>>();
        disposeQueueList = new ArrayList<BasicQueue<?>>();
    }

    @After
    public void after() {
        for (Cache<?, ?> cache : disposeList) {
            cache.dispose();
        }
        for (BasicQueue<?> queue : disposeQueueList) {
            queue.dispose();
        }
    }

    @Test
    public void testCreateOffHeapCache() throws Exception {
        Cache<Integer, Serializable> serializableCache =
                Reservoir.createOffHeapCache("serializable", 8192, 256, 4, new SerializableCoder<Serializable>());
        disposeList.add(serializableCache);

        serializableCache.put(0, "fizzbuzz");
        serializableCache.put(1, new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3)));
        serializableCache.put(2, "あいうえお");

        assertThat((String) serializableCache.get(0), is("fizzbuzz"));
        assertThat(serializableCache.get(1), is((Serializable) Arrays.asList(0, 1, 2, 3)));
        assertThat((String) serializableCache.get(2), is("あいうえお"));
        assertThat(serializableCache.size(), is(3));

        serializableCache.remove(0);
        assertThat(serializableCache.get(0), is(nullValue()));
        assertThat(serializableCache.containsKey(0), is(false));
        assertThat(serializableCache.size(), is(2));
    }

    @Test
    public void testCreateQueue() throws Exception {
        BasicQueue<byte[]> queue = Reservoir.newQueueBuilder()
                .cacheAccessorType(Reservoir.CacheAccessorType.BYTE_BUFFER)
                .property("reservoir.ByteBufferCacheAccessor.direct", "true")
                .property("reservoir.ByteBufferCacheAccessor.size", "8192")
                .property("reservoir.ByteBufferCacheAccessor.blockSize", "256")
                .property("reservoir.ByteBufferCacheAccessor.coder", "net.ihiroky.reservoir.coder.ByteArrayCoder")
                .property("reservoir.ByteArrayCoder.compress.enabled", "true")
                .build();
        disposeQueueList.add(queue);

        byte[][] b = new byte[3][];
        for (int i = 0; i < b.length; i++) {
            b[i] = new byte[512];
            for (int j = 0; j < b[i].length; j++) {
                b[i][j] = (byte) ((i + '0') % 10);
            }
        }
        queue.offer(b[0]);
        queue.offer(b[1]);
        queue.offer(b[2]);
        // TODO check memory (byte buffer) usage.
        assertThat(queue.poll(), is(b[0]));
        assertThat(queue.poll(), is(b[1]));
        assertThat(queue.poll(), is(b[2]));
        assertThat(queue.poll(), is(nullValue()));
    }
}
