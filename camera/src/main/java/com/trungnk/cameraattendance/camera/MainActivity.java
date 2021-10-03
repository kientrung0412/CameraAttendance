package com.trungnk.cameraattendance.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.FileUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.CompositePermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.prv_camera)
    PreviewView prvCamera;
    @BindView(R.id.iv_shutter)
    ImageView ivShutter;
    @BindView(R.id.iv_flash)
    ImageView ivFlash;
    @BindView(R.id.iv_connect_thermometer)
    ImageView ivConnectThermometer;
    @BindView(R.id.iv_change_camera)
    ImageView ivChangeCamera;
    @BindView(R.id.tv_title)
    TextView tvTitle;

    private ListenableFuture<ProcessCameraProvider> providerListenableFuture;
    private ProcessCameraProvider processCameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;

    private boolean flashOn = false;
    private boolean mainCamera = false;
    private boolean connectThermometer = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        askPermission();
    }

    private void askPermission() {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                bindView();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        };

        PermissionListener snackbarPermissionListener = SnackbarOnDeniedPermissionListener.Builder
                .with(prvCamera, "Ứng dụng cần quyền để hoạt động bình thường")
                .withOpenSettingsButton("Cài đặt")
                .withDuration(3000)
                .build();

        PermissionListener compositePermissionListener = new CompositePermissionListener(permissionListener, snackbarPermissionListener);


        Dexter.withContext(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(compositePermissionListener)
                .check();
    }

    private void bindView() {
        providerListenableFuture = ProcessCameraProvider.getInstance(this);
        providerListenableFuture.addListener(
                () -> {
                    try {
                        processCameraProvider = providerListenableFuture.get();
                        startCamera();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    private void startCamera() {
        processCameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(mainCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(prvCamera.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        bindViewFlash();
        bindViewConnectThermometer();
        bindViewCamera();
    }

    private boolean supportFlash() {
        if (camera == null) return false;
        return camera.getCameraInfo().hasFlashUnit();
    }

    private void bindViewFlash() {
        ivFlash.setVisibility(supportFlash() ? View.VISIBLE : View.GONE);
        Drawable drawable = flashOn ?
                AppCompatResources.getDrawable(this, R.drawable.ic_baseline_flash_off_24) :
                AppCompatResources.getDrawable(this, R.drawable.ic_baseline_flash_on_24);

        ivFlash.setImageDrawable(drawable);
    }

    private void bindViewConnectThermometer() {
//        ivConnectThermometer.setVisibility(supportFlash() ? View.VISIBLE : View.GONE);
        Drawable drawable = connectThermometer ?
                AppCompatResources.getDrawable(this, R.drawable.ic_thermometer_connect) :
                AppCompatResources.getDrawable(this, R.drawable.ic_thermometer);

        ivConnectThermometer.setImageDrawable(drawable);
    }

    private void bindViewCamera() {
//        ivFlash.setVisibility(supportFlash() ? View.VISIBLE : View.GONE);
        Drawable drawable = mainCamera ?
                AppCompatResources.getDrawable(this, R.drawable.ic_baseline_camera_rear_24) :
                AppCompatResources.getDrawable(this, R.drawable.ic_baseline_camera_front_24);

        ivChangeCamera.setImageDrawable(drawable);
    }

    @OnClick(R.id.iv_flash)
    void onClickFlash() {
        if (camera == null) return;
        flashOn = !flashOn;
        camera.getCameraControl().enableTorch(flashOn);
        bindViewFlash();
    }

    @OnClick(R.id.iv_shutter)
    void onClickShutter() {
        String filename = String.format("getFilesDir()/%s.jpg", System.currentTimeMillis());
        File file = new File(filename);
        FileUtils.createOrExistsFile(file);

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions
                        .Builder(file)
                        .build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "Thành công", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                    }

                }
        );
    }

    @OnClick(R.id.iv_change_camera)
    void onClickChangeCamera() {
        mainCamera = !mainCamera;
        startCamera();
    }

    @OnClick(R.id.iv_connect_thermometer)
    void onClickConnectThermometer() {

    }

}