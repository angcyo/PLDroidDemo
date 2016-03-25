package com.jemi.androidh264streamdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;


public class MainActivity extends Activity implements SurfaceHolder.Callback, PreviewCallback {
    private final static String TAG = MainActivity.class.getSimpleName();

    private final static String SP_CAM_WIDTH = "cam_width";
    private final static String SP_CAM_HEIGHT = "cam_height";
    private final static String SP_DEST_IP = "dest_ip";
    private final static String SP_DEST_PORT = "dest_port";

    private final static int DEFAULT_FRAME_RATE = 15;
    private final static int DEFAULT_BIT_RATE = 500000;

    public static boolean isSend = false;
    protected Object lock = new Object();
    Camera camera;
    SurfaceHolder previewHolder;
    byte[] previewBuffer;
    boolean isStreaming = false;
    AvcEncoder encoder;
    DatagramSocket udpSocket;
    InetAddress address;
    int port;
    ArrayList<byte[]> encDataList = new ArrayList<byte[]>();
    ArrayList<Integer> encDataLengthList = new ArrayList<Integer>();
    Runnable senderRun = new Runnable() {
        @Override
        public void run() {
            while (isStreaming) {
                boolean empty = false;
                byte[] encData = null;

                synchronized (encDataList) {
                    if (encDataList.size() == 0) {
                        empty = true;
                    } else
                        encData = encDataList.remove(0);
                }
                if (empty) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                try {
                    DatagramPacket packet = new DatagramPacket(encData, encData.length, address, port);
                    udpSocket.send(packet);

                    Log.e("udpSocket-->", encData.length + " " + address + " " + port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //TODO:
        }
    };
    SaveFileThread saveFileThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_main);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        isSend = false;

        this.findViewById(R.id.btnCamSize).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSettingsDlg();
                    }
                });

        this.findViewById(R.id.btnStream).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isStreaming) {
                            ((Button) v).setText("Stream");
                            stopStream();
                        } else {
                            showStreamDlg();
                        }
                    }
                });

        SurfaceView svCameraPreview = (SurfaceView) this.findViewById(R.id.svCameraPreview);
        this.previewHolder = svCameraPreview.getHolder();
        this.previewHolder.addCallback(this);


        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        ((ImageView) findViewById(R.id.image)).setImageBitmap(bitmap);

        findViewById(R.id.clean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmap.recycle();
                ((ImageView) findViewById(R.id.image)).setImageBitmap(null);
            }
        });
    }

    @Override
    protected void onPause() {
        this.stopStream();


        if (encoder != null)
            encoder.close();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings)
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        Log.e("onPreviewFrame-->", data.length + " " + this.previewBuffer.length);

        this.camera.addCallbackBuffer(this.previewBuffer);

        if (this.isStreaming) {
            if (this.encDataLengthList.size() > 100) {
                Log.e(TAG, "OUT OF BUFFER");
                return;
            }

            byte[] encData = this.encoder.offerEncoder(data);
            byte[] src;
//            synchronized (lock) {
//                src = new byte[encData.length - 4];
//                System.arraycopy(encData, 4, src, 0, src.length);
//            }

            if (encData.length > 0 && !isSend) {
                synchronized (this.encDataList) {

//                    this.encDataList.add(src);

                    this.encDataList.add(encData);

//                    isSend = true;
//                    if (isSend) {
                        if (saveFileThread == null) {
                            saveFileThread = new SaveFileThread();
                            saveFileThread.start();
                        }
                        saveFileThread.saveData(encData);
                    }
//                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    private void startStream(String ip, int port) {
        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        int width = sp.getInt(SP_CAM_WIDTH, 0);
        int height = sp.getInt(SP_CAM_HEIGHT, 0);

        this.encoder = new AvcEncoder();
        this.encoder.init(width, height, DEFAULT_FRAME_RATE, DEFAULT_BIT_RATE);

        try {
            this.udpSocket = new DatagramSocket();
            this.address = InetAddress.getByName(ip);
            this.port = port;
        } catch (SocketException e) {
            // TODO Auto-generated catch block  
            e.printStackTrace();
            return;
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        sp.edit().putString(SP_DEST_IP, ip).commit();
        sp.edit().putInt(SP_DEST_PORT, port).commit();

        this.isStreaming = true;
        Thread thrd = new Thread(senderRun);
        thrd.start();

        ((Button) this.findViewById(R.id.btnStream)).setText("Stop");
        this.findViewById(R.id.btnCamSize).setEnabled(false);
    }

    private void stopStream() {
        this.isStreaming = false;

        if (this.encoder != null)
            this.encoder.close();
        this.encoder = null;

        this.findViewById(R.id.btnCamSize).setEnabled(true);
    }

    private void startCamera() {
        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        int width = sp.getInt(SP_CAM_WIDTH, 0);
        int height = sp.getInt(SP_CAM_HEIGHT, 0);
        if (width == 0) {
            Camera tmpCam = Camera.open();
            Camera.Parameters params = tmpCam.getParameters();
            final List<Size> prevSizes = params.getSupportedPreviewSizes();
            int i = prevSizes.size() - 1;
            width = prevSizes.get(i).width;
            height = prevSizes.get(i).height;
            sp.edit().putInt(SP_CAM_WIDTH, width).commit();
            sp.edit().putInt(SP_CAM_HEIGHT, height).commit();
            tmpCam.release();
            tmpCam = null;
        }

        this.previewHolder.setFixedSize(width, height);

        int stride = (int) Math.ceil(width / 16.0f) * 16;
        int cStride = (int) Math.ceil(width / 32.0f) * 16;
        final int frameSize = stride * height;
        final int qFrameSize = cStride * height / 2;

        this.previewBuffer = new byte[frameSize + qFrameSize * 2];
//        this.previewBuffer = new byte[3110400];

        try {
            camera = Camera.open();
            camera.setPreviewDisplay(this.previewHolder);
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

            params.setPreviewSize(width, height);
            params.setPreviewFormat(ImageFormat.YV12);
            camera.setParameters(params);
            camera.addCallbackBuffer(previewBuffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
        } catch (IOException e) {
            //TODO:
        } catch (RuntimeException e) {
            //TODO:
        }
    }

    private void stopCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void showStreamDlg() {
        LayoutInflater inflater = this.getLayoutInflater();
        View content = inflater.inflate(R.layout.stream_dlg_view, null);

        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        String ip = sp.getString(SP_DEST_IP, "");
        int port = sp.getInt(SP_DEST_PORT, -1);
        if (ip.length() > 0) {
            EditText etIP = (EditText) content.findViewById(R.id.etIP);
            etIP.setText(ip);
            EditText etPort = (EditText) content.findViewById(R.id.etPort);
            etPort.setText(String.valueOf(port));
        }

        AlertDialog.Builder dlgBld = new AlertDialog.Builder(this);
        dlgBld.setTitle(R.string.app_name);
        dlgBld.setView(content);
        dlgBld.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText etIP = (EditText) ((AlertDialog) dialog).findViewById(R.id.etIP);
                        EditText etPort = (EditText) ((AlertDialog) dialog).findViewById(R.id.etPort);
                        String ip = etIP.getText().toString();
                        int port = Integer.valueOf(etPort.getText().toString());
                        if (ip.length() > 0 && (port >= 0 && port <= 65535)) {
                            startStream(ip, port);
                        } else {
                            //TODO:
                        }
                    }
                });
        dlgBld.setNegativeButton(android.R.string.cancel, null);
        dlgBld.show();
    }

    private void showSettingsDlg() {
        Camera.Parameters params = camera.getParameters();
        final List<Size> prevSizes = params.getSupportedPreviewSizes();
        String[] choiceStrItems = new String[prevSizes.size()];
        ArrayList<String> choiceItems = new ArrayList<String>();
        for (Size s : prevSizes) {
            choiceItems.add(s.width + "x" + s.height);
        }
        choiceItems.toArray(choiceStrItems);

        AlertDialog.Builder dlgBld = new AlertDialog.Builder(this);
        dlgBld.setTitle(R.string.app_name);
        dlgBld.setSingleChoiceItems(choiceStrItems, 0, null);
        dlgBld.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int pos = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        Size s = prevSizes.get(pos);
                        SharedPreferences sp = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                        sp.edit().putInt(SP_CAM_WIDTH, s.width).commit();
                        sp.edit().putInt(SP_CAM_HEIGHT, s.height).commit();

                        stopCamera();
                        startCamera();
                    }
                });
        dlgBld.setNegativeButton(android.R.string.cancel, null);
        dlgBld.show();
    }

    /**
     * 保存文件的线程
     */
    class SaveFileThread extends Thread {
        private Vector<byte[]> saveData;
        private volatile int length;

        public SaveFileThread() {
            this.saveData = new Vector<>();
        }

        private String getSaveFileName() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH-mm-ss-SSS");
            String format = simpleDateFormat.format(new Date());
            return format;
//            return (format + ".png");
        }

        public void saveData(byte[] data) {
            saveData.add(data);
        }

        @Override
        public void run() {
            while (isStreaming) {
                if (!saveData.isEmpty()) {
                    byte[] data = saveData.remove(0);
                    try {
                        File fileName = new File(getExternalCacheDir() + File.separator + getSaveFileName());
                        FileOutputStream outputStream = new FileOutputStream(fileName, true);

                        outputStream.write(data, 0, data.length);
                        outputStream.close();
                        Log.e(getId() + " 保存至:-->" + data.length, fileName.getAbsolutePath());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
