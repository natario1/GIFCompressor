---
layout: page
title: "Strategies"
description: "How to choose compression options"
order: 5
disqus: 1
---


Track strategies return options for the engine to understand how the GIF file (or files)
should be compressed.

```java
Transcoder.into(filePath)
        .setStrategy(strategy)
        // ...
```

The point of `Strategy` is to inspect the input `android.media.MediaFormat` and return
the output `android.media.MediaFormat`, filled with required options.

This library offers a default strategy that covers most use cases, called `DefaultStrategy`.
This strategy converts the GIF stream to AVC format and is very configurable. 

### Frame Size

The `DefaultStrategy` helps in defining a consistent output size. If the chosen size does not 
match the aspect ratio of the input GIF(s) size, `GIFCompressor` will automatically crop part 
of the input so it matches the final ratio.

We provide helpers for common tasks:

```java
DefaultStrategy strategy;

// Sets an exact size. If aspect ratio does not match, cropping will take place.
strategy = DefaultStrategy.exact(1080, 720).build();

// Keeps the aspect ratio, but scales down the input size with the given fraction.
strategy = DefaultStrategy.fraction(0.5F).build();

// Ensures that each video size is at most the given value - scales down otherwise.
strategy = DefaultStrategy.atMost(1000).build();

// Ensures that minor and major dimension are at most the given values - scales down otherwise.
strategy = DefaultStrategy.atMost(500, 1000).build();
```

In fact, all of these will simply call `new DefaultStrategy.Builder(resizer)` with a special
resizer. We offer handy resizers:

|Name|Description|
|----|-----------|
|`ExactResizer`|Returns the exact dimensions passed to the constructor.|
|`AspectRatioResizer`|Crops the input size to match the given aspect ratio.|
|`FractionResizer`|Reduces the input size by the given fraction (0..1).|
|`AtMostResizer`|If needed, reduces the input size so that the "at most" constraints are matched. Aspect ratio is kept.|
|`PassThroughResizer`|Returns the input size unchanged.|

You can also group resizers through `MultiResizer`, which applies resizers in chain:

```java
// First scales down, then ensures size is at most 1000. Order matters!
Resizer resizer = new MultiResizer();
resizer.addResizer(new FractionResizer(0.5F));
resizer.addResizer(new AtMostResizer(1000));

// First makes it 16:9, then ensures size is at most 1000. Order matters!
Resizer resizer = new MultiResizer();
resizer.addResizer(new AspectRatioResizer(16F / 9F));
resizer.addResizer(new AtMostResizer(1000));
```

This option is already available through the `DefaultStrategy` builder, so you can do:

```java
DefaultStrategy strategy = new DefaultStrategy.Builder()
        .addResizer(new AspectRatioResizer(16F / 9F))
        .addResizer(new FractionResizer(0.5F))
        .addResizer(new AtMostResizer(1000))
        .build();
```

### Other options

You can configure the `DefaultStrategy` with other options unrelated to the video size:

```java
DefaultStrategy strategy = new DefaultStrategy.Builder()
        .bitRate(bitRate)
        .bitRate(DefaultVideoStrategy.BITRATE_UNKNOWN) // tries to estimate
        .frameRate(frameRate) // will be capped to the input frameRate
        .keyFrameInterval(interval) // interval between key-frames in seconds
        .build();
```

### Compatibility

As stated pretty much everywhere, **not all codecs/devices/manufacturers support all sizes/options**.
This is a complex issue which has no easy solution - a wrong size can lead to a compression error 
or corrupted file.

Android platform specifies requirements for manufacturers through the [CTS (Compatibility test suite)](https://source.android.com/compatibility/cts).
Only a few codecs and sizes are **strictly** required to work.

We collect common presets in the `DefaultStrategies` class:

```java
GIFCompressor.into(filePath)
        .setStrategy(DefaultStrategies.for720x1280()) // 16:9
        .setStrategy(DefaultStrategies.for360x480()) // 4:3
        // ...
```