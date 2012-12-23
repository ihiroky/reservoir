package net.ihiroky.reservoir.storage;

import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.PropertiesSupport;
import net.ihiroky.reservoir.coder.SerializableCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Created on 12/10/31, 15:18
 *
 * @author Hiroki Itoh
 */
public class FileStorageAccessor<K, V> extends AbstractBlockedByteStorageAccessor<K, V> {

    private Logger logger = LoggerFactory.getLogger(FileStorageAccessor.class);

    private static final String KEY_PREFIX = "reservoir.";
    private static final String KEY_BLOCK_SIZE_SUFFIX = ".blockSize";
    private static final String KEY_REJECTED_ALLOCATION_HANDLER_SUFFIX = ".rejectedAllocationHandler";

    private static final String KEY_CODER_SUFFIX = ".coder";
    private static final String KEY_FILE_PREFIX = ".file.";
    private static final String KEY_PATH_SUFFIX = ".path";
    private static final String KEY_SIZE_SUFFIX = ".size";
    private static final String KEY_MODE_SUFFIX = ".mode";

    private static final String KEY_PARTITIONS_SUFFIX = ".partitions";
    private static final String KEY_DIRECTORY_SUFFIX = ".directory";

    private static final int DEFAULT_SIZE = 512 * 1024 * 1024;
    private static final int DEFAULT_BLOCK_SIZE = 512;
    private static final int DEFAULT_PARTITIONS = 4;

    private volatile Collection<RandomAccessFile> randomAccessFiles = Collections.emptyList();

    private String getClassName() {
        String className = this.getClass().getName();
        int lastDotIndex = className.lastIndexOf('.');
        return className.substring(lastDotIndex + 1);
    }

    static String key(String className, String suffix) {
        return KEY_PREFIX + className + suffix;
    }

    @Override
    public void prepare(String name, Properties props) {
        final String cn = getClassName();
        int blockSize = PropertiesSupport.intValue(props, key(cn, KEY_BLOCK_SIZE_SUFFIX), DEFAULT_BLOCK_SIZE);
        Coder<V> coder = PropertiesSupport.newInstance(props, key(cn, KEY_CODER_SUFFIX), SerializableCoder.class);
        String rah = props.getProperty(key(cn, KEY_REJECTED_ALLOCATION_HANDLER_SUFFIX));
        Collection<FileInfo> fileInfos = parseFileInfo(props, cn);
        if (fileInfos.size() > 0) {
            try {
                prepare(name, blockSize, coder, fileInfos, createRejectedAllocationHandler(rah));
                return;
            } catch (IOException ioe) {
                throw new RuntimeException("failed to prepare " + cn + " " + fileInfos, ioe);
            }
        }

        long totalSize = PropertiesSupport.longValue(props, key(cn, KEY_SIZE_SUFFIX), DEFAULT_SIZE);
        int partitions = PropertiesSupport.intValue(props, key(cn, KEY_PARTITIONS_SUFFIX), DEFAULT_PARTITIONS);
        String directory = props.getProperty(key(cn, KEY_DIRECTORY_SUFFIX));
        BulkInfo bulkInfo = new BulkInfo(totalSize, blockSize, partitions);
        try {
            prepare(name, coder, new File(directory), FileInfo.Mode.READ_WRITE, bulkInfo, createRejectedAllocationHandler(rah));
        } catch (IOException ioe) {
            throw new RuntimeException("failed to prepare " + cn + " " + bulkInfo, ioe);
        }
    }

    private Collection<FileInfo> parseFileInfo(Properties props, String className) {
        Map<Integer, FileInfo> fileInfoMap = new TreeMap<Integer, FileInfo>();
        String fileKeyPrefix = KEY_PREFIX + className + KEY_FILE_PREFIX;
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(fileKeyPrefix)) {
                continue;
            }
            int idEndIndex = key.indexOf('.', fileKeyPrefix.length());
            if (idEndIndex == -1) {
                continue;
            }
            String value = props.getProperty(key);
            try {
                int id = Integer.parseInt(key.substring(fileKeyPrefix.length(), idEndIndex));
                FileInfo fileInfo = fileInfoMap.get(id);
                if (fileInfo == null) {
                    fileInfo = new FileInfo();
                    fileInfoMap.put(id, fileInfo);
                }
                if (key.endsWith(KEY_PATH_SUFFIX)) {
                    fileInfo.setPath(value);
                } else if (key.endsWith(KEY_SIZE_SUFFIX)) {
                    fileInfo.size = Long.parseLong(value);
                } else if (key.endsWith(KEY_MODE_SUFFIX)) {
                    fileInfo.setMode(value);
                }
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("failed to parse property. key:" + key + ", value:" + value, nfe);
            }
        }
        return fileInfoMap.values();
    }
    public void prepare(final String name, Coder<V> coder,
                        final File directory, final FileInfo.Mode mode,
                        BulkInfo bulkInfo, RejectedAllocationHandler rejectedAllocationHandler)
            throws IOException {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (coder == null) {
            throw new NullPointerException("coder must not be null.");
        }
        if (bulkInfo == null) {
            throw new NullPointerException("bulkInfo must not be null.");
        }
        if (directory == null) {
            throw new NullPointerException("directory must not be null.");
        }
        if (mode == null) {
            throw new NullPointerException("mode must not be null.");
        }

        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IllegalStateException(directory + " is not a directory.");
            }
        } else {
            if (!directory.mkdirs()) {
                throw new IllegalStateException("failed to create a directory : " + directory);
            }
            logger.warn("[prepare] create directories {}.", directory);
        }

        randomAccessFiles = new ArrayList<RandomAccessFile>();
        BulkInfo.ByteBlockManagerAllocator allocator = new BulkInfo.ByteBlockManagerAllocator() {

            @Override
            public ByteBlockManager allocate(long size, int blockSize, int index) {
                File file = new File(directory, name + '-' + String.format("%05d", index));
                try {
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, mode.value);
                    randomAccessFiles.add(randomAccessFile);
                    randomAccessFile.setLength(size);
                    return createInstance(file.getName(), file, randomAccessFile, blockSize);
                } catch (IOException ioe) {
                    throw new RuntimeException("failed to allocate ByteBlockManager : " + file, ioe);
                }
            }
        };
        ByteBlockManager[] managers= bulkInfo.allocate(allocator, getMaxPartitionSize(bulkInfo.blockSize));
        prepare(name, managers, bulkInfo.blockSize, coder, rejectedAllocationHandler);
    }

    public void prepare(String name, int blockSize, Coder<V> coder,
                        Collection<FileInfo> fileInfos, RejectedAllocationHandler rejectedAllocationHandler)
            throws IOException {

        if (name == null || coder == null || fileInfos == null) {
            throw new NullPointerException();
        }
        if (blockSize < BlockedFile.MIN_BYTES_PER_BLOCK) {
            throw new IllegalArgumentException("blockSize must be greater than or equal "
                    + BlockedFile.MIN_BYTES_PER_BLOCK);
        }
        if (fileInfos.isEmpty()) {
            throw new IllegalArgumentException("fileUnitList is empty.");
        }

        ByteBlockManager[] array = new ByteBlockManager[fileInfos.size()];
        Collection<RandomAccessFile> randomAccessFiles = new ArrayList<RandomAccessFile>(fileInfos.size());
        int count = 0;
        for (FileInfo fileInfo : fileInfos) {
            if (fileInfo.size < blockSize) {
                fileInfo.size = blockSize;
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileInfo.file, fileInfo.mode.value);
            randomAccessFiles.add(randomAccessFile);
            randomAccessFile.setLength(fileInfo.size);
            ByteBlockManager byteBlockManager = createInstance(
                    name + '-' + String.format("%05d", count), fileInfo.file, randomAccessFile, blockSize);
            array[count++] = byteBlockManager;
        }

        this.randomAccessFiles = randomAccessFiles;
        prepare(name, array, blockSize, coder, rejectedAllocationHandler);
    }

    protected long getMaxPartitionSize(int blockSize) {
        return Long.MAX_VALUE;
    }

    protected ByteBlockManager createInstance(
            String name, File file, RandomAccessFile randomAccessFile, int blockSize) throws IOException {
        BlockedFile blockedFile = new BlockedFile(file.getAbsolutePath(), randomAccessFile, blockSize);
        blockedFile.setName(name);
        return blockedFile;
    }

    @Override
    public void dispose() {
        super.dispose();
        synchronized (this) {
            for (RandomAccessFile randomAccessFile : randomAccessFiles) {
                try {
                    randomAccessFile.close();
                } catch (IOException ioe) {
                    logger.warn("[dispose]", ioe);
                }
            }
        }
    }

}
