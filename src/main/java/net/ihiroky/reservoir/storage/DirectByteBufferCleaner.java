package net.ihiroky.reservoir.storage;

import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * Deallocates a direct buffer.
 *
 * ref.
 * http://stackoverflow.com/questions/8462200/examples-of-forcing-freeing-of-native-memory-direct-bytebuffer-has-allocated-us
 * http://static.netty.io/3.6/xref/org/jboss/netty/util/internal/ByteBufferUtil.html
 * http://www.ibm.com/developerworks/java/library/j-nativememory-aix/
 *
 * @author Hiroki Itoh
 */
public final class DirectByteBufferCleaner {

    private DirectByteBufferCleaner() {
        throw new AssertionError();
    }

    /**
     * Deallocates a specified byte buffer.
     *
     * @param byteBuffer a byte buffer to be deallocated
     */
    static void clean(ByteBuffer byteBuffer) {
        if (!byteBuffer.isDirect()) {
            return;
        }

        DirectBuffer direct = (DirectBuffer) byteBuffer;
        Cleaner cleaner = direct.cleaner();
        cleaner.clean();
    }
}
