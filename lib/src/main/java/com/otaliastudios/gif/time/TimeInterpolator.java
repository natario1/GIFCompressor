package com.otaliastudios.gif.time;

/**
 * An interface to redefine the time between frames.
 */
public interface TimeInterpolator {

    /**
     * Given the frame timestamp in microseconds,
     * should return the corrected timestamp.
     *
     * @param time frame timestamp in microseconds
     * @return the new frame timestamp
     */
    long interpolate(long time);
}
