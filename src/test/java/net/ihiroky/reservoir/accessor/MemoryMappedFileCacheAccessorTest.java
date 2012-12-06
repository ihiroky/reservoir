package net.ihiroky.reservoir.accessor;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

/**
 * Created on 12/10/31, 17:17
 *
 * @author Hiroki Itoh
 */
public class MemoryMappedFileCacheAccessorTest extends ByteBufferCacheAccessorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
    protected AbstractBlockedByteCacheAccessor<Integer, String> createInstance() {
        return new MemoryMappedFileCacheAccessor<Integer, String>();
    }

    @Override
    protected Properties createProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("reservoir.MemoryMappedFileCacheAccessor.blockSize", "8");
        props.setProperty("reservoir.MemoryMappedFileCacheAccessor.coder", "net.ihiroky.reservoir.coder.StringCoder");
        for (int i = 0; i < 4; i++) {
            File file = folder.newFile();
            props.setProperty("reservoir.MemoryMappedFileCacheAccessor.file." + i + ".path", file.getPath());
            props.setProperty("reservoir.MemoryMappedFileCacheAccessor.file." + i + ".size", "16");
        }
        return props;
    }
}
