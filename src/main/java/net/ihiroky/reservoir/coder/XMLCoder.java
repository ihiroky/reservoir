package net.ihiroky.reservoir.coder;

import net.ihiroky.reservoir.Cache;
import net.ihiroky.reservoir.StreamingCoder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created on 12/10/24, 9:13
 *
 * @author Hiroki Itoh
 */
public abstract class XMLCoder<K, V> implements StreamingCoder<K, V> {

    private XMLOutputFactory outputFactory;
    private XMLInputFactory inputFactory;

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private static final String ELEMENT_OUTER = "entries";
    private static final String ELEMENT_INNER = "entry";
    private static final String ATTRIBUTE_KEY = "key";

    private static final XMLCoder<String, String> STRING_CODER = new XMLCoder<String, String>() {
        @Override
        protected String toKeyString(String key) {
            return key;
        }

        @Override
        protected String toValueString(String value) {
            return value;
        }

        @Override
        protected String toKey(String key) {
            return key;
        }

        @Override
        protected String toValue(String value) {
            return value;
        }
    };

    public static XMLCoder<String, String> getStringCoder() {
        return STRING_CODER;
    }

    public XMLCoder() {
        outputFactory = XMLOutputFactory.newInstance();
        inputFactory = XMLInputFactory.newInstance();
    }

    @Override
    public void write(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        K k = toKey(key);
        write(k, cache.get(k), outputStream);
    }

    private void write(K key, V value, OutputStream outputStream) throws Exception {
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(
                new OutputStreamWriter(outputStream, CHARSET));
        writer.writeStartDocument();
        writer.writeStartElement(ELEMENT_OUTER);
        if (value != null) {
            writer.writeStartElement(ELEMENT_INNER);
            writer.writeAttribute(ATTRIBUTE_KEY, toKeyString(key));
            writer.writeCharacters(toValueString(value));
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
    }

    @Override
    public void write(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        write(pattern, cache, outputStream, false);
    }

    private void write(Pattern pattern, Cache<K, V> cache, OutputStream outputStream,
                       boolean removeIfMatched) throws Exception {
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(
                new OutputStreamWriter(outputStream, CHARSET));
        writer.writeStartDocument();
        writer.writeStartElement(ELEMENT_OUTER);
        Map.Entry<K, V> entry;
        for (Iterator<Map.Entry<K, V>> iterator = cache.iterator(); iterator.hasNext(); ) {
            entry = iterator.next();
            String keyString = toKeyString(entry.getKey());
            if (pattern.matcher(keyString).find()) {
                writer.writeStartElement(ELEMENT_INNER);
                writer.writeAttribute(ATTRIBUTE_KEY, keyString);
                writer.writeCharacters(toValueString(entry.getValue()));
                writer.writeEndElement();
                if (removeIfMatched) {
                    iterator.remove();
                }
            }
        }
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
    }

    @Override
    public void read(Cache<K, V> cache, InputStream inputStream) throws Exception {
        XMLStreamReader reader = inputFactory.createXMLStreamReader(
                new InputStreamReader(inputStream, CHARSET));
        K key = null;
        StringBuilder valueBuilder = new StringBuilder();
        LOOP:
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT: {
                    String localName = reader.getLocalName();
                    if (!ELEMENT_INNER.equals(localName)) {
                        continue LOOP;
                    }
                    key = parseKey(reader);
                }
                break;
                case XMLStreamReader.CHARACTERS:
                    if (key != null) {
                        valueBuilder.append(reader.getText());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT: {
                    String localName = reader.getLocalName();
                    if (!ELEMENT_INNER.equals(localName)) {
                        continue LOOP;
                    }
                    V value = toValue(valueBuilder.toString());
                    if (key != null && value != null) {
                        cache.put(key, value);
                        key = null;
                        valueBuilder = new StringBuilder();
                    }
                }
                break;
            }
        }
    }


    private K parseKey(XMLStreamReader reader) throws Exception {
        int attrCount = reader.getAttributeCount();
        String attrName;
        for (int i = 0; i < attrCount; i++) {
            attrName = reader.getAttributeLocalName(i);
            if (ATTRIBUTE_KEY.equals(attrName)) {
                return toKey(reader.getAttributeValue(i));
            }
        }
        return null;
    }

    @Override
    public void delete(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        K k = toKey(key);
        write(k, cache.remove(k), outputStream);
    }

    @Override
    public void delete(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception {
        write(pattern, cache, outputStream, true);
    }

    @Override
    public void delete(Cache<K, V> cache, InputStream inputStream) throws Exception {
        XMLStreamReader reader = inputFactory.createXMLStreamReader(
                new InputStreamReader(inputStream, CHARSET));
        K key = null;
        LOOP:
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT: {
                    String localName = reader.getLocalName();
                    if (!ELEMENT_INNER.equals(localName)) {
                        continue LOOP;
                    }
                    key = parseKey(reader);
                }
                break;
                case XMLStreamReader.END_ELEMENT: {
                    String localName = reader.getLocalName();
                    if (!ELEMENT_INNER.equals(localName)) {
                        continue LOOP;
                    }
                    if (key != null) {
                        cache.delete(key);
                        key = null;
                    }
                }
                break;
            }
        }
    }

    abstract protected String toKeyString(K key);

    abstract protected String toValueString(V value);

    abstract protected K toKey(String key);

    abstract protected V toValue(String value);
}
