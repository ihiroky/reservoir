package net.ihiroky.reservoir;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Created on 12/10/24, 10:30
 *
 * @author Hiroki Itoh
 */
public interface StreamingCoder<K, V> {
    void write(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    void write(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    void read(Cache<K, V> cache, InputStream inputStream) throws Exception;

    void delete(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    void delete(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    void delete(Cache<K, V> cache, InputStream inputStream) throws Exception;
}
