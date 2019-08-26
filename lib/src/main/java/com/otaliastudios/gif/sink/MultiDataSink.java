package com.otaliastudios.gif.sink;

import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class MultiDataSink implements DataSink {

    private final List<DataSink> sinks;

    public MultiDataSink(@NonNull DataSink... sink) {
        sinks = Arrays.asList(sink);
    }

    @Override
    public void setOrientation(int orientation) {
        for (DataSink sink : sinks) {
            sink.setOrientation(orientation);
        }
    }

    @Override
    public void setTrackFormat(@NonNull MediaFormat format) {
        for (DataSink sink : sinks) {
            sink.setTrackFormat(format);
        }
    }

    @Override
    public void writeTrack(@NonNull ByteBuffer byteBuffer, @NonNull MediaCodec.BufferInfo bufferInfo) {
        int position = byteBuffer.position();
        int limit = byteBuffer.limit();
        for (DataSink sink : sinks) {
            sink.writeTrack(byteBuffer, bufferInfo);
            byteBuffer.position(position);
            byteBuffer.limit(limit);
        }
    }

    @Override
    public void stop() {
        for (DataSink sink : sinks) {
            sink.stop();
        }
    }

    @Override
    public void release() {
        for (DataSink sink : sinks) {
            sink.release();
        }
    }
}
