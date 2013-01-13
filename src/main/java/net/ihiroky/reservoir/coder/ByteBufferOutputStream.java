package net.ihiroky.reservoir.coder;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * This class implements an output stream in which the data is
 * written into a internal {@code java.nio.ByteBuffer}. The {@code ByteBuffer} automatically grows as data
 * is written to it. The data can be retrieved using field {@code byteBuffer}.
 * <p>
 * Closing a {@code ByteArrayOutputStream} has no effect. The methods in this class can be called
 * after the stream has been closed without generating an {@code IOException}.
 *
 * @author Hiroki Itoh
 */
public class ByteBufferOutputStream extends OutputStream {

    /** {@code ByteBuffer} into which the data is written */
    ByteBuffer byteBuffer;

    /**
     * Constructs a new {@code ByteBufferOutputStream}.
     * The internal {@code ByteBuffer} is heap byte buffer.
     *
     * @param initialCapacity an initial capacity of the internal {@code ByteBuffer}.
     */
    ByteBufferOutputStream(int initialCapacity) {
        byteBuffer = ByteBuffer.allocate(initialCapacity);
    }

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of increments specified by the argument.
     *
     * @param minimumIncrement the minimum number of size increments
     */
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

    /**
     * Writes the specified byte to this byte buffer output stream.
     *
     * @param b the byte to be written
     */
    @Override
    public void write(int b) {
        if (!byteBuffer.hasRemaining()) {
            ensureCapacity(1);
        }
        byteBuffer.put((byte) b);
    }

    /**
     * Writes {@codelenth} bytes from the specified byte array
     * starting at offset {@code offset} to this byte buffer output stream.
     *
     * @param   bytes the data.
     * @param   offset the start offset in the data.
     * @param   length the number of bytes to write.
     */
    @Override
    public void write(byte[] bytes, int offset, int length) {
        if (byteBuffer.remaining() < length) {
            ensureCapacity(length);
        }
        byteBuffer.put(bytes, offset, length);
    }

    /**
     * Flips the internal {@code ByteBuffer}.
     */
    @Override
    public void close() {
        byteBuffer.flip();
    }
}
