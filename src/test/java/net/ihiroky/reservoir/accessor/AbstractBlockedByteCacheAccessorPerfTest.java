package net.ihiroky.reservoir.accessor;

import net.ihiroky.reservoir.CacheAccessor;
import net.ihiroky.reservoir.ConcurrentTestUtil;
import net.ihiroky.reservoir.Index;
import net.ihiroky.reservoir.PropertiesSupport;
import net.ihiroky.reservoir.Ref;
import net.ihiroky.reservoir.index.ConcurrentLRUIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created on 12/11/22, 9:42
 *
 * @author Hiroki Itoh
 */
public class AbstractBlockedByteCacheAccessorPerfTest {

    Set<CacheAccessor<?, ?>> disposeSet;

    static final int size = 8192;
    static final int blockSize = 128;
    static final int partitions = 16;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        disposeSet = new HashSet<CacheAccessor<?, ?>>();
    }

    @After
    public void after() {
        for (CacheAccessor<?, ?> b : disposeSet) {
            b.dispose();
        }
    }

    private void testUpdateMultiThread(String name, Properties props,
                                       final AbstractBlockedByteCacheAccessor<Integer, String> accessor) throws Exception {
        accessor.prepare(name.concat("-testUpdateMultiThread"), props);
        disposeSet.add(accessor);

        char[] a = new char[500];
        Arrays.fill(a, 'a');
        final String str = new String(a);
        final Index<Integer, Ref<String>> index = new ConcurrentLRUIndex<Integer, Ref<String>>(16, 16);
        final int times = PropertiesSupport.intValue(props, "times", 100000);
        Runnable updater = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < times; i++) {
                    int key = (int) (Math.random() * 8);
                    accessor.update(key, str, index);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(4, updater, TimeUnit.MILLISECONDS);
        System.out.println(accessor.getClass() + "#testUpdateMultiThread : "
                + result.getElapsed() + " ms, " + times + " times, " + (times / result.getElapsed() * 1000) + " times/ms");
    }

    @Test
    public void testByteBufferCacheAccessor() throws Exception {
        Properties props = PropertiesSupport.builder()
                .set("reservoir.ByteBufferCacheAccessor.direct", "false")
                .set("reservoir.ByteBufferCacheAccessor.size", String.valueOf(size))
                .set("reservoir.ByteBufferCacheAccessor.blockSize", String.valueOf(blockSize))
                .set("reservoir.ByteBufferCacheAccessor.partitions", String.valueOf(partitions))
                .set("reservoir.ByteBufferCacheAccessor.coder", "net.ihiroky.reservoir.coder.SimpleStringCoder")
                .set("times", "500000").properties();

        testUpdateMultiThread("ByteBufferCacheAccessorTest", props,
                new ByteBufferCacheAccessor<Integer, String>());
    }

    @Test
    public void testMemoryMappedFileCacheAccessor() throws Exception {

        PropertiesSupport builder = PropertiesSupport.builder();
        builder.set("reservoir.MemoryMappedFileCacheAccessor.blockSize", String.valueOf(blockSize));
        builder.set("reservoir.MemoryMappedFileCacheAccessor.coder", "net.ihiroky.reservoir.coder.SimpleStringCoder");
        int partitionSize = size / partitions;
        for (int i = 0; i < partitions; i++) {
            builder.set("reservoir.MemoryMappedFileCacheAccessor.file." + i + ".size", String.valueOf(partitionSize));
            builder.set("reservoir.MemoryMappedFileCacheAccessor.file." + i + ".path", folder.newFile().getPath());
        }
        builder.set("times", "500000");

        testUpdateMultiThread("MemoryMappedFileCacheAccessorTest", builder.properties(),
                new MemoryMappedFileCacheAccessor<Integer, String>());
    }

    @Test
    public void testFileCacheAccessor() throws Exception {
        PropertiesSupport builder = PropertiesSupport.builder();
        builder.set("reservoir.FileCacheAccessor.blockSize", String.valueOf(blockSize));
        builder.set("reservoir.FileCacheAccessor.coder", "net.ihiroky.reservoir.coder.SimpleStringCoder");
        int partitionSize = size / partitions;
        for (int i = 0; i < partitions; i++) {
            builder.set("reservoir.FileCacheAccessor.file." + i + ".size", String.valueOf(partitionSize));
            builder.set("reservoir.FileCacheAccessor.file." + i + ".path", folder.newFile().getPath());
        }
        builder.set("times", "150000");

        testUpdateMultiThread("FileCacheAccessor", builder.properties(),
                new FileCacheAccessor<Integer, String>());
    }
}
