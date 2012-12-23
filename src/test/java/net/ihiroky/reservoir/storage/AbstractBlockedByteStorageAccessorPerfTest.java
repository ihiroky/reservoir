package net.ihiroky.reservoir.storage;

import net.ihiroky.reservoir.StorageAccessor;
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
public class AbstractBlockedByteStorageAccessorPerfTest {

    Set<StorageAccessor<?, ?>> disposeSet;

    static final int size = 8192;
    static final int blockSize = 128;
    static final int partitions = 16;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        disposeSet = new HashSet<StorageAccessor<?, ?>>();
    }

    @After
    public void after() {
        for (StorageAccessor<?, ?> b : disposeSet) {
            b.dispose();
        }
    }

    private void testUpdateMultiThread(String name, Properties props,
                                       final AbstractBlockedByteStorageAccessor<Integer, String> accessor) throws Exception {
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
                .set("reservoir.ByteBufferStorageAccessor.direct", "false")
                .set("reservoir.ByteBufferStorageAccessor.size", String.valueOf(size))
                .set("reservoir.ByteBufferStorageAccessor.blockSize", String.valueOf(blockSize))
                .set("reservoir.ByteBufferStorageAccessor.partitions", String.valueOf(partitions))
                .set("reservoir.ByteBufferStorageAccessor.coder", "net.ihiroky.reservoir.coder.SimpleStringCoder")
                .set("times", "500000").build();

        testUpdateMultiThread("ByteBufferStorageAccessorTest", props,
                new ByteBufferStorageAccessor<Integer, String>());
    }

    @Test
    public void testMemoryMappedFileCacheAccessor() throws Exception {

        PropertiesSupport.PropertiesBuilder builder = PropertiesSupport.builder();
        builder.set("reservoir.MemoryMappedFileStorageAccessor.blockSize", String.valueOf(blockSize));
        builder.set("reservoir.MemoryMappedFileStorageAccessor.coder", "net.ihiroky.reservoir.coder.SimpleStringCoder");
        int partitionSize = size / partitions;
        for (int i = 0; i < partitions; i++) {
            builder.set("reservoir.MemoryMappedFileStorageAccessor.file." + i + ".size", String.valueOf(partitionSize));
            builder.set("reservoir.MemoryMappedFileStorageAccessor.file." + i + ".path", folder.newFile().getPath());
        }
        builder.set("times", "500000");

        testUpdateMultiThread("MemoryMappedFileStorageAccessorTest", builder.build(),
                new MemoryMappedFileStorageAccessor<Integer, String>());
    }

    @Test
    public void testFileCacheAccessor() throws Exception {
        PropertiesSupport.PropertiesBuilder builder = PropertiesSupport.builder();
        builder.set("reservoir.FileStorageAccessor.blockSize", String.valueOf(blockSize));
        builder.set("reservoir.FileStorageAccessor.coder", "net.ihiroky.reservoir.coder.SimpleStringCoder");
        int partitionSize = size / partitions;
        for (int i = 0; i < partitions; i++) {
            builder.set("reservoir.FileStorageAccessor.file." + i + ".size", String.valueOf(partitionSize));
            builder.set("reservoir.FileStorageAccessor.file." + i + ".path", folder.newFile().getPath());
        }
        builder.set("times", "150000");

        testUpdateMultiThread("FileStorageAccessor", builder.build(),
                new FileStorageAccessor<Integer, String>());
    }
}
