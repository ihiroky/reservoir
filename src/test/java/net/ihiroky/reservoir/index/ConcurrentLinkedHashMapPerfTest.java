package net.ihiroky.reservoir.index;

import net.ihiroky.reservoir.ConcurrentTestUtil;
import net.ihiroky.reservoir.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/11/22, 9:37
 *
 * @author Hiroki Itoh
 */
public class ConcurrentLinkedHashMapPerfTest {

    @Test(timeout = 3000)
    public void testMTPut() throws InterruptedException {
        final ConcurrentLinkedHashMap<Integer, Integer> m0 = new ConcurrentLinkedHashMap<Integer, Integer>();

        Runnable r0 = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.put(i % 5, i);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(2, r0, TimeUnit.MILLISECONDS);
        System.out.println("put/put:" + result.getElapsed());
        assertThat(result.getUncaughtExceptionMap(), is(Collections.<Integer, Throwable>emptyMap()));
        assertThat(m0.entrySet().containsAll(
                Pair.newImmutableEntries(0, 999995, 1, 999996, 2, 999997, 3, 999998, 4, 999999)), is(true));
        assertThat(m0.entrySet().size(), is(5));
        assertThat(m0.entrySet().containsAll(m0.orderQueue()), is(true));
        assertThat(m0.orderQueue().size(), is(m0.entrySet().size()));
    }

    @Test(timeout = 3000)
    public void testMTPutGet() throws InterruptedException {
        final ConcurrentLinkedHashMap<Integer, Integer> m0 = new ConcurrentLinkedHashMap<Integer, Integer>();

        Runnable putter = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.put(i % 5, i);
                }
            }
        };
        Runnable getter = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.get(i % 5);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(Arrays.asList(putter, getter), TimeUnit.MILLISECONDS);
        System.out.println("put/get:" + result.getElapsed());
        assertThat(result.getUncaughtExceptionMap(), is(Collections.<Integer, Throwable>emptyMap()));
        assertThat(m0.entrySet().containsAll(
                Pair.newImmutableEntries(0, 999995, 1, 999996, 2, 999997, 3, 999998, 4, 999999)), is(true));
        assertThat(m0.entrySet().size(), is(5));
        assertThat(m0.entrySet().containsAll(m0.orderQueue()), is(true));
        assertThat(m0.orderQueue().size(), is(m0.entrySet().size()));
    }

    @Test(timeout = 3000)
    public void testMTPutRemove() throws InterruptedException {
        final ConcurrentLinkedHashMap<Integer, Integer> m0 = new ConcurrentLinkedHashMap<Integer, Integer>();

        Runnable putter = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.put(i % 5, i);
                }
            }
        };
        Runnable remover = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.remove(i % 5);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(Arrays.asList(putter, remover), TimeUnit.MILLISECONDS);
        System.out.println("put/remove:" + result.getElapsed());
        assertThat(result.getUncaughtExceptionMap(), is(Collections.<Integer, Throwable>emptyMap()));
        assertThat(m0.size() <= 5, is(true));
        assertThat(m0.entrySet().containsAll(m0.orderQueue()), is(true));
        assertThat(m0.orderQueue().size(), is(m0.entrySet().size()));
    }

    @Test(timeout = 3000)
    public void testMTPutRemove1() throws InterruptedException {
        final ConcurrentLinkedHashMap<Integer, Integer> m0 = new ConcurrentLinkedHashMap<Integer, Integer>();

        Runnable putter = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.put(i % 333, i);
                }
            }
        };
        Runnable remover = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.remove(i % 333);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(Arrays.asList(putter, remover), TimeUnit.MILLISECONDS);
        System.out.println("put/remove1:" + result.getElapsed());
        assertThat(result.getUncaughtExceptionMap(), is(Collections.<Integer, Throwable>emptyMap()));
        // assertThat(m0.size() <= 5, is(true));
        assertThat(m0.orderQueue().size(), is(m0.entrySet().size()));
        assertThat(m0.entrySet().containsAll(m0.orderQueue()), is(true));
    }

    @Test(timeout = 3000)
    public void testMTMutRemove2() throws InterruptedException {
        final ConcurrentLinkedHashMap<Integer, Integer> m0 = new ConcurrentLinkedHashMap<Integer, Integer>();

        Runnable putter = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.put(i % 5, i);
                }
            }
        };
        Runnable remover = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    m0.remove(i % 5, i);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(Arrays.asList(putter, remover), TimeUnit.MILLISECONDS);
        System.out.println("put/remove2:" + result.getElapsed());
        assertThat(result.getUncaughtExceptionMap(), is(Collections.<Integer, Throwable>emptyMap()));
        assertThat(m0.size() <= 5, is(true));
        assertThat(m0.entrySet().containsAll(m0.orderQueue()), is(true));
        assertThat(m0.orderQueue().size(), is(m0.entrySet().size()));
    }

    @Test(timeout = 3000)
    public void testMTPutReplace() throws InterruptedException {
        final ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        Runnable putter = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    map.put(i % 5, i);
                }
            }
        };
        Runnable replace = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    map.replace(i % 5, i - 5);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(Arrays.asList(putter, replace), TimeUnit.MILLISECONDS);
        System.out.println("put/replace:" + result.getElapsed());
        assertThat(result.getUncaughtExceptionMap(), is(Collections.<Integer, Throwable>emptyMap()));
        assertThat(map.size(), is(5));
        assertThat(map.entrySet().containsAll(map.orderQueue()), is(true));
        assertThat(map.orderQueue().size(), is(map.entrySet().size()));
    }

    @Test(timeout = 3000)
    public void testMTPutReplace2() throws InterruptedException {
        final ConcurrentLinkedHashMap<Integer, Integer> map = new ConcurrentLinkedHashMap<Integer, Integer>();

        Runnable putter = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    map.put(i % 5, i);
                }
            }
        };
        Runnable replace = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    map.replace(i % 5, i - 5, i + 5);
                }
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(Arrays.asList(putter, replace), TimeUnit.MILLISECONDS);
        System.out.println("put/replace2:" + result.getElapsed());
        assertThat(result.getUncaughtExceptionMap(), is(Collections.<Integer, Throwable>emptyMap()));
        assertThat(map.size(), is(5));
        assertThat(map.entrySet().containsAll(map.orderQueue()), is(true));
        assertThat(map.orderQueue().size(), is(map.entrySet().size()));
    }
}
