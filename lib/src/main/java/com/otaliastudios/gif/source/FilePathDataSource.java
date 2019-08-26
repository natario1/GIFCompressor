package com.otaliastudios.gif.source;

import android.content.Context;

import com.otaliastudios.gif.internal.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import androidx.annotation.NonNull;

/**
 * A {@link DataSource} backed by a file absolute path.
 */
public class FilePathDataSource extends DefaultDataSource {
    private static final String TAG = FilePathDataSource.class.getSimpleName();
    private static final Logger LOG = new Logger(TAG);

    private String path;

    public FilePathDataSource(@NonNull Context context, @NonNull String path) {
        super(context);
        this.path = path;
    }

    @NonNull
    @Override
    protected InputStream openInputStream() {
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
