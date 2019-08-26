package com.otaliastudios.gif.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * An utility class for storing data relative to a single TrackType
 * in a map, with handy nullability annotations.
 *
 * @param <T> the map type
 */
public class TrackTypeMap<T> {

    public TrackTypeMap() {
    }

    public TrackTypeMap(@NonNull T videoValue) {
        setVideo(videoValue);
    }

    private Map<String, T> map = new HashMap<>();

    public void setVideo(@Nullable T value) {
        //noinspection ConstantConditions
        map.put("video", value);
    }

    @Nullable
    public T getVideo() {
        return map.get("video");
    }

    @NonNull
    public T requireVideo() {
        //noinspection ConstantConditions
        return map.get("video");
    }

    public boolean has() {
        return map.containsKey("video");
    }
}
