package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.PropertiesSupport;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/05, 12:46
 *
 * @author Hiroki Itoh
 */
public class SimpleStringCoderTest {

    @Test
    public void testEncodeDecode() {
        SimpleStringCoder coder = new SimpleStringCoder();
        Coder.Encoder<String> encoder = coder.createEncoder();
        Coder.Decoder<String> decoder = coder.createDecoder();
        String a = "aあiいuうeえoお";
        String result = decoder.decode(encoder.encode(a));
        assertThat(result, is(a));
    }

    @Test
    public void testCompress() {
        SimpleStringCoder coder = new SimpleStringCoder();
        coder.init(PropertiesSupport.builder()
                .set(SimpleStringCoder.class, "compress.enabled", "true").properties());
        Coder.Encoder<String> encoder = coder.createEncoder();
        Coder.Decoder<String> decoder = coder.createDecoder();
        char[] a = new char[512];
        Arrays.fill(a, 'a');
        String s = new String(a);
        ByteBuffer byteBuffer = encoder.encode(s);
        String result = decoder.decode(byteBuffer);
        assertThat("compress check", byteBuffer.limit() < a.length, is(true));
        assertThat(result, is(s));
    }
}
