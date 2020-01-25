package com.otaliastudios.gif.transcode.internal;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.otaliastudios.opengl.draw.GlRect;
import com.otaliastudios.opengl.program.GlTextureProgram;
import com.otaliastudios.gif.internal.Logger;
import com.otaliastudios.opengl.texture.GlTexture;

/**
 * The purpose of this class is to create a {@link Surface} associated to a certain GL texture.
 *
 * When {@link #drawFrame(Bitmap)} is called, this class will draw the bitmap onto that surface
 * so that the SurfaceTexture receives it and we can pass this to OpenGL as soon as it is available.
 */
public class VideoDecoderOutput {
    private static final String TAG = VideoDecoderOutput.class.getSimpleName();
    private static final Logger LOG = new Logger(TAG);

    private static final long NEW_IMAGE_TIMEOUT_MILLIS = 10000;

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private GlTextureProgram mProgram;
    private GlRect mDrawable;

    private float mScaleX = 1F;
    private float mScaleY = 1F;
    private int mRotation = 0;

    @GuardedBy("mFrameAvailableLock")
    private boolean mFrameAvailable;
    private final Object mFrameAvailableLock = new Object();

    /**
     * Creates an VideoDecoderOutput using the current EGL context (rather than establishing a
     * new one). Creates a Surface that can be passed to MediaCodec.configure().
     */
    public VideoDecoderOutput() {
        GlTexture texture = new GlTexture();
        mProgram = new GlTextureProgram();
        mProgram.setTexture(texture);
        mDrawable = new GlRect();

        // Even if we don't access the SurfaceTexture after the constructor returns, we
        // still need to keep a reference to it.  The Surface doesn't retain a reference
        // at the Java level, so if we don't either then the object can get GCed, which
        // causes the native finalizer to run.
        mSurfaceTexture = new SurfaceTexture(texture.getId());
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                LOG.v("New frame available");
                synchronized (mFrameAvailableLock) {
                    if (mFrameAvailable) {
                        throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
                    }
                    mFrameAvailable = true;
                    mFrameAvailableLock.notifyAll();
                }
            }
        });
    }

    /**
     * Sets the frame size, should be called before drawing anything.
     * @param width frame width
     * @param height frame height
     */
    public void setSize(int width, int height) {
        mSurfaceTexture.setDefaultBufferSize(width, height);
        mSurface = new Surface(mSurfaceTexture);
    }

    /**
     * Sets the frame scale along the two axes.
     * @param scaleX x scale
     * @param scaleY y scale
     */
    public void setScale(float scaleX, float scaleY) {
        mScaleX = scaleX;
        mScaleY = scaleY;
    }

    /**
     * Sets the desired frame rotation with respect
     * to its natural orientation.
     * @param rotation rotation
     */
    public void setRotation(int rotation) {
        mRotation = rotation;
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        mProgram.release();
        mSurface.release();
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        // W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        // mSurfaceTexture.release();
        mSurface = null;
        mSurfaceTexture = null;
        mDrawable = null;
        mProgram = null;
    }

    /**
     * Draws a Bitmap into our surface, then waits for it to be available to
     * the SurfaceTexture (not sure this is needed anymore), then renders
     * through OpenGL.
     */
    public void drawFrame(@NonNull Bitmap bitmap) {
        drawBitmap(bitmap);
        awaitNewFrame();
        renderNewFrame();
    }

    private void drawBitmap(@NonNull Bitmap bitmap) {
        Canvas canvas = mSurface.lockCanvas(null);
        if (bitmap.getWidth() != canvas.getWidth() || bitmap.getHeight() != canvas.getHeight()) {
            throw new RuntimeException("Unexpected width / height." +
                    " bw:" + bitmap.getWidth() +
                    " bh:" + bitmap.getHeight() +
                    " cw:" + canvas.getWidth() +
                    " ch:" + canvas.getHeight());
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
        mSurface.unlockCanvasAndPost(canvas);
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the VideoDecoderOutput object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    private void awaitNewFrame() {
        synchronized (mFrameAvailableLock) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us. Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameAvailableLock.wait(NEW_IMAGE_TIMEOUT_MILLIS);
                    if (!mFrameAvailable) {
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        // Latch the data.
        mSurfaceTexture.updateTexImage();
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    private void renderNewFrame() {
        mSurfaceTexture.getTransformMatrix(mProgram.getTextureTransform());
        // Invert the scale.
        float glScaleX = 1F / mScaleX;
        float glScaleY = 1F / mScaleY;
        // Compensate before scaling.
        float glTranslX = (1F - glScaleX) / 2F;
        float glTranslY = (1F - glScaleY) / 2F;
        Matrix.translateM(mProgram.getTextureTransform(), 0, glTranslX, glTranslY, 0);
        // Scale.
        Matrix.scaleM(mProgram.getTextureTransform(), 0, glScaleX, glScaleY, 1);
        // Apply rotation.
        Matrix.translateM(mProgram.getTextureTransform(), 0, 0.5F, 0.5F, 0);
        Matrix.rotateM(mProgram.getTextureTransform(), 0, mRotation, 0, 0, 1);
        Matrix.translateM(mProgram.getTextureTransform(), 0, -0.5F, -0.5F, 0);
        // Draw.
        mProgram.draw(mDrawable);
    }
}
