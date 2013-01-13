package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.PropertiesSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Provides for compression and decompression support methods. To be enable the support, following properties
 * must be set by {@link net.ihiroky.reservoir.coder.CompressionSupport#loadProperties(java.util.Properties, String)};
 * <i>prefix</i> is the argument of {@code loadProperties()}.
 * <pre>
 *     - <i>prefix</i>{@code .compress.enabled}
 *       true if compression and decompression is activated. Default value is false.
 *     - <i>prefix</i>{@code .compress.level}
 *       compression level if compression and decompression is activated. Default value is
 * </pre>
 *
 * @author Hiroki Itoh
 */
public class CompressionSupport {

    /** a compression level */
    private volatile int level = Deflater.BEST_SPEED;

    /** true if compression and decompression is enabled. */
    private volatile boolean enabled = false;

    /** a property suffix to specify a compression level */
    private static final String KEY_LEVEL_SUFFIX = ".compress.level";

    /** a property suffix to specify that compression and decompression is enabled or not */
    private static final String KEY_ENABLE_SUFFIX = ".compress.enabled";

    /**
     * Set properties from a specified {@code props} which keys has a specified {@code prefix}.
     *
     * @param props properties which contians compression parameters
     * @param prefix key prefix
     */
    public void loadProperties(Properties props, String prefix) {
        enabled = PropertiesSupport.booleanValue(props, prefix.concat(KEY_ENABLE_SUFFIX), false);
        level = PropertiesSupport.intValue(props, prefix.concat(KEY_LEVEL_SUFFIX), Deflater.BEST_SPEED);
    }

    /**
     * Returns true if compression and decompression is enabled.
     * @return true if compression and decompression is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Has a constructor that sets a compression level.
     * The compression level is able to set and the deflater is closed when this stream is closed.
     */
    static class LevelDeflaterOutputStream extends DeflaterOutputStream {

        /**
         * Creates {@code LevelDeflaterOutputStream}.
         * @param outputStream base OutputStream.
         * @param level compression level.
         */
        LevelDeflaterOutputStream(OutputStream outputStream, int level) {
            super(outputStream, new Deflater(level));
        }

        /**
         * Closes this stream and deflater used by this stream..
         * @throws IOException
         */
        @Override
        public void close() throws IOException {
            super.close();
            def.end();
        }
    }

    /**
     * Creates {@code java.util.DeflaterOutputStream} with the designated compression level.
     * @param outputStream base output stream wrapped by the {@code DeflaterOutputStream}
     * @return {@code java.util.DeflaterOutputStream} if compression support is enabled
     */
    public OutputStream createOutputStreamIfEnabled(OutputStream outputStream) {
        return enabled ? new LevelDeflaterOutputStream(outputStream, level) : outputStream;
    }

    /**
     * Creates {@code java.util.InflaterInputStream}.
     * @param inputStream base input stream wrapped by the {@code InflaterInputStream}
     * @return {@code java.util.InflaterInputStream}.
     */
    public InputStream createInputStreamIfEnabled(InputStream inputStream) {
        return enabled ? new InflaterInputStream(inputStream) : inputStream;
    }

    /**
     * Creates {@link net.ihiroky.reservoir.Coder.Encoder} to compress an output of the specified {@code encoder}.
     * @param encoder base encoder wrapped by the compression support encoder
     * @param <V> the type of a value to be encoded by the returned encoder
     * @return the encoder to compress the base encoder output if compression support is enabled
     */
    public <V> Coder.Encoder<V> createEncoderIfEnabled(Coder.Encoder<V> encoder) {
        return enabled ? new DeflateEncoder<V>(encoder, level) : encoder;
    }

    /**
     * Creates {@link net.ihiroky.reservoir.Coder.Decoder} to decompress an output of the specified {@code decoder}.
     * @param decoder base decoder wrapped by the decompression support decoder
     * @param <V> the type of a value to be decoded by the returned decoder
     * @return the decoder to decompress the base decoder output if compresson support is enabled
     */
    public <V> Coder.Decoder<V> createDecoderIfEnabled(Coder.Decoder<V> decoder) {
        return enabled ? new InflateDecoder<V>(decoder) : decoder;
    }

    /**
     * Expands a given byte array.
     *
     * @param bytes byte array to be expand
     * @return expanded byte array
     */
    private static byte[] expand(byte[] bytes) {
        byte[] t = new byte[bytes.length / 2 * 3];
        System.arraycopy(bytes, 0, t, 0, bytes.length);
        return t;
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Encoder} implementation that supports to compress bytes array created by
     * a {@code decoder} specified in the constructor.
     * @param <V> the type of values to be decoded by this object
     */
    static class DeflateEncoder<V> implements Coder.Encoder<V> {

        /** an encoder to encode input values. */
        private final Coder.Encoder<V> encoder;

        /** compression level  */
        private final int level;

        /**
         * Creates this constuct.
         * @param encoder a base encoder.
         * @param level a compression level. default is 1.;
         */
        DeflateEncoder(Coder.Encoder<V> encoder, int level) {
            this.encoder = encoder;
            this.level = level;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer encode(V value) {
            ByteBuffer encoded = encoder.encode(value);
            if (!encoded.hasRemaining()) {
                return encoded;
            }

            int inputSize = encoded.remaining();
            byte[] buffer = new byte[inputSize / 2 + 2]; // + 2 : keep ge 2 for expend()
            int deflated = 0;
            Deflater deflater = new Deflater(level);
            deflater.setInput(encoded.array(), encoded.arrayOffset(), inputSize);
            deflater.finish();
            while (!deflater.finished()) {
                if (deflated == buffer.length) {
                    buffer = expand(buffer);
                }
                deflated += deflater.deflate(buffer, deflated, buffer.length - deflated);
            }
            deflater.end();
            return ByteBuffer.wrap(buffer, 0, deflated);
        }
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Decoder} implementation that supports to compress bytes array created by
     * a {@code encoder} specified in the constructor.
     * @param <V> the type of values to be decoded by this object
     */
    static class InflateDecoder<V> implements Coder.Decoder<V> {

        /** a base decoder*/
        private final Coder.Decoder<V> decoder;

        /**
         * Creates this class object.
         * @param decoder a base decoder
         */
        InflateDecoder(Coder.Decoder<V> decoder) {
            this.decoder = decoder;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V decode(ByteBuffer byteBuffer) {
            if (!byteBuffer.hasArray()) {
                // TODO
                throw new UnsupportedOperationException("no implemented yet.");
            }
            int inputSize = byteBuffer.remaining();
            byte[] buffer = new byte[inputSize * 2 + 2];
            int inflated = 0;
            Inflater inflater = new Inflater();
            inflater.setInput(byteBuffer.array(), byteBuffer.arrayOffset(), inputSize);
            try {
                while (!inflater.finished()) {
                    if (inflated == buffer.length) {
                        buffer = expand(buffer);
                    }
                    inflated += inflater.inflate(buffer, inflated, buffer.length - inflated);
                }
            } catch (DataFormatException e) {
                throw new RuntimeException("failed to decode.", e);
            } finally {
                inflater.end();
            }

            return decoder.decode(ByteBuffer.wrap(buffer, 0, inflated));
        }
    }
}
