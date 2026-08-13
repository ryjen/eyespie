# Camera frame ownership and conversion

Eyespie treats camera producer buffers as borrowed platform resources. Application actions and asynchronous consumers receive only application-owned pixel data; platform producer objects do not escape their bounded conversion lifetime.

## Android

CameraX owns `ImageProxy` and its wrapped `Media.Image`.

The analyzer:

1. accepts at most one conversion callback at a time;
2. converts the proxy to an app-owned `Bitmap` with CameraX `ImageProxy.toBitmap()`;
3. closes the `ImageProxy` before launching asynchronous application work;
4. drops and closes later proxies while conversion/callback work is in flight;
5. uses `ImageProcessingOptions` for MediaPipe rotation/ROI while presentation/export paths rotate the owned bitmap separately.

`PlatformCameraImage.copy(ImageProxy)` copies pixels but deliberately does not close the proxy; ownership of the proxy remains with the caller.

MediaPipe `MPImage` wrappers are reference-counted resources. The embedding caller creates one wrapper around the owned bitmap and closes it after inference instead of caching a per-frame native wrapper.

## iOS

`CMSampleBufferGetImageBuffer` returns a borrowed Core Video buffer. The capture delegate retains that pixel buffer only long enough for a background worker to copy and convert its pixels into a Kotlin-owned BGRA `ByteArray`. The retained `CVPixelBuffer` is released before `CameraImage` is dispatched to application actions, so queued consumers cannot observe a dangling CoreVideo reference.

Only one native frame conversion is allowed in flight; subsequent delegate frames are dropped until the producer buffer has been copied.

For the configured bi-planar video-range YUV input:

- conversion uses `CVPixelBufferGetBytesPerRowOfPlane` rather than assuming tightly packed planes;
- YUV offsets are calculated with signed arithmetic and output channels are clamped;
- the converted BGRA frame is stored in a Kotlin `ByteArray`, avoiding unmanaged per-frame native heap allocation;
- camera orientation is applied to the owned BGRA frame;
- PNG encoding is lazy and operates only on owned Kotlin memory;
- encoded PNG bytes preserve the cross-platform `CameraImage.toByteArray()` contract used by capture persistence.

### iOS still-image capture

The still-capture path consumes only the application-owned `CameraImage` described above. It never retains a `CMSampleBuffer` or `CVPixelBuffer`.

- `IosCameraCaptureController` keeps one most-recent owned frame and serializes explicit capture actions.
- Temporary-storage preparation is serialized before persistence so stale-file cleanup cannot race a new capture.
- PNG encoding and filesystem writes execute off the main/UI and AVCapture delegate queues.
- Captures are written under a dedicated system-temporary `eyespie-camera-captures` directory.
- Entering a new capture surface prunes stale files left by prior completed or abandoned capture flows.
- After a successful `Path` callback, ownership of that active temporary file transfers to the downstream `ScanEdit` flow; composable disposal during navigation must not delete it prematurely.
- Upload object names are derived from the encoded image signature (`.png` or `.jpg`) rather than assuming JPEG, so the stored object identity matches the bytes.
- Capture/preparation/encoding/storage failures use bounded diagnostic categories and flow through the existing camera error path.

## Remaining release validation

This ownership contract still requires sustained physical-device scanning on Android and iOS as part of the closed-alpha device proof. iOS still capture additionally requires physical-device proof for preview → capture → persisted image → downstream challenge flow and repeated capture sessions without unbounded temporary-file growth.
