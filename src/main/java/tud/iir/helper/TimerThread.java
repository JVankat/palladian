package tud.iir.helper;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import tud.iir.news.FeedReader;
import tud.iir.news.FeedProcessingAction;

/**
 * The {@link FeedReader} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time
 * the feed is checked and also performs all
 * set {@link FeedProcessingAction}s.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @see FeedReader
 * 
 */
public abstract class TimerThread {

    protected static final Logger LOGGER = Logger.getLogger(TimerThread.class);
    private Timer timer;
    private long timeout = -1l;

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public TimerThread() {
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (getTimeout() > 0l) {
                    try {

                        // for (int i = 0; i < getTimeout() / 1000; i++) {
                        // if (isActive()) {
                        // Thread.sleep(1000);
                        // } else {
                        // break;
                        // }
                        // }
                        LOGGER.debug("start sleeping");
                        Thread.sleep(getTimeout());
                        LOGGER.debug("woke up after " + DateHelper.getTimeString(getTimeout()));

                    } catch (InterruptedException e) {
                        LOGGER.debug("connection timeout has been interrupted, " + e.getMessage());
                    }

                    stopThread();
                }

            }
        }, 0);
    }

    protected abstract void stopThread();

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}