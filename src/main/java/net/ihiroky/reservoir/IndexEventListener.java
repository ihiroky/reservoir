package net.ihiroky.reservoir;

/**
 * Created on 12/09/18, 15:38
 *
 * @author Hiroki Itoh
 */
public interface IndexEventListener<K, V> {

    void onPut(Index<K, V> index, K key, V value);

    void onRemove(Index<K, V> index, K key, V value);

    /**
     * Called just before cache out.
     *
     * @param index
     * @param key
     * @param value
     * @return true if it is possible to remove the {@code key}/{@code value}.
     */
    boolean onCacheOut(Index<K, V> index, K key, V value);

    IndexEventListener<?, ?> NULL_LISTENER = new IndexEventListener<Object, Object>() {
        @Override
        public void onPut(Index<Object, Object> index, Object key, Object value) {
        }

        @Override
        public void onRemove(Index<Object, Object> index, Object key, Object value) {
        }

        @Override
        public boolean onCacheOut(Index<Object, Object> index, Object key, Object value) {
            return true;
        }
    };
}
