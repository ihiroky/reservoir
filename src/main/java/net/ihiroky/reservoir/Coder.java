package net.ihiroky.reservoir;

import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * Created on 12/09/27, 18:56
 *
 * @author Hiroki Itoh
 */
public interface Coder<V> {

    void init(Properties props);

    Encoder<V> createEncoder();

    Decoder<V> createDecoder();

    /**
     * Created on 12/09/27, 18:57
     *
     * @author Hiroki Itoh
     */
    interface Decoder<V> {

        V decode(ByteBuffer byteBuffer);
    }

    /**
     * Created on 12/09/27, 18:57
     *
     * @author Hiroki Itoh
     */
    interface Encoder<V> {

        ByteBuffer encode(V value);
    }
}
