package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Coder;
import net.ihiroky.reservoir.PropertiesSupport;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Properties;

/**
 * A {@link net.ihiroky.reservoir.Coder} implementation to handle strings.
 *
 * Default encoding / decoding is UTF-8. The charset can be changed using the {@code reservoir.StringCoder.charset}
 * parameter.
 *
 * @author Hiroki Itoh
 */
public class StringCoder implements Coder<String> {

    private Charset charset = DEFAULT_CHARSET;

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static final String KEY_CHARSET = "reservoir.StringCoder.charset";

    /**
     * Initializes this object.
     * <ul>
     *     <li>{@code reservoir.StringCoder.charset}</li>
     *     specify a charset used by encoding / decoding.
     * </ul>
     *
     * @param props properties containing initialization parameters
     */
    @Override
    public void init(Properties props) {
        Charset charset = (Charset) PropertiesSupport.newInstance(props, KEY_CHARSET, null);
        if (charset != null) {
            this.charset = charset;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Encoder<String> createEncoder() {
        return new StringEncoder(charset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Decoder<String> createDecoder() {
        return new StringDecoder(charset);
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Encoder} implementation to handle strings.
     */
    static class StringEncoder implements Encoder<String> {

        /** charset encoder */
        CharsetEncoder encoder;

        /** zero byte {@code java.nio.ByteBuffer} */
        static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

        /**
         * Constructs a new {@code StringEncoder}.
         * @param charset encoding charset
         */
        StringEncoder(Charset charset) {
            this.encoder = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer encode(String value) {
            if (value == null || value.length() == 0) {
                return EMPTY;
            }
            try {
                return encoder.encode(CharBuffer.wrap(value));
            } catch (CharacterCodingException e) {
                throw new RuntimeException("failed to encode string : " + value, e);
            }
        }
    }

    /**
     * A {@link net.ihiroky.reservoir.Coder.Decoder} implementaion to handle stings.
     */
    static class StringDecoder implements Decoder<String> {

        /** charset encoder */
        CharsetDecoder decoder;

        /**
         * Constructs a new {@code StringDecoder}.
         * @param charset decoding charset
         */
        StringDecoder(Charset charset) {
            this.decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String decode(ByteBuffer byteBuffer) {
            try {
                return decoder.decode(byteBuffer).toString();
            } catch (CharacterCodingException e) {
                throw new RuntimeException("failed to decode string : " + byteBuffer, e);
            }
        }
    }
}
