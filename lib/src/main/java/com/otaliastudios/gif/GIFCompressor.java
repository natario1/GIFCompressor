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
package com.otaliastudios.gif;

import android.os.Handler;

import com.otaliastudios.gif.engine.Engine;
import com.otaliastudios.gif.sink.DataSink;
import com.otaliastudios.gif.internal.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;

public class GIFCompressor {
    private static final String TAG = GIFCompressor.class.getSimpleName();
    private static final Logger LOG = new Logger(TAG);

    private static volatile GIFCompressor sGIFCompressor;

    private class Factory implements ThreadFactory {
        private AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable, TAG + " Thread #" + count.getAndIncrement());
        }
    }

    private ThreadPoolExecutor mExecutor;

    private GIFCompressor() {
        // This executor will execute at most 'pool' tasks concurrently,
        // then queue all the others. CPU + 1 is used by AsyncTask.
        int pool = Runtime.getRuntime().availableProcessors() + 1;
        mExecutor = new ThreadPoolExecutor(pool, pool,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new Factory());
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static GIFCompressor getInstance() {
        if (sGIFCompressor == null) {
            synchronized (GIFCompressor.class) {
                if (sGIFCompressor == null) {
                    sGIFCompressor = new GIFCompressor();
                }
            }
        }
        return sGIFCompressor;
    }

    /**
     * Starts building compression options.
     * Requires a non null absolute path to the output file.
     *
     * @param outPath path to output file
     * @return an options builder
     */
    @NonNull
    public static GIFOptions.Builder into(@NonNull String outPath) {
        return new GIFOptions.Builder(outPath);
    }

    /**
     * Starts building compression options.
     * Requires a non null sink.
     *
     * @param dataSink the output sink
     * @return an options builder
     */
    @NonNull
    public static GIFOptions.Builder into(@NonNull DataSink dataSink) {
        return new GIFOptions.Builder(dataSink);
    }

    /**
     * Compresses GIF file asynchronously.
     *
     * @param options The compression options.
     * @return a Future that completes when compression is completed
     */
    @NonNull
    public Future<Void> compress(@NonNull final GIFOptions options) {
        final GIFListener listenerWrapper = new ListenerWrapper(options.listenerHandler,
                options.listener);
        return mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    Engine engine = new Engine(new Engine.ProgressCallback() {
                        @Override
                        public void onProgress(final double progress) {
                            listenerWrapper.onGIFCompressionProgress(progress);
                        }
                    });
                    engine.compress(options);
                    listenerWrapper.onGIFCompressionCompleted();

                } catch (Throwable e) {
                    // Check InterruptedException in e and in its causes.
                    Throwable current = e;
                    boolean isInterrupted = e instanceof InterruptedException;
                    while (!isInterrupted && current.getCause() != null && !current.getCause().equals(current)) {
                        current = current.getCause();
                        if (current instanceof InterruptedException) isInterrupted = true;
                    }
                    if (isInterrupted) {
                        LOG.i("Compression canceled.", current);
                        listenerWrapper.onGIFCompressionCanceled();

                    } else if (e instanceof RuntimeException) {
                        LOG.e("Fatal error while compressing, this might be invalid format or bug in engine or Android.", e);
                        listenerWrapper.onGIFCompressionFailed(e);
                        throw e;

                    } else {
                        LOG.e("Unexpected error while compressing", e);
                        listenerWrapper.onGIFCompressionFailed(e);
                        throw e;
                    }
                }
                return null;
            }
        });
    }

    /**
     * Wraps a GIFListener and posts events on the given handler.
     */
    private static class ListenerWrapper implements GIFListener {

        private Handler mHandler;
        private GIFListener mListener;

        private ListenerWrapper(@NonNull Handler handler, @NonNull GIFListener listener) {
            mHandler = handler;
            mListener = listener;
        }

        @Override
        public void onGIFCompressionCanceled() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onGIFCompressionCanceled();
                }
            });
        }

        @Override
        public void onGIFCompressionCompleted() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onGIFCompressionCompleted();
                }
            });
        }

        @Override
        public void onGIFCompressionFailed(@NonNull final Throwable exception) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onGIFCompressionFailed(exception);
                }
            });
        }

        @Override
        public void onGIFCompressionProgress(final double progress) {
            // Don't think there's a safe way to avoid this allocation?
            // Other than creating a pool of runnables.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onGIFCompressionProgress(progress);
                }
            });
        }
    }
}
