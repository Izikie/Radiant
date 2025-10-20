package net.minecraft.world.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadedFileIOBase implements Runnable {
    private static final ThreadedFileIOBase THREADED_IO_INSTANCE = new ThreadedFileIOBase();
    private final List<IThreadedFileIO> threadedIOQueue = Collections.synchronizedList(new ArrayList<>());
    private volatile long writeQueuedCounter;
    private volatile long savedIOCounter;
    private volatile boolean isThreadWaiting;

    private ThreadedFileIOBase() {
        Thread thread = new Thread(this, "File IO Thread");
        thread.setPriority(1);
        thread.start();
    }

    public static ThreadedFileIOBase getThreadedIOInstance() {
        return THREADED_IO_INSTANCE;
    }

    @Override
    public void run() {
        while (true) {
            this.processQueue();
        }
    }

    private void processQueue() {
        for (int i = 0; i < this.threadedIOQueue.size(); ++i) {
            IThreadedFileIO ithreadedfileio = this.threadedIOQueue.get(i);
            boolean flag = ithreadedfileio.writeNextIO();

            if (!flag) {
                this.threadedIOQueue.remove(i--);
                ++this.savedIOCounter;
            }

            try {
                Thread.sleep(this.isThreadWaiting ? 0L : 10L);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        if (this.threadedIOQueue.isEmpty()) {
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void queueIO(IThreadedFileIO p_75735_1_) {
        if (!this.threadedIOQueue.contains(p_75735_1_)) {
            ++this.writeQueuedCounter;
            this.threadedIOQueue.add(p_75735_1_);
        }
    }

    public void waitForFinish() throws InterruptedException {
        this.isThreadWaiting = true;

        while (this.writeQueuedCounter != this.savedIOCounter) {
            Thread.sleep(10L);
        }

        this.isThreadWaiting = false;
    }
}
