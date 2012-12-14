package net.ihiroky.reservoir.accessor;

/**
 * @author Hiroki Itoh
 */
public interface RejectedAllocationHandler {
    boolean handle(AbstractBlockedByteCacheAccessor<?, ?> accessor) throws InterruptedException;

    abstract class AllocateByteBlockManager implements RejectedAllocationHandler {
        @Override
        public boolean handle(AbstractBlockedByteCacheAccessor<?, ?> accessor) throws InterruptedException {
            ByteBlockManager byteBlockManager = createByteBlockManager(accessor);
            accessor.addByteBlockManager(byteBlockManager);
            return true;
        }

        public abstract ByteBlockManager createByteBlockManager(AbstractBlockedByteCacheAccessor<?, ?> accessor);
    }
}
