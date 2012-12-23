package net.ihiroky.reservoir.storage;

/**
* @author Hiroki Itoh
*/
public enum RejectedAllocationPolicy implements RejectedAllocationHandler {
    WAIT_FOR_FREE_BLOCK {
        @Override
        public boolean handle(AbstractBlockedByteStorageAccessor accessor) throws InterruptedException {
            accessor.waitOnFreeWaitMutex();
            return true;
        }
    },
    ABORT {
        @Override
        public boolean handle(AbstractBlockedByteStorageAccessor accessor) throws InterruptedException {
            return false;
        }
    },
    ;
}
