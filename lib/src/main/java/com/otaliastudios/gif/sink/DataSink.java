package com.otaliastudios.gif.sink;

import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import com.otaliastudios.gif.transcode.Transcoder;

import java.nio.ByteBuffer;

/**
 * A DataSink is an abstract representation of an encoded data collector.
 * Currently the only implementation is {@link DefaultDataSink} which collects
 * data into a {@link java.io.File} using {@link android.media.MediaMuxer}.
 *
 * However there might be other implementations in the future, for example to stream data
 * to a server.
 */
public interface DataSink {

    /**
     * Called before starting to set the orientation metadata.
     * @param orientation 0, 90, 180 or 270
     */
    void setOrientation(int orientation);

    /**
     * Called by {@link Transcoder}s when they have an output format.
     * This is not the output format chosen by the library user but rather the
     * output format determined by {@link MediaCodec}, which contains more information,
     * and should be inspected to know what kind of data we're collecting.
     *
     * @param format the track format
     */
    void setFormat(@NonNull MediaFormat format);

    /**
     * Called by {@link Transcoder}s to write data into this sink.
     *
     * @param byteBuffer the data
     * @param bufferInfo the metadata
     */
    void write(@NonNull ByteBuffer byteBuffer,
               @NonNull MediaCodec.BufferInfo bufferInfo);

    /**
     * Called when transcoders have stopped writing.
     */
    void stop();

    /**
     * Called to release resources.
     * Any exception should probably be caught here.
     */
    void release();
}
