package com.otaliastudios.gif.time;

import com.otaliastudios.gif.internal.Logger;


/**
 * A {@link TimeInterpolator} that modifies the playback speed by the given
 * float factor. A factor less than 1 will slow down, while a bigger factor will
 * accelerate.
 */
public class SpeedTimeInterpolator implements TimeInterpolator {

    private final static String TAG = SpeedTimeInterpolator.class.getSimpleName();
    private final static Logger LOG = new Logger(TAG);

    private double mFactor;
    private long mLastRealTime = Long.MIN_VALUE;
    private long mLastCorrectedTime = Long.MIN_VALUE;

    /**
     * Creates a new speed interpolator for the given factor.
     * Throws if factor is less than 0 or equal to 0.
     * @param factor a factor
     */
    public SpeedTimeInterpolator(float factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Invalid speed factor: " + factor);
        }
        mFactor = factor;
    }

    /**
     * Returns the factor passed to the constructor.
     * @return the factor
     */
    @SuppressWarnings("unused")
    public float getFactor() {
        return (float) mFactor;
    }

    @Override
    public long interpolate(long time) {
        if (mLastRealTime == Long.MIN_VALUE) {
            mLastRealTime = time;
            mLastCorrectedTime = time;
        } else {
            long realDelta = time - mLastRealTime;
            long correctedDelta = (long) ((double) realDelta / mFactor);
            mLastRealTime = time;
            mLastCorrectedTime += correctedDelta;
        }
        LOG.i("inputTime:" + time + " outputTime:" + mLastCorrectedTime);
        return mLastCorrectedTime;
    }
}
