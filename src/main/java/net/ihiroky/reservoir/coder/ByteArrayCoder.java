package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.PropertiesSupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * A {@link net.ihiroky.reservoir.Coder} implementation to encode and decode a byte array.
 * A Byte array is stored to a storage directly as much as possible.
 *
 * This class supports {@link net.ihiroky.reservoir.coder.CompressionSupport}.
 * To be enable compression / decompression, set the property {@code reservoir.ByteArrayCoder.compress.enabled}
 * on true. See {@link net.ihiroky.reservoir.coder.CompressionSupport} for detail.
 *
 * @author Hiroki Itoh
 */
public class ByteArrayCoder implements Coder<byte[]> {

    /** a compression and decompression support object */
    private CompressionSupport compressionSupport = new CompressionSupport();

    /** an initial buffer size on compresing an input byte array */
    private int initialByteSize = DEFAULT_INITIAL_BYTE_SIZE;

    /** a property key prefix */
    private static final String KEY_PREFIX = "reservoir.ByteArrayCoder";

    /** a property key for {@code initialByteSize} */
    private static final String KEY_INIT_BYTE_SIZE = KEY_PREFIX.concat(".initByteSize");

    /** default {@code initialByteSize} */
    private static final int DEFAULT_INITIAL_BYTE_SIZE = 512;

    /** minimum {@code initialByteSize} */
    private static final int MIN_INITIAL_BYTE_SIZE = 16;

    /**
     * Initializes this object.
     * <ul>
     *     <li>{@code reservoir.ByteArrayCoder.initByteSize}</li>
     *       An initial buffer size on compressing an input byte array. Default value is 512.
     *     <li>{@code reservoir.ByteArrayCoder.compress.enabled}</li>
     *     <li>{@code reservoir.ByteArrayCoder.compress.level}</li>
     * </ul>
     * See {@link net.ihiroky.reservoir.coder.CompressionSupport} for detail.
     *
     * @param props properties containing initialization parameters
     */
    @Override
    public void init(Properties props) {
        compressionSupport.loadProperties(props, KEY_PREFIX);
        int initialByteSize = PropertiesSupport.intValue(props, KEY_INIT_BYTE_SIZE, DEFAULT_INITIAL_BYTE_SIZE);
        if (initialByteSize < MIN_INITIAL_BYTE_SIZE) {
            initialByteSize = MIN_INITIAL_BYTE_SIZE;
        }
        this.initialByteSize = initialByteSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Encoder<byte[]> createEncoder() {
        return new ByteArrayEncoder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Decoder<byte[]> createDecoder() {
        return new ByteArrayDecoder();
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Encoder} implementation to handle bytes array.
     */
    class ByteArrayEncoder implements Encoder<byte[]> {

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer encode(byte[] value) {
            if (!compressionSupport.isEnabled()) {
                return ByteBuffer.wrap(value);
            }

            ByteBufferOutputStream out = new ByteBufferOutputStream(initialByteSize);
            OutputStream wrapper = compressionSupport.createOutputStreamIfEnabled(out);
            try {
                out.write(CoderStream.asBytes(value.length)); // write directly.
                wrapper.write(value, 0, value.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    wrapper.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return out.byteBuffer;
        }
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Decoder} implementation to handle bytes array.
     */
    class ByteArrayDecoder implements Decoder<byte[]> {

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] decode(ByteBuffer byteBuffer) {
            if (!compressionSupport.isEnabled()) {
                byte[] bytes = byteBuffer.array();
                int remaining = byteBuffer.remaining();
                if (bytes.length == remaining) {
                    return bytes;
                } else {
                    byte[] result = new byte[remaining];
                    System.arraycopy(bytes, byteBuffer.arrayOffset(), result, 0, remaining);
                    return result;
                }
            }

            byte[] bytes = byteBuffer.array();
            int offset = byteBuffer.arrayOffset();
            int encodedLength = byteBuffer.remaining();
            int decodedLength = CoderStream.asInt(bytes, offset);
            InputStream in = new ByteArrayInputStream(
                    bytes, offset + CoderStream.bytesLength(decodedLength), encodedLength);
            byte[] buffer = new byte[decodedLength];
            InputStream wrapper = compressionSupport.createInputStreamIfEnabled(in);
            try {
                wrapper.read(buffer, 0, decodedLength);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    wrapper.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return buffer;
        }
    }
}
