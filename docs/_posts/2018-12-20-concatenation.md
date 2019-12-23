---
layout: page
title: "GIF Concatenation"
subtitle: "How to concatenate GIF segments"
description: "How to concatenate GIF segments"
category: docs
date: 2018-12-20 20:02:08
order: 2
disqus: 1
---

As you might have guessed, you can use `addDataSource(source)` multiple times. All the source
GIFs will be stitched together:

```java
GIFCompressor.into(filePath)
        .addDataSource(source1)
        .addDataSource(source2)
        .addDataSource(source3)
        // ...
```

In the above example, the three GIF files will be stitched together in the order they are added
to the builder. Once `source1` ends, we'll append `source2` and so on. The library will take care
of applying consistent parameters (frame rate, bit rate) during the conversion.

For Example:

```java
GIFCompressor.into(filePath)
        .addDataSource(source1) // 20 seconds
        .addDataSource(source2) // 5 seconds
        .addDataSource(source3) // 5 seconds
        // ...
```

In the above example, the output file will be 30 seconds long:

```
Video: | •••••••••••••••••• source1 •••••••••••••••••• | •••• source2 •••• | •••• source3 •••• |  
```

