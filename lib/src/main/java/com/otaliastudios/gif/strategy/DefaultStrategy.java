package com.otaliastudios.gif.strategy;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import com.otaliastudios.gif.strategy.size.AspectRatioResizer;
import com.otaliastudios.gif.strategy.size.AtMostResizer;
import com.otaliastudios.gif.strategy.size.ExactResizer;
import com.otaliastudios.gif.strategy.size.ExactSize;
import com.otaliastudios.gif.strategy.size.FractionResizer;
import com.otaliastudios.gif.strategy.size.MultiResizer;
import com.otaliastudios.gif.strategy.size.Size;
import com.otaliastudios.gif.strategy.size.Resizer;
import com.otaliastudios.gif.internal.Logger;
import com.otaliastudios.gif.internal.MediaFormatConstants;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * An {@link Strategy} for video that converts it AVC with the given size.
 * The input and output aspect ratio must match.
 */
public class DefaultStrategy implements Strategy {
    private final static String TAG = DefaultStrategy.class.getSimpleName();
    private final static Logger LOG = new Logger(TAG);

    @SuppressWarnings("WeakerAccess")
    public final static long BITRATE_UNKNOWN = Long.MIN_VALUE;

    @SuppressWarnings("WeakerAccess")
    public final static float DEFAULT_KEY_FRAME_INTERVAL = 3;

    public final static int DEFAULT_FRAME_RATE = 30;

    /**
     * Holds configuration values.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Options {
        private Options() {}
        private Resizer resizer;
        private long targetBitRate;
        private int targetFrameRate;
        private float targetKeyFrameInterval;
        private String targetMimeType;
    }

    /**
     * Creates a new {@link Builder} with an {@link ExactResizer}
     * using given dimensions.
     *
     * @param firstSize the exact first size
     * @param secondSize the exact second size
     * @return a strategy builder
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static Builder exact(int firstSize, int secondSize) {
        return new Builder(new ExactResizer(firstSize, secondSize));
    }

    /**
     * Creates a new {@link Builder} with a {@link FractionResizer}
     * using given downscale fraction.
     *
     * @param fraction the downscale fraction
     * @return a strategy builder
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Builder fraction(float fraction) {
        return new Builder(new FractionResizer(fraction));
    }

    /**
     * Creates a new {@link Builder} with a {@link AspectRatioResizer}
     * using given aspect ratio value.
     *
     * @param aspectRatio the desired aspect ratio
     * @return a strategy builder
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Builder aspectRatio(float aspectRatio) {
        return new Builder(new AspectRatioResizer(aspectRatio));
    }

    /**
     * Creates a new {@link Builder} with an {@link AtMostResizer}
     * using given constraint.
     *
     * @param atMostSize size constraint
     * @return a strategy builder
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Builder atMost(int atMostSize) {
        return new Builder(new AtMostResizer(atMostSize));
    }

    /**
     * Creates a new {@link Builder} with an {@link AtMostResizer}
     * using given constraints.
     *
     * @param atMostMajor constraint for the major dimension
     * @param atMostMinor constraint for the minor dimension
     * @return a strategy builder
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Builder atMost(int atMostMinor, int atMostMajor) {
        return new Builder(new AtMostResizer(atMostMinor, atMostMajor));
    }

    public static class Builder {
        private MultiResizer resizer = new MultiResizer();
        private int targetFrameRate = DEFAULT_FRAME_RATE;
        private long targetBitRate = BITRATE_UNKNOWN;
        private float targetKeyFrameInterval = DEFAULT_KEY_FRAME_INTERVAL;
        private String targetMimeType = MediaFormatConstants.MIMETYPE_VIDEO_AVC;

        @SuppressWarnings("unused")
        public Builder() { }

        @SuppressWarnings("WeakerAccess")
        public Builder(@NonNull Resizer resizer) {
            this.resizer.addResizer(resizer);
        }

        /**
         * Adds another resizer to the resizer chain. By default, we use
         * a {@link MultiResizer} so you can add more than one resizer in chain.
         * @param resizer new resizer for backed {@link MultiResizer}
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("unused")
        public Builder addResizer(@NonNull Resizer resizer) {
            this.resizer.addResizer(resizer);
            return this;
        }

        /**
         * The desired bit rate. Can optionally be {@link #BITRATE_UNKNOWN},
         * in which case the strategy will try to estimate the bitrate.
         * @param bitRate desired bit rate (bits per second)
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("WeakerAccess")
        public Builder bitRate(long bitRate) {
            targetBitRate = bitRate;
            return this;
        }

        /**
         * The desired frame rate. It will never be bigger than
         * the input frame rate, if that information is available.
         * @param frameRate desired frame rate (frames per second)
         * @return this for chaining
         */
        @NonNull
        public Builder frameRate(int frameRate) {
            targetFrameRate = frameRate;
            return this;
        }

        /**
         * The interval between key-frames in seconds.
         * @param keyFrameInterval desired key-frame interval
         * @return this for chaining
         */
        @NonNull
        @SuppressWarnings("WeakerAccess")
        public Builder keyFrameInterval(float keyFrameInterval) {
            targetKeyFrameInterval = keyFrameInterval;
            return this;
        }

        @SuppressWarnings("unused")
        @NonNull
        public Builder mimeType(@NonNull String mimeType) {
            this.targetMimeType = mimeType;
            return this;
        }

        @NonNull
        @SuppressWarnings("WeakerAccess")
        public Options options() {
            Options options = new Options();
            options.resizer = resizer;
            options.targetFrameRate = targetFrameRate;
            options.targetBitRate = targetBitRate;
            options.targetKeyFrameInterval = targetKeyFrameInterval;
            options.targetMimeType = targetMimeType;
            return options;
        }

        @NonNull
        public DefaultStrategy build() {
            return new DefaultStrategy(options());
        }
    }

    private final Options options;

    @SuppressWarnings("WeakerAccess")
    public DefaultStrategy(@NonNull Options options) {
        this.options = options;
    }

    @Override
    public void createOutputFormat(@NonNull List<MediaFormat> inputFormats,
                                   @NonNull MediaFormat outputFormat) {

        // Compute output size in rotation=0 reference.
        ExactSize inSize = getBestInputSize(inputFormats);
        int inWidth = inSize.getWidth();
        int inHeight = inSize.getHeight();
        LOG.i("Input width&height: " + inWidth + "x" + inHeight);
        Size outSize;
        try {
            outSize = options.resizer.getOutputSize(inSize);
        } catch (Exception e) {
            throw new RuntimeException("Resizer error:", e);
        }
        int outWidth, outHeight;
        if (outSize instanceof ExactSize) {
            outWidth = ((ExactSize) outSize).getWidth();
            outHeight = ((ExactSize) outSize).getHeight();
        } else if (inWidth >= inHeight) {
            outWidth = outSize.getMajor();
            outHeight = outSize.getMinor();
        } else {
            outWidth = outSize.getMinor();
            outHeight = outSize.getMajor();
        }
        LOG.i("Output width&height: " + outWidth + "x" + outHeight);

        // Compute output frame rate. It can't be bigger than input frame rate.
        int outFrameRate;
        int inputFrameRate = getMinFrameRate(inputFormats);
        if (inputFrameRate > 0) {
            outFrameRate = Math.min(inputFrameRate, options.targetFrameRate);
        } else {
            outFrameRate = options.targetFrameRate;
        }

        // Create the actual format.
        outputFormat.setString(MediaFormat.KEY_MIME, options.targetMimeType);
        outputFormat.setInteger(MediaFormat.KEY_WIDTH, outWidth);
        outputFormat.setInteger(MediaFormat.KEY_HEIGHT, outHeight);
        outputFormat.setInteger(MediaFormatConstants.KEY_ROTATION_DEGREES, 0);
        outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, outFrameRate);
        if (Build.VERSION.SDK_INT >= 25) {
            outputFormat.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, options.targetKeyFrameInterval);
        } else {
            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, (int) Math.ceil(options.targetKeyFrameInterval));
        }
        outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        int outBitRate = (int) (options.targetBitRate == BITRATE_UNKNOWN ?
                estimateBitRate(outWidth, outHeight, outFrameRate) : options.targetBitRate);
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, outBitRate);
    }

    /**
     * Chooses one of the input sizes that is considered to be the best.
     * After thinking about it, I think the best size is the one that is closer to the
     * average aspect ratio.
     *
     * Of course, we must consider all formats' rotation.
     * The size returned is rotated in the reference of a format with rotation = 0.
     *
     * @param formats input formats
     * @return best input size
     */
    private ExactSize getBestInputSize(@NonNull List<MediaFormat> formats) {
        int count = formats.size();
        float averageAspectRatio = 0;
        float[] aspectRatio = new float[count];
        boolean[] flipSize = new boolean[count];
        for (int i = 0; i < count; i++) {
            MediaFormat format = formats.get(i);
            float width = format.getInteger(MediaFormat.KEY_WIDTH);
            float height = format.getInteger(MediaFormat.KEY_HEIGHT);
            int rotation = 0;
            if (format.containsKey(MediaFormatConstants.KEY_ROTATION_DEGREES)) {
                rotation = format.getInteger(MediaFormatConstants.KEY_ROTATION_DEGREES);
            }
            boolean flip = (rotation % 180) != 0;
            flipSize[i] = flip;
            aspectRatio[i] = flip ? height / width : width / height;
            averageAspectRatio += aspectRatio[i];
        }
        averageAspectRatio = averageAspectRatio / count;
        float bestDelta = Float.MAX_VALUE;
        int bestMatch = 0;
        for (int i = 0; i < count; i++) {
            float delta = Math.abs(aspectRatio[i] - averageAspectRatio);
            if (delta < bestDelta) {
                bestMatch = i;
                bestDelta = delta;
            }
        }
        MediaFormat bestFormat = formats.get(bestMatch);
        int bestWidth = bestFormat.getInteger(MediaFormat.KEY_WIDTH);
        int bestHeight = bestFormat.getInteger(MediaFormat.KEY_HEIGHT);
        return new ExactSize(
                flipSize[bestMatch] ? bestHeight : bestWidth,
                flipSize[bestMatch] ? bestWidth : bestHeight);
    }

    private int getMinFrameRate(@NonNull List<MediaFormat> formats) {
        int frameRate = Integer.MAX_VALUE;
        for (MediaFormat format : formats) {
            if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                frameRate = Math.min(frameRate, format.getInteger(MediaFormat.KEY_FRAME_RATE));
            }
        }
        return (frameRate == Integer.MAX_VALUE) ? -1 : frameRate;
    }

    /**
     * Depends on the codec, but for AVC this is a reasonable default.
     * https://stackoverflow.com/a/5220554/4288782
     *
     * However, GIF often have extremely low frame rates and I think that
     * Android encoders add repeated frames when frame rate is too low.
     * So let's add a min frame rate here into the computation.
     *
     * @param width width
     * @param height height
     * @param frameRate rate
     * @return bitRate
     */
    private static long estimateBitRate(int width, int height, int frameRate) {
        return (long) (0.07F * 2 * width * height * Math.max(frameRate, 24));
    }
}
