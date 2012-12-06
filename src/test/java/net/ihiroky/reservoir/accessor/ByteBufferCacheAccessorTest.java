package net.ihiroky.reservoir.accessor;

import net.ihiroky.reservoir.CacheAccessor;
import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.PropertiesSupport;
import net.ihiroky.reservoir.Ref;
import net.ihiroky.reservoir.index.SimpleIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
public class ByteBufferCacheAccessorTest {

    AbstractBlockedByteCacheAccessor<Integer, String> byteBufferCacheAccessor;
    Index<Integer, Ref<String>> index;
    Properties props;
    Set<CacheAccessor<?, ?>> disposeSet;

    protected AbstractBlockedByteCacheAccessor<Integer, String> createInstance() {
        return new ByteBufferCacheAccessor<Integer, String>();
    }

    protected Properties createProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("reservoir.ByteBufferCacheAccessor.direct", "false");
        props.setProperty("reservoir.ByteBufferCacheAccessor.size", "64");
        props.setProperty("reservoir.ByteBufferCacheAccessor.blockSize", "8");
        props.setProperty("reservoir.ByteBufferCacheAccessor.partitions", "4");
        props.setProperty("reservoir.ByteBufferCacheAccessor.coder", "net.ihiroky.reservoir.coder.StringCoder");
        return props;
    }

    @Before
    public void before() throws Exception {
        byteBufferCacheAccessor = createInstance();
        index = new SimpleIndex<Integer, Ref<String>>();
        props = createProperties();

        disposeSet = new HashSet<CacheAccessor<?, ?>>();
        disposeSet.add(byteBufferCacheAccessor);
    }

    @After
    public void after() {
        for (CacheAccessor<?, ?> b : disposeSet) {
            b.dispose();
        }
    }

    @Test
    public void testPrepare() {
        Properties props2 = PropertiesSupport.builder()
                .set(ByteBufferCacheAccessor.class, "blockSize", "8")
                .set(ByteBufferCacheAccessor.class, "partition.1.direct", "true")
                .set(ByteBufferCacheAccessor.class, "partition.1.capacity", "16")
                .set(ByteBufferCacheAccessor.class, "partition.2.direct", "true")
                .set(ByteBufferCacheAccessor.class, "partition.2.capacity", "16")
                .set(ByteBufferCacheAccessor.class, "partition.3.direct", "true")
                .set(ByteBufferCacheAccessor.class, "partition.3.capacity", "16")
                .set(ByteBufferCacheAccessor.class, "partition.4.direct", "true")
                .set(ByteBufferCacheAccessor.class, "partition.4.capacity", "16")
                .properties();

        ByteBufferCacheAccessor<Integer, String> instance = new ByteBufferCacheAccessor<Integer, String>();
        try {
            byteBufferCacheAccessor.prepare("ByteBufferCacheAccessorTest#testPrepare", this.props);
            instance.prepare("ByteBufferCacheAccessorTest#testPrepare2", props2);
            assertThat(instance.getPartitions(), is(byteBufferCacheAccessor.getPartitions()));
            assertThat(instance.getWholeBlocks(), is(byteBufferCacheAccessor.getWholeBlocks()));
        } finally {
            instance.dispose();
        }

    }

    @Test
    public void testUpdate() {
        byteBufferCacheAccessor.prepare(ByteBufferCacheAccessor.class + "#testUpdate", props);

        byteBufferCacheAccessor.update(0, "01234567890123456789", index);
        assertThat(index.get(0).value(), is("01234567890123456789"));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(3L));

        char[] a = new char[70];
        Arrays.fill(a, '0');
        try {
            byteBufferCacheAccessor.update(0, new String(a), index);
        } catch (IllegalStateException ise) {
            assertThat(ise.getMessage(), is("no free block."));
        }
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(0L));
    }

    @Test
    public void testUpdateMulti() {
        byteBufferCacheAccessor.prepare(ByteBufferCacheAccessorTest.class + "#testUpdateMulti", props);

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
        byteBufferCacheAccessor.prepare(ByteBufferCacheAccessor.class + "#testRemove", props);

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
        byteBufferCacheAccessor.prepare(ByteBufferCacheAccessorTest.class + "#testRemoveMulti", props);

        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(0, "hoge");
        map.put(1, "hoge");
        map.put(2, "hoge");
        byteBufferCacheAccessor.update(map, index);
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(3L));
        byteBufferCacheAccessor.remove(index.remove(Arrays.asList(0, 1, 2)));
        assertThat(byteBufferCacheAccessor.getAllocatedBlocks(), is(0L));
    }
}
