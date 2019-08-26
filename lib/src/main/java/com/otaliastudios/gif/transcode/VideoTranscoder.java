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
package com.otaliastudios.gif.transcode;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import com.otaliastudios.gif.internal.MediaCodecBuffers;
import com.otaliastudios.gif.sink.DataSink;
import com.otaliastudios.gif.source.DataSource;
import com.otaliastudios.gif.time.TimeInterpolator;
import com.otaliastudios.gif.transcode.internal.VideoDecoderOutput;
import com.otaliastudios.gif.transcode.internal.VideoEncoderInput;
import com.otaliastudios.gif.internal.Logger;
import com.otaliastudios.gif.internal.MediaFormatConstants;
import com.otaliastudios.gif.transcode.internal.VideoFrameDropper;

import java.nio.ByteBuffer;

// Refer: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
public class VideoTranscoder extends BaseTranscoder {

    private static final String TAG = VideoTranscoder.class.getSimpleName();
    @SuppressWarnings("unused")
    private static final Logger LOG = new Logger(TAG);

    private VideoDecoderOutput mDecoderOutputSurface;
    private VideoEncoderInput mEncoderInputSurface;
    private MediaCodec mEncoder; // Keep this since we want to signal EOS on it.
    private VideoFrameDropper mFrameDropper;
    private final TimeInterpolator mTimeInterpolator;
    private final int mExtraRotation;

    public VideoTranscoder(
            @NonNull DataSource dataSource,
            @NonNull DataSink dataSink,
            @NonNull TimeInterpolator timeInterpolator,
            int rotation) {
        super(dataSource, dataSink);
        mTimeInterpolator = timeInterpolator;
        mExtraRotation = rotation;
    }

    @Override
    protected void onConfigureEncoder(@NonNull MediaFormat format, @NonNull MediaCodec encoder) {
        // Flip the width and height as needed. This means rotating the VideoStrategy rotation
        // by the amount that was set in the GIFOptions.
        // It is possible that the format has its own KEY_ROTATION, but we don't care, that will
        // be respected at playback time.
        int width = format.getInteger(MediaFormat.KEY_WIDTH);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);
        boolean flip = (mExtraRotation % 180) != 0;
        format.setInteger(MediaFormat.KEY_WIDTH, flip ? height : width);
        format.setInteger(MediaFormat.KEY_HEIGHT, flip ? width : height);
        super.onConfigureEncoder(format, encoder);
    }

    @Override
    protected void onStartEncoder(@NonNull MediaFormat format, @NonNull MediaCodec encoder) {
        mEncoderInputSurface = new VideoEncoderInput(encoder.createInputSurface());
        super.onStartEncoder(format, encoder);
    }

    @Override
    protected void onStarted(@NonNull MediaFormat inputFormat, @NonNull MediaFormat outputFormat, @NonNull MediaCodec encoder) {
        super.onStarted(inputFormat, outputFormat, encoder);
        mEncoder = encoder;

        // The rotation we should apply is the intrinsic source rotation, plus any extra
        // rotation that was set into the GIFOptions.
        mDecoderOutputSurface = new VideoDecoderOutput();
        mDecoderOutputSurface.setRotation(mExtraRotation % 360);

        // Frame dropping support.
        mFrameDropper = VideoFrameDropper.newDropper(
                inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE),
                outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE));

        // Cropping support.
        // Ignoring any outputFormat KEY_ROTATION (which is applied at playback time), the rotation
        // difference between input and output is mSourceRotation + mExtraRotation.
        int rotation = mExtraRotation % 360;
        boolean flip = (rotation % 180) != 0;
        float inputWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        float inputHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        float inputRatio = inputWidth / inputHeight;
        float outputWidth = flip ? outputFormat.getInteger(MediaFormat.KEY_HEIGHT) : outputFormat.getInteger(MediaFormat.KEY_WIDTH);
        float outputHeight = flip ? outputFormat.getInteger(MediaFormat.KEY_WIDTH) : outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        float outputRatio = outputWidth / outputHeight;
        float scaleX = 1, scaleY = 1;
        if (inputRatio > outputRatio) { // Input wider. We have a scaleX.
            scaleX = inputRatio / outputRatio;
        } else if (inputRatio < outputRatio) { // Input taller. We have a scaleY.
            scaleY = outputRatio / inputRatio;
        }

        mDecoderOutputSurface.setSize((int) inputWidth, (int) inputHeight);
        mDecoderOutputSurface.setScale(scaleX, scaleY);
    }

    @Override
    public void release() {
        if (mDecoderOutputSurface != null) {
            mDecoderOutputSurface.release();
            mDecoderOutputSurface = null;
        }
        if (mEncoderInputSurface != null) {
            mEncoderInputSurface.release();
            mEncoderInputSurface = null;
        }
        super.release();
        mEncoder = null;
    }

    @Override
    protected boolean onFeedEncoder(@NonNull MediaCodec encoder, @NonNull MediaCodecBuffers encoderBuffers, long timeoutUs) {
        // We do not feed the encoder, instead we wait for the encoder surface onFrameAvailable callback.
        return false;
    }

    @Override
    protected void onDrainSource(long timeoutUs, @NonNull Bitmap bitmap, long presentationTimeUs, boolean endOfStream) {
        long interpolatedTimeUs = mTimeInterpolator.interpolate(presentationTimeUs);
        if (mFrameDropper.shouldRenderFrame(interpolatedTimeUs)) {
            mDecoderOutputSurface.drawFrame(bitmap);
            mEncoderInputSurface.onFrame(interpolatedTimeUs);
        }
        if (endOfStream) {
            mEncoder.signalEndOfInputStream();
        }
    }
}
