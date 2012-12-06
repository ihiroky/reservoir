package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;

import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * Created on 12/10/05, 12:20
 *
 * @author Hiroki Itoh
 */
public class SimpleStringCoder implements Coder<String> {

    private CompressionSupport compressionSupport = new CompressionSupport();

    private static final String KEY_PREFIX = "reservoir.SimpleStringCoder";

    @Override
    public void init(Properties props) {
        compressionSupport.loadProperties(props, KEY_PREFIX);
    }

    @Override
    public Encoder<String> createEncoder() {
        return compressionSupport.createEncoderIfEnabled(new StringEncoder());
    }

    @Override
    public Decoder<String> createDecoder() {
        return compressionSupport.createDecoderIfEnabled(new StringDecoder());
    }

    private static byte[] expand(byte[] bytes) {
        byte[] t = new byte[bytes.length / 2 * 3];
        System.arraycopy(bytes, 0, t, 0, bytes.length);
        return t;
    }

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

    static class StringEncoder implements Encoder<String> {

        @Override
        public ByteBuffer encode(String value) {
            return ByteBuffer.wrap(SimpleStringCoder.encode(value));
        }
    }

    static class StringDecoder implements Decoder<String> {

        @Override
        public String decode(ByteBuffer byteBuffer) {
            return SimpleStringCoder.decode(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.remaining());
        }
    }
}
