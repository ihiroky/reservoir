package net.ihiroky.reservoir.rest;

import net.ihiroky.reservoir.coder.JSONCoder;

/**
 * Created on 12/10/24, 12:31
 *
 * @author Hiroki Itoh
 */
public class IntegerJSONCoder extends JSONCoder<Integer, Integer> {
    @Override
    protected void writeKey(JsonWriter writer, Integer key) throws Exception {
        writer.setNumber(key);
    }

    @Override
    protected void writeValue(JsonWriter writer, Integer value) throws Exception {
        writer.setNumber(value);
    }

    @Override
    protected Integer readKey(JsonReader reader) throws Exception {
        return reader.getInt();
    }

    @Override
    protected Integer readValue(JsonReader reader) throws Exception {
        return reader.getInt();
    }

    @Override
    protected String toKeyString(Integer key) throws Exception {
        return String.valueOf(key);
    }

    @Override
    protected Integer toKey(String key) throws Exception {
        return Integer.parseInt(key);
    }
}
