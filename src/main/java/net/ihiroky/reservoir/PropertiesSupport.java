package net.ihiroky.reservoir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Provides for utility methods to use {@code java.util.Properties}.
 * This class has methods to get typed values stored in {@code java.util.Properties}.
 * The sub class {@link PropertiesBuilder} is a builder to create {@code java.util.Properties}
 * with a fluent interface.
 *
 * @author Hiroki Itoh
 */
public final class PropertiesSupport {

    private static Logger logger = LoggerFactory.getLogger(PropertiesSupport.class);

    private static final String KEY_PREFIX = "reservoir.";

    private PropertiesSupport() {
        throw new AssertionError("this class can't be instantiated.");
    }

    /**
     * Creates a builder to create {@code java.util.Properties}.
     *
     * @return a builder to create {@code java.util.Properties}
     */
    public static PropertiesBuilder builder() {
        return new PropertiesBuilder();
    }

    /**
     * Returns a boolean value associated with the {@code key} from a specified {@code props}.
     * If the {@code props} doesn't contains {@code key}, returns {@code defaultValue}.
     *
     * @param props a {@code java.util.Properties} from which the value is got
     * @param key a key associated with a value to get
     * @param defaultValue a value to be returned if {@code props} doesn't contains {@code key}
     * @return a mapped value if {@code props} contains {@code key}, or {@code defaultValue}
     */
    public static boolean booleanValue(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        return (value != null) ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Returns an int value associated with the {@code key} from a specified {@code props}.
     * If the {@code props} doesn't contains {@code key}, returns {@code defaultValue}.
     *
     * @param props a {@code java.util.Properties} from which the value is got
     * @param key a key associated with a value to get
     * @param defaultValue a value to be returned if {@code props} doesn't contains {@code key}
     * @return a mapped value if {@code props} contains {@code key}, or {@code defaultValue}
     */
    public static int intValue(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                logger.info("[intValue] failed to parse property({}:{}), use default:{}",
                        key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Returns a long value associated with the {@code key} from a specified {@code props}.
     * If the {@code props} doesn't contains {@code key}, returns {@code defaultValue}.
     *
     * @param props a {@code java.util.Properties} from which the value is got
     * @param key a key associated with a value to get
     * @param defaultValue a value to be returned if {@code props} doesn't contains {@code key}
     * @return a mapped value if {@code props} contains {@code key}, or {@code defaultValue}
     */
    public static long longValue(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException nfe) {
                logger.info("[longValue] failed to parse property({}:{}), use default:{}",
                        key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Returns an instance which class name is a value associated with the {@code key} from a specified {@code props}.
     * If the {@code props} doesn't contains {@code key}, returns a instance of {@code defaultClass}.
     *
     * @param props a {@code java.util.Properties} from which the value is got
     * @param key a key associated with a value to get
     * @param defaultClass a class of the result instance if {@code props} doesn't contains {@code key}
     * @return a instance which class name is mapped value if {@code props} contains {@code key}
     * or {@code defaultValue}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Properties props, String key, Class<?> defaultClass) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return (T) Class.forName(value).newInstance();
            } catch (Throwable t) {
                logger.info("[newInstance] failed to instantiate by property(" + key + "," + value + ")", t);
            }
        }
        try {
            return (T) ((defaultClass != null) ? defaultClass.newInstance() : null);
        } catch (Throwable t) {
            logger.info("[newInstance] failed to instantiate default class: " + defaultClass, t);
        }
        return null;
    }

    /**
     * Creates {@code java.util.Properties} which contains specified keys and values.
     *
     * @param key first key associated with {@code value}
     * @param value a value with which {@code key} is associated
     * @param keyValues the other keys and values
     * @return {@code java.util.Properties} which contains specified keys and values
     * @throws IllegalArgumentException if a length of {@code keyValues} is odd, this means a value with which
     * a last key associated is lost
     */
    public static Properties create(String key, String value, String... keyValues) {
        if (keyValues.length % 2 == 1) {
            throw new IllegalArgumentException();
        }
        Properties props = new Properties();
        props.put(key, value);
        for (int i = 0; i < keyValues.length; i += 2) {
            props.put(keyValues[i], keyValues[i + 1]);
        }
        return props;
    }

    /**
     * Returns a base name of a specified {@code java.lang.Class}.
     *
     * @param cls a {@code java.lang.Class} to be parsed
     * @return a base name of a specified {@code java.lang.Class}
     */
    public static String baseName(Class<?> cls) {
        String fqcn = cls.getName();
        int lastDotIndex = fqcn.lastIndexOf('.');
        return (lastDotIndex != -1) ? fqcn.substring(lastDotIndex + 1) : fqcn;
    }

    public static String key(Class<?> cls, String key) {
        return KEY_PREFIX + baseName(cls) + '.' + key;
    }

    /**
     * A builder to create {@code java.util.Properties}.
     */
    public final static class PropertiesBuilder {

        private Properties props;

        private PropertiesBuilder() {
            props = new Properties();
        }

        /**
         * Puts key and value into a {@code java.util.Properties} to build.
         * @param key a key associated with {@code value}
         * @param value a value against {@code key}
         * @return this instance
         */
        public PropertiesBuilder put(String key, String value) {
            props.setProperty(key, value);
            return this;
        }

        /**
         * Puts key in a specified class and value into a {@code java.util.Properties} to build.
         * @param cls a class object which contains {@code key}
         * @param key a key defined in {@code cls}
         * @param value a value against a specified key.
         * @return this instance
         */
        public PropertiesBuilder put(Class<?> cls, String key, String value) {
            props.setProperty(key(cls, key), value);
            return this;
        }

        /**
         * Removes a mapping spcified with a {@code key}.
         * @param key a key to be remove from a {@code java.util.Properties} to build
         * @return this instance
         */
        public PropertiesBuilder remove(String key) {
            props.remove(key);
            return this;
        }

        /**
         * Removes a mapping spcified with a {@code key}.
         * @param cls a class object which contains {@code key}
         * @param key a key to be remove from a {@code java.util.Properties} to build
         * @return this instance
         */
        public PropertiesBuilder remove(Class<?> cls, String key) {
            props.remove(key(cls, key));
            return this;
        }

        /**
         * Returns a instance of {@code java.util.Properties} representing the current data.
         * The current properties is returned and a new properties to record future operations is created.
         * @return a instance of {@code java.util.Properties} representing the current data
         */
        public Properties build() {
            Properties result = props;
            props = new Properties();
            return result;
        }
    }
}
