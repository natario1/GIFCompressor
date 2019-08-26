package com.otaliastudios.gif.validator;

import androidx.annotation.NonNull;

import com.otaliastudios.gif.engine.TrackStatus;
import com.otaliastudios.gif.strategy.TrackStrategy;

/**
 * A validator determines if the transcoding process should proceed or not,
 * after the {@link TrackStrategy} have
 * provided the output format.
 */
public interface Validator {

    /**
     * Return true if the transcoding should proceed, false otherwise.
     *
     * @param videoStatus the status of the video track
     * @param audioStatus the status of the audio track
     * @return true to proceed
     */
    boolean validate(@NonNull TrackStatus videoStatus, @NonNull TrackStatus audioStatus);

}
