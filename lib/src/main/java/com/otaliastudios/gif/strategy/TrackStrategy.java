package com.otaliastudios.gif.strategy;

import android.media.MediaFormat;

import com.otaliastudios.gif.strategy.size.Resizer;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Base class for video/audio format strategy.
 * Video strategies should use a {@link Resizer} instance to compute the output
 * video size.
 */
public interface TrackStrategy {

    /**
     * Create the output format for this track (either audio or video).
     * Implementors should fill the outputFormat object.
     *
     * Subclasses can also throw to abort the whole transcoding operation.
     *
     * @param inputFormats the input formats
     * @param outputFormat the output format to be filled
     */
    void createOutputFormat(@NonNull List<MediaFormat> inputFormats, @NonNull MediaFormat outputFormat);
}
