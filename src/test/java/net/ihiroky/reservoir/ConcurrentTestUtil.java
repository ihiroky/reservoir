package net.ihiroky.reservoir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created on 12/10/11, 9:41
 *
 * @author Hiroki Itoh
 */
public class ConcurrentTestUtil {

    public static <T> Result<T> runCallable(
            final List<Callable<T>> testList, TimeUnit resultTimeUnit) throws InterruptedException {
        int threadNum = testList.size();
        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch endSignal = new CountDownLatch(threadNum);
        final AtomicReferenceArray<T> resultArray = new AtomicReferenceArray<T>(threadNum);
        final AtomicReferenceArray<Throwable> exceptionArray = new AtomicReferenceArray<Throwable>(threadNum);
        for (int i = 0; i < threadNum; i++) {
            final int index = i;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Callable<T> test = testList.get(index);
                        startSignal.await();
                        resultArray.set(index, test.call());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new RuntimeException(t);
                    } finally {
                        endSignal.countDown();
                    }
                }
            }, "RunTest:" + i);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    exceptionArray.set(index, e);
                }
            });
            t.start();
        }

        long start = System.nanoTime();
        startSignal.countDown();
        endSignal.await();
        long elapsed = resultTimeUnit.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return new Result<T>(elapsed, resultTimeUnit, asList(resultArray), asMap(exceptionArray));

    }

    public static Result<?> runRunnable(
            List<Runnable> testList, TimeUnit resultTimeUnit) throws InterruptedException {
        List<Callable<Object>> callableList = new ArrayList<Callable<Object>>(testList.size());
        for (Runnable r : testList) {
            callableList.add(Executors.callable(r));
        }
        return runCallable(callableList, resultTimeUnit);
    }

    public static <T> Result<T> runCallable(
            int threadNum, Callable<T> test, TimeUnit resultTimeUnit) throws InterruptedException {
        List<Callable<T>> testList = new ArrayList<Callable<T>>(threadNum);
        for (int i = 0; i < threadNum; i++) {
            testList.add(test);
        }
        return runCallable(testList, resultTimeUnit);
    }

    public static Result<?> runRunnable(
            int threadNum, Runnable test, TimeUnit resultTimeUnit) throws InterruptedException {
        Result<Object> result = runCallable(threadNum, Executors.callable(test), resultTimeUnit);
        return new Result<Object>(result.getElapsed(), result.getElapsedUnit(),
                Collections.<Object>emptyList(), result.getUncaughtExceptionMap());
    }

    private static <T> List<T> asList(AtomicReferenceArray<T> a) {
        List<T> list = new ArrayList<T>(a.length());
        for (int i = 0; i < a.length(); i++) {
            list.add(a.get(i));
        }
        return list;
    }

    private static <T> Map<Integer, T> asMap(AtomicReferenceArray<T> a) {
        Map<Integer, T> map = new TreeMap<Integer, T>();
        for (int i = 0; i < a.length(); i++) {
            T t = a.get(i);
            if (t != null) {
                map.put(i, t);
            }
        }
        return map;
    }

    public static class Result<T> {
        private long elapsed;
        private TimeUnit elapsedUnit;
        private List<T> resultList;
        private Map<Integer, Throwable> uncaughtExceptionMap;

        public Result(long elapsed, TimeUnit elapsedUnit,
                      List<T> resultList, Map<Integer, Throwable> uncaughtExceptionMap) {
            this.elapsed = elapsed;
            this.elapsedUnit = elapsedUnit;
            this.resultList = Collections.unmodifiableList(resultList);
            this.uncaughtExceptionMap = Collections.unmodifiableMap(uncaughtExceptionMap);
        }

        public long getElapsed() {
            return elapsed;
        }

        public TimeUnit getElapsedUnit() {
            return elapsedUnit;
        }

        public List<T> getResultList() {
            return resultList;
        }

        public Map<Integer, Throwable> getUncaughtExceptionMap() {
            return uncaughtExceptionMap;
        }
    }
}
