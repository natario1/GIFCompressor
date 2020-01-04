---
layout: page
title: "Advanced Options"
description: "Advanced compression options"
category: docs
date: 2018-12-20 20:02:08
order: 5
disqus: 1
---

### Frame rotation

You can set the output rotation with the `setRotation(int)` method. This will apply a clockwise
rotation to the input GIF frames. Accepted values are `0`, `90`, `180`, `270`:

```java
GIFCompressor.into(filePath)
        .setRotation(rotation) // 0, 90, 180, 270
        // ...
```

### Time interpolation

We offer APIs to change the timestamp of each GIF frame. You can pass a `TimeInterpolator`
to the compressor builder to be able to receive the frame timestamp as input, and return a new one
as output.

```java
GIFCompressor.into(filePath)
        .setTimeInterpolator(timeInterpolator)
        // ...
```

As an example, this is the implementation of the default interpolator, called `DefaultTimeInterpolator`,
that will just return the input time unchanged:

```java
@Override
public long interpolate(long time) {
    // Receive input time in microseconds and return a possibly different one.
    return time;
}
```

It should be obvious that returning invalid times can make the process crash at any point, or at least
the compression operation fail.

### Video speed

We also offer a special time interpolator called `SpeedTimeInterpolator` that accepts a `float` parameter
and will modify the video speed.

- A speed factor equal to 1 will leave speed unchanged
- A speed factor < 1 will slow the GIF down
- A speed factor > 1 will accelerate the GIF

This interpolator can be set using `setTimeInterpolator(TimeInterpolator)`, or, as a shorthand, 
using `setSpeed(float)`:

```java
GIFCompressor.into(filePath)
        .setSpeed(0.5F) // 0.5x
        .setSpeed(1F) // Unchanged
        .setSpeed(2F) // Twice as fast
        // ...
```

