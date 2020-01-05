---
layout: page
title: "Data Sources"
description: "Sources of media data"
order: 1
disqus: 1
---

Starting a compression operation will require a source for our data, which is not necessarily
a `File`. The `DataSource` objects will automatically take care about releasing streams / resources,
which is convenient but it means that they can not be used twice.

```java
GIFCompressor.into(filePath)
        .addDataSource(source1)
        .compress()
```

##### UriDataSource

The Android friendly source can be created with `new UriDataSource(context, uri)` or simply
using `addDataSource(context, uri)` in the compression builder.

##### FileDescriptorDataSource

A data source backed by a file descriptor. Use `new FileDescriptorDataSource(context, descriptor)` or
simply `addDataSource(context, descriptor)` in the compression builder.

##### FilePathDataSource

A data source backed by a file absolute path. Use `new FilePathDataSource(context, path)` or
simply `addDataSource(context, path)` in the compression builder.
 
### Related APIs

|Method|Description|
|------|-----------|
|`addDataSource(Context, Uri)`|Adds a new source for the given Uri.|
|`addDataSource(FileDescriptor)`|Adds a new source for the given FileDescriptor.|
|`addDataSource(String)`|Adds a new source for the given file path.|
|`addDataSource(DataSource)`|Adds a new source.|

