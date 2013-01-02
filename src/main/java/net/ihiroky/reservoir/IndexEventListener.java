package net.ihiroky.reservoir;

/**
 * Provides for methods to receive index structure change events.
 *
 * @author Hiroki Itoh
 */
public interface IndexEventListener<K, V> {

    /**
     * Invoked when a new entry is inserted in the specified {@code index}.
     *
     * @param index an index into which a new entry is inserted
     * @param key a key of a new entry
     * @param value a value of a new entry
     */
    void onPut(Index<K, V> index, K key, V value);

    /**
     * Invoked when a entry in the specified {@code index} is removed from.
     *
     * @param index an index from which a entry is removed.
     * @param key a key of a entry to be removed
     * @param value a value of a new entry to be removed
     */
    void onRemove(Index<K, V> index, K key, V value);

    /**
     * Invoked just before a entry is pushed out from the specified index because of size limitation, time limitation
     * and so on. This method controls whether the entry is remove from the index or not by its return value.
     *
     * @param index an index from which a entry is pushed out
     * @param key a key of a entry to be pushed out
     * @param value a value of a entry to be pushed out
     * @return true if it is possible to remove the {@code key}/{@code value}.
     */
    boolean onDiscard(Index<K, V> index, K key, V value);

    IndexEventListener<?, ?> NULL_LISTENER = new IndexEventListener<Object, Object>() {
        @Override
        public void onPut(Index<Object, Object> index, Object key, Object value) {
        }

        @Override
        public void onRemove(Index<Object, Object> index, Object key, Object value) {
        }

        @Override
        public boolean onDiscard(Index<Object, Object> index, Object key, Object value) {
            return true;
        }
    };
}
