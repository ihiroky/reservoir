package net.ihiroky.reservoir;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Provides interfaces to access a {@link net.ihiroky.reservoir.Cache} from its outside.
 * This interface is used by REST operations and {@code Cache} saving to or loading from an outside storage.
 *
 * @param <K> the type of keys associated with a value in a {@code Cache}
 * @param <V> the type of mapped values in a {@code Cache}
 * @author Hiroki Itoh
 */
public interface StreamingCoder<K, V> {

    /**
     * Writes a {@code key} and a mapped value in the {@code Cache} to the specified {@code outputStream}.
     *
     * @param key a key associated with the {@code value} in the {@code Cache}
     * @param cache an object that manages key - value mappings
     * @param outputStream a stream to be written the {@code key} and the {@code value}
     * @throws Exception if failed to write.
     */
    void write(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    /**
     * Writes keys that are matched a specified {@code pattern} and mapped values to the specified {@code outputStream}.
     * If the pattern matches more than one keys, all keys and mapped values are written.
     *
     * @param pattern a regular expression to determine target keys and mapped valeus to be written
     * @param cache an object that manages key - value mappings
     * @param outputStream a stream to be written the {@code key} and the {@code value}
     * @throws Exception if failed to write
     */
    void write(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    /**
     * Reads keys and mapped values from a specified {@code java.io.InputStream}.
     *
     * @param cache an object which holds the mappings
     * @param inputStream a stream to be read the {@code value}
     * @throws Exception if failed to read
     */
    void read(Cache<K, V> cache, InputStream inputStream) throws Exception;

    /**
     * Removes a key and a mapped value from a specified {@link net.ihiroky.reservoir.Cache}. The Removed
     * key and value are written to a specified {@code outputStream}.
     *
     * @param key a key to be removed from the {@code cache}
     * @param cache a cache from which is removed the {@code key}
     * @param outputStream a stream written the removed key and mapped value
     * @throws Exception if failed to delete
     */
    void delete(String key, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    /**
     * Removes keys matched by {@code pattern} and mapped values from a specified {@link net.ihiroky.reservoir.Cache}.
     * The Removed keys and values are written to a specified {@codpe outputStream}.
     *
     * @param pattern a regular expression to determine keys to be removed.
     * @param cache a cache from which is removed the {@code key}
     * @param outputStream a stream written the removed key and mapped value
     * @throws Exception if failed to delete
     */
    void delete(Pattern pattern, Cache<K, V> cache, OutputStream outputStream) throws Exception;

    /**
     * Removes keys read from {@code inputStream} and mapped values from a specified
     * {@link net.ihiroky.reservoir.Cache}.
     *
     * @param cache a cache from which is removed
     * @param inputStream a stream that contains keys to be removed
     * @throws Exception if failed to delete
     */
    void delete(Cache<K, V> cache, InputStream inputStream) throws Exception;
}
