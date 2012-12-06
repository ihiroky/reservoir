package net.ihiroky.reservoir.coder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created on 12/10/04, 19:26
 *
 * @author Hiroki Itoh
 */
public class ByteBufferOutputStream extends OutputStream {

    ByteBuffer byteBuffer;

    ByteBufferOutputStream(int initialCapacity) {
        byteBuffer = ByteBuffer.allocate(initialCapacity);
    }

    private void ensureCapacity(int minimumIncrement) {
        int c = byteBuffer.capacity();
        int newSize = c / 2 * 3;
        if (newSize < c + minimumIncrement) {
            newSize = c + minimumIncrement;
        }
        ByteBuffer t = ByteBuffer.allocate(newSize);
        byteBuffer.flip();
        t.put(byteBuffer);
        byteBuffer = t;
    }

    @Override
    public void write(int b) throws IOException {
        if (!byteBuffer.hasRemaining()) {
            ensureCapacity(1);
        }
        byteBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        if (byteBuffer.remaining() < length) {
            ensureCapacity(length);
        }
        byteBuffer.put(bytes, offset, length);
    }

    @Override
    public void close() {
        byteBuffer.flip();
    }
}
