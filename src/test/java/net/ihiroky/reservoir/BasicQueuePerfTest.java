package net.ihiroky.reservoir;

import net.ihiroky.reservoir.storage.ByteBufferStorageAccessor;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class BasicQueuePerfTest {

    @Test
    public void testMultiThread() throws Exception {
        final BasicQueue<Integer> queue = Reservoir.newQueueBuilder()
                .name("BasicQueueTest#testMultiThread")
                .cacheAccessorType(Reservoir.CacheAccessorType.BYTE_BUFFER)
                .property(ByteBufferStorageAccessor.class, "direct", "true")
                .property(ByteBufferStorageAccessor.class, "blockSize", "8")
                .property(ByteBufferStorageAccessor.class, "partitions", "2")
                .property(ByteBufferStorageAccessor.class, "size", "8192")
                .property(ByteBufferStorageAccessor.class, "coder", this.getClass().getName() + "$IntegerCoder")
                .build();
        final int END = 100000;
        Runnable put = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < END; i++) {
                    queue.offer(i);
                }
                queue.offer(END);
            }
        };
        Runnable get = new Runnable() {
            int expected = 0;

            @Override
            public void run() {
                Integer i = queue.poll();
                if (i == null) {
                    return;
                }
                if (i == END) {
                    return;
                }
                assertThat(i, is(expected));
                expected++;
            }
        };
        ConcurrentTestUtil.Result<?> result =
                ConcurrentTestUtil.runRunnable(Arrays.asList(put, get), TimeUnit.MILLISECONDS);
        System.out.println("exception:" + result.getUncaughtExceptionMap());
        System.out.println("elapsed:" + result.getElapsed() + ", speed:" + (END / result.getElapsed() * 1000));
    }

    static class IntegerCoder implements Coder<Integer> {

        @Override
        public void init(Properties props) {
        }

        @Override
        public Encoder<Integer> createEncoder() {
            return new Encoder<Integer>() {
                @Override
                public ByteBuffer encode(Integer value) {
                    ByteBuffer buffer = ByteBuffer.allocate(4);
                    buffer.putInt(value);
                    return buffer;
                }
            };
        }

        @Override
        public Decoder<Integer> createDecoder() {
            return new Decoder<Integer>() {
                @Override
                public Integer decode(ByteBuffer byteBuffer) {
                    return byteBuffer.getInt();
                }
            };
        }
    }

}
