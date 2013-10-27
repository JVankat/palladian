package ws.palladian.helper;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * The ProgressMonitor eases the progress visualization needed in many long-running processes. Usage example:
 * 
 * <pre>
 * ProgressMonitor pm = new ProgressMonitor();
 * for (int i = 0; i &lt; 10; i++) {
 *     performSophisticatedCalculations(i);
 *     pm.incrementAndPrintProgress();
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProgressMonitor {

    private final static char PROGRESS_CHAR = '■';
    private final StopWatch stopWatch = new StopWatch();
    private final String processName;
    private long currentCount = 0;
    private final long totalCount;
    private final double showEveryPercent;
    private boolean compactRemaining = false;

    /**
     * <p>
     * Create a new {@link ProgressMonitor} showing the current progress with each percent.
     * </p>
     * 
     * @param totalCount The total iterations to perform.
     */
    public ProgressMonitor(int totalCount) {
        this(totalCount, 1);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     * 
     * @param totalCount The total iterations to perform.
     * @param showEveryPercent Step size for outputting the progress.
     */
    public ProgressMonitor(int totalCount, double showEveryPercent) {
        this(totalCount, showEveryPercent, null);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     * 
     * @param totalCount The total iterations to perform.
     * @param showEveryPercent Step size for outputting the progress.
     * @param processName The name of the process, for identification purposes when outputting the bar.
     */
    public ProgressMonitor(int totalCount, double showEveryPercent, String processName) {
        this.totalCount = totalCount;
        this.showEveryPercent = showEveryPercent;
        this.processName = processName;
    }

    private String createProgressBar(double percent) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        int scaledPercent = (int)Math.round(percent / 2);
        stringBuilder.append(StringUtils.repeat(PROGRESS_CHAR, scaledPercent));
        stringBuilder.append(StringUtils.repeat(' ', Math.max(50 - scaledPercent, 0)));
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    /**
     * <p>
     * Prints the current progress to the System's standard output.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    public void printProgress(long counter) {
        String progress = getProgress(counter);
        if (!progress.isEmpty()) {
            System.out.println(progress);
        }
    }

    /**
     * <p>
     * Increments the counter by one and prints the current progress to the System's standard output.
     * </p>
     * 
     */
    public void incrementAndPrintProgress() {
        currentCount++;
        printProgress(currentCount);
    }
    
    /**
     * <p>
     * Increments the counter by the step size and prints the current progress to the System's standard output.
     * </p>
     * 
     * @param steps The number of steps to increment the counter with.
     */
    public void incrementByAndPrintProgress(int steps) {
        for (int i = 0; i < steps; i++) {
            incrementAndPrintProgress();
        }
    }

    /**
     * <p>
     * Increments the counter by one and gets the current progress.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    public String incrementAndGetProgress() {
        currentCount++;
        return getProgress(currentCount);
    }

    /**
     * <p>
     * Returns the current progress.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    public String getProgress(long counter) {
        StringBuilder progressString = new StringBuilder();
        try {
            if (showEveryPercent == 0 || counter % (showEveryPercent * totalCount / 100.0) < 1) {
                if (processName != null) {
                    progressString.append(processName).append(" ");
                }
                double percent = MathHelper.round(100 * counter / (double)totalCount, 2);
                progressString.append(createProgressBar(percent));
                progressString.append(" ").append(percent).append("% (");
                progressString.append(totalCount - counter).append(" remaining");
                if (stopWatch != null && percent > 0) {
                    long msRemaining = (long)((100 - percent) * stopWatch.getTotalElapsedTime() / percent);
                    // if elapsed not possible (timer started long before progress helper used) =>
                    // long msRemaining = (long)((100 - percent) * stopWatch.getElapsedTime() / 10); => in case total
                    progressString.append(", elapsed: ").append(stopWatch.getTotalElapsedTimeString());
                    progressString.append(", iteration: ").append(stopWatch.getElapsedTimeString());
                    if (counter < totalCount) {
                        progressString.append(", ~remaining: ").append(
                                DateHelper.formatDuration(0, msRemaining, compactRemaining));
                    }
                    stopWatch.start();
                }
                progressString.append(")");
            }
        } catch (ArithmeticException e) {
        } catch (Exception e) {
        }

        return progressString.toString();
    }

    public boolean isCompactRemaining() {
        return compactRemaining;
    }

    /**
     * <p>
     * Sets whether the remaining time should be shown in compact format.
     * </p>
     * 
     * @param compactRemaining True if the remaining time should be shown in compact format.
     */
    public void setCompactRemaining(boolean compactRemaining) {
        this.compactRemaining = compactRemaining;
    }

    public String getTotalElapsedTimeString() {
        return stopWatch.getTotalElapsedTimeString();
    }

    public static void main(String[] args) {
        int totalCount = 1759600335;
        ProgressMonitor pm = new ProgressMonitor(totalCount, .5, "My Progress");
        pm.setCompactRemaining(true);
        for (int i = 1; i <= totalCount; i++) {
            // ThreadHelper.deepSleep(200);
            pm.incrementAndPrintProgress();
        }
    }
    
}
