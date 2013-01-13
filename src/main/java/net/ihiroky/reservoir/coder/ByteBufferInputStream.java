package net.ihiroky.reservoir.coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Contains an internal {@code java.nio.ByteBuffer} that may be read from the stream. An internal
 * counter held by {@code java.nio.ByteBuffer} keeps track of the next byte to be supplied by the {@code read} method.
 * <p>
 * Closing a <tt>ByteBufferInputStream</tt> has no effect. The methods in this class can be called after the stream
 * has been closed without generating an {@code IOException}.
 *
 * @author Hiroki Itoh
 */
public class ByteBufferInputStream extends InputStream {

    /** an internal buffer to be read */
    private ByteBuffer byteBuffer;

    /**
     * Creates {@code java.io.ByteArrayInputStream} if a specified {@code byteBuffer} is backed by an accessible
     * byte array. Otherwise, creates {@code ByteBufferInputStream}.
     * @param byteBuffer a byte buffer to be read from the returned stream
     * @return the stream to read the {@code byteBuffer}
     */
    static InputStream createInputStream(ByteBuffer byteBuffer) {
        return byteBuffer.hasArray() ?
                new ByteArrayInputStream(
                        byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.remaining()) :
                new ByteBufferInputStream(byteBuffer);
    }

    /**
     * Constructs a new {@code ByteBufferInputStream}.
     * @param byteBuffer a byte buffer to be read from
     */
    private ByteBufferInputStream(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            throw new NullPointerException("byteBuffer must not be null.");
        }
        this.byteBuffer = byteBuffer;
    }

    /**
     * Reads next byte from the internal buffer. The buffer has no remaining, return -1.
     * @return next byte from the internal buffer. Return -1 if the buffer has no remaining
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        return byteBuffer.hasRemaining() ? (byteBuffer.get() & 0xFF) : -1;
    }

    /**
     * Reads up to {@code length} bytes of data from the internal buffer into
     * an array of bytes.  An attempt is made to read as many as {@code len} bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * @param bytes the array of bytes into which the data is read
     * @param offset the start offset in array {@code bytes} at which the data is written.
     * @param length the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the internal
     * buffer has no remaining..
     */
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
