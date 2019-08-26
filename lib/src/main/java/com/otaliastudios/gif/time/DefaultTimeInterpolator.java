package com.otaliastudios.gif.time;

/**
 * A {@link TimeInterpolator} that does no time interpolation or correction -
 * it just returns the input time.
 */
public class DefaultTimeInterpolator implements TimeInterpolator {

    @Override
    public long interpolate(long time) {
        return time;
    }
}
