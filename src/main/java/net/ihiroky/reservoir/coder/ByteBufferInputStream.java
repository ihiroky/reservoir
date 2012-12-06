package net.ihiroky.reservoir.coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created on 12/10/04, 19:26
 *
 * @author Hiroki Itoh
 */
public class ByteBufferInputStream extends InputStream {

    private ByteBuffer byteBuffer;

    static InputStream createInputStream(ByteBuffer byteBuffer) {
        return (byteBuffer.hasArray()) ?
                new ByteArrayInputStream(
                        byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.remaining()) :
                new ByteBufferInputStream(byteBuffer);
    }

    private ByteBufferInputStream(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            throw new NullPointerException("byteBuffer must not be null.");
        }
        this.byteBuffer = byteBuffer;
    }

    @Override
    public int read() throws IOException {
        return byteBuffer.hasRemaining() ? (byteBuffer.get() & 0xFF) : -1;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
        int remaining = byteBuffer.remaining();
        if (remaining == 0) {
            return -1;
        }
        int bytesToRead = (remaining >= length) ? length : remaining;
        byteBuffer.get(bytes, offset, bytesToRead);
        return bytesToRead;
    }
}
