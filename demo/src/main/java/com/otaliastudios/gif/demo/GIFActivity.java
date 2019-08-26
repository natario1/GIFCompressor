package com.otaliastudios.gif.demo;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.gif.GIFCompressor;
import com.otaliastudios.gif.GIFListener;
import com.otaliastudios.gif.GIFOptions;
import com.otaliastudios.gif.internal.Logger;
import com.otaliastudios.gif.sink.DataSink;
import com.otaliastudios.gif.sink.DefaultDataSink;
import com.otaliastudios.gif.strategy.DefaultStrategy;
import com.otaliastudios.gif.strategy.Strategy;
import com.otaliastudios.gif.strategy.size.AspectRatioResizer;
import com.otaliastudios.gif.strategy.size.FractionResizer;
import com.otaliastudios.gif.strategy.size.PassThroughResizer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


public class GIFActivity extends AppCompatActivity implements
        GIFListener,
        RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "DemoApp";
    private static final Logger LOG = new Logger(TAG);

    private static final String FILE_PROVIDER_AUTHORITY = "com.otaliastudios.gif.demo.fileprovider";
    private static final int REQUEST_CODE_PICK = 1;
    private static final int PROGRESS_BAR_MAX = 1000;

    private RadioGroup mFrameRateGroup;
    private RadioGroup mResolutionGroup;
    private RadioGroup mAspectRatioGroup;
    private RadioGroup mRotationGroup;
    private RadioGroup mSpeedGroup;

    private ProgressBar mProgressView;
    private TextView mButtonView;

    private boolean mIsCompressing;
    private Future<Void> mCompressionFuture;
    private Uri mInputUri1;
    private Uri mInputUri2;
    private Uri mInputUri3;
    private File mOutputFile;
    private long mStartTime;
    private Strategy mStrategy;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.setLogLevel(Logger.LEVEL_VERBOSE);
        setContentView(R.layout.activity_gif);

        mButtonView = findViewById(R.id.button);
        mButtonView.setOnClickListener(v -> {
            if (!mIsCompressing) {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("image/gif")
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), REQUEST_CODE_PICK);
            } else {
                mCompressionFuture.cancel(true);
            }
        });
        setIsCompressing(false);

        mProgressView = findViewById(R.id.progress);
        mProgressView.setMax(PROGRESS_BAR_MAX);

        mFrameRateGroup = findViewById(R.id.frames);
        mResolutionGroup = findViewById(R.id.resolution);
        mAspectRatioGroup = findViewById(R.id.aspect);
        mRotationGroup = findViewById(R.id.rotation);
        mSpeedGroup = findViewById(R.id.speed);

        mFrameRateGroup.setOnCheckedChangeListener(this);
        mResolutionGroup.setOnCheckedChangeListener(this);
        mAspectRatioGroup.setOnCheckedChangeListener(this);
        syncParameters();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        syncParameters();
    }

    private void syncParameters() {
        int frames;
        switch (mFrameRateGroup.getCheckedRadioButtonId()) {
            case R.id.frames_24: frames = 24; break;
            case R.id.frames_30: frames = 30; break;
            case R.id.frames_60: frames = 60; break;
            default: frames = DefaultStrategy.DEFAULT_FRAME_RATE;
        }
        float fraction;
        switch (mResolutionGroup.getCheckedRadioButtonId()) {
            case R.id.resolution_half: fraction = 0.5F; break;
            case R.id.resolution_third: fraction = 1F / 3F; break;
            default: fraction = 1F;
        }
        float aspectRatio;
        switch (mAspectRatioGroup.getCheckedRadioButtonId()) {
            case R.id.aspect_169: aspectRatio = 16F / 9F; break;
            case R.id.aspect_43: aspectRatio = 4F / 3F; break;
            case R.id.aspect_square: aspectRatio = 1F; break;
            default: aspectRatio = 0F;
        }
        mStrategy = new DefaultStrategy.Builder()
                .addResizer(aspectRatio > 0 ? new AspectRatioResizer(aspectRatio) : new PassThroughResizer())
                .addResizer(new FractionResizer(fraction))
                .frameRate(frames)
                .build();
    }

    private void setIsCompressing(boolean isCompressing) {
        mIsCompressing = isCompressing;
        mButtonView.setText(mIsCompressing ? "Cancel Compression" : "Select GIF(s) & Compress");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK
                && resultCode == RESULT_OK
                && data != null) {
            if (data.getData() != null) {
                mInputUri1 = data.getData();
                mInputUri2 = null;
                mInputUri3 = null;
                transcode();
            } else if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                mInputUri1 = clipData.getItemAt(0).getUri();
                mInputUri2 = clipData.getItemCount() >= 2 ? clipData.getItemAt(1).getUri() : null;
                mInputUri3 = clipData.getItemCount() >= 3 ? clipData.getItemAt(2).getUri() : null;
                transcode();
            }
        }
    }

    private void transcode() {
        // Create a temporary file for output.
        try {
            File outputDir = new File(getExternalFilesDir(null), "outputs");
            //noinspection ResultOfMethodCallIgnored
            outputDir.mkdir();
            mOutputFile = File.createTempFile("transcode_test", ".mp4", outputDir);
            LOG.i("Transcoding into " + mOutputFile);
        } catch (IOException e) {
            LOG.e("Failed to create temporary file.", e);
            Toast.makeText(this, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
            return;
        }

        int rotation;
        switch (mRotationGroup.getCheckedRadioButtonId()) {
            case R.id.rotation_90: rotation = 90; break;
            case R.id.rotation_180: rotation = 180; break;
            case R.id.rotation_270: rotation = 270; break;
            default: rotation = 0;
        }

        float speed;
        switch (mSpeedGroup.getCheckedRadioButtonId()) {
            case R.id.speed_05x: speed = 0.5F; break;
            case R.id.speed_2x: speed = 2F; break;
            default: speed = 1F;
        }

        // Launch the transcoding operation.
        mStartTime = SystemClock.uptimeMillis();
        setIsCompressing(true);
        DataSink sink = new DefaultDataSink(mOutputFile.getAbsolutePath());
        GIFOptions.Builder builder = GIFCompressor.into(sink);
        if (mInputUri1 != null) builder.addDataSource(this, mInputUri1);
        if (mInputUri2 != null) builder.addDataSource(this, mInputUri2);
        if (mInputUri3 != null) builder.addDataSource(this, mInputUri3);
        mCompressionFuture = builder.setListener(this)
                .setStrategy(mStrategy)
                .setRotation(rotation)
                .setSpeed(speed)
                .compress();
    }

    @Override
    public void onGIFCompressionProgress(double progress) {
        if (progress < 0) {
            mProgressView.setIndeterminate(true);
        } else {
            mProgressView.setIndeterminate(false);
            mProgressView.setProgress((int) Math.round(progress * PROGRESS_BAR_MAX));
        }
    }

    @Override
    public void onGIFCompressionCompleted() {
        LOG.w("Compression took " + (SystemClock.uptimeMillis() - mStartTime) + "ms");
        onCompressionFinished(true, "Compressed video placed on " + mOutputFile);
        File file = mOutputFile;
        String type = "video/mp4";
        Uri uri = FileProvider.getUriForFile(GIFActivity.this,
                FILE_PROVIDER_AUTHORITY, file);
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, type)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    @Override
    public void onGIFCompressionCanceled() {
        onCompressionFinished(false, "GIFCompressor canceled.");
    }

    @Override
    public void onGIFCompressionFailed(@NonNull Throwable exception) {
        onCompressionFinished(false, "GIFCompressor error occurred. " + exception.getMessage());
    }

    private void onCompressionFinished(boolean isSuccess, @NonNull String toastMessage) {
        mProgressView.setIndeterminate(false);
        mProgressView.setProgress(isSuccess ? PROGRESS_BAR_MAX : 0);
        setIsCompressing(false);
        Toast.makeText(GIFActivity.this, toastMessage, Toast.LENGTH_LONG).show();
    }

}
