package com.github.libffmpeg;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

class Util {

    static boolean isDebug(Context context) {
        return (0 != (context.getApplicationContext().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }

    static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    static void close(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    static String convertInputStreamToString(InputStream inputStream) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            String str;
            StringBuilder sb = new StringBuilder();
            while ((str = r.readLine()) != null) {
                sb.append(str);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e("error converting input stream to string", e);
        }
        return null;
    }

    static void destroyProcess(Process process) {
        if (process != null)
            process.destroy();
    }

    static boolean killAsync(AsyncTask asyncTask) {
        return asyncTask != null && !asyncTask.isCancelled() && asyncTask.cancel(true);
    }

    static boolean isProcessCompleted(Process process) {
        try {
            if (process == null) return true;
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            // do nothing
        }
        return false;
    }

    public static double parseFpsIfPresent(String message) {
        //Stream #0:0(eng): Video: h264 (Constrained Baseline) (avc1 / 0x31637661), yuv420p, 1920x1080, 12091 kb/s, SAR 65536:65536 DAR 16:9, 30.22 fps, 30.33 tbr, 90k tbn, 180k tbc (default)

        if (message.trim().startsWith("Stream #") && message.contains("Video")) {
            int fpsEnd = message.indexOf("fps,");
            int fpsStart = fpsEnd != -1 ? message.substring(0, fpsEnd).lastIndexOf(",") + 1 : -1;

            if (fpsEnd != -1 && fpsStart != -1) {
                String fpsString = message.substring(fpsStart, fpsEnd).trim();

                try {
                    return Double.valueOf(fpsString);
                } catch (NumberFormatException e) {
                    android.util.Log.e("MetaParser", "Cannot obtain fps message: " + message);
                }
            }
        }
        return -1;
    }

    public static long parseDurationIfPresent(String message) {
        //Duration: 00:00:11.76, start: 0.000000, bitrate: 12405 kb/s
        int durationIndex = message.indexOf("Duration: ");
        int startIndex = message.indexOf(", start:");
        if (durationIndex >= 0 && startIndex >= 0) {
            String durationStr = message.substring(durationIndex + 10, startIndex);

            return toMillis(durationStr);
        }
        return -1;
    }

    public static long getProcessTime(String message) {
        //frame=   83 fps=6.4 q=25.0 size=    5568kB time=00:00:03.05 bitrate=14947.2kbits/s
        int timeIndex = message.indexOf("time=");
        int bitrateIndex = message.indexOf(" bitrate=");

        if (timeIndex >= 0 && bitrateIndex >= 0) {
            String timeStr = message.substring(timeIndex + 5, bitrateIndex);

            return toMillis(timeStr);
        }

        return -1;
    }

    public static long toMillis(String time) {
        // 00:00:11.76
        String[] split = time.split(":");

        int hours = Integer.valueOf(split[0]);
        int minutes = Integer.valueOf(split[1]);
        double seconds = Double.valueOf(split[2]);

        return (long) (1000 * (60 * 60 * hours + 60 * minutes + seconds));
    }
}
