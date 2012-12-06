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
 * Created on 12/09/28, 13:55
 *
 * @author Hiroki Itoh
 */
public class StringCoder implements Coder<String> {

    private Charset charset = DEFAULT_CHARSET;

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static final String KEY_CHARSET = "reservoir.StringCoder.charset";

    @Override
    public void init(Properties props) {
        Charset charset = (Charset) PropertiesSupport.newInstance(props, KEY_CHARSET, null);
        if (charset != null) {
            this.charset = charset;
        }
    }

    @Override
    public Encoder<String> createEncoder() {
        return new StringEncoder(charset);
    }

    @Override
    public Decoder<String> createDecoder() {
        return new StringDecoder(charset);
    }

    static class StringEncoder implements Encoder<String> {

        CharsetEncoder encoder;

        static final ByteBuffer EMPTY = ByteBuffer.allocate(0).asReadOnlyBuffer();

        StringEncoder(Charset charset) {
            this.encoder = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

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

    static class StringDecoder implements Decoder<String> {

        CharsetDecoder decoder;

        StringDecoder(Charset charset) {
            this.decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

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
