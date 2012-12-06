package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * Created on 12/09/28, 13:20
 *
 * @author Hiroki Itoh
 */
public class SerializableCoder<V extends Serializable> implements Coder<V> {

    private CompressionSupport compressionSupport = new CompressionSupport();

    private static final String KEY_PREFIX = "reservoir.SerializableCoder";

    @Override
    public void init(Properties props) {
        compressionSupport.loadProperties(props, KEY_PREFIX);
    }

    @Override
    public Encoder<V> createEncoder() {
        return new SerializableEncoder<V>(compressionSupport);
    }

    @Override
    public Decoder<V> createDecoder() {
        return new SerializableDecoder<V>(compressionSupport);
    }

    static class SerializableEncoder<V> implements Encoder<V> {

        CompressionSupport compressionSupport;

        SerializableEncoder(CompressionSupport compressionSupport) {
            this.compressionSupport = compressionSupport;
        }

        @Override
        public ByteBuffer encode(Object value) {
            ByteBufferOutputStream base = new ByteBufferOutputStream(1024);
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(compressionSupport.createOutputStreamIfEnabled(base));
                oos.writeObject(value);
            } catch (Exception e) {
                throw new RuntimeException("failed to serialize object : " + value, e);
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return base.byteBuffer;
        }
    }

    static class SerializableDecoder<V> implements Decoder<V> {

        CompressionSupport compressionSupport;

        SerializableDecoder(CompressionSupport compressionSupport) {
            this.compressionSupport = compressionSupport;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V decode(ByteBuffer byteBuffer) {
            InputStream base = ByteBufferInputStream.createInputStream(byteBuffer);
            ObjectInputStream ois = null;
            V value = null;
            try {
                ois = new ObjectInputStream(compressionSupport.createInputStreamIfEnabled(base));
                value = (V) ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException("failed to deserialize object : " + byteBuffer, e);
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return value;
        }
    }
}
