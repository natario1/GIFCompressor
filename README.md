[![Build Status](https://github.com/natario1/GIFCompressor/workflows/Build/badge.svg?event=push)](https://github.com/natario1/GIFCompressor/actions)
[![Release](https://img.shields.io/github/release/natario1/GIFCompressor.svg)](https://github.com/natario1/GIFCompressor/releases)
[![Issues](https://img.shields.io/github/issues-raw/natario1/GIFCompressor.svg)](https://github.com/natario1/GIFCompressor/issues)

&#10240;  <!-- Hack to add whitespace -->

<p align="center">
  <img src="docs/static/banner.png" width="100%">
</p>

*Looking for a complete and powerful video transcoder? Take a look at our [Transcoder](https://github.com/natario1/Transcoder).*

*Need support, consulting, or have any other business-related question? Feel free to <a href="mailto:mat.iavarone@gmail.com">get in touch</a>.*

*Like the project, make profit from it, or simply want to thank back? Please consider [sponsoring me](https://github.com/sponsors/natario1)!*

# GIFCompressor

An Android tool to compress GIF files into the MP4 format, using hardware-accelerated Android codecs available on the device. Works on API 18+.

```groovy
implementation 'com.otaliastudios.gif:compressor:1.0.0'
```

- Fast compression to lightweight MP4 (AVC)
- Hardware accelerated
- Multithreaded
- Convenient, fluent API
- Concatenate multiple GIF files [[docs]](https://natario1.github.io/GIFCompressor/docs/concatenation)
- Choose output size, with automatic cropping [[docs]](https://natario1.github.io/GIFCompressor/docs/strategies#frame-size)
- Choose output rotation [[docs]](https://natario1.github.io/GIFCompressor/docs/advanced-options#frame-rotation) 
- Choose output speed [[docs]](https://natario1.github.io/GIFCompressor/docs/advanced-options#video-speed)
- Choose output frame rate [[docs]](https://natario1.github.io/GIFCompressor/docs/strategies#other-options)
- Override frames timestamp, e.g. to slow down the middle part of the video [[docs]](https://natario1.github.io/GIFCompressor/docs/advanced-options#time-interpolation) 
- Error handling [[docs]](https://natario1.github.io/GIFCompressor/docs/events)
- Configurable strategies [[docs]](https://natario1.github.io/GIFCompressor/docs/strategies)

&#10240;  <!-- Hack to add whitespace -->

<p align="center">
  <img src="docs/static/screenshot-1.png" width="250" hspace="25"><img src="docs/static/screenshot-2.png" width="250" hspace="25">
</p>

&#10240;  <!-- Hack to add whitespace -->

## Support

If you like the project, make profit from it, or simply want to thank back, please consider 
[sponsoring me](https://github.com/sponsors/natario1) through the GitHub Sponsors program! 
You can have your company logo here, get private support hours or simply help me push this forward. 

Feel free to <a href="mailto:mat.iavarone@gmail.com">contact me</a> for support, consulting or any 
other business-related question.

## Setup

Please read the [official website](https://natario1.github.io/GIFCompressor) for setup instructions and documentation.
You might also be interested in our [changelog](https://natario1.github.io/GIFCompressor/about/changelog). 
Using GIFCompressor in the most basic form is pretty simple:

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

Take a look at the demo app for a complete example.
