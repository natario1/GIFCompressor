package com.otaliastudios.gif.source;

import android.media.MediaFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * Represents the source of input data.
 */
public interface DataSource {

    /**
     * Metadata information. Returns the video orientation, or 0.
     *
     * @return video metadata orientation
     */
    int getOrientation();

    /**
     * Returns the video total duration in microseconds.
     *
     * @return duration in us
     */
    long getDurationUs();

    /**
     * Returns a MediaFormat-like format for this GIF.
     *
     * @return format
     */
    @NonNull
    MediaFormat getTrackFormat();

    /**
     * Called to notify that we'll start reading.
     */
    void start();

    /**
     * Called to read contents for the current track type.
     * Contents should be put inside {@link DataSource.Chunk#buffer}, and the
     * other chunk flags should be filled.
     *
     * @param chunk output chunk
     */
    void readTrack(@NonNull DataSource.Chunk chunk);

    /**
     * Returns the total number of microseconds that have been read until now.
     *
     * @return total read us
     */
    long getReadUs();

    /**
     * When this source has been totally read, it can return true here to
     * notify an end of input stream.
     *
     * @return true if drained
     */
    boolean isDrained();

    /**
     * Called to release resources.
     */
    void release();

    /**
     * Represents a chunk of data.
     * Can be used to read input from {@link #readTrack(Chunk)}.
     */
    class Chunk {
        public ByteBuffer buffer;
        public boolean isKeyFrame;
        public long timestampUs;
        public int bytes;
    }
}
