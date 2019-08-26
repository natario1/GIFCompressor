package com.otaliastudios.gif.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.gif.engine.TrackType;

import java.util.HashMap;
import java.util.Map;

/**
 * An utility class for storing data relative to a single {@link TrackType}
 * in a map, with handy nullability annotations.
 *
 * @param <T> the map type
 */
public class TrackTypeMap<T> {

    public TrackTypeMap() {
    }

    public TrackTypeMap(@NonNull T videoValue) {
        set(TrackType.VIDEO, videoValue);
    }

    private Map<TrackType, T> map = new HashMap<>();

    public void set(@NonNull TrackType type, @Nullable T value) {
        //noinspection ConstantConditions
        map.put(type, value);
    }

    public void setVideo(@Nullable T value) {
        set(TrackType.VIDEO, value);
    }

    @Nullable
    public T get(@NonNull TrackType type) {
        return map.get(type);
    }

    @Nullable
    public T getVideo() {
        return get(TrackType.VIDEO);
    }

    @NonNull
    public T require(@NonNull TrackType type) {
        //noinspection ConstantConditions
        return map.get(type);
    }

    @NonNull
    public T requireVideo() {
        return require(TrackType.VIDEO);
    }

    public boolean has(@NonNull TrackType type) {
        return map.containsKey(type);
    }
}
