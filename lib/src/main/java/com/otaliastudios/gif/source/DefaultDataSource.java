package com.otaliastudios.gif.source;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;

import com.otaliastudios.gif.internal.Logger;

import java.io.IOException;

/**
 * A DataSource implementation that uses Android's Media APIs.
 */
public abstract class DefaultDataSource implements DataSource {

    private final static String TAG = DefaultDataSource.class.getSimpleName();
    private final static Logger LOG = new Logger(TAG);

    private final MediaMetadataRetriever mMetadata = new MediaMetadataRetriever();
    private final MediaExtractor mExtractor = new MediaExtractor();
    private boolean mMetadataApplied;
    private boolean mExtractorApplied;
    private MediaFormat mFormat;
    private int mIndex;
    private long mLastTimestampUs;
    private long mFirstTimestampUs = Long.MIN_VALUE;

    private void ensureMetadata() {
        if (!mMetadataApplied) {
            mMetadataApplied = true;
            applyRetriever(mMetadata);
        }
    }

    private void ensureExtractor() {
        if (!mExtractorApplied) {
            mExtractorApplied = true;
            try {
                applyExtractor(mExtractor);
            } catch (IOException e) {
                LOG.e("Got IOException while trying to open MediaExtractor.", e);
                throw new RuntimeException(e);
            }
        }
    }

    protected abstract void applyExtractor(@NonNull MediaExtractor extractor) throws IOException;

    protected abstract void applyRetriever(@NonNull MediaMetadataRetriever retriever);

    @Override
    public void start() {
        mExtractor.selectTrack(mIndex);
    }

    @Override
    public boolean isDrained() {
        ensureExtractor();
        return mExtractor.getSampleTrackIndex() < 0;
    }

    @Override
    public void readTrack(@NonNull Chunk chunk) {
        ensureExtractor();
        chunk.bytes = mExtractor.readSampleData(chunk.buffer, 0);
        chunk.isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        chunk.timestampUs = mExtractor.getSampleTime();
        mLastTimestampUs = chunk.timestampUs;
        if (mFirstTimestampUs == Long.MIN_VALUE) {
            mFirstTimestampUs = mLastTimestampUs;
        }
        mExtractor.advance();
    }

    @Override
    public long getReadUs() {
        if (mFirstTimestampUs == Long.MIN_VALUE) {
            return 0;
        }
        return mLastTimestampUs - mFirstTimestampUs;
    }

    @Override
    public int getOrientation() {
        ensureMetadata();
        String string = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ignore) {
            return 0;
        }
    }

    @Override
    public long getDurationUs() {
        ensureMetadata();
        try {
            return Long.parseLong(mMetadata
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @NonNull
    @Override
    public MediaFormat getTrackFormat() {
        if (mFormat != null) return mFormat;
        ensureExtractor();
        int trackCount = mExtractor.getTrackCount();
        MediaFormat format = null;
        for (int i = 0; i < trackCount; i++) {
            format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                mIndex = i;
                break;
            }
        }
        mFormat = format;
        return format;
    }

    @Override
    public void release() {
        try {
            mExtractor.release();
        } catch (Exception e) {
            LOG.w("Could not release extractor:", e);
        }
    }
}
