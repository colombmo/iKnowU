/*
 * Copyright(C) Seiko Epson Corporation 2016. All rights reserved.
 *
 * Warranty Disclaimers.
 * You acknowledge and agree that the use of the software is at your own risk.
 * The software is provided "as is" and without any warranty of any kind.
 * Epson and its licensors do not and cannot warrant the performance or results
 * you may obtain by using the software.
 * Epson and its licensors make no warranties, express or implied, as to non-infringement,
 * merchantability or fitness for any particular purpose.
 */

package com.epson.moverio.bt2000.sample.samplecamerapreview;

import android.content.res.AssetFileDescriptor;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int[][] RESOLUTIONS = {
            {640, 480},
            {1280, 720},
            {1920, 1080},
    };

    public static String ipAddress = "192.168.1.103";

    private int mResolutionIndex;

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private DrawView dw;


    private boolean sendpic = true;
    private int iter = 0;

    private boolean locked = false;
    private boolean played = false;

    MediaPlayer player = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //The MOVERIO Pro BT-2000 is based on Android 4.0 Tablet UI. This specification does not
        //normally allow for full application display; however, you can use the following code
        //to enable full display by specifying a unique flag.
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= winParams.flags |= 0x80000000;
        win.setAttributes(winParams);

        setContentView(R.layout.activity_main);
        dw = (DrawView) findViewById(R.id.drawView);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResolutionIndex = 0;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setVideoStabilization(true);
        parameters.setEpsonCameraMode((Camera.Parameters.EPSON_CAMERA_MODE_SINGLE_THROUGH_VGA));
        parameters.setPreviewFpsRange(60000, 60000);
        int[] resolution = getResolution();
        parameters.setPreviewSize(resolution[0], resolution[1]);
        //parameters.setPreviewFormat(ImageFormat.JPEG);

        mCamera.setParameters(parameters);

        mCamera.setPreviewCallback(mCameraCallback);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private int[] getResolution() {
        if (RESOLUTIONS.length <= mResolutionIndex) {
            mResolutionIndex = 0;
        } else if (mResolutionIndex < 0) {
            mResolutionIndex = RESOLUTIONS.length - 1;
        }
        return RESOLUTIONS[mResolutionIndex];
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me) {
        if(me.getActionMasked() == MotionEvent.ACTION_DOWN){}

        return super.dispatchTouchEvent(me);
    }

    Camera.PreviewCallback mCameraCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(sendpic) {
                sendBytes(data);
            }
            if (iter==100){
                iter=0;
                played = false;
            }
            iter++;
        }
    };

    private void sendBytes(final byte[] data) {
        if (/*data.length == 460800 && iter%5 == 0 &&*/ !locked) {
            locked = true;
            final Handler handler = new Handler();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int[] resolution = getResolution();
                        //Replace below IP with the IP of that device in which server socket open.
                        //If you change port then change the port number in the server side code also.
                        Socket s = new Socket();
                        s.setSoTimeout(2000);
                        s.connect(new InetSocketAddress(ipAddress, 9002), 100);
                        //Socket s = new Socket("134.21.161.48", 9002);
                        OutputStream out = s.getOutputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));

                        /**
                         * TODO: Uncomment this part for faster reaction time, but lower resolution ( and distance!!)
                         */
                        long t1 = (new Date()).getTime();

                        ByteArrayOutputStream fos = new ByteArrayOutputStream();

                        // Compress image to jpeg and send with its length
                        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, resolution[0], resolution[1], null);
                        yuvImage.compressToJpeg(new Rect(0, 0, resolution[0], resolution[1]), 60, fos);
                        byte[] compressed = fos.toByteArray();
                        fos.close();

                        Log.d("Compressed image size", ""+ compressed.length);
                        out.write((""+compressed.length).getBytes());
                        out.flush();
                        out.write(compressed);
                        out.flush();


                        /**
                         * TODO: Uncomment this part for increased resolution and distance
                         */
/*
                        byte[] argbdata = new byte[resolution[0]*resolution[1]*4];

                        YUV_NV21_TO_RGB(argbdata, data, resolution[0], resolution[1]);

                        ByteArrayOutputStream blob = new ByteArrayOutputStream();
                        Bitmap bmp = Bitmap.createBitmap(resolution[0], resolution[1], Bitmap.Config.ARGB_8888);
                        ByteBuffer buffer = ByteBuffer.wrap(argbdata);
                        bmp.copyPixelsFromBuffer(buffer);

                        //Bitmap scaled = Bitmap.createScaledBitmap(bmp, 533, 400, false);

                        //Bitmap bitmap = BitmapFactory.decodeByteArray(argbdata, 0, resized.length);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 30, blob);
                        byte[] send = blob.toByteArray();

                        Log.d("Compressed image size", ""+ send.length);
                        out.write((""+send.length).getBytes());
                        out.flush();
                        out.write(send);
                        out.flush();
*/
                        ///////////////////////////////////////////////////////////////

                        Log.d("Compression", "Time: "+((new Date()).getTime()-t1));
                        Log.d("Message", "sent");
                        long start = (new Date()).getTime();

                        String str;
                        if ((str = br.readLine()) != null && str.length() > 2) {
                            Log.d("socket", "received: " + str);

                            // Display names
                            String[] parts = str.split(",");
                            final int[] rects = new int[parts.length/5*4];
                            final String[] names = new String[parts.length/5];
                            for(int i=0; i*5<parts.length; i++){
                                //person_det+=parts[5*i]+", ";
                                names[i] = parts[5*i];
                                rects[i*4] = 2*Integer.parseInt(parts[5*i+1]);
                                rects[i*4+1] = 2*Integer.parseInt(parts[5*i+2]);
                                rects[i*4+2] = 2*Integer.parseInt(parts[5*i+3]);
                                rects[i*4+3] = 2*Integer.parseInt(parts[5*i+4]);
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dw.update(rects, names);
                                }
                            });

                            for(String n:names){if (n.length() > 1) playSound("pascale"); break;}
                        }else {
                            Log.d("socket","nothing received");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dw.update(new int[]{}, new String[]{});
                                }
                            });
                        }
                        Log.d("message", "answer received in "+((new Date()).getTime()-start)+ " ms");
                        out.close();
                        br.close();
                        s.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                    locked = false;
                }
            });
            thread.start();
        }
    }

    public static void YUV_NV21_TO_RGB(byte[] argb, byte[] yuv, int width, int height) {
        final int frameSize = width * height;

        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                argb[a++] = (byte)r;
                argb[a++] = (byte)g;
                argb[a++] = (byte)b;
                argb[a++] = (byte)255;
//                argb[a++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    public void playSound(String em){
        if (!played) {
            iter = 0;
            // Say recognized face
            player = new MediaPlayer();
            AssetFileDescriptor afd = null;
            try {
                afd = getAssets().openFd(em + ".mp3");
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                player.prepare();
                player.start();
                Log.d("ExMP", "Sound played");
                played = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}