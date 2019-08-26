package com.otaliastudios.gif.sink;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import androidx.annotation.NonNull;

import com.otaliastudios.gif.internal.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


/**
 * A {@link DataSink} implementation that:
 *
 * - Uses {@link MediaMuxer} to collect data
 * - Creates an output file with the readable media
 */
public class DefaultDataSink implements DataSink {

    /**
     * A queued sample is a sample that we haven't written yet because
     * the muxer is still being started (waiting for output formats).
     */
    private static class QueuedSample {
        private final int mSize;
        private final long mTimeUs;
        private final int mFlags;

        private QueuedSample(@NonNull MediaCodec.BufferInfo bufferInfo) {
            mSize = bufferInfo.size;
            mTimeUs = bufferInfo.presentationTimeUs;
            mFlags = bufferInfo.flags;
        }
    }

    private final static String TAG = DefaultDataSink.class.getSimpleName();
    private final static Logger LOG = new Logger(TAG);

    // I have no idea whether this value is appropriate or not...
    private final static int BUFFER_SIZE = 64 * 1024;

    private boolean mMuxerStarted = false;
    private final MediaMuxer mMuxer;
    private final List<QueuedSample> mQueue = new ArrayList<>();
    private ByteBuffer mQueueBuffer;
    private int mMuxerIndex;
    private final DefaultDataSinkChecks mMuxerChecks = new DefaultDataSinkChecks();

    public DefaultDataSink(@NonNull String outputFilePath) {
        this(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    @SuppressWarnings("WeakerAccess")
    public DefaultDataSink(@NonNull String outputFilePath, int format) {
        try {
            mMuxer = new MediaMuxer(outputFilePath, format);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setOrientation(int rotation) {
        mMuxer.setOrientationHint(rotation);
    }

    @Override
    public void setFormat(@NonNull MediaFormat format) {
        mMuxerChecks.checkOutputFormat(format);
        if (mMuxerStarted) return;

        mMuxerIndex = mMuxer.addTrack(format);
        LOG.v("Added track #" + mMuxerIndex + " with " + format.getString(MediaFormat.KEY_MIME) + " to muxer");
        mMuxer.start();
        mMuxerStarted = true;
        drainQueue();
    }

    @Override
    public void write(@NonNull ByteBuffer byteBuffer, @NonNull MediaCodec.BufferInfo bufferInfo) {
        if (mMuxerStarted) {
            mMuxer.writeSampleData(mMuxerIndex, byteBuffer, bufferInfo);
        } else {
            enqueue(byteBuffer, bufferInfo);
        }
    }

    /**
     * Enqueues the given byffer by writing it into our own buffer and
     * just storing its position and size.
     *
     * @param buffer input buffer
     * @param bufferInfo input buffer info
     */
    private void enqueue(@NonNull ByteBuffer buffer,
                         @NonNull MediaCodec.BufferInfo bufferInfo) {
        if (mQueueBuffer == null) {
            mQueueBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder());
        }
        buffer.limit(bufferInfo.offset + bufferInfo.size);
        buffer.position(bufferInfo.offset);
        mQueueBuffer.put(buffer);
        mQueue.add(new QueuedSample(bufferInfo));
    }

    /**
     * Writes all enqueued samples into the muxer, now that it is
     * open and running.
     */
    private void drainQueue() {
        if (mQueue.isEmpty()) return;
        mQueueBuffer.flip();
        LOG.i("Output format determined, writing pending data into the muxer. "
                + "samples:" + mQueue.size() + " "
                + "bytes:" + mQueueBuffer.limit());
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int offset = 0;
        for (QueuedSample sample : mQueue) {
            bufferInfo.set(offset, sample.mSize, sample.mTimeUs, sample.mFlags);
            write(mQueueBuffer, bufferInfo);
            offset += sample.mSize;
        }
        mQueue.clear();
        mQueueBuffer = null;
    }

    @Override
    public void stop() {
        mMuxer.stop(); // If this fails, let's throw.
    }

    @Override
    public void release() {
        try {
            mMuxer.release();
        } catch (Exception e) {
            LOG.w("Failed to release the muxer.", e);
        }
    }
}
