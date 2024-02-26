package com.example.pineapple.privacy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.net.Socket;
import java.util.Collections;

public class CameraHolder {
    final String TAG = "CameraHolder";
    public CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private Size mSize;
    private ImageReader mImageReader;
    private Context context;
    private Long exposureTime = null;
    private Integer iso = null;


    public CameraHolder(Context context, Size size, ImageReader.OnImageAvailableListener listener) {
        mSize = size;
        this.context = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mImageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.YUV_420_888, 4);
        mImageReader.setOnImageAvailableListener(listener, null);
    }

    public void stop() {
        try {
            mCaptureSession.stopRepeating();
            mCaptureSession.close();
            mCameraDevice.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void startCapture() {
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    try {
                        mCameraDevice = cameraDevice;
                        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);


                        //builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                        //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
                        // set iso
                        //builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
                        // set auto focus
                        //builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // set fps
                        //builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(30, 30));

                        builder.addTarget(mImageReader.getSurface());
                        //builder.addTarget(surfaceHolder.getSurface());
                        cameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                mCaptureSession = cameraCaptureSession;
                                try {
                                    mCaptureSession.setRepeatingRequest(builder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            }
                        }, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {

                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {

                }
            };

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.i(TAG, "permission not given");
                return;
            }
            String camId = null;
            for (String id : cameraIdList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) {
                    continue;
                }
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    camId = id;
                    break;
                }
            }
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(camId);
            Range<Long> exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            Log.i(TAG, "exposure range: " + exposureRange.getLower() + " - " + exposureRange.getUpper());
            exposureTime = exposureRange.getLower() + (exposureRange.getUpper() - exposureRange.getLower()) / 30;
            //exposureTime = 6000000L;
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            for (Size size : sizes) {
                Log.i(TAG, "size: " + size.getWidth() + "x" + size.getHeight());
            }
            // get iso range
            Range<Integer> isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            Log.i(TAG, "iso range: " + isoRange.getLower() + " - " + isoRange.getUpper());
            iso = isoRange.getUpper();

            mCameraManager.openCamera(cameraIdList[0], callback, null);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mCameraDevice != null) {
                            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                            builder.addTarget(mImageReader.getSurface());
                            mCaptureSession.setRepeatingRequest(builder.build(), null, null);
                        }
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
