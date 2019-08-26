/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.otaliastudios.gif.engine;

import android.media.MediaFormat;

import com.otaliastudios.gif.GIFOptions;
import com.otaliastudios.gif.sink.DataSink;
import com.otaliastudios.gif.sink.InvalidOutputFormatException;
import com.otaliastudios.gif.source.DataSource;
import com.otaliastudios.gif.time.TimeInterpolator;
import com.otaliastudios.gif.transcode.Transcoder;
import com.otaliastudios.gif.transcode.VideoTranscoder;
import com.otaliastudios.gif.internal.Logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal engine, do not use this directly.
 */
public class Engine {

    private static final String TAG = Engine.class.getSimpleName();
    private static final Logger LOG = new Logger(TAG);

    private static final long TRANSCODER_SLEEP_TIME = 10;
    private static final long PROGRESS_INTERVAL_STEPS = 10;


    public interface ProgressCallback {

        /**
         * Called to notify progress. Same thread which initiated compress is used.
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onProgress(double progress);
    }

    private DataSink mDataSink;
    private List<DataSource> mDataSources = null;
    private final List<Transcoder> mTranscoders = new ArrayList<>();
    private final List<TimeInterpolator> mInterpolators = new ArrayList<>();
    private int mCurrentStep = 0;
    private MediaFormat mOutputFormat = null;
    private volatile double mProgress;
    private final ProgressCallback mProgressCallback;

    public Engine(@Nullable ProgressCallback progressCallback) {
        mProgressCallback = progressCallback;
    }

    /**
     * Returns the current progress.
     * Note: This method is thread safe.
     * @return the current progress
     */
    @SuppressWarnings("unused")
    public double getProgress() {
        return mProgress;
    }

    private void setProgress(double progress) {
        mProgress = progress;
        if (mProgressCallback != null) {
            mProgressCallback.onProgress(progress);
        }
    }

    private boolean isCompleted() {
        return mCurrentStep == mDataSources.size() - 1
                && mCurrentStep == mTranscoders.size() - 1
                && mTranscoders.get(mCurrentStep).isFinished();
    }

    private void openCurrentStep(@NonNull GIFOptions options) {
        // Notify the data source that we'll be transcoding this track.
        DataSource dataSource = mDataSources.get(mCurrentStep);
        dataSource.start();

        // Create a TimeInterpolator, wrapping the external one.
        TimeInterpolator interpolator = createStepTimeInterpolator(mCurrentStep,
                options.getTimeInterpolator());
        mInterpolators.add(interpolator);

        // Create a Transcoder for this track.
        Transcoder transcoder = new VideoTranscoder(dataSource,
                mDataSink,
                interpolator,
                options.getRotation());
        transcoder.setUp(mOutputFormat);
        mTranscoders.add(transcoder);
    }

    private void closeCurrentStep() {
        mTranscoders.get(mCurrentStep).release();
        mDataSources.get(mCurrentStep).release();
        mCurrentStep = mCurrentStep + 1;
    }

    @NonNull
    private Transcoder getCurrentStepTranscoder(@NonNull GIFOptions options) {
        int current = mCurrentStep;
        int last = mTranscoders.size() - 1;
        if (last == current) {
            // We have already created a transcoder for this step.
            // But this step might be completed and we might need to create a new one.
            Transcoder transcoder = mTranscoders.get(last);
            if (transcoder.isFinished()) {
                closeCurrentStep();
                return getCurrentStepTranscoder(options);
            } else {
                return mTranscoders.get(current);
            }
        } else if (last < current) {
            // We need to create a new step.
            openCurrentStep(options);
            return mTranscoders.get(current);
        } else {
            throw new IllegalStateException("This should never happen. last:" + last + ", current:" + current);
        }
    }

    @NonNull
    private TimeInterpolator createStepTimeInterpolator(int step,
                                                        final @NonNull TimeInterpolator wrap) {
        final long timebase;
        if (step > 0) {
            TimeInterpolator previous = mInterpolators.get(step - 1);
            timebase = previous.interpolate(Long.MAX_VALUE);
        } else {
            timebase = 0;
        }
        return new TimeInterpolator() {

            private long mLastInterpolatedTime;
            private long mFirstInputTime = Long.MAX_VALUE;
            private long mTimeBase = timebase + 10;

            @Override
            public long interpolate(long time) {
                if (time == Long.MAX_VALUE) return mLastInterpolatedTime;
                if (mFirstInputTime == Long.MAX_VALUE) mFirstInputTime = time;
                mLastInterpolatedTime = mTimeBase + (time - mFirstInputTime);
                return wrap.interpolate(mLastInterpolatedTime);
            }
        };
    }

    private long getTotalDurationUs() {
        long totalDurationUs = 0;
        for (int i = 0; i < mDataSources.size(); i++) {
            DataSource source = mDataSources.get(i);
            if (i < mCurrentStep) { // getReadUs() is a better approximation for sure.
                totalDurationUs += source.getReadUs();
            } else {
                totalDurationUs += source.getDurationUs();
            }
        }
        return totalDurationUs;
    }

    private long getTotalReadUs() {
        long completedDurationUs = 0;
        for (int i = 0; i < mDataSources.size(); i++) {
            DataSource source = mDataSources.get(i);
            if (i <= mCurrentStep) {
                completedDurationUs += source.getReadUs();
            }
        }
        return completedDurationUs;
    }

    private double computeProgress() {
        long readUs = getTotalReadUs();
        long totalUs = getTotalDurationUs();
        LOG.v("computeProgress - readUs:" + readUs + ", totalUs:" + totalUs);
        if (totalUs == 0) totalUs = 1; // Avoid NaN
        return (double) readUs / (double) totalUs;
    }

    /**
     * Compresses the GIF. Blocks current thread.
     *
     * @param options GIF options.
     * @throws InvalidOutputFormatException when output format is not supported.
     * @throws InterruptedException when cancel to compress
     */
    public void compress(@NonNull GIFOptions options) throws InterruptedException {
        mDataSink = options.getDataSink();
        mDataSources = options.getDataSources();
        mDataSink.setOrientation(0); // Explicitly set 0 to output - we rotate the textures instead.

        MediaFormat outputFormat = new MediaFormat();
        List<MediaFormat> inputFormats = new ArrayList<>();
        for (DataSource source : options.getDataSources()) {
            MediaFormat inputFormat = source.getTrackFormat();
            inputFormats.add(inputFormat);
        }
        options.getStrategy().createOutputFormat(inputFormats, outputFormat);
        mOutputFormat = outputFormat;
        LOG.v("Duration (us): " + getTotalDurationUs());

        // Do the actual work.
        try {
            long loopCount = 0;
            boolean advanced = false;
            boolean isCompleted = false;
            double progress = 0;
            while (!isCompleted) {
                LOG.v("new loop: " + loopCount);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                advanced = false;

                isCompleted = isCompleted();
                if (!isCompleted) {
                    advanced = getCurrentStepTranscoder(options).transcode(false);
                }
                if (++loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                    progress = computeProgress();
                    LOG.v("progress:" + progress);
                    setProgress(progress);
                }
                if (!advanced) {
                    Thread.sleep(TRANSCODER_SLEEP_TIME);
                }
            }
            mDataSink.stop();
        } finally {
            try {
                closeCurrentStep();
            } catch (Exception ignore) {}
            mDataSink.release();
        }
    }
}
