package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/17, 13:06
 *
 * @author Hiroki Itoh
 */
public class ByteArrayCoderTest {

    @Test
    public void testEncodeDecode() {
        ByteArrayCoder coder = new ByteArrayCoder();
        Coder.Encoder<byte[]> encoder = coder.createEncoder();
        Coder.Decoder<byte[]> decoder = coder.createDecoder();

        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        assertThat(decoder.decode(encoder.encode(bytes)), is(bytes));
    }

    @Test
    public void testCompression() {
        Properties props = new Properties();
        props.setProperty("reservoir.ByteArrayCoder.compress.enabled", "true");
        ByteArrayCoder coder = new ByteArrayCoder();
        coder.init(props);
        Coder.Encoder<byte[]> encoder = coder.createEncoder();
        Coder.Decoder<byte[]> decoder = coder.createDecoder();

        byte[] bytes = new byte[512];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((i + ' ') % 36);
        }

        ByteBuffer compressed = encoder.encode(bytes);
        assertThat(compressed.remaining() < bytes.length, is(true));
        assertThat(decoder.decode(compressed), is(bytes));
    }
}
