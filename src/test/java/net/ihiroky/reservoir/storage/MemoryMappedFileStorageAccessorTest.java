package net.ihiroky.reservoir.storage;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

/**
 * Created on 12/10/31, 17:17
 *
 * @author Hiroki Itoh
 */
public class MemoryMappedFileStorageAccessorTest extends ByteBufferStorageAccessorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
    protected AbstractBlockedByteStorageAccessor<Integer, String> createInstance() {
        return new MemoryMappedFileStorageAccessor<Integer, String>();
    }

    @Override
    protected Properties createProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("reservoir.MemoryMappedFileStorageAccessor.blockSize", "8");
        props.setProperty("reservoir.MemoryMappedFileStorageAccessor.coder", "net.ihiroky.reservoir.coder.StringCoder");
        for (int i = 0; i < 4; i++) {
            File file = folder.newFile();
            props.setProperty("reservoir.MemoryMappedFileStorageAccessor.file." + i + ".path", file.getPath());
            props.setProperty("reservoir.MemoryMappedFileStorageAccessor.file." + i + ".size", "16");
        }
        return props;
    }
}
