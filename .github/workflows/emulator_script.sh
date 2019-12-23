#!/usr/bin/env bash
ADB_TAGS="GIFCompressor:I Engine:I"
ADB_TAGS="$ADB_TAGS DefaultStrategy:I"
ADB_TAGS="$ADB_TAGS VideoDecoderOutput:I VideoFrameDropper:I"
adb logcat -c
adb logcat $ADB_TAGS *:E -v color &
./gradlew lib:connectedCheck