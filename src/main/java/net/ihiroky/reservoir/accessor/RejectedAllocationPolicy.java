package net.ihiroky.reservoir.accessor;

/**
* @author Hiroki Itoh
*/
public enum RejectedAllocationPolicy implements RejectedAllocationHandler {
    WAIT_FOR_FREE_BLOCK {
        @Override
        public boolean handle(AbstractBlockedByteCacheAccessor accessor) throws InterruptedException {
            accessor.freeWaitMutex.wait();
            return true;
        }
    },
    ABORT {
        @Override
        public boolean handle(AbstractBlockedByteCacheAccessor accessor) throws InterruptedException {
            return false;
        }
    },
    ;
}
