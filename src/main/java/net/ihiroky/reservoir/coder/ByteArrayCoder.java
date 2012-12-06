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
 * Created on 12/10/17, 10:35
 *
 * @author Hiroki Itoh
 */
public class ByteArrayCoder implements Coder<byte[]> {

    private CompressionSupport compressionSupport = new CompressionSupport();
    private int initialByteSize = DEFAULT_INITIAL_BYTE_SIZE;

    private static final String KEY_PREFIX = "reservoir.ByteArrayCoder";
    private static final String KEY_INIT_BYTE_SIZE = KEY_PREFIX.concat(".initByteSize");

    private static final int DEFAULT_INITIAL_BYTE_SIZE = 512;
    private static final int MIN_INITIAL_BYTE_SIZE = 16;

    @Override
    public void init(Properties props) {
        compressionSupport.loadProperties(props, KEY_PREFIX);
        int initialByteSize = PropertiesSupport.intValue(props, KEY_INIT_BYTE_SIZE, DEFAULT_INITIAL_BYTE_SIZE);
        if (initialByteSize < MIN_INITIAL_BYTE_SIZE) {
            initialByteSize = MIN_INITIAL_BYTE_SIZE;
        }
        this.initialByteSize = initialByteSize;
    }

    @Override
    public Encoder<byte[]> createEncoder() {
        return new ByteArrayEncoder();
    }

    @Override
    public Decoder<byte[]> createDecoder() {
        return new ByteArrayDecoder();
    }

    static byte[] expand(byte[] original) {
        byte[] t = new byte[original.length / 2 * 3];
        System.arraycopy(original, 0, t, 0, original.length);
        return t;
    }

    class ByteArrayEncoder implements Encoder<byte[]> {

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

    class ByteArrayDecoder implements Decoder<byte[]> {

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
            int readTotal = 0;
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
