package com.otaliastudios.gif.source;

import android.content.Context;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.InputStream;

import androidx.annotation.NonNull;

/**
 * A {@link DataSource} backed by an Uri, possibly
 * a content:// uri.
 */
public class UriDataSource extends DefaultDataSource {

    @NonNull private Context context;
    @NonNull private Uri uri;

    public UriDataSource(@NonNull Context context, @NonNull Uri uri) {
        super(context);
        this.context = context.getApplicationContext();
        this.uri = uri;
    }

    @NonNull
    @Override
    protected InputStream openInputStream() {
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            if (stream == null) throw new FileNotFoundException();
            return stream;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
