package net.ihiroky.reservoir.storage;

import net.ihiroky.reservoir.StorageAccessor;
import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.PropertiesSupport;
import net.ihiroky.reservoir.Ref;
import net.ihiroky.reservoir.Reservoir;
import net.ihiroky.reservoir.index.SimpleIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/03, 14:20
 *
 * @author Hiroki Itoh
 */
public class ByteBufferStorageAccessorTest {

    AbstractBlockedByteStorageAccessor<Integer, String> byteBufferCacheAccessor;
    Index<Integer, Ref<String>> index;
    Properties props;
    Set<StorageAccessor<?, ?>> disposeSet;

    protected AbstractBlockedByteStorageAccessor<Integer, String> createInstance() {
        return new ByteBufferStorageAccessor<Integer, String>();
    }

    protected Properties createProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("reservoir.ByteBufferStorageAccessor.direct", "false");
        props.setProperty("reservoir.ByteBufferStorageAccessor.size", "64");
        props.setProperty("reservoir.ByteBufferStorageAccessor.blockSize", "8");
        props.setProperty("reservoir.ByteBufferStorageAccessor.partitions", "4");
        props.setProperty("reservoir.ByteBufferStorageAccessor.coder", "net.ihiroky.reservoir.coder.StringCoder");
        return props;
    }

    @Before
    public void before() throws Exception {
        byteBufferCacheAccessor = createInstance();
        index = new SimpleIndex<Integer, Ref<String>>();
        props = createProperties();

        disposeSet = new HashSet<StorageAccessor<?, ?>>();
        disposeSet.add(byteBufferCacheAccessor);
    }

    @After
    public void after() {
        for (StorageAccessor<?, ?> b : disposeSet) {
            b.dispose();
        }
    }

    @Test
    public void testPrepare() {
        Properties props2 = PropertiesSupport.builder()
                .set(ByteBufferStorageAccessor.class, "blockSize", "8")
                .set(ByteBufferStorageAccessor.class, "partition.1.direct", "true")
                .set(ByteBufferStorageAccessor.class, "partition.1.capacity", "16")
                .set(ByteBufferStorageAccessor.class, "partition.2.direct", "true")
                .set(ByteBufferStorageAccessor.class, "partition.2.capacity", "16")
                .set(ByteBufferStorageAccessor.class, "partition.3.direct", "true")
                .set(ByteBufferStorageAccessor.class, "partition.3.capacity", "16")
                .set(ByteBufferStorageAccessor.class, "partition.4.direct", "true")
                .set(ByteBufferStorageAccessor.class, "partition.4.capacity", "16")
                .build();

        ByteBufferStorageAccessor<Integer, String> instance = new ByteBufferStorageAccessor<Integer, String>();
        try {
            byteBufferCacheAccessor.prepare("ByteBufferStorageAccessorTest#testPrepare", this.props);
            instance.prepare("ByteBufferStorageAccessorTest#testPrepare2", props2);
            assertThat(instance.getPartitions(), is(byteBufferCacheAccessor.getPartitions()));
            assertThat(instance.getWholeBlocks(), is(byteBufferCacheAccessor.getWholeBlocks()));
        } finally {
            instance.dispose();
        }
    }

    @Test
    public void testPrepareUsagePercent() {
        Properties props2 = PropertiesSupport.builder()
                .set(ByteBufferStorageAccessor.class, "direct", "true")
                .set(ByteBufferStorageAccessor.class, "usagePercent", "10")
                .set(ByteBufferStorageAccessor.class, "blockSize", "256")
                .set(ByteBufferStorageAccessor.class, "partitions", "1")
                .build();

        ByteBufferStorageAccessor<Integer, String> instance = new ByteBufferStorageAccessor<Integer, String>();
        disposeSet.add(instance);
        instance.prepare("ByteBufferStorageAccessorTest#testPrepareUsagePercent", props2);
        assertThat(instance.getWholeBlocks(), is(Reservoir.getMaxDirectMemorySize() / 256 / 10));
        assertThat(instance.getBlockSize(), is(256));
        assertThat(instance.getPartitions(), is(1));
    }

    @Test
    public void testUpdate() {
        byteBufferCacheAccessor.prepare(ByteBufferStorageAccessor.class + "#testUpdate", props);

        byteBufferCacheAccessor.update(0, "01234567890123456789", index);
        assertThat(index.get(0).value(), is("01234567890123456789"));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(3L));

        char[] a = new char[70];
        Arrays.fill(a, '0');
        try {
            byteBufferCacheAccessor.update(0, new String(a), index);
        } catch (IllegalStateException ise) {
            assertThat(ise.getMessage(), is("No free block is found. A ByteBlock allocation is aborted."));
        }
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(0L));
    }

    @Test
    public void testUpdateMulti() {
        byteBufferCacheAccessor.prepare(ByteBufferStorageAccessorTest.class + "#testUpdateMulti", props);

        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(0, "hoge");
        map.put(1, "fizzbuzzfizz");
        byteBufferCacheAccessor.update(map, index);
        assertThat(index.get(0).value(), is("hoge"));
        assertThat(index.get(1).value(), is("fizzbuzzfizz"));
        assertThat(index.size(), is(2));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(3L));

        map.clear();
        map.put(1, "hogehoge");
        map.put(2, "fizzbuzzfizzbuzz");
        byteBufferCacheAccessor.update(map, index);
        assertThat(index.get(1).value(), is("hogehoge"));
        assertThat(index.get(2).value(), is("fizzbuzzfizzbuzz"));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(4L));
    }

    @Test
    public void testRemove() {
        byteBufferCacheAccessor.prepare(ByteBufferStorageAccessor.class + "#testRemove", props);

        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(0, "hoge");
        map.put(1, "fizzbuzzfizz");
        byteBufferCacheAccessor.update(map, index);

        byteBufferCacheAccessor.remove(0, index.remove(0));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(2L));

        byteBufferCacheAccessor.remove(0, index.remove(0)); // double remove has no side effect.
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(2L));

        byteBufferCacheAccessor.remove(1, index.remove(1));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(0L));
    }

    @Test
    public void testRemoveMulti() {
        byteBufferCacheAccessor.prepare(ByteBufferStorageAccessorTest.class + "#testRemoveMulti", props);

        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(0, "hoge");
        map.put(1, "hoge");
        map.put(2, "hoge");
        byteBufferCacheAccessor.update(map, index);
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(3L));
        byteBufferCacheAccessor.remove(index.remove(Arrays.asList(0, 1, 2)));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(0L));
    }

    @Test
    public void testWaitForFreeBlock() {
        props.setProperty(
                PropertiesSupport.key(byteBufferCacheAccessor.getClass(), "rejectedAllocationHandler"),
                "WAIT_FOR_FREE_BLOCK");
        byteBufferCacheAccessor.prepare(ByteBufferStorageAccessorTest.class + "#testWaitForFreeBlock", props);
        final List<Ref<String>> refList = new ArrayList<Ref<String>>();
        final int stopTime = 100;
        for (int i = 0; i < 4; i++) {
            Ref<String> ref = byteBufferCacheAccessor.create(i, "0123456789012345");
            refList.add(ref);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(stopTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byteBufferCacheAccessor.remove(0, refList.get(0));
            }
        });
        long start = System.currentTimeMillis();
        t.start();
        byteBufferCacheAccessor.create(4, "0123456789012345"); // Wait until two free block is found.
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed >= stopTime, is(true));
    }
}