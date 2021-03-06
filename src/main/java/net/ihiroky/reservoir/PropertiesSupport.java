package net.ihiroky.reservoir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created on 12/09/28, 12:56
 * <p/>
 * TODO logging
 *
 * @author Hiroki Itoh
 */
public final class PropertiesSupport {

    private Properties properties;

    private static Logger logger = LoggerFactory.getLogger(PropertiesSupport.class);

    private static final String KEY_PREFIX = "reservoir.";

    private PropertiesSupport() {
        properties = new Properties();
    }

    public static PropertiesSupport builder() {
        return new PropertiesSupport();
    }

    public PropertiesSupport set(String key, String value) {
        properties.setProperty(key, value);
        return this;
    }

    public PropertiesSupport set(Class<?> cls, String key, String value) {
        properties.setProperty(key(cls, key), value);
        return this;
    }

    public PropertiesSupport remove(String key) {
        properties.remove(key);
        return this;
    }

    public PropertiesSupport remove(Class<?> cls, String key) {
        properties.remove(key(cls, key));
        return this;
    }

    public Properties properties() {
        return properties;
    }

    public static boolean booleanValue(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        return (value != null) ? Boolean.parseBoolean(value) : defaultValue;
    }

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

    public static Object newInstance(Properties props, String key, Class<?> defaultClass) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Class.forName(value).newInstance();
            } catch (Throwable t) {
                logger.info("[newInstance] failed to instantiate by property(" + key + "," + value + ")", t);
            }
        }
        try {
            return (defaultClass != null) ? defaultClass.newInstance() : null;
        } catch (Throwable t) {
            logger.info("[newInstance] failed to instantiate default class: " + defaultClass, t);
        }
        return null;
    }

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

    public static String baseName(Class<?> cls) {
        String fqcn = cls.getName();
        int lastDotIndex = fqcn.lastIndexOf('.');
        return (lastDotIndex != -1) ? fqcn.substring(lastDotIndex + 1) : fqcn;
    }

    public static String key(Class<?> cls, String key) {
        return KEY_PREFIX + baseName(cls) + '.' + key;
    }
}
