package net.ihiroky.reservoir;

import net.ihiroky.reservoir.coder.SimpleStringCoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 12/11/22, 9:40
 *
 * @author Hiroki Itoh
 */
public class BasicCachePerfTest {

    List<Cache<?, ?>> disposeList;

    @Before
    public void before() {
        disposeList = new ArrayList<Cache<?, ?>>();
    }

    @After
    public void after() {
        for (Cache<?, ?> cache : disposeList) {
            cache.dispose();
        }
    }

    @Test
    public void test() {
        char[] a = new char[1024];
        Arrays.fill(a, 'あ');
        String s = new String(a);
        Cache<Integer, String> stringCache =
                Reservoir.createOffHeapCache("string", 8192, 512, 4, new SimpleStringCoder());
        disposeList.add(stringCache);

        int times = 1000000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            stringCache.put(i % 4, s);
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed:" + elapsed + "msec, avg:"
                + ((double) elapsed / times) + "msec. speed:" + ((double) times / elapsed * 1000) + " times/sec");
    }

}
