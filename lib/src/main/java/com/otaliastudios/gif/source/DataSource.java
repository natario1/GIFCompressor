package com.otaliastudios.gif.source;

import android.graphics.Bitmap;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Represents the source of input GIF data.
 */
public interface DataSource {

    /**
     * Returns the GIF total duration in microseconds.
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
     * Contents should be put inside {@link DataSource.Chunk#bitmap}, and the
     * other chunk flags should be filled.
     *
     * @param chunk output chunk
     */
    void read(@NonNull DataSource.Chunk chunk);

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
     * Can be used to read input from {@link #read(Chunk)}.
     */
    class Chunk {
        public Bitmap bitmap;
        public long timestampUs;
    }
}
