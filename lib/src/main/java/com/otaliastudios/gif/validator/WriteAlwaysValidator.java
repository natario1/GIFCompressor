package com.otaliastudios.gif.validator;

import androidx.annotation.NonNull;

import com.otaliastudios.gif.engine.TrackStatus;

/**
 * A {@link Validator} that always writes to target file, no matter the track status,
 * presence of tracks and so on. The output container file might be empty or unnecessary.
 */
@SuppressWarnings("unused")
public class WriteAlwaysValidator implements Validator {

    @Override
    public boolean validate(@NonNull TrackStatus videoStatus, @NonNull TrackStatus audioStatus) {
        return true;
    }
}
