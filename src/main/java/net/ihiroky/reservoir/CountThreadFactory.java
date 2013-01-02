package net.ihiroky.reservoir;

import java.util.concurrent.ThreadFactory;

/**
 * Creates threads whose name suffix are counted up.
 *
 * @author Hiroki Itoh
 */
public class CountThreadFactory implements ThreadFactory {

    /** a thread base name */
    private String name;

    /** a thread name suffix */
    private int count;

    /**
     * Constructs this object.
     *
     * @param name base name of threads that are created by this factory
     */
    public CountThreadFactory(String name) {
        if (name == null || name.length() == 0) {
            throw new NullPointerException("name must not be null or empty.");
        }
        this.name = name.concat(":");
    }

    /**
     * Creates a new thread. The thread base name is add by the constructor and its suffix is the number of threads
     * created before this method call.
     */
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name.concat(Integer.toString(count++)));
    }
}
