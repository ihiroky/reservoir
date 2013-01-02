package net.ihiroky.reservoir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Provides for methods to register or unregister MBean to the platform MBean server.
 *
 * @author Hiroki Itoh
 */
public class MBeanSupport {

    private static final String PACKAGE_NAME = MBeanSupport.class.getPackage().getName();
    private static Logger logger = LoggerFactory.getLogger(MBeanSupport.class);

    /**
     * This class can't be instantiate.
     */
    private MBeanSupport() {
        throw new AssertionError("this class can't be instantiated.");
    }

    /**
     * Creates {@code javax.management.ObjectName}.
     *
     * @param object an object to decide the 'type' key.
     * @param name a name to decide the 'name' key.
     * @return a {@code ObjectName} which has a name such as
     * {@code net.ihiroky.reservoir:type='base name of object',name='name'}, or null if failed to create
     * {@code ObjectName} instance
     */
    private static ObjectName createObjectName(Object object, String name) {
        String className = object.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        String objectNameStr = PACKAGE_NAME + ":type=" + className + ",name=" + name;
        try {
            return new ObjectName(objectNameStr);
        } catch (Exception e) {
            logger.warn("failed to create ObjectName instance : ".concat(objectNameStr), e);
        }
        return null;
    }

    /**
     * Registers a specified object to the platform MBean server. The object is required to be MBean.
     *
     * @param object MBean to be registered
     * @param name name key of the {@code object}
     * @return true if the specified MBean is registered to the platform MBean server
     */
    public static boolean registerMBean(Object object, String name) {
        ObjectName objectName = createObjectName(object, name);
        if (objectName != null) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                mBeanServer.registerMBean(object, objectName);
                return true;
            } catch (Exception e) {
                logger.warn("failed to register a MBean specified by " + object + ", " + name, e);
            }
        }
        return false;
    }

    /**
     * Unregister a MBean specified by arguments from the platform MBean server if the MBean is not registered
     * in the platform MBean server.
     * @param object MBen to be unregistered
     * @param name name key of the {@code object}
     * @return true if the specified MBean is unregistered from the platform MBean server
     */
    public static boolean unregisterMBean(Object object, String name) {
        ObjectName objectName = createObjectName(object, name);
        if (objectName != null) {
            try {
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                synchronized (MBeanSupport.class) {
                    if (mBeanServer.isRegistered(objectName)) {
                        mBeanServer.unregisterMBean(objectName);
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.warn("failed to unregister a MBean specified by " + object + ", " + name, e);
            }
        }
        return false;
    }
}
