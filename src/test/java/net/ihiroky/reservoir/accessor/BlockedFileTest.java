package net.ihiroky.reservoir.accessor;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 12/10/31, 13:11
 *
 * @author Hiroki Itoh
 */
public class BlockedFileTest extends BlockedByteBufferTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
    protected ByteBlockManager createBlockedByteManager() throws Exception {
        File file = folder.newFile();
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(64);
        return new BlockedFile(file.getPath(), raf, 16);
    }

    @Override
    protected List<Number> asList(Number... numbers) {
        List<Number> list = new ArrayList<Number>(numbers.length);
        for (Number number : numbers) {
            list.add(number.longValue());
        }
        return list;
    }
}
