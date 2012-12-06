package net.ihiroky.reservoir.rest;

import net.ihiroky.reservoir.coder.XMLCoder;

/**
 * Created on 12/10/24, 12:19
 *
 * @author Hiroki Itoh
 */
class IntegerXMLCoder extends XMLCoder<Integer, Integer> {

    @Override
    protected String toKeyString(Integer key) {
        return String.valueOf(key);
    }

    @Override
    protected String toValueString(Integer value) {
        return String.valueOf(value);
    }

    @Override
    protected Integer toKey(String key) {
        return Integer.parseInt(key);
    }

    @Override
    protected Integer toValue(String value) {
        return Integer.parseInt(value);
    }
}
