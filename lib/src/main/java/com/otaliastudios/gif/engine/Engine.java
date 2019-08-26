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
import com.otaliastudios.gif.internal.TrackTypeMap;
import com.otaliastudios.gif.sink.DataSink;
import com.otaliastudios.gif.sink.InvalidOutputFormatException;
import com.otaliastudios.gif.source.DataSource;
import com.otaliastudios.gif.strategy.TrackStrategy;
import com.otaliastudios.gif.time.TimeInterpolator;
import com.otaliastudios.gif.transcode.NoOpTrackTranscoder;
import com.otaliastudios.gif.transcode.PassThroughTrackTranscoder;
import com.otaliastudios.gif.transcode.TrackTranscoder;
import com.otaliastudios.gif.transcode.VideoTrackTranscoder;
import com.otaliastudios.gif.internal.Logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Internal engine, do not use this directly.
 */
public class Engine {

    private static final String TAG = Engine.class.getSimpleName();
    private static final Logger LOG = new Logger(TAG);

    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;
    private static final long PROGRESS_INTERVAL_STEPS = 10;


    public interface ProgressCallback {

        /**
         * Called to notify progress. Same thread which initiated compress is used.
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onProgress(double progress);
    }

    private DataSink mDataSink;
    private final TrackTypeMap<List<DataSource>> mDataSources = new TrackTypeMap<>();
    private final TrackTypeMap<ArrayList<TrackTranscoder>> mTranscoders = new TrackTypeMap<>(new ArrayList<TrackTranscoder>());
    private final TrackTypeMap<ArrayList<TimeInterpolator>> mInterpolators = new TrackTypeMap<>(new ArrayList<TimeInterpolator>());
    private final TrackTypeMap<Integer> mCurrentStep = new TrackTypeMap<>(0);
    private final TrackTypeMap<TrackStatus> mStatuses = new TrackTypeMap<>();
    private final TrackTypeMap<MediaFormat> mOutputFormats = new TrackTypeMap<>();
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

    private boolean hasVideoSources() {
        return !mDataSources.requireVideo().isEmpty();
    }

    private Set<DataSource> getUniqueSources() {
        Set<DataSource> sources = new HashSet<>();
        sources.addAll(mDataSources.requireVideo());
        return sources;
    }

    private void computeTrackStatus(@NonNull TrackStrategy strategy,
                                    @NonNull List<DataSource> sources) {
        TrackStatus status = TrackStatus.ABSENT;
        MediaFormat outputFormat = new MediaFormat();
        if (!sources.isEmpty()) {
            List<MediaFormat> inputFormats = new ArrayList<>();
            for (DataSource source : sources) {
                MediaFormat inputFormat = source.getTrackFormat();
                inputFormats.add(inputFormat);
            }
            status = strategy.createOutputFormat(inputFormats, outputFormat);
        }
        mOutputFormats.setVideo(outputFormat);
        mDataSink.setTrackStatus(status);
        mStatuses.setVideo(status);
    }

    private boolean isCompleted() {
        if (mDataSources.requireVideo().isEmpty()) return true;
        int current = mCurrentStep.requireVideo();
        return current == mDataSources.requireVideo().size() - 1
                && current == mTranscoders.requireVideo().size() - 1
                && mTranscoders.requireVideo().get(current).isFinished();
    }

    private void openCurrentStep(@NonNull GIFOptions options) {
        int current = mCurrentStep.requireVideo();
        TrackStatus status = mStatuses.requireVideo();

        // Notify the data source that we'll be transcoding this track.
        DataSource dataSource = mDataSources.requireVideo().get(current);
        if (status.isTranscoding()) {
            dataSource.start();
        }

        // Create a TimeInterpolator, wrapping the external one.
        TimeInterpolator interpolator = createStepTimeInterpolator(current,
                options.getTimeInterpolator());
        mInterpolators.requireVideo().add(interpolator);

        // Create a Transcoder for this track.
        TrackTranscoder transcoder;
        switch (status) {
            case PASS_THROUGH: {
                transcoder = new PassThroughTrackTranscoder(dataSource,
                        mDataSink, interpolator);
                break;
            }
            case COMPRESSING: {
                transcoder = new VideoTrackTranscoder(dataSource, mDataSink,
                        interpolator,
                        options.getVideoRotation());
                break;
            }
            case ABSENT:
            case REMOVING:
            default: {
                transcoder = new NoOpTrackTranscoder();
                break;
            }
        }
        transcoder.setUp(mOutputFormats.requireVideo());
        mTranscoders.requireVideo().add(transcoder);
    }

    private void closeCurrentStep() {
        int current = mCurrentStep.requireVideo();
        TrackTranscoder transcoder = mTranscoders.requireVideo().get(current);
        DataSource dataSource = mDataSources.requireVideo().get(current);
        transcoder.release();
        dataSource.release();
        mCurrentStep.setVideo(current + 1);
    }

    @NonNull
    private TrackTranscoder getCurrentTrackTranscoder(@NonNull GIFOptions options) {
        int current = mCurrentStep.requireVideo();
        int last = mTranscoders.requireVideo().size() - 1;
        if (last == current) {
            // We have already created a transcoder for this step.
            // But this step might be completed and we might need to create a new one.
            TrackTranscoder transcoder = mTranscoders.requireVideo().get(last);
            if (transcoder.isFinished()) {
                closeCurrentStep();
                return getCurrentTrackTranscoder(options);
            } else {
                return mTranscoders.requireVideo().get(current);
            }
        } else if (last < current) {
            // We need to create a new step.
            openCurrentStep(options);
            return mTranscoders.requireVideo().get(current);
        } else {
            throw new IllegalStateException("This should never happen. last:" + last + ", current:" + current);
        }
    }

    @NonNull
    private TimeInterpolator createStepTimeInterpolator(int step,
                                                        final @NonNull TimeInterpolator wrap) {
        final long timebase;
        if (step > 0) {
            TimeInterpolator previous = mInterpolators.requireVideo().get(step - 1);
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
        if (!mStatuses.requireVideo().isTranscoding()) return 0L;
        int current = mCurrentStep.requireVideo();
        long totalDurationUs = 0;
        for (int i = 0; i < mDataSources.requireVideo().size(); i++) {
            DataSource source = mDataSources.requireVideo().get(i);
            if (i < current) { // getReadUs() is a better approximation for sure.
                totalDurationUs += source.getReadUs();
            } else {
                totalDurationUs += source.getDurationUs();
            }
        }
        return totalDurationUs;
    }

    private long getTrackReadUs() {
        if (!mStatuses.requireVideo().isTranscoding()) return 0L;
        int current = mCurrentStep.requireVideo();
        long completedDurationUs = 0;
        for (int i = 0; i < mDataSources.requireVideo().size(); i++) {
            DataSource source = mDataSources.requireVideo().get(i);
            if (i <= current) {
                completedDurationUs += source.getReadUs();
            }
        }
        return completedDurationUs;
    }

    private double getTrackProgress() {
        if (!mStatuses.requireVideo().isTranscoding()) return 0.0D;
        long readUs = getTrackReadUs();
        long totalUs = getTotalDurationUs();
        LOG.v("getTrackProgress - readUs:" + readUs + ", totalUs:" + totalUs);
        if (totalUs == 0) totalUs = 1; // Avoid NaN
        return (double) readUs / (double) totalUs;
    }

    /**
     * Performs transcoding. Blocks current thread.
     *
     * @param options Transcoding options.
     * @throws InvalidOutputFormatException when output format is not supported.
     * @throws InterruptedException when cancel to compress
     */
    public void transcode(@NonNull GIFOptions options) throws InterruptedException {
        mDataSink = options.getDataSink();
        mDataSources.setVideo(options.getVideoDataSources());
        mDataSink.setOrientation(0); // Explicitly set 0 to output - we rotate the textures instead.

        // Compute the TrackStatus.
        int activeTracks = 0;
        computeTrackStatus(options.getVideoTrackStrategy(), options.getVideoDataSources());
        TrackStatus videoStatus = mStatuses.requireVideo();
        if (videoStatus.isTranscoding()) activeTracks++;
        LOG.v("Duration (us): " + getTotalDurationUs());

        // Do the actual transcoding work.
        try {
            long loopCount = 0;
            boolean stepped = false;
            boolean videoCompleted = false;
            boolean forceVideoEos = false;
            double videoProgress = 0;
            while (!videoCompleted) {
                LOG.v("new step: " + loopCount);

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                stepped = false;

                // First, check if we have to force an input end of stream for some track.
                // This can happen, for example, if user adds 1 minute (video only) with 20 seconds
                // of audio. The video track must be stopped once the audio stops.
                long totalUs = getTotalDurationUs() + 100 /* tolerance */;
                forceVideoEos = getTrackReadUs() > totalUs;

                // Now step for transcoders that are not completed.
                videoCompleted = isCompleted();
                if (!videoCompleted) {
                    stepped |= getCurrentTrackTranscoder(options).transcode(forceVideoEos);
                }
                if (++loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                    videoProgress = getTrackProgress();
                    LOG.v("progress - video:" + videoProgress);
                    setProgress((videoProgress) / activeTracks);
                }
                if (!stepped) {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
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
