package com.otaliastudios.gif.source;

import android.content.Context;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;

import androidx.annotation.NonNull;

/**
 * A {@link DataSource} backed by a file descriptor.
 */
public class FileDescriptorDataSource extends DefaultDataSource {

    @NonNull
    private FileDescriptor descriptor;

    public FileDescriptorDataSource(@NonNull Context context, @NonNull FileDescriptor descriptor) {
        super(context);
        this.descriptor = descriptor;
    }

    @NonNull
    @Override
    protected InputStream openInputStream() {
        return new FileInputStream(descriptor);
    }
}
