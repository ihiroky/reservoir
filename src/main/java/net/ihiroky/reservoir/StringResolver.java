package net.ihiroky.reservoir;

/**
 * Created on 12/10/18, 15:18
 *
 * @author Hiroki Itoh
 */
public interface StringResolver<K> {

    K resolve(String key);

    StringResolver<?> NULL = new StringResolver<Object>() {
        @Override
        public Object resolve(String key) {
            return null;
        }
    };

    StringResolver<String> STRING = new StringResolver<String>() {
        @Override
        public String resolve(String key) {
            return key;
        }
    };

    StringResolver<Integer> INTEGER = new StringResolver<Integer>() {
        @Override
        public Integer resolve(String key) {
            try {
                return Integer.parseInt(key);
            } catch (NumberFormatException nfe) {
            }
            return 0;
        }
    };

    StringResolver<Long> LONG = new StringResolver<Long>() {
        @Override
        public Long resolve(String key) {
            try {
                return Long.parseLong(key);
            } catch (NumberFormatException nfe) {
            }
            return 0L;
        }
    };
}
