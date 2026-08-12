# Camera frame ownership and conversion

Eyespie treats camera producer buffers as borrowed platform resources. Analysis crosses asynchronous boundaries only after the application has either copied the frame into owned memory or explicitly retained the native buffer for the bounded analysis lifetime.

## Android

CameraX owns `ImageProxy` and its wrapped `Media.Image`.

The analyzer:

1. accepts at most one analysis frame at a time;
2. converts the proxy to an app-owned `Bitmap` with CameraX `ImageProxy.toBitmap()`;
3. closes the `ImageProxy` before launching asynchronous analysis;
4. drops and closes later proxies while analysis is in flight;
5. uses `ImageProcessingOptions` for MediaPipe rotation/ROI while presentation/export paths rotate the owned bitmap separately.

`PlatformCameraImage.copy(ImageProxy)` copies pixels but deliberately does not close the proxy; ownership of the proxy remains with the caller.

## iOS

`CMSampleBufferGetImageBuffer` returns a borrowed Core Video buffer. The capture delegate explicitly retains the pixel buffer before asynchronous analysis and releases it exactly once after the callback completes. Only one retained frame is allowed in flight; subsequent delegate frames are dropped until analysis finishes.

For the configured bi-planar video-range YUV input:

- conversion uses `CVPixelBufferGetBytesPerRowOfPlane` rather than assuming tightly packed planes;
- YUV offsets are calculated with signed arithmetic and output channels are clamped;
- the converted BGRA frame is stored in a Kotlin `ByteArray`, avoiding unmanaged per-frame native heap allocation;
- camera orientation is applied before PNG encoding;
- encoded PNG bytes preserve the cross-platform `CameraImage.toByteArray()` contract used by capture persistence.

## Remaining release validation

This ownership contract still requires sustained physical-device scanning on Android and iOS as part of the closed-alpha device proof. The separate iOS still-image capture/load path also remains incomplete and must not be treated as implemented by this change.
