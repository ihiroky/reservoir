package net.ihiroky.reservoir.coder;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/16, 12:03
 *
 * @author Hiroki Itoh
 */
public class SerializableCoderTest {

    @Test
    public void testCompression() {
        String value = "000000000000000000000000000000";

        SerializableCoder<String> serializableCoder = new SerializableCoder<String>();
        ByteBuffer normal = serializableCoder.createEncoder().encode(value);

        Properties props = new Properties();
        props.setProperty("reservoir.SerializableCoder.compress.enabled", "true");
        props.setProperty("reservoir.SerializableCoder.compress.level", "1");
        serializableCoder.init(props);
        ByteBuffer compressed = serializableCoder.createEncoder().encode(value);
        assertThat(compressed.remaining() < normal.remaining(), is(true));

        String decoded = serializableCoder.createDecoder().decode(compressed);
        assertThat(decoded, is(value));
    }
}
