package net.ihiroky.reservoir.accessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/09/28, 10:28
 *
 * @author Hiroki Itoh
 */
public class BlockedByteBufferTest {

    private ByteBlockManager bbb;

    protected ByteBlockManager createBlockedByteManager() throws Exception {
        return new BlockedByteBuffer(ByteBuffer.allocate(64), 16);
    }

    protected List<Number> asList(Number... numbers) {
        List<Number> list = new ArrayList<Number>(numbers.length);
        for (Number number : numbers) {
            list.add(number.intValue());
        }
        return list;
    }

    @Before
    public void before() throws Exception {
        bbb = createBlockedByteManager();
    }

    @After
    public void after() {
        if (bbb != null) {
            bbb.free();
        }
    }

    @Test
    public void testAllocateFree() {
        assertThat(bbb.freeBlockListView(), is(asList(0, 1, 2, 3)));
        ByteBlock block0 = bbb.allocate();
        assertThat(bbb.hasFreeBlock(), is(true));
        assertThat(bbb.freeBlockListView(), is(asList(1, 2, 3)));
        ByteBlock block1 = bbb.allocate();
        assertThat(bbb.hasFreeBlock(), is(true));
        assertThat(bbb.freeBlockListView(), is(asList(2, 3)));
        ByteBlock block2 = bbb.allocate();
        assertThat(bbb.hasFreeBlock(), is(true));
        assertThat(bbb.freeBlockListView(), is(asList(3)));
        ByteBlock block3 = bbb.allocate();
        assertThat(bbb.hasFreeBlock(), is(false));
        assertThat(bbb.freeBlockListView(), is(Collections.<Number>emptyList()));

        block1.free();
        assertThat(bbb.hasFreeBlock(), is(true));
        assertThat(bbb.freeBlockListView(), is(asList(1)));
        block3.free();
        assertThat(bbb.hasFreeBlock(), is(true));
        assertThat(bbb.freeBlockListView(), is(asList(1, 3)));
        block2.free();
        assertThat(bbb.hasFreeBlock(), is(true));
        assertThat(bbb.freeBlockListView(), is(asList(1, 3, 2)));
        block0.free();
        assertThat(bbb.hasFreeBlock(), is(true));
        assertThat(bbb.freeBlockListView(), is(asList(1, 3, 2, 0)));

        block1 = bbb.allocate();
        assertThat(bbb.freeBlockListView(), is(asList(3, 2, 0)));
        block3 = bbb.allocate();
        assertThat(bbb.freeBlockListView(), is(asList(2, 0)));
        block2 = bbb.allocate();
        assertThat(bbb.freeBlockListView(), is(asList(0)));
        block0 = bbb.allocate();
        assertThat(bbb.freeBlockListView(), is(Collections.<Number>emptyList()));
        assertThat(bbb.hasFreeBlock(), is(false));

        try {
            bbb.allocate();
        } catch (IllegalStateException ise) {
            assertThat(ise.getMessage(), is("no free block."));
        }

        block0.free();
        block0.free();
        block1.free();
        block1.free();
        block2.free();
        block2.free();
        block3.free();
        block3.free();
    }

    @Test
    public void testBlockGetPut() throws Exception {
        ByteBlock block0 = bbb.allocate();
        ByteBlock block1 = bbb.allocate();
        assertThat(block0.capacity(), is(16L));
        assertThat(block1.capacity(), is(16L));

        assertThat(block1.put(8, 255), is(1));
        assertThat(block1.get(8), is(255));
        assertThat(bbb.get(16 + 8), is((byte) 255));

        try {
            block1.put(16, 255);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            block1.get(16);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
        assertThat(bbb.get(16 + 16), is((byte) 0));
        assertThat(block1.put(0, 254), is(1));
        assertThat(block1.get(0), is(254));
        assertThat(bbb.get(16), is((byte) 254));
        try {
            block1.put(-1, 254);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            block1.get(-1);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
        assertThat(bbb.get(16 - 1), is((byte) 0));
    }

    @Test
    public void testBlockGetMulti() {
        bbb.allocate();
        ByteBlock block1 = bbb.allocate();
        for (int i = 0; i < 4; i++) {
            block1.put(i, '0');
        }
        for (int i = 4; i < 16; i++) {
            block1.put(i, '1');
        }

        byte[] get = new byte[4];
        assertThat(block1.get(2, get, 0, 4), is(4));
        assertThat(get, is(new byte[]{'0', '0', '1', '1'}));

        try {
            block1.get(-1, get, 0, get.length);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }

        Arrays.fill(get, (byte) '0');
        assertThat(block1.get(14, get, 1, get.length), is(2));
        assertThat(get, is(new byte[]{'0', '1', '1', '0'}));

        get = new byte[20];
        assertThat(block1.get(0, get, 2, get.length), is(16));
        byte[] expected = new byte[20];
        Arrays.fill(expected, 2, 6, (byte) '0');
        Arrays.fill(expected, 6, 18, (byte) '1');
        assertThat(get, is(expected));
    }

    @Test
    public void testBlockPutMulti() {
        bbb.allocate();
        ByteBlock block1 = bbb.allocate();

        byte[] put = new byte[8];
        byte[] get = new byte[(int) block1.capacity()];
        byte[] expected = new byte[(int) block1.capacity()];

        Arrays.fill(put, (byte) 255);
        Arrays.fill(expected, 0, 8, (byte) 255);
        assertThat(block1.put(0, put, 0, put.length), is(8));
        assertThat(block1.get(0, get, 0, get.length), is(get.length));
        assertThat(get, is(expected));

        Arrays.fill(expected, (byte) 255);
        assertThat(block1.put(8, put, 0, put.length), is(8));
        assertThat(block1.get(0, get, 0, get.length), is(get.length));
        assertThat(get, is(expected));

        Arrays.fill(put, (byte) 0);
        Arrays.fill(expected, 0, 4, (byte) 0);
        assertThat(block1.put(0, put, 0, 4), is(4));
        assertThat(block1.get(0, get, 0, get.length), is(get.length));
        assertThat(get, is(expected));

        Arrays.fill(expected, 12, 16, (byte) 0);
        assertThat(block1.put(12, put, 0, 4), is(4));
        assertThat(block1.get(0, get, 0, get.length), is(get.length));
        assertThat(get, is(expected));

        try {
            block1.put(-1, put, 0, put.length);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            block1.put(16, put, 0, put.length);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
        block1.get(0, get, 0, get.length);
        assertThat(get, is(expected));

        put = new byte[24];
        Arrays.fill(expected, 8, 16, (byte) 0);
        assertThat(block1.put(8, put, 0, 8), is(8));
        block1.get(0, get, 0, get.length);
        assertThat(get, is(expected));
        try {
            block1.put(16, put, 0, 9);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
        block1.get(0, get, 0, get.length);
        assertThat(get, is(expected));
    }
}
