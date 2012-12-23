package net.ihiroky.reservoir;

import net.ihiroky.reservoir.storage.HeapStorageAccessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/17, 10:07
 *
 * @author Hiroki Itoh
 */
public class BasicQueueTest {

    private BasicQueue<byte[]> queue;
    private StorageAccessor<Object, byte[]> storageAccessor;
    private byte[] b0 = new byte[]{1, 2, 3, 4, 5};
    private byte[] b1 = new byte[]{2, 3, 4, 5, 6};
    private byte[] b2 = new byte[]{3, 4, 5, 6, 7};

    @Before
    public void before() {
        storageAccessor = new HeapStorageAccessor<Object, byte[]>();
        queue = new BasicQueue<byte[]>("BasicQueueTest", storageAccessor);
        b0 = new byte[]{1, 2, 3, 4, 5};
        b1 = new byte[]{2, 3, 4, 5, 6};
        b2 = new byte[]{3, 4, 5, 6, 7};
    }

    @After
    public void after() {
        if (queue != null) {
            queue.dispose();
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
}
