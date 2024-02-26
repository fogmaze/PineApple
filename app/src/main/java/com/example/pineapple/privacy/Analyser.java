package com.example.pineapple.privacy;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Analyser implements ImageReader.OnImageAvailableListener {
    private final String TAG = "Analyser";
    private Context context;
    private Long currentTimeMillis = null;
    private ImageProcess processer = new ImageProcess();

    public Analyser(Context context) {
        this.context = context;
        this.currentTimeMillis = System.currentTimeMillis();
    }
    private Object lock = new Object();
    private Object lock2 = new Object();
    private Socket socket = null;
    private byte[] last_jpeg_bytes = null;

    public void setConnection(Socket s) {
        Log.i(TAG, "setConnection");
        synchronized (lock) {
            this.socket = s;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (socket != null && !socket.isClosed() && last_jpeg_bytes != null) {
                        try {
                            OutputStream os = socket.getOutputStream();
                            Log.i(TAG, "byte size " + last_jpeg_bytes.length);
                            // write image size
                            os.write(ByteBuffer.allocate(4).putInt(last_jpeg_bytes.length).array());
                            os.write(last_jpeg_bytes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }
    int i = 0;
    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            return;
        }
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();
        // encode to jpeg code
        ByteBuffer[] buffers = new ByteBuffer[3];
        int[] i_buffers = new int[3];
        byte[][] byte_planes = new byte[3][];
        for (int i = 0; i < 3; i++) {
            buffers[i] = image.getPlanes()[i].getBuffer();
            i_buffers[i] = buffers[i].remaining();
            byte_planes[i] = new byte[i_buffers[i]];
            buffers[i].get(byte_planes[i]);
        }

        byte[] yuv_bytes = mergeBytes(byte_planes[0], byte_planes[2]);

        byte[] jpeg_bytes = convertYuv2Jpg(yuv_bytes, imageWidth, imageHeight, 80);
        //Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg_bytes, 0, jpeg_bytes.length);
        //imageView.setImageBitmap(bitmap);
        synchronized (lock2) {
            last_jpeg_bytes = jpeg_bytes;
        }



        //Log.i(TAG, "fps:" + 1000 / (System.currentTimeMillis() - this.currentTimeMillis));
        this.currentTimeMillis = System.currentTimeMillis();
        image.close();
    }

    private static byte[] convertYuv2Jpg(byte[] data, int width, int height, int quality) {
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, width, height), quality, stream);
        return stream.toByteArray();
    }

    private static byte[] mergeBytes(byte[]... bytes) {
        int length_byte = 0;
        for (byte[] b : bytes) {
            length_byte += b.length;
        }
        byte[] result = new byte[length_byte];
        int index = 0;
        for (byte[] b : bytes) {
            System.arraycopy(b, 0, result, index, b.length);
            index += b.length;
        }
        return result;
    }
}
