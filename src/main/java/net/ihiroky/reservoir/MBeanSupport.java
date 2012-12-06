package net.ihiroky.reservoir;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Created on 12/10/18, 17:35
 *
 * @author Hiroki Itoh
 */
public class MBeanSupport {

    private MBeanSupport() {
        throw new AssertionError("this class can't be instantiated.");
    }

    private static ObjectName createObjectName(Object object, String name) {
        String packageName = object.getClass().getPackage().getName();
        String className = object.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        try {
            return new ObjectName(packageName + ":type=" + className + ",name=" + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void registerMBean(Object object, String name) {
        ObjectName objectName = createObjectName(object, name);
        if (objectName != null) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                mBeanServer.registerMBean(object, objectName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void unregisterMBean(Object object, String name) {
        ObjectName objectName = createObjectName(object, name);
        if (objectName != null) {
            try {
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                synchronized (MBeanSupport.class) {
                    if (mBeanServer.isRegistered(objectName)) {
                        mBeanServer.unregisterMBean(objectName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
