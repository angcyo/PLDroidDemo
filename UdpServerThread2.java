package com.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by angcyo 2016-03-18 01:01.
 */
public class UdpServerThread2 extends Thread {

    public static final PrintStream p = System.out;
    private static final int DATA_LEN = 65535;
    public static String serverIp = "192.168.124.78";
    public static int serverPort = 8919;
    private static Object lock = new Object();
    private final SaveFileThread fileThread;
    byte[] data;
    DatagramSocket socket;
    private boolean isExit = false;

    private UdpServerThread2() throws SocketException {
        socket = new DatagramSocket(serverPort);

        fileThread = new SaveFileThread();
        fileThread.start();
    }

    public static void receive() throws SocketException {
        new UdpServerThread2().start();
    }

    public static void main(String... args) {
        try {
            receive();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!isExit) {
            data = new byte[DATA_LEN];
            DatagramPacket packet = new DatagramPacket(data, DATA_LEN);
            try {

                p.println("等待中...");
                socket.receive(packet);
//                String s = new String(data);
//                p.println("收到数据包:" + s + " 大小:" + packet.getLength() + " 字节" + " 长度:" + s.length());
                p.println("收到数据包大小:" + packet.getLength() + " 字节");
                fileThread.saveData(data, packet.getLength());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存文件的线程
     */
    class SaveFileThread extends Thread {
        private Vector<byte[]> saveData;
        private volatile int length;
        private boolean isAppend = true;

        public SaveFileThread() {
            this.saveData = new Vector<>();

            File png = new File("png");
            if (!png.exists()) {
                png.mkdirs();
            }

        }

        private String getSaveFileName() {
            if (isAppend) {
                return "2016-3-24";
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH-mm-ss-SSS");
            String format = simpleDateFormat.format(new Date());
            return format;
//            return (format + ".png");
        }

        public void saveData(byte[] data, int len) {
            saveData.add(data);
            length = len;
        }

        @Override
        public void run() {
            while (!isExit) {
                if (!saveData.isEmpty()) {
                    byte[] data = saveData.remove(0);
                    try {
                        String fileName = getSaveFileName();
                        FileOutputStream outputStream = new FileOutputStream(new File("png" + File.separator + fileName), isAppend);

                        outputStream.write(data, 0, length);
                        outputStream.close();
                        p.println("保存至:-->" + fileName);
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
