package net.ihiroky.reservoir;

import net.ihiroky.reservoir.storage.BulkInfo;
import net.ihiroky.reservoir.storage.ByteBufferStorageAccessor;
import net.ihiroky.reservoir.storage.RejectedAllocationPolicy;
import net.ihiroky.reservoir.coder.ByteArrayCoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/17, 10:07
 *
 * @author Hiroki Itoh
 */
public class BasicBlockingQueueTest {

    private BasicBlockingQueue<byte[]> queue;
    private Collection<BasicBlockingQueue<?>> disposeSet;
    private ByteBufferStorageAccessor<Object, byte[]> cacheAccessor;
    private byte[] b0;
    private byte[] b1;
    private byte[] b2;

    @Before
    public void before() throws Exception{
        cacheAccessor = new ByteBufferStorageAccessor<Object, byte[]>();
        cacheAccessor.prepare("BasicBlockingQueueTest", false, new ByteArrayCoder(), new BulkInfo(48, 8, 1),
                RejectedAllocationPolicy.WAIT_FOR_FREE_BLOCK);
        queue = new BasicBlockingQueue<byte[]>("BasicBlockingQueueTest", cacheAccessor);
        disposeSet = new HashSet<BasicBlockingQueue<?>>(Arrays.<BasicBlockingQueue<?>>asList(queue));
        b0 = new byte[]{1, 2, 3, 4, 5};
        b1 = new byte[]{2, 3, 4, 5, 6};
        b2 = new byte[]{3, 4, 5, 6, 7};
    }

    @After
    public void after() {
        for (BasicBlockingQueue<?> q : disposeSet) {
            q.dispose();
        }
    }

    @Test
    public void testEmptyIterator() {
        Iterator<byte[]> iterator = queue.iterator();
        assertThat(iterator.hasNext(), is(false));
        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testIterator() {
        queue.offer(b0);
        queue.offer(b1);
        queue.offer(b2);
        Iterator<byte[]> iterator = queue.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(b0));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(b1));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(b2));
        assertThat(iterator.hasNext(), is(false));
        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testSize() {
        assertThat(queue.size(), is(0));
        queue.offer(b0);
        assertThat(queue.size(), is(1));
        queue.offer(b0);
        assertThat(queue.size(), is(2));
        queue.offer(b1);
        assertThat(queue.size(), is(3));
        queue.offer(b1);
        assertThat(queue.size(), is(4));
    }

    @Test
    public void testPoll() {
        assertThat(queue.poll(), is(nullValue()));

        queue.offer(b0);
        queue.offer(b1);
        queue.offer(b2);
        assertThat(queue.poll(), is(b0));
        assertThat(queue.poll(), is(b1));
        assertThat(queue.poll(), is(b2));
        assertThat(queue.poll(), is(nullValue()));
    }

    @Test
    public void testPeek() {
        assertThat(queue.peek(), is(nullValue()));

        queue.offer(b0);
        assertThat(queue.peek(), is(b0));
        queue.offer(b1);
        assertThat(queue.peek(), is(b0));
        queue.offer(b2);
        assertThat(queue.peek(), is(b0));
    }

    @Test
    public void testIsEmpty() {
        assertThat(queue.isEmpty(), is(true));

        queue.offer(b0);
        assertThat(queue.isEmpty(), is(false));
    }

    @Test
    public void testPut() throws Exception {
        final BasicBlockingQueue<byte[]> queue1 =
                new BasicBlockingQueue<byte[]>("BasicBlockingQueueTest#testPut", cacheAccessor, 2);
        disposeSet.add(queue1);

        queue1.put(b0);
        queue1.put(b1);

        // call poll() after 100 ms.
        long start = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                queue1.poll();
            }
        }).start();
        queue1.put(b2); // blocked
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed >= 100, is(true));
        assertThat(queue1.poll(), is(b1));
        assertThat(queue1.poll(), is(b2));
        assertThat(queue1.isEmpty(), is(true));
    }

    @Test
    public void testOfferTimeout() throws Exception {
        final BasicBlockingQueue<byte[]> queue1 =
                new BasicBlockingQueue<byte[]>("BasicBlockingQueueTest#testPut", cacheAccessor, 2);
        disposeSet.add(queue1);

        queue1.offer(b0);
        long start = System.currentTimeMillis();
        queue1.offer(b1, 10, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed < 10, is(true));

        start = System.currentTimeMillis();
        queue1.offer(b2, 100, TimeUnit.MILLISECONDS);
        elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed >= 100, is(true));

        assertThat(queue1.poll(), is(b0));
        assertThat(queue1.poll(), is(b1));
        assertThat(queue1.isEmpty(), is(true));
    }

    @Test
    public void testTake() throws Exception {
        final BasicBlockingQueue<byte[]> queue1 =
                new BasicBlockingQueue<byte[]>("BasicBlockingQueueTest#testPut", cacheAccessor, 2);
        disposeSet.add(queue1);

        Runnable offerAfter100ms = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                queue1.offer(b0);
            }
        };

        long start = System.currentTimeMillis();
        new Thread(offerAfter100ms).start();
        byte[] bytes = queue1.take();
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed >= 100, is(true));
        assertThat(bytes, is(b0));
        assertThat(cacheAccessor.getAllocatedBlocks(), is(0L));

        // no wait if not empty.
        queue1.offer(b1);
        assertThat(cacheAccessor.getAllocatedBlocks(), is(1L));
        start = System.currentTimeMillis();
        bytes = queue1.take();
        elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed < 10, is(true));
        assertThat(bytes, is(b1));
        assertThat(cacheAccessor.getAllocatedBlocks(), is(0L));
    }

    @Test
    public void testPollTimeout() throws Exception {

        queue.offer(b0);
        assertThat(cacheAccessor.getAllocatedBlocks(), is(1L));
        long start = System.currentTimeMillis();
        byte[] bytes = queue.poll(100, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed < 10, is(true));
        assertThat(cacheAccessor.getAllocatedBlocks(), is(0L));

        start = System.currentTimeMillis();
        bytes = queue.poll(100, TimeUnit.MILLISECONDS);
        elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed >= 100, is(true));
        assertThat(bytes, is(nullValue()));
    }

    @Test
    public void testRemainingCapacity() {
        assertThat(queue.remainingCapacity(), is(Integer.MAX_VALUE));
        queue.offer(b0);
        assertThat(queue.remainingCapacity(), is(Integer.MAX_VALUE - 1));
    }

    @Test
    public void testDrainTo() {
        queue.offer(b0);
        queue.offer(b1);
        queue.offer(b2);
        List<byte[]> c = new ArrayList<byte[]>();
        queue.drainTo(c);
        assertThat(c.get(0), is(b0));
        assertThat(c.get(1), is(b1));
        assertThat(c.get(2), is(b2));
        assertThat(c.size(), is(3));
        assertThat(queue.isEmpty(), is(true));
    }

    @Test
    public void testDrainToLimit() {
        queue.offer(b0);
        queue.offer(b1);
        queue.offer(b2);
        List<byte[]> c = new ArrayList<byte[]>();
        queue.drainTo(c, 2);
        assertThat(c.get(0), is(b0));
        assertThat(c.get(1), is(b1));
        assertThat(c.size(), is(2));
        assertThat(queue.poll(), is(b2));
        assertThat(queue.isEmpty(), is(true));

        c = new ArrayList<byte[]>();
        queue.offer(b0);
        queue.drainTo(c, 2);
        assertThat(c.get(0), is(b0));
        assertThat(c.size(), is(1));
        assertThat(queue.isEmpty(), is(true));
    }
}
