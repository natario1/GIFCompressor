package com.otaliastudios.gif.strategy;

import androidx.annotation.NonNull;

/**
 * Contains presets and utilities for defining a {@link DefaultStrategy}.
 */
public class DefaultStrategies {

    private DefaultStrategies() {}

    /**
     * A {@link DefaultStrategy} that uses 720x1280.
     * This preset is ensured to work on any Android &gt;=4.3 devices by Android CTS,
     * assuming that the codec is available.
     *
     * @return a default video strategy
     */
    @NonNull
    public static DefaultStrategy for720x1280() {
        return DefaultStrategy.exact(720, 1280)
                .bitRate(2L * 1000 * 1000)
                .frameRate(30)
                .keyFrameInterval(3F)
                .build();
    }

    /**
     * A {@link DefaultStrategy} that uses 360x480 (3:4),
     * ensured to work for 3:4 videos as explained by
     * https://developer.android.com/guide/topics/media/media-formats
     *
     * @return a default video strategy
     */
    @SuppressWarnings("unused")
    @NonNull
    public static DefaultStrategy for360x480() {
        return DefaultStrategy.exact(360, 480)
                .bitRate(500L * 1000)
                .frameRate(30)
                .keyFrameInterval(3F)
                .build();
    }
}
