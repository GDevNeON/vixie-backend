package com.neong.vixie.services.wallet;

/**
 * Thrown when a user has exceeded their daily ad coin reward cap.
 */
public class AdCapExceededException extends RuntimeException {

    private final int currentCount;
    private final int maxCount;

    public AdCapExceededException(int currentCount, int maxCount) {
        super("Daily ad reward cap exceeded: " + currentCount + "/" + maxCount);
        this.currentCount = currentCount;
        this.maxCount = maxCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxCount() {
        return maxCount;
    }
}
