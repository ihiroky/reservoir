package net.ihiroky.reservoir.coder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.ihiroky.reservoir.Cache;
import net.ihiroky.reservoir.StreamingCoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created on 12/10/24, 10:31
 *
 * @author Hiroki Itoh
 */
public abstract class JSONCoder<K, V> implements StreamingCoder<K, V> {

    private JsonFactory jsonFactory;

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private static final String KEY = "k";
    private static final String VALUE = "v";

    private static final JSONCoder<String, String> STRING_CODER = new JSONCoder<String, String>() {
        @Override
        protected void writeKey(JsonWriter writer, String key) throws Exception {
            writer.setString(key);
        }

        @Override
        protected void writeValue(JsonWriter writer, String value) throws Exception {
            writer.setString(value);
        }

        @Override
        protected String readKey(JsonReader reader) throws Exception {
            return reader.getString();
        }

        @Override
        protected String readValue(JsonReader reader) throws Exception {
            return reader.getString();
        }

        @Override
        protected String toKeyString(String key) throws Exception {
            return key;
        }

        @Override
        protected String toKey(String key) throws Exception {
            return key;
        }
    };

    public JSONCoder<String, String> getStringCoder() {
        return STRING_CODER;
    }

    public JSONCoder() {
        jsonFactory = new JsonFactory();
    }

    @Override
    public void write(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        K k = toKey(key);
        write(k, cache.get(k), outputStream);
    }

    private void write(K key, V value, OutputStream outputStream) throws Exception {
        JsonGenerator generator =
                jsonFactory.createJsonGenerator(new OutputStreamWriter(outputStream, CHARSET));
        generator.writeStartArray();
        if (value != null) {
            JsonWriter writer = new JsonWriter(generator);
            generator.writeStartObject();
            generator.writeFieldName(KEY);
            writeKey(writer, key);
            generator.writeFieldName(VALUE);
            writeValue(writer, value);
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.flush();
    }

    @Override
    public void write(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        write(pattern, cache, outputStream, false);
    }

    private void write(Pattern pattern, Cache<K, V> cache, OutputStream outputStream,
                       boolean removeIfMatched) throws Exception {
        JsonGenerator generator =
                jsonFactory.createJsonGenerator(new OutputStreamWriter(outputStream, CHARSET));
        JsonWriter writer = new JsonWriter(generator);
        generator.writeStartArray();
        Map.Entry<K, V> entry;
        K key;
        for (Iterator<Map.Entry<K, V>> iterator = cache.iterator(); iterator.hasNext(); ) {
            entry = iterator.next();
            key = entry.getKey();
            if (pattern.matcher(toKeyString(key)).find()) {
                generator.writeStartObject();
                generator.writeFieldName(KEY);
                writeKey(writer, key);
                generator.writeFieldName(VALUE);
                writeValue(writer, entry.getValue());
                generator.writeEndObject();
                if (removeIfMatched) {
                    iterator.remove();
                }
            }
        }
        generator.writeEndArray();
        generator.flush();
    }

    @Override
    public void read(Cache<K, V> cache, InputStream inputStream) throws Exception {
        JsonParser parser =
                jsonFactory.createJsonParser(new InputStreamReader(inputStream, CHARSET));
        JsonToken token = parser.nextToken();
        JsonToken endToken = (token == JsonToken.START_ARRAY) ? JsonToken.END_ARRAY
                : ((token == JsonToken.START_OBJECT) ? JsonToken.END_OBJECT : null);
        if (endToken == null) {
            throw new IOException("first token is " + token + ", must be "
                    + JsonToken.START_ARRAY + " or " + JsonToken.START_OBJECT);
        }
        JsonReader reader = new JsonReader(parser);
        K key = null;
        V value = null;
        while ((token = parser.nextToken()) != endToken) {
            switch (token) {
                case FIELD_NAME:
                    String name = parser.getCurrentName();
                    if (KEY.equals(name)) {
                        token = parser.nextToken();
                        key = readKey(reader);
                    } else if (VALUE.equals(name)) {
                        token = parser.nextToken();
                        value = readValue(reader);
                    }
                    break;
                case END_OBJECT:
                    if (key != null && value != null) {
                        cache.put(key, value);
                    }
                    key = null;
                    value = null;
                    break;
            }
        }
        if (key != null && value != null) {
            cache.put(key, value);
        }
    }

    @Override
    public void delete(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        K k = toKey(key);
        write(k, cache.poll(k), outputStream);
    }

    @Override
    public void delete(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        write(pattern, cache, outputStream, true);
    }

    @Override
    public void delete(Cache<K, V> cache, InputStream inputStream) throws Exception {
        JsonParser parser =
                jsonFactory.createJsonParser(new InputStreamReader(inputStream, CHARSET));
        JsonToken token = parser.nextToken();
        JsonToken endToken = (token == JsonToken.START_ARRAY) ? JsonToken.END_ARRAY
                : ((token == JsonToken.START_OBJECT) ? JsonToken.END_OBJECT : null);
        if (endToken == null) {
            throw new IOException("first token is " + token + ", must be "
                    + JsonToken.START_ARRAY + " or " + JsonToken.START_OBJECT);
        }
        JsonReader reader = new JsonReader(parser);
        K key = null;
        V value = null;
        while ((token = parser.nextToken()) != endToken) {
            if (token == JsonToken.FIELD_NAME) {
                String name = parser.getCurrentName();
                if (KEY.equals(name)) {
                    token = parser.nextToken();
                    key = readKey(reader);
                    cache.remove(key);
                }
            }
        }
    }

    abstract protected void writeKey(JsonWriter writer, K key) throws Exception;

    abstract protected void writeValue(JsonWriter writer, V value) throws Exception;

    abstract protected K readKey(JsonReader reader) throws Exception;

    abstract protected V readValue(JsonReader reader) throws Exception;

    abstract protected String toKeyString(K key) throws Exception;

    abstract protected K toKey(String key) throws Exception;

    protected static class JsonWriter {

        JsonGenerator generator;

        JsonWriter(JsonGenerator generator) {
            this.generator = generator;
        }

        public void setBytes(byte[] bytes, int offset, int length) throws Exception {
            generator.writeBinary(bytes, offset, length);
        }

        public void setBoolean(boolean value) throws Exception {
            generator.writeBoolean(value);
        }

        public void setNumber(BigDecimal value) throws Exception {
            if (value == null) {
                throw new NullPointerException();
            }
            generator.writeNumber(value);
        }

        public void setNumber(BigInteger value) throws Exception {
            if (value == null) {
                throw new NullPointerException();
            }
            generator.writeNumber(value);
        }

        public void setNumber(double value) throws Exception {
            generator.writeNumber(value);
        }

        public void setNumber(float value) throws Exception {
            generator.writeNumber(value);
        }

        public void setNumber(int value) throws Exception {
            generator.writeNumber(value);
        }

        public void setNumber(long value) throws Exception {
            generator.writeNumber(value);
        }

        public void setObject(Object object) throws Exception {
            if (object == null) {
                throw new NullPointerException();
            }
            generator.writeObject(object);
        }

        public void setString(String value) throws Exception {
            if (value == null) {
                throw new NullPointerException();
            }
            generator.writeString(value);
        }
    }

    protected static class JsonReader {

        JsonParser parser;

        JsonReader(JsonParser parser) {
            this.parser = parser;
        }

        public byte[] readBytes() throws Exception {
            return parser.getBinaryValue();
        }

        public boolean readBoolean() throws Exception {
            return parser.getBooleanValue();
        }

        public BigDecimal readBigDecimal() throws Exception {
            return parser.getDecimalValue();
        }

        public BigInteger getBigInteger() throws Exception {
            return parser.getBigIntegerValue();
        }

        public double getDouble() throws Exception {
            return parser.getDoubleValue();
        }

        public float getFloat() throws Exception {
            return parser.getFloatValue();
        }

        public int getInt() throws Exception {
            return parser.getIntValue();
        }

        public long getLong() throws Exception {
            return parser.getLongValue();
        }

        public Object getObject() throws Exception {
            return parser.getEmbeddedObject();
        }

        public String getString() throws Exception {
            return parser.getText();
        }
    }
}
