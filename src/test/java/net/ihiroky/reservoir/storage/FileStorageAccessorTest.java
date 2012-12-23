package net.ihiroky.reservoir.storage;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

/**
 * Created on 12/10/31, 16:20
 *
 * @author Hiroki Itoh
 */
public class FileStorageAccessorTest extends ByteBufferStorageAccessorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
    protected AbstractBlockedByteStorageAccessor<Integer, String> createInstance() {
        return new FileStorageAccessor<Integer, String>();
    }

    @Override
    protected Properties createProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("reservoir.FileStorageAccessor.blockSize", "8");
        props.setProperty("reservoir.FileStorageAccessor.coder", "net.ihiroky.reservoir.coder.StringCoder");
        for (int i = 0; i < 4; i++) {
            File file = folder.newFile();
            props.setProperty("reservoir.FileStorageAccessor.file." + i + ".path", file.getPath());
            props.setProperty("reservoir.FileStorageAccessor.file." + i + ".size", "16");
        }
        return props;
    }
}
