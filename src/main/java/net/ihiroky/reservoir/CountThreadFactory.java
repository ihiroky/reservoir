package net.ihiroky.reservoir;

import java.util.concurrent.ThreadFactory;

/**
 * Created on 12/09/27, 11:41
 *
 * @author Hiroki Itoh
 */
public class CountThreadFactory implements ThreadFactory {

    private String name;
    private int count;

    public CountThreadFactory(String name) {
        if (name == null || name.length() == 0) {
            throw new NullPointerException("name must not be null or empty.");
        }
        this.name = name.concat(":");
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name.concat(Integer.toString(count++)));
    }
}
