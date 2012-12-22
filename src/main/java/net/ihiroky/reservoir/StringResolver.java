package net.ihiroky.reservoir;

/**
 * Resolves {@code String} value to a type value .
 *
 * @param <K> the type of value to be resolved.
 * @author Hiroki Itoh
 */
public interface StringResolver<K> {

    /**
     * Resolves a {@code String} expression key to a K value.
     * @param key @ key to be resolved.
     * @return a value of the type specified with K.
     */
    K resolve(String key);

    /** Resolver to change any {@code String} value to null. */
    StringResolver<?> NULL = new StringResolver<Object>() {
        @Override
        public Object resolve(String key) {
            return null;
        }
    };

    /** Identity function resolver. */
    StringResolver<String> STRING = new StringResolver<String>() {
        @Override
        public String resolve(String key) {
            return key;
        }
    };

    /**
     * Resolver to change {@code String} value to integer value.
     */
    StringResolver<Integer> INTEGER = new StringResolver<Integer>() {
        @Override
        public Integer resolve(String key) {
            try {
                return Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
            }
            return 0;
        }
    };

    /**
     * Resolver to change {@code String} value to long value.
     */
    StringResolver<Long> LONG = new StringResolver<Long>() {
        @Override
        public Long resolve(String key) {
            try {
                return Long.parseLong(key);
            } catch (NumberFormatException ignored) {
            }
            return 0L;
        }
    };
}
