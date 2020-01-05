---
layout: page
title: "Compression Events"
description: "Listening to compression events"
order: 3
disqus: 1
---

GIF compression will happen on a background thread, but we will send updates through the `GIFListener`
interface, which can be applied when building the request:

```java
GIFCompressor.into(filePath)
        .setListenerHandler(handler)
        .setListener(new GIFListener() {
             public void onGIFCompressionProgress(double progress) {}
             public void onGIFCompressionCompleted() {}
             public void onGIFCompressionCanceled() {}
             public void onGIFCompressionFailed(@NonNull Throwable exception) {}
        })
        // ...
```

All of the listener callbacks are called:

- If present, on the handler specified by `setListenerHandler()`
- If it has a handler, on the thread that started the `compress()` call
- As a last resort, on the UI thread

##### onGIFCompressionProgress

This simply sends a double indicating the current progress. The value is typically between 0 and 1,
but can be a negative value to indicate that we are not able to compute progress (yet?).

This is the right place to update a ProgressBar, for example.

##### onGIFCompressionCanceled

The compression operation was canceled. This can happen when the `Future` returned by `compress()`
is cancelled by the user.

##### onGIFCompressionFailed

This can happen in a number of cases and is typically out of our control. Input options might be
wrong, write permissions might be missing, codec might be absent, input file might be not supported
or simply corrupted.

You can take a look at the `Throwable` being passed to know more about the exception.

##### onGIFCompressionCompleted

Compression operation succeeded. The output file now contains the desired video.

