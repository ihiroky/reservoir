package net.ihiroky.reservoir;

import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * Provides for factory methods to create a {@link net.ihiroky.reservoir.Coder.Encoder} implementation and
 * a {@link net.ihiroky.reservoir.Coder.Decoder} implementation. These class is used to store to and load from
 * a storage through {@link net.ihiroky.reservoir.StorageAccessor}.
 *
 * @param <V> the type of a value to encode and decode.
 * @author Hiroki Itoh
 */
public interface Coder<V> {

    /**
     * Initialize this instance by {@code java.util.Properties}. Properties to be add is defined
     * by a implementation class.
     *
     * @param props properties containing initialization parameters
     */
    void init(Properties props);

    /**
     * Creates {@code Encoder} to encode a value that the type is {@code V}.
     *
     * @return {@code Encoder} to encode a value that the type is {@code V}
     */
    Encoder<V> createEncoder();

    /**
     * Creates {@code Decoder} to decode a byte array to a value that the type is {@code V}.
     *
     * @return {@code Decoder} to decode a byte array to a value that the type is {@code V}
     */
    Decoder<V> createDecoder();

    /**
     * {@code Decoder} is used to read {@code java.nio.ByteBuffer} and create a object that type is {@code V}.
     *
     * @param <V> the type of a value to encode and decode
     * @author Hiroki Itoh
     */
    interface Decoder<V> {

        /**
         * Creates a object from the specified {@code byteBuffer}.
         *
         * @param byteBuffer a byte buffer to be converted into a object
         * @return a object created from the specified {@code byteBuffer}
         */
        V decode(ByteBuffer byteBuffer);
    }

    /**
     * {@code Encoder} is used to convert a object that type is {@code V} into {@code java.nio.ByteBuffer}.
     *
     * @param <V> the type of a value to encode and decode
     * @author Hiroki Itoh
     */
    interface Encoder<V> {

        /**
         * Creates byte buffer from the specified {@code value}.
         *
         * @param value a value to be converted into a byte buffer
         * @return a byte buffer created from the specified value.
         */
        ByteBuffer encode(V value);
    }
}
