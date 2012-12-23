package net.ihiroky.reservoir;

import net.ihiroky.reservoir.storage.ByteBufferStorageAccessor;
import net.ihiroky.reservoir.storage.FileStorageAccessor;
import net.ihiroky.reservoir.storage.FileInfo;
import net.ihiroky.reservoir.storage.RejectedAllocationHandler;
import net.ihiroky.reservoir.storage.RejectedAllocationPolicy;
import net.ihiroky.reservoir.coder.ByteArrayCoder;
import net.ihiroky.reservoir.coder.SerializableCoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
    List<StorageAccessor<?, ?>> disposeStorageAccessorList;

    static final int DEFAULT_MAX_DIRECT_MEMORY_SIZE = 64 * 1024 * 1024;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() {
        disposeList = new ArrayList<Cache<?, ?>>();
        disposeQueueList = new ArrayList<BasicQueue<?>>();
        disposeStorageAccessorList = new ArrayList<StorageAccessor<?, ?>>();
    }

    @After
    public void after() {
        for (Cache<?, ?> cache : disposeList) {
            cache.dispose();
        }
        for (BasicQueue<?> queue : disposeQueueList) {
            queue.dispose();
        }
        for (StorageAccessor<?, ?> ca : disposeStorageAccessorList) {
            ca.dispose();
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

        serializableCache.delete(0);
        assertThat(serializableCache.get(0), is(nullValue()));
        assertThat(serializableCache.containsKey(0), is(false));
        assertThat(serializableCache.size(), is(2));
    }

    @Test
    public void testCreateQueue() throws Exception {
        BasicQueue<byte[]> queue = Reservoir.newQueueBuilder()
                .cacheAccessorType(Reservoir.CacheAccessorType.BYTE_BUFFER)
                .property("reservoir.ByteBufferStorageAccessor.direct", "true")
                .property("reservoir.ByteBufferStorageAccessor.size", "8192")
                .property("reservoir.ByteBufferStorageAccessor.blockSize", "256")
                .property("reservoir.ByteBufferStorageAccessor.coder", "net.ihiroky.reservoir.coder.ByteArrayCoder")
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

    @Test
    public void testByteBufferCacheAccessorBuilder() {
        final int usagePercent = 10;
        final int blockSize = 256;
        final int partitionsHint = 3;
        final boolean direct = true;
        final Class<?> coderClass = ByteArrayCoder.class;
        final RejectedAllocationHandler rah = RejectedAllocationPolicy.WAIT_FOR_FREE_BLOCK;

        StorageAccessor<Object, byte[]> ca = Reservoir.newByteBufferCacheAccessorBuilder()
                .direct(direct).usagePercent(usagePercent).blockSize(blockSize).partitionsHint(partitionsHint)
                .coderClass(coderClass).rejectedAllocationHandler(rah)
                .build(ReservoirTest.class.getName() + "#testByteBufferCacheAssessorBuilder");
        disposeStorageAccessorList.add(ca);

        ByteBufferStorageAccessor<Object, byte[]> bbca = (ByteBufferStorageAccessor<Object, byte[]>) ca;

        assertThat(bbca.getWholeBlocks(), is(DEFAULT_MAX_DIRECT_MEMORY_SIZE / 256L / usagePercent));
        assertThat(bbca.getPartitions(), is(partitionsHint));
        assertThat(bbca.getBlockSize(), is(blockSize));
        assertThat(bbca.getEncoderClassName(), is(ByteArrayCoder.class.getName() + "$ByteArrayEncoder"));
        assertThat(bbca.getDecoderClassName(), is(ByteArrayCoder.class.getName() + "$ByteArrayDecoder"));
        assertThat(bbca.getRejectedAllocationHandlerName(), is(rah.toString()));
    }

    @Test
    public void testByteBufferCacheAccessorBuilderWithByteBufferInfo() {
        StorageAccessor<Object, byte[]> ca = Reservoir.newByteBufferCacheAccessorBuilder()
                .blockSize(128).byteBufferInfo(true, 256).byteBufferInfo(true, 256)
                .build(ReservoirTest.class.getName() + "#testByteBufferCacheAccessorBuilderWithByteBufferInfo");
        disposeStorageAccessorList.add(ca);

        ByteBufferStorageAccessor<Object, byte[]> bbca = (ByteBufferStorageAccessor<Object, byte[]>)ca;
        assertThat(bbca.getWholeBlocks(), is(4L));
        assertThat(bbca.getBlockSize(), is(128));
        assertThat(bbca.getPartitions(), is(2));
    }

    @Test
    public void testByteBufferCacheAccessorBuilderDefault() {

        StorageAccessor<Object, byte[]> ca = Reservoir.newByteBufferCacheAccessorBuilder()
                .build(ReservoirTest.class.getName() + "#testByteBufferCacheAssessorBuilderDefault");
        disposeStorageAccessorList.add(ca);

        ByteBufferStorageAccessor<Object, byte[]> bbca = (ByteBufferStorageAccessor<Object, byte[]>) ca;
        assertThat(bbca.getWholeBlocks(), is(DEFAULT_MAX_DIRECT_MEMORY_SIZE / 512L / 10 * 9 + 1)); // 1 : fraction
        assertThat(bbca.getPartitions(), is(1));
        assertThat(bbca.getBlockSize(), is(512));
        assertThat(bbca.getEncoderClassName(), is(SerializableCoder.class.getName() + "$SerializableEncoder"));
        assertThat(bbca.getDecoderClassName(), is(SerializableCoder.class.getName() + "$SerializableDecoder"));
        assertThat(bbca.getRejectedAllocationHandlerName(), is(RejectedAllocationPolicy.ABORT.toString()));
    }

    private void deleteRecursively(File d) throws Exception {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                deleteRecursively(f);
            }
            return;
        }
        if ( ! d.delete()) {
            throw new IOException(d.getName());
        }
    }

    @Test
    public void testFileCacheAccessorBuilder() throws Exception {
        File directory = folder.newFolder();
        StorageAccessor<Object, byte[]> ca = Reservoir.newFileCacheAccessorBuilder()
                .totalSize(DEFAULT_MAX_DIRECT_MEMORY_SIZE).blockSize(256).partitionsHint(4)
                .directory(directory).mode(FileInfo.Mode.READ_WRITE).coderClass(ByteArrayCoder.class)
                .rejectedAllocationHandler(RejectedAllocationPolicy.WAIT_FOR_FREE_BLOCK)
                .build(ReservoirTest.class.getName() + "#testFileCacheAccessorBuilder");
        disposeStorageAccessorList.add(ca);

        FileStorageAccessor<Object, byte[]> fca = (FileStorageAccessor<Object, byte[]>) ca;
        try {
            assertThat(fca.getWholeBlocks(), is(DEFAULT_MAX_DIRECT_MEMORY_SIZE / 256L));
            assertThat(fca.getPartitions(), is(4));
            assertThat(fca.getBlockSize(), is(256));
            assertThat(fca.getEncoderClassName(), is(ByteArrayCoder.class.getName() + "$ByteArrayEncoder"));
            assertThat(fca.getDecoderClassName(), is(ByteArrayCoder.class.getName() + "$ByteArrayDecoder"));
            assertThat(fca.getRejectedAllocationHandlerName(), is(RejectedAllocationPolicy.WAIT_FOR_FREE_BLOCK.toString()));

            File[] files = directory.listFiles();
            Arrays.sort(files);
            for (int i = 0; i < 4; i++) {
                assertThat(files[i].getName().endsWith("-0000" + i), is(true));
            }
            assertThat(files.length, is(4));
        } finally {
            deleteRecursively(directory);
        }
    }

    @Test
    public void testFileCacheAccessorBuilderWithFileInfo() throws IOException {
        File file0 = folder.newFile();
        File file1 = folder.newFile();
        StorageAccessor<Object, byte[]> ca = Reservoir.newFileCacheAccessorBuilder().blockSize(128)
                .fileInfo(file0.getPath(), 256, FileInfo.Mode.READ_WRITE)
                .fileInfo(file1.getPath(), 256, FileInfo.Mode.READ_WRITE)
                .build(ReservoirTest.class.getName() + "#testByteBufferCacheAccessorBuilderWithFileInfo");
        disposeStorageAccessorList.add(ca);

        FileStorageAccessor<Object, byte[]> fca = (FileStorageAccessor<Object, byte[]>)ca;
        assertThat(fca.getWholeBlocks(), is(4L));
        assertThat(fca.getBlockSize(), is(128));
        assertThat(fca.getPartitions(), is(2));
    }

    @Test
    public void testFileCacheAccessorBuilderDefault() throws Exception {

        StorageAccessor<Object, byte[]> ca = Reservoir.newFileCacheAccessorBuilder()
                .build(ReservoirTest.class.getName() + "#testFileCacheAccessorBuilderDefault");
        disposeStorageAccessorList.add(ca);

        FileStorageAccessor<Object, byte[]> fca = (FileStorageAccessor<Object, byte[]>) ca;
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("BuilderDefault"); // part of StorageAccessor name
            }
        };
        try {
            assertThat(fca.getWholeBlocks(), is(DEFAULT_MAX_DIRECT_MEMORY_SIZE / 512L));
            assertThat(fca.getPartitions(), is(1));
            assertThat(fca.getBlockSize(), is(512));
            assertThat(fca.getEncoderClassName(), is(SerializableCoder.class.getName() + "$SerializableEncoder"));
            assertThat(fca.getDecoderClassName(), is(SerializableCoder.class.getName() + "$SerializableDecoder"));
            assertThat(fca.getRejectedAllocationHandlerName(), is(RejectedAllocationPolicy.ABORT.toString()));

            File[] files = new File(".").listFiles(filenameFilter);
            assertThat(files[0].getName().endsWith("-00000"), is(true));
            assertThat(files.length, is(1));
        } finally {
            for (File f : new File(".").listFiles(filenameFilter)) {
                if ( ! f.delete()) {
                    System.out.println("failed to delete file : " + f);
                }
            }
        }
    }

    // Skip testMemoryFileCacheAccessorBuilder(). the same as testFileCacheAccessorBuilder() except of
    // FileStorageAccessor#createInstance(). This method is test on testMemoryMappedFileCacheAccessorBuilderDefault().

    @Test
    public void testMemoryMappedFileCacheAccessorBuilderDefault() throws Exception {

        StorageAccessor<Object, byte[]> ca = Reservoir.newMemoryMappedFileCacheAccessorBuilder()
                .build(ReservoirTest.class.getName() + "#testMemofyMappedFileCacheAccessorBuilderDefault");
        disposeStorageAccessorList.add(ca);

        FileStorageAccessor<Object, byte[]> fca = (FileStorageAccessor<Object, byte[]>) ca;
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("BuilderDefault"); // part of StorageAccessor name
            }
        };
        try {
            assertThat(fca.getWholeBlocks(), is(DEFAULT_MAX_DIRECT_MEMORY_SIZE / 512L));
            assertThat(fca.getPartitions(), is(1));
            assertThat(fca.getBlockSize(), is(512));
            assertThat(fca.getEncoderClassName(), is(SerializableCoder.class.getName() + "$SerializableEncoder"));
            assertThat(fca.getDecoderClassName(), is(SerializableCoder.class.getName() + "$SerializableDecoder"));
            assertThat(fca.getRejectedAllocationHandlerName(), is(RejectedAllocationPolicy.ABORT.toString()));

            File[] files = new File(".").listFiles(filenameFilter);
            assertThat(files[0].getName().endsWith("-00000"), is(true));
            assertThat(files.length, is(1));
        } finally {
            for (File f : new File(".").listFiles(filenameFilter)) {
                if ( ! f.delete()) {
                    System.out.println("failed to delete file : " + f);
                }
            }
        }
    }

}
