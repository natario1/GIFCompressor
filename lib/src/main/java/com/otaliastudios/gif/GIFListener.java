package com.otaliastudios.gif;

import android.os.Handler;

import androidx.annotation.NonNull;

/**
 * Listeners for compression events. All the callbacks are called on the handler
 * specified with {@link GIFOptions.Builder#setListenerHandler(Handler)}.
 */
public interface GIFListener {
    /**
     * Called to notify progress.
     *
     * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
     */
    void onGIFCompressionProgress(double progress);

    /**
     * Called when compress completed. The success code can be either
     * {@link GIFCompressor#SUCCESS_COMPRESSED} or {@link GIFCompressor#SUCCESS_NOT_NEEDED}.
     *
     * @param successCode the success code
     */
    void onGIFCompressionCompleted(int successCode);

    /**
     * Called when compression was canceled.
     */
    void onGIFCompressionCanceled();

    /**
     * Called when compression failed.
     * @param exception the failure exception
     */
    void onGIFCompressionFailed(@NonNull Throwable exception);
}
