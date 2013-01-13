package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;

import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * A {@link net.ihiroky.reservoir.Coder} implementation to handle strings.
 * The way of encoding and decoding is according to the simple conversion between Java char and byte.
 *
 * This class supports {@link net.ihiroky.reservoir.coder.CompressionSupport}.
 * To be enable compression / decompression, set the property {@code reservoir.SimpleStringCoder.compress.enabled}
 * on true. See {@link net.ihiroky.reservoir.coder.CompressionSupport} for detail.
 * If the strings have a lot of ascii characters, low level compression is very useful to suppress encoded size.
 *
 * @author Hiroki Itoh
 */
public class SimpleStringCoder implements Coder<String> {

    private CompressionSupport compressionSupport = new CompressionSupport();

    private static final String KEY_PREFIX = "reservoir.SimpleStringCoder";

    /**
     * Initializes this object.
     * <ul>
     *     <li>{@code reservoir.SimpleStringCoder.compress.enabled}</li>
     *     <li>{@code reservoir.SimpleStringCoder.compress.level}</li>
     * </ul>
     * See {@link net.ihiroky.reservoir.coder.CompressionSupport}.
     *
     * @param props properties containing initialization parameters
     */
    @Override
    public void init(Properties props) {
        compressionSupport.loadProperties(props, KEY_PREFIX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Encoder<String> createEncoder() {
        return compressionSupport.createEncoderIfEnabled(new StringEncoder());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Decoder<String> createDecoder() {
        return compressionSupport.createDecoderIfEnabled(new StringDecoder());
    }

    /**
     * Encodes a specified string into a byte array.
     * 
     * @param value a string to be encoded
     * @return the encoded byte array
     */
    static byte[] encode(String value) {
        int length = value.length();
        byte[] result = new byte[length * 2];
        char c;
        int i2;
        for (int i = 0; i < length; i++) {
            c = value.charAt(i);
            result[(i2 = i * 2)] = (byte) (c >> 8);
            result[i2 + 1] = (byte) c;
        }
        return result;
    }

    /**
     * Decodes a specified byte array into a string.
     * 
     * @param bytes a byte array to be decoded
     * @param offset an {@code bytes} offset pointed to the head of the content
     * @param length an {@code bytes} content length 
     * @return the decoded string
     */
    static String decode(byte[] bytes, int offset, int length) {
        int charLength = length / 2;
        char[] a = new char[charLength];
        int j;
        for (int i = 0; i < charLength; i++) {
            j = i * 2 + offset;
            a[i] = (char) (((bytes[j] & 0xFF) << 8) + (bytes[j + 1] & 0xFF));
        }
        return new String(a);
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Encoder} implementation to handle strings with the simple coding.
     */
    static class StringEncoder implements Encoder<String> {

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer encode(String value) {
            return ByteBuffer.wrap(SimpleStringCoder.encode(value));
        }
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Decoder} implementation to handle strings with the simple coding.
     */
    static class StringDecoder implements Decoder<String> {

        /**
         * {@inheritDoc}
         */
        @Override
        public String decode(ByteBuffer byteBuffer) {
            return SimpleStringCoder.decode(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.remaining());
        }
    }
}
