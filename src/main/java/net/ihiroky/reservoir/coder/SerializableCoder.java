package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * A {@link net.ihiroky.reservoir.Coder} implementation to encode and decode serializable objects.
 *
 * This class supports {@link net.ihiroky.reservoir.coder.CompressionSupport}.
 * To be enable compression / decompression, set the property {@code reservoir.SerializableCoder.compress.enabled}
 * on true. See {@link net.ihiroky.reservoir.coder.CompressionSupport} for detail.
 *
 *
 * @param <V> the type of a serializable object
 * @author Hiroki Itoh
 */
public class SerializableCoder<V extends Serializable> implements Coder<V> {

    private CompressionSupport compressionSupport = new CompressionSupport();

    private static final String KEY_PREFIX = "reservoir.SerializableCoder";

    /**
     * Initializes this object.
     * <ul>
     *     <li>{@code reservoir.SerializableCoder.compress.enabled}</li>
     *     <li>{@code reservoir.SerializableCoder.compress.level}</li>
     * </ul>
     * See {@link net.ihiroky.reservoir.coder.CompressionSupport} for detail.
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
    public Encoder<V> createEncoder() {
        return new SerializableEncoder<V>(compressionSupport);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Decoder<V> createDecoder() {
        return new SerializableDecoder<V>(compressionSupport);
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Encoder} implementation to handle serializable objects.
     *
     * @param <V> the type of objects to be encoded ({@code java.io.Serializable})
     */
    static class SerializableEncoder<V> implements Encoder<V> {

        /** a compression support object */
        CompressionSupport compressionSupport;

        /**
         * Constructs a new {@code SerializableEncoder}.
         * @param compressionSupport a compression support object
         */
        SerializableEncoder(CompressionSupport compressionSupport) {
            this.compressionSupport = compressionSupport;
        }

        /**
         * {@inheritDoc}
         */
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

    /**
     * A {@link net.ihiroky.reservoir.Coder.Decoder} implementation to handle serializable objects.
     *
     * @param <V> the type of objects to be decoded into ({@code java.io.Serializable})
     */
    static class SerializableDecoder<V> implements Decoder<V> {

        /** a compression support object */
        CompressionSupport compressionSupport;

        /**
         * Constructs a new {@code SerializableCoder}.
         *
         * @param compressionSupport a compression support object
         */
        SerializableDecoder(CompressionSupport compressionSupport) {
            this.compressionSupport = compressionSupport;
        }

        /**
         * {@inheritDoc}
         */
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
