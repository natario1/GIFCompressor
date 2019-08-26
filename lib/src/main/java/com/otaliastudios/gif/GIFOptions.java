package com.otaliastudios.gif;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.otaliastudios.gif.sink.DataSink;
import com.otaliastudios.gif.sink.DefaultDataSink;
import com.otaliastudios.gif.source.DataSource;
import com.otaliastudios.gif.source.FileDescriptorDataSource;
import com.otaliastudios.gif.source.FilePathDataSource;
import com.otaliastudios.gif.source.UriDataSource;
import com.otaliastudios.gif.strategy.DefaultStrategies;
import com.otaliastudios.gif.strategy.Strategy;
import com.otaliastudios.gif.time.DefaultTimeInterpolator;
import com.otaliastudios.gif.time.SpeedTimeInterpolator;
import com.otaliastudios.gif.time.TimeInterpolator;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Collects compression options consumed by {@link GIFCompressor}.
 */
public class GIFOptions {

    private GIFOptions() {}

    private DataSink dataSink;
    private List<DataSource> dataSources;
    private Strategy strategy;
    private int rotation;
    private TimeInterpolator timeInterpolator;

    GIFListener listener;
    Handler listenerHandler;

    @NonNull
    public DataSink getDataSink() {
        return dataSink;
    }

    @NonNull
    public List<DataSource> getDataSources() {
        return dataSources;
    }

    @NonNull
    public Strategy getStrategy() {
        return strategy;
    }

    public int getRotation() {
        return rotation;
    }

    @NonNull
    public TimeInterpolator getTimeInterpolator() {
        return timeInterpolator;
    }

    public static class Builder {
        private DataSink dataSink;
        private final List<DataSource> dataSources = new ArrayList<>();
        private GIFListener listener;
        private Handler listenerHandler;
        private Strategy strategy;
        private int rotation;
        private TimeInterpolator timeInterpolator;

        Builder(@NonNull String outPath) {
            this.dataSink = new DefaultDataSink(outPath);
        }

        Builder(@NonNull DataSink dataSink) {
            this.dataSink = dataSink;
        }

        @NonNull
        @SuppressWarnings("WeakerAccess")
        public Builder addDataSource(@NonNull DataSource dataSource) {
            dataSources.add(dataSource);
            return this;
        }

        @NonNull
        @SuppressWarnings("unused")
        public Builder addDataSource(@NonNull Context context, @NonNull FileDescriptor fileDescriptor) {
            return addDataSource(new FileDescriptorDataSource(context, fileDescriptor));
        }

        @NonNull
        @SuppressWarnings("unused")
        public Builder addDataSource(@NonNull Context context, @NonNull String inPath) {
            return addDataSource(new FilePathDataSource(context, inPath));
        }

        @NonNull
        @SuppressWarnings({"unused", "UnusedReturnValue"})
        public Builder addDataSource(@NonNull Context context, @NonNull Uri uri) {
            return addDataSource(new UriDataSource(context, uri));
        }

        /**
         * Sets the output strategy. If absent, this defaults to the 16:9
         * strategy returned by {@link DefaultStrategies#for720x1280()}.
         *
         * @param strategy the desired strategy
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("unused")
        public Builder setStrategy(@Nullable Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        @NonNull
        public Builder setListener(@NonNull GIFListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Sets an handler for {@link GIFListener} callbacks.
         * If null, this will default to the thread that starts the transcoding, if it
         * has a looper, or the UI thread otherwise.
         *
         * @param listenerHandler the thread to receive callbacks
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("WeakerAccess")
        public Builder setListenerHandler(@Nullable Handler listenerHandler) {
            this.listenerHandler = listenerHandler;
            return this;
        }

        /**
         * The clockwise rotation to be applied to the input video frames.
         * Defaults to 0, which leaves the input rotation unchanged.
         *
         * @param rotation either 0, 90, 180 or 270
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("unused")
        public Builder setRotation(int rotation) {
            this.rotation = rotation;
            return this;
        }

        /**
         * Sets a {@link TimeInterpolator} to change the frames timestamps - either video or
         * audio or both - before they are written into the output file.
         * Defaults to {@link com.otaliastudios.gif.time.DefaultTimeInterpolator}.
         *
         * @param timeInterpolator a time interpolator
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("WeakerAccess")
        public Builder setTimeInterpolator(@NonNull TimeInterpolator timeInterpolator) {
            this.timeInterpolator = timeInterpolator;
            return this;
        }

        /**
         * Shorthand for calling {@link #setTimeInterpolator(TimeInterpolator)}
         * and passing a {@link com.otaliastudios.gif.time.SpeedTimeInterpolator}.
         * This interpolator can modify the video speed by the given factor.
         *
         * @param speedFactor a factor, greather than 0
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("unused")
        public Builder setSpeed(float speedFactor) {
            return setTimeInterpolator(new SpeedTimeInterpolator(speedFactor));
        }

        @NonNull
        public GIFOptions build() {
            if (listener == null) {
                throw new IllegalStateException("listener can't be null");
            }
            if (dataSources.isEmpty()) {
                throw new IllegalStateException("we need at least one data source");
            }
            if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
                throw new IllegalArgumentException("Accepted values for rotation are 0, 90, 180, 270");
            }
            if (listenerHandler == null) {
                Looper looper = Looper.myLooper();
                if (looper == null) looper = Looper.getMainLooper();
                listenerHandler = new Handler(looper);
            }
            if (strategy == null) {
                strategy = DefaultStrategies.for720x1280();
            }
            if (timeInterpolator == null) {
                timeInterpolator = new DefaultTimeInterpolator();
            }
            GIFOptions options = new GIFOptions();
            options.listener = listener;
            options.dataSources = dataSources;
            options.dataSink = dataSink;
            options.listenerHandler = listenerHandler;
            options.strategy = strategy;
            options.rotation = rotation;
            options.timeInterpolator = timeInterpolator;
            return options;
        }

        @NonNull
        public Future<Void> compress() {
            return GIFCompressor.getInstance().compress(build());
        }
    }
}
