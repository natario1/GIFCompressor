package com.otaliastudios.gif.transcode.internal;

import androidx.annotation.NonNull;

import com.otaliastudios.gif.internal.Logger;

/**
 * Drops input frames to respect the output frame rate.
 */
public abstract class VideoFrameDropper {

    private final static String TAG = VideoFrameDropper.class.getSimpleName();
    private final static Logger LOG = new Logger(TAG);

    private VideoFrameDropper() {}

    public abstract boolean shouldRenderFrame(long presentationTimeUs);

    @NonNull
    public static VideoFrameDropper newDropper(int inputFrameRate, int outputFrameRate) {
        return new Dropper1(inputFrameRate, outputFrameRate);
    }

    /**
     * A simple and more elegant dropper.
     * Reference: https://stackoverflow.com/questions/4223766/dropping-video-frames
     */
    private static class Dropper1 extends VideoFrameDropper {

        private double mInFrameRateReciprocal;
        private double mOutFrameRateReciprocal;
        private double mFrameRateReciprocalSum;
        private int mFrameCount;

        private Dropper1(int inputFrameRate, int outputFrameRate) {
            mInFrameRateReciprocal = 1.0d / inputFrameRate;
            mOutFrameRateReciprocal = 1.0d / outputFrameRate;
            LOG.i("inFrameRateReciprocal:" + mInFrameRateReciprocal + " outFrameRateReciprocal:" + mOutFrameRateReciprocal);
        }

        @Override
        public boolean shouldRenderFrame(long presentationTimeUs) {
            mFrameRateReciprocalSum += mInFrameRateReciprocal;
            if (mFrameCount++ == 0) {
                LOG.v("RENDERING (first frame) - frameRateReciprocalSum:" + mFrameRateReciprocalSum);
                return true;
            } else if (mFrameRateReciprocalSum > mOutFrameRateReciprocal) {
                mFrameRateReciprocalSum -= mOutFrameRateReciprocal;
                LOG.v("RENDERING - frameRateReciprocalSum:" + mFrameRateReciprocalSum);
                return true;
            } else {
                LOG.v("DROPPING - frameRateReciprocalSum:" + mFrameRateReciprocalSum);
                return false;
            }
        }
    }
}
