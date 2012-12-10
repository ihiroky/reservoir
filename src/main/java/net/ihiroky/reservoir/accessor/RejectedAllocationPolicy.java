package net.ihiroky.reservoir.accessor;

/**
* @author Hiroki Itoh
*/
public enum RejectedAllocationPolicy implements RejectedAllocationHandler {
    WAIT_FOR_FREE_BLOCK {
        @Override
        public void handle(AbstractBlockedByteCacheAccessor accessor) throws InterruptedException {
            accessor.freeWaitMutex.wait();
        }
    },
    ABORT {
        @Override
        public void handle(AbstractBlockedByteCacheAccessor accessor) throws InterruptedException {
            throw new IllegalStateException("No free block is found. A ByteBlock allocation is aborted.");
        }
    },
    ;
}
