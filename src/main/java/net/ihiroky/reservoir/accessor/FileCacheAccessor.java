package net.ihiroky.reservoir.accessor;

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
public class FileCacheAccessor<K, V> extends AbstractBlockedByteCacheAccessor<K, V> {

    private Logger logger = LoggerFactory.getLogger(FileCacheAccessor.class);

    private static final String KEY_PREFIX = "reservoir.";
    private static final String KEY_BLOCK_SIZE_SUFFIX = ".blockSize";
    private static final String KEY_CODER_SUFFIX = ".coder";
    private static final String KEY_FILE_PREFIX = ".file.";
    private static final String KEY_PATH_SUFFIX = ".path";
    private static final String KEY_SIZE_SUFFIX = ".size";
    private static final String KEY_MODE_SUFFIX = ".mode";

    private volatile Collection<RandomAccessFile> randomAccessFiles = Collections.emptyList();

    private String getClassName() {
        String className = this.getClass().getName();
        int lastDotIndex = className.lastIndexOf('.');
        return className.substring(lastDotIndex + 1);
    }

    @Override
    public void prepare(String name, Properties props) {
        final String cn = getClassName();
        int blockSize = PropertiesSupport.intValue(
                props, KEY_PREFIX + cn + KEY_BLOCK_SIZE_SUFFIX, 1024);
        Map<Integer, FileInfo> fileUnitMap = new TreeMap<Integer, FileInfo>();
        String fileKeyPrefix = KEY_PREFIX + cn + KEY_FILE_PREFIX;
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
                FileInfo fileInfo = fileUnitMap.get(id);
                if (fileInfo == null) {
                    fileInfo = new FileInfo();
                    fileUnitMap.put(id, fileInfo);
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
        @SuppressWarnings("unchecked")
        Coder<V> coder = (Coder<V>) PropertiesSupport.newInstance(
                props, KEY_PREFIX + cn + KEY_CODER_SUFFIX, SerializableCoder.class);

        try {
            prepare(name, blockSize, coder, fileUnitMap.values());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void prepare(String name, int blockSize, Coder<V> coder,
                        Collection<FileInfo> fileInfos) throws IOException {

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
                    name + '-' + String.format("%5d", count), fileInfo.file, randomAccessFile, blockSize);
            array[count++] = byteBlockManager;
        }

        this.randomAccessFiles = randomAccessFiles;
        prepare(name, array, blockSize, coder);
    }

    protected ByteBlockManager createInstance(
            String name, File file, RandomAccessFile randomAccessFile, int blockSize) throws IOException {
        BlockedFile blockedFile =
                new BlockedFile(file.getAbsolutePath(), randomAccessFile, blockSize);
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
