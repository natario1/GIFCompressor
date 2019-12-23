---
layout: page
title: "Getting Started"
subtitle: "Simple guide to compress your first GIF"
description: "Simple guide to compress your first GIF"
category: about
date: 2018-12-20 17:48:58
order: 2
disqus: 1
---

### Before you start

If your app targets versions older than API 18, you can override the minSdkVersion by
adding this line to your manifest file:

```xml
<uses-sdk tools:overrideLibrary="com.otaliastudios.gif" />
```

In this case you should check at runtime that API level is at least 18, before
calling any method here.

### Compressing your first GIF

Compression happens through the `GIFCompressor` class by passing it an output file path,
and one of more input data sources. It's pretty simple:

```java
GIFCompressor.into(filePath)
        .addDataSource(context, uri) // or...
        .addDataSource(context, filePath) // or...
        .addDataSource(context, fileDescriptor) // or...
        .addDataSource(dataSource)
        .setListener(new GIFListener() {
             public void onGIFCompressionProgress(double progress) {}
             public void onGIFCompressionCompleted() {}
             public void onGIFCompressionCanceled() {}
             public void onGIFCompressionFailed(@NonNull Throwable exception) {}
        }).compress()
```

However, we offer many APIs and additional features on top that you can read about in the
in-depth documentation.

