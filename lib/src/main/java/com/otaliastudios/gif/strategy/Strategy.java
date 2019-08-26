package com.otaliastudios.gif.strategy;

import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Base interface for format strategy.
 * See {@link DefaultStrategy} for a concrete implementation.
 */
public interface Strategy {

    /**
     * Create the output format for the video.
     * Implementors should fill the outputFormat object, or they can throw
     * to abort the whole compression operation.
     *
     * @param inputFormats the input formats
     * @param outputFormat the output format to be filled
     */
    void createOutputFormat(@NonNull List<MediaFormat> inputFormats, @NonNull MediaFormat outputFormat);
}
