package com.otaliastudios.gif.source;

import android.content.Context;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.otaliastudios.gif.internal.Logger;
import com.otaliastudios.gif.internal.MediaFormatConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;


public abstract class DefaultDataSource implements DataSource {

    private final static String TAG = DefaultDataSource.class.getSimpleName();
    private final static Logger LOG = new Logger(TAG);

    private Context mContext;
    private GifHeader mGifHeader;
    private GifDecoder mGifDecoder;
    private int mGifFrame = 0;
    private int mGifFrames;
    private MediaFormat mFormat;
    private final long mFirstTimestampUs = 10;
    private long mLastTimestampUs = mFirstTimestampUs;
    private long mDurationUs = Long.MIN_VALUE;

    protected DefaultDataSource(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @NonNull
    protected abstract InputStream openInputStream();

    private byte[] getInputStreamData() {
        try {
            InputStream inputStream = openInputStream();
            int readBytes;
            byte[] buffer = new byte[16 * 1024];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while ((readBytes = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, readBytes);
            }
            outputStream.flush();
            byte[] data = outputStream.toByteArray();
            inputStream.close();
            outputStream.close();
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureGifHeader() {
        if (mGifHeader != null) return;
        GifHeaderParser parser = new GifHeaderParser();
        parser.setData(getInputStreamData());
        mGifHeader = parser.parseHeader();
        parser.clear();
        if (mGifHeader.getStatus() != GifDecoder.STATUS_OK) {
            throw new RuntimeException("Illegal status: " + mGifHeader.getStatus());
        }
    }

    private void ensureGifDecoder() {
        if (mGifDecoder != null) return;
        ensureGifHeader();
        GifDecoder.BitmapProvider provider = new GifBitmapProvider(
                Glide.get(mContext).getBitmapPool(),
                Glide.get(mContext).getArrayPool()
        );
        mGifDecoder = new StandardGifDecoder(provider);
        mGifDecoder.setData(mGifHeader, getInputStreamData());
        mGifFrames = mGifDecoder.getFrameCount() + 1;
    }

    @Override
    public long getDurationUs() {
        if (mDurationUs == Long.MIN_VALUE) {
            ensureGifHeader();
            long durationUs = 0L;
            try {
                Field framesField = GifHeader.class.getDeclaredField("frames");
                framesField.setAccessible(true);
                List frames = (List) framesField.get(mGifHeader);
                Class frameClass = Class.forName("com.bumptech.glide.gifdecoder.GifFrame");
                Field frameDelayField = frameClass.getDeclaredField("delay");
                frameDelayField.setAccessible(true);
                for (Object frame : frames) {
                    durationUs += frameDelayField.getInt(frame) * 1000L;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mDurationUs = durationUs;
        }
        return mDurationUs;
    }

    @NonNull
    @Override
    public MediaFormat getTrackFormat() {
        if (mFormat == null) {
            ensureGifHeader();
            mFormat = new MediaFormat();
            mFormat.setInteger(MediaFormat.KEY_WIDTH, mGifHeader.getWidth());
            mFormat.setInteger(MediaFormat.KEY_HEIGHT, mGifHeader.getHeight());
            mFormat.setInteger(MediaFormatConstants.KEY_ROTATION_DEGREES, 0);
            int frames = mGifHeader.getNumFrames();
            double durationSeconds = (double) getDurationUs() / 1000000D;
            int framesPerSecond = (int) Math.round(frames / durationSeconds);
            mFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framesPerSecond);
        }
        return mFormat;
    }


    @Override
    public void start() {
        ensureGifDecoder();
        mGifFrame = -1;
    }

    @Override
    public void read(@NonNull Chunk chunk) {
        mGifDecoder.advance();
        mGifFrame++;
        if (mGifFrame == 0) {
            // First frame.
            mLastTimestampUs = mFirstTimestampUs;
            chunk.bitmap = mGifDecoder.getNextFrame();
        } else if (mGifFrame < mGifDecoder.getFrameCount() - 1) {
            // Middle frame.
            mLastTimestampUs += mGifDecoder.getDelay(mGifFrame - 1) * 1000L;
            chunk.bitmap = mGifDecoder.getNextFrame();
        } else {
            // Last frame.
            // Here we repeat the last bitmap with an increased delay.
            mLastTimestampUs += mGifDecoder.getDelay(mGifFrame - 1) * 1000L;
        }
        chunk.timestampUs = mLastTimestampUs;
    }

    @Override
    public void release() {
        mGifHeader = null;
        if (mGifDecoder != null) {
            mGifDecoder.clear();
            mGifDecoder = null;
        }
    }

    @Override
    public boolean isDrained() {
        return mGifFrame == mGifFrames - 1;
    }

    @Override
    public final long getReadUs() {
        return mLastTimestampUs - mFirstTimestampUs;
    }
}
