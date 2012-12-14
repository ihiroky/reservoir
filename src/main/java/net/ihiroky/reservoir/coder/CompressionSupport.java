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
 * Created on 12/10/04, 19:32
 *
 * @author Hiroki Itoh
 */
public class CompressionSupport {

    private volatile int level = Deflater.BEST_SPEED;
    private volatile boolean enabled = false;

    private static final String KEY_LEVEL_SUFFIX = ".compress.level";
    private static final String KEY_ENABLE_SUFFIX = ".compress.enabled";

    public void loadProperties(Properties props, String prefix) {
        enabled = PropertiesSupport.booleanValue(props, prefix.concat(KEY_ENABLE_SUFFIX), false);
        level = PropertiesSupport.intValue(props, prefix.concat(KEY_LEVEL_SUFFIX), Deflater.BEST_SPEED);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Has a constructor that sets a compression level.
     */
    static class LevelDeflaterOutputStream extends DeflaterOutputStream {

        /**
         * Creates LevelDeflaterOutputStream.
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

    public OutputStream createOutputStreamIfEnabled(OutputStream outputStream) {
        return enabled ? new LevelDeflaterOutputStream(outputStream, level) : outputStream;
    }

    public InputStream createInputStreamIfEnabled(InputStream inputStream) {
        return enabled ? new InflaterInputStream(inputStream) : inputStream;
    }

    public <V> Coder.Encoder<V> createEncoderIfEnabled(Coder.Encoder<V> encoder) {
        return enabled ? new DeflateEncoder<V>(encoder, level) : encoder;
    }

    public <V> Coder.Decoder<V> createDecoderIfEnabled(Coder.Decoder<V> decoder) {
        return enabled ? new InflateDecoder<V>(decoder) : decoder;
    }

    private static byte[] expand(byte[] bytes) {
        byte[] t = new byte[bytes.length / 2 * 3];
        System.arraycopy(bytes, 0, t, 0, bytes.length);
        return t;
    }

    static class DeflateEncoder<V> implements Coder.Encoder<V> {

        private final Coder.Encoder<V> encoder;
        private final int level;

        DeflateEncoder(Coder.Encoder<V> encoder, int level) {
            this.encoder = encoder;
            this.level = level;
        }

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

    static class InflateDecoder<V> implements Coder.Decoder<V> {

        private final Coder.Decoder<V> decoder;

        InflateDecoder(Coder.Decoder<V> decoder) {
            this.decoder = decoder;
        }

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
