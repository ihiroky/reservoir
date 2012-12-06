package net.ihiroky.reservoir.accessor;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

/**
 * Created on 12/10/31, 16:20
 *
 * @author Hiroki Itoh
 */
public class FileCacheAccessorTest extends ByteBufferCacheAccessorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
    protected AbstractBlockedByteCacheAccessor<Integer, String> createInstance() {
        return new FileCacheAccessor<Integer, String>();
    }

    @Override
    protected Properties createProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("reservoir.FileCacheAccessor.blockSize", "8");
        props.setProperty("reservoir.FileCacheAccessor.coder", "net.ihiroky.reservoir.coder.StringCoder");
        for (int i = 0; i < 4; i++) {
            File file = folder.newFile();
            props.setProperty("reservoir.FileCacheAccessor.file." + i + ".path", file.getPath());
            props.setProperty("reservoir.FileCacheAccessor.file." + i + ".size", "16");
        }
        return props;
    }
}
