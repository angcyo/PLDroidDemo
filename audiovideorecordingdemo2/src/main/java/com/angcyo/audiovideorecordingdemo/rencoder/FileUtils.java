package com.angcyo.audiovideorecordingdemo.rencoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.angcyo.audiovideorecordingdemo.FileSwapHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 文件操作工具类
 *
 * @author 赵圣琪
 */
public class FileUtils {

    private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte
    public static String T_FLASH_PATH = "/storage/sdcard1";
    public static SimpleDateFormat simpleDateFormat;

    static {
        simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
    }

    /**
     * 获取录像存储目录
     */
    public static File getVideoStorageDir() {
        File dir = new File(getStorageDir(), "/video");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        FileUtils.makeNoMediaFile(dir);
        return dir;
    }

    public static String getVideoStorageDirWhenInsertSdcard() {
        File dir;
        if (getStorageDir() != null) {
            dir = new File(getStorageDirWhenInsertSdcard(), "/video");
        } else {
            return null;
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }

        FileUtils.makeNoMediaFile(dir);
        return dir.getAbsolutePath();
    }

    public static void clearLostDirFolder() {
        if (FileUtils.isTFlashCardExists()) {
            File root = new File(T_FLASH_PATH, "LOST.DIR");
            if (root != null && root.exists()) {
                delectAllFiles(root);
            }
        }
    }

    public static boolean isTFlashCardExists() {
        boolean tfExistsFlag = false;
        tfExistsFlag = new File(T_FLASH_PATH, "Android").exists();

        if (getStorageDirWhenInsertSdcard() != null && testNewTfFile() == true) {
            tfExistsFlag = true;
        }
        return tfExistsFlag;
    }

    public static double getTFlashCardSpace() {
        File dir;
        if (isTFlashCardExists()) {
            dir = new File(T_FLASH_PATH);
            return dir.getTotalSpace() * 0.8;
        }

        return 0;
    }

    public static double getTFlashCardSpaceWhenInsertSdcard() {
        File dir;
        if (getStorageDirWhenInsertSdcard() != null) {
            dir = new File(T_FLASH_PATH);
            return dir.getTotalSpace() * 0.8;
        }

        return 0;
    }

    public static double getTFlashCardFreeSpace() {
        File dir;
        if (isTFlashCardExists()) {
            dir = new File(T_FLASH_PATH);
            return dir.getFreeSpace();
        }

        return 0;
    }

    /**
     * 获取临时目录
     */
    public static File getStorageDir() {
        File dir;
        if (isTFlashCardExists()) {
            dir = new File(T_FLASH_PATH, getMainDirName());
        } else {
            dir = new File(Environment.getExternalStorageDirectory(), getMainDirName());
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获得动画的额根目录
     */

    public static File getAnimDir() {
        File dir = null;
        if (isTFlashCardExists()) {
            dir = new File(T_FLASH_PATH, getAnimMainName());
            if (!dir.exists()) {
                dir = new File(Environment.getExternalStorageDirectory(), getAnimMainName());
            }
        } else {
            dir = new File(Environment.getExternalStorageDirectory(), getAnimMainName());
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
//        log.debug("dir:{}, free:{}", dir.getAbsolutePath(), dir.getFreeSpace());

        return dir;
    }

    private static String getAnimMainName() {
        return "/dudu/animation";
    }

    public static File getStorageDirWhenInsertSdcard() {
        File dir;
        try {
            dir = new File(T_FLASH_PATH, getMainDirName());
        } catch (Exception e) {
            return null;
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
//        log.debug("dir:{}, free:{}", dir.getAbsolutePath(), dir.getFreeSpace());

        return dir;
    }

    public static boolean testNewTfFile() {
        File testFile = new File(T_FLASH_PATH, "testNewFile");
        boolean returnFlag = false;
        if (!testFile.exists()) {
            try {
                if (testFile.createNewFile()) {
                    returnFlag = true;
                    testFile.delete();
                }
            } catch (IOException e) {
                returnFlag = false;
            }
        } else {
            testFile.delete();
            returnFlag = true;
        }
        return returnFlag;
    }

    public static String getMainDirName() {
        return "/dudu";
    }

    /**
     * 读取asset目录下文件。
     */
    public static String readFile(Context mContext, String file, String code) {
        int len = 0;
        byte[] buf = null;
        String result = "";
        try {
            InputStream in = mContext.getAssets().open(file);
            len = in.available();
            buf = new byte[len];
            in.read(buf, 0, len);

            result = new String(buf, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除指定文件
     */
    public static void delectCardFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 目录下面的所有文件夹跟文件
     */
    public static void delectCardFiles(String path) {
        File file = new File(path);
        if (file.exists()) {
            delectAllFiles(file);
        }
    }

    private static void delectAllFiles(File root) {
        File files[] = root.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isDirectory()) { // 判断是否为文件夹
                    delectAllFiles(f);
                } else {
                    if (f.exists()) { // 判断是否存在
                        try {
                            f.delete();
                        } catch (Exception e) {
                        }
                    }
                }
            }
    }

    /**
     * 判断文件 是不是 type结尾
     */
    public static boolean isFileType(File file, String type) {
        String name = file.getName();
        if (!"".equals(name) && name != null) {
            String fileEnd = name.substring(name.lastIndexOf("."));
            if (type.equals(fileEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断sdcard是否存在
     */
    public static boolean isSdCard() {
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 返回sdCard路径
     */
    public static String getSdPath() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(
                Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    public static String fileByte2Mb(double size) {
        double mbSize = size / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(mbSize);
    }

    public static String fileByte2Kb(double size) {
        double mbSize = size / 1024;
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(mbSize);
    }

    public static void makeNoMediaFile(File dir) {
        try {
            File f = new File(dir, ".nomedia");
            if (!f.exists()) {
                f.createNewFile();
            }
        } catch (Exception e) {
        }
    }

    /**
     * 拼接路径 concatPath("/mnt/sdcard", "/DCIM/Camera") => /mnt/sdcard/DCIM/Camera
     * concatPath("/mnt/sdcard", "DCIM/Camera") => /mnt/sdcard/DCIM/Camera
     * concatPath("/mnt/sdcard/", "/DCIM/Camera") => /mnt/sdcard/DCIM/Camera
     */
    public static String concatPath(String... paths) {
        StringBuilder result = new StringBuilder();
        if (paths != null) {
            for (String path : paths) {
                if (path != null && path.length() > 0) {
                    int len = result.length();
                    boolean suffixSeparator = len > 0
                            && result.charAt(len - 1) == File.separatorChar;// 后缀是否是'/'
                    boolean prefixSeparator = path.charAt(0) == File.separatorChar;// 前缀是否是'/'
                    if (suffixSeparator && prefixSeparator) {
                        result.append(path.substring(1));
                    } else if (!suffixSeparator && !prefixSeparator) {// 补前缀
                        result.append(File.separatorChar);
                        result.append(path);
                    } else {
                        result.append(path);
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * 检测文件是否可用
     */
    public static boolean checkFile(File f) {
        if (f != null && f.exists() && f.canRead()
                && (f.isDirectory() || (f.isFile() && f.length() > 0))) {
            return true;
        }
        return false;
    }

    /**
     * 获取sdcard路径
     */
    public static String getExternalStorageDirectory() {
        String path = Environment.getExternalStorageDirectory().getPath();
        return path;
    }

    public static long getFileSize(String fn) {
        File f = null;
        long size = 0;

        try {
            f = new File(fn);
            size = f.length();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            f = null;
        }
        return size < 0 ? null : size;
    }

    public static long getFileSize(File fn) {
        return fn == null ? 0 : fn.length();
    }

    public static long getDirSize(File file) {
        if (file.isFile())
            return file.length();
        final File[] children = file.listFiles();
        long total = 0;
        if (children != null)
            for (final File child : children)
                total += getDirSize(child);
        return total;
    }

    public static long getDirSize(String filePath) {
        File file = new File(filePath);
        if (file.isFile())
            return file.length();
        final File[] children = file.listFiles();
        long total = 0;
        if (children != null)
            for (final File child : children)
                total += getDirSize(child);
        return total;
    }

    public static String getFileType(String fn, String defaultType) {
        FileNameMap fNameMap = URLConnection.getFileNameMap();
        String type = fNameMap.getContentTypeFor(fn);
        return type == null ? defaultType : type;
    }

    public static String getFileType(String fn) {
        return getFileType(fn, "application/octet-stream");
    }

    public static String getFileExtension(String filename) {
        String extension = "";
        if (filename != null) {
            int dotPos = filename.lastIndexOf(".");
            if (dotPos >= 0 && dotPos < filename.length() - 1) {
                extension = filename.substring(dotPos + 1);
            }
        }
        return extension.toLowerCase();
    }

    public static boolean deleteFile(File f) {
        if (f != null && f.exists() && !f.isDirectory()) {
            return f.delete();
        }
        return false;
    }

    public static void deleteDir(File f) {
        if (f != null && f.exists() && f.isDirectory()) {
            for (File file : f.listFiles()) {
                if (file.isDirectory())
                    deleteDir(file);
                file.delete();
            }
            f.delete();
        }
    }

    public static void deleteDir(String f) {
        if (f != null && f.length() > 0) {
            deleteDir(new File(f));
        }
    }

    public static boolean deleteFile(String f) {
        if (f != null && f.length() > 0) {
            return deleteFile(new File(f));
        }
        return false;
    }

    /**
     * read file
     *
     * @param charsetName The name of a supported {@link java.nio.charset.Charset
     *                    </code>charset<code>}
     * @return if file not exist, return null, else return content of file
     * @throws RuntimeException if an error occurs while operator BufferedReader
     */
    public static String readFile(File file, String charsetName) {
        StringBuilder fileContent = new StringBuilder("");
        if (file == null || !file.isFile()) {
            return fileContent.toString();
        }

        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(
                    file), charsetName);
            reader = new BufferedReader(is);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!fileContent.toString().equals("")) {
                    fileContent.append("\r\n");
                }
                fileContent.append(line);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
        return fileContent.toString();
    }

    public static String readFile(String filePath, String charsetName) {
        return readFile(new File(filePath), charsetName);
    }

    public static String readFile(File file) {
        return readFile(file, "utf-8");
    }

    /**
     * 文件拷贝
     */
    public static boolean fileCopy(String from, String to) {
        boolean result = false;

        int size = 1 * 1024;

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            byte[] buffer = new byte[size];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            result = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
            }
        }
        return result;
    }

    /**
     * 清除WebView缓存
     */
    public static void clearWebViewCache(Context context) {

        // 清理Webview缓存数据库
        try {
            context.deleteDatabase("webview.db");
            context.deleteDatabase("webviewCache.db");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // WebView 缓存文件
        File appCacheDir = new File(context.getFilesDir().getAbsolutePath()
                + "/webcache");
        // Log.e(TAG, "appCacheDir path="+appCacheDir.getAbsolutePath());

        File webviewCacheDir = new File(context.getCacheDir().getAbsolutePath()
                + "/webviewCache");
        // Log.e(TAG,
        // "webviewCacheDir path="+webviewCacheDir.getAbsolutePath());

        // 删除webview 缓存目录
        if (webviewCacheDir.exists()) {
            deleteFile2(webviewCacheDir);
        }
        // 删除webview 缓存 缓存目录
        if (appCacheDir.exists()) {
            deleteFile2(appCacheDir);
        }
    }

    /**
     * 递归删除 文件/文件夹
     */
    public static void deleteFile2(File file) {

        // Log.i(TAG, "delete file path=" + file.getAbsolutePath());

        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile2(files[i]);
                }
            }
            file.delete();
        } else {
            // Log.e(TAG, "delete file no exists " + file.getAbsolutePath());
        }
    }

    /**
     * @param context        :上下文
     * @param trafficControl 控制
     * @param downloadLimit  下载流量限制
     * @param uploadLimit    上传流量限制
     */
    public static boolean refreshFlowLimit(Context context, boolean trafficControl, int downloadLimit, int uploadLimit) {
        boolean flag = false;
        File file = new File(getExternalStorageDirectory() + File.separator + "nodogsplash", "nodogsplash.conf");
        if (file.exists()) {
            try {
                FileOutputStream out = new FileOutputStream(file, false);
                //获得assets目录下nodogsplash.conf的内容
                String content = readFile(context, "nodogsplash.conf", "UTF_8");
                String control = "";
                if (trafficControl) {
                    control = "yes";
                } else {
                    control = "no";
                }
                String result = content + "\n" + "\n" + "TrafficControl " + control + "\n" + "\n"
                        + "DownloadLimit " + downloadLimit + "\n" + "\n" +
                        "UploadLimit " + uploadLimit;
                out.write(result.getBytes());
                out.close();
                flag = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    /**
     * 复制文件
     *
     * @param sdFile :目标文件
     * @params assetFile :被复制文件的输入流
     */
    public static Boolean copyFileToSd(InputStream assetFile, File sdFile) {
        boolean flags = false;
        try {
            FileOutputStream fos = new FileOutputStream(sdFile);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = assetFile.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            flags = true;
            fos.close();
            assetFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flags;
    }

    /**
     * 解压缩一个文件
     *
     * @param zipFile    压缩文件
     * @param folderPath 解压缩的目标目录
     * @throws IOException 当解压缩过程出错时抛出
     */
    public static void upZipFile(File zipFile, String folderPath) {
//        String strZipName = zipFile.getName();
//        folderPath += "/" + strZipName.substring(0, strZipName.lastIndexOf("."));
//        File desDir = new File(folderPath);
//        if (!desDir.exists())
//        {
//            desDir.mkdirs();
//        }
        ZipFile zf;
        try {
            zf = new ZipFile(zipFile);
            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                if (entry.isDirectory()) {
                    String dirstr = entry.getName();
                    dirstr = new String(dirstr.getBytes("8859_1"), "GB2312");
                    File f = new File(dirstr);
                    f.mkdir();
                    continue;
                }

                InputStream in = zf.getInputStream(entry);
                String str = folderPath + File.separator + entry.getName();
                str = new String(str.getBytes("8859_1"), "GB2312");
                File desFile = new File(str);
                if (!desFile.exists()) {
                    File fileParentDir = desFile.getParentFile();
                    if (!fileParentDir.exists()) {
                        fileParentDir.mkdirs();
                    }
                    desFile.createNewFile();
                }

                OutputStream out = new FileOutputStream(desFile);
                byte buffer[] = new byte[BUFF_SIZE];
                int realLength;
                while ((realLength = in.read(buffer)) > 0) {
                    out.write(buffer, 0, realLength);
                }
                in.close();
                out.close();
            }
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 压缩一个文件
     *
     * @param srcFilePath 需要压缩文件的路径
     * @param zipFilePath 压缩后文件的路径
     * @throws IOException 当解压缩过程出错时抛出
     */
    public static void zipFolder(String srcFilePath, String zipFilePath) throws Exception {
        // 创建Zip包
        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFilePath));
        // 打开要输出的文件
        File file = new File(srcFilePath);
        // 压缩
        zipFiles(file.getParent() + File.separator, file.getName(), outZip);
        // 完成,关闭
        outZip.finish();
        outZip.close();
    }

    private static void zipFiles(String folderPath, String filePath, ZipOutputStream zipOut)
            throws Exception {
        if (zipOut == null) {
            return;
        }
        File file = new File(folderPath + filePath);
        // 判断是不是文件
        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(filePath);
            FileInputStream inputStream = new FileInputStream(file);
            zipOut.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[100000];
            while ((len = inputStream.read(buffer)) != -1) {
                zipOut.write(buffer, 0, len);
            }
            inputStream.close();
            zipOut.closeEntry();
        } else {
            // 文件夹的方式,获取文件夹下的子文件
            String fileList[] = file.list();
            // 如果没有子文件, 则添加进去即可
            if (fileList.length <= 0) {
                ZipEntry zipEntry = new ZipEntry(filePath + File.separator);
                zipOut.putNextEntry(zipEntry);
                zipOut.closeEntry();
            }
            // 如果有子文件, 遍历子文件
            for (int i = 0; i < fileList.length; i++) {
                zipFiles(folderPath, filePath + File.separator + fileList[i], zipOut);
            }
        }
    }

    /**
     * 复制整个文件夹内容
     *
     * @param oldPath String 原文件路径
     * @param newPath String 复制后路径
     * @return boolean
     */
    public static void
    copyFolder(String oldPath, String newPath) {
        try {
            (new File(newPath)).mkdirs(); //如果文件夹不存在 则建立新文件夹
            File a = new File(oldPath);
            String[] file = a.list();
            File temp = null;
            for (int i = 0; i < file.length; i++) {
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + file[i]);
                } else {
                    temp = new File(oldPath + File.separator + file[i]);
                }
                if (temp.isFile()) {
                    FileInputStream input = new FileInputStream(temp);
                    FileOutputStream output = new FileOutputStream(newPath + "/" +
                            (temp.getName()).toString());
                    byte[] b = new byte[1024 * 5];
                    int len;
                    while ((len = input.read(b)) != -1) {
                        output.write(b, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                if (temp.isDirectory()) {//如果是子文件夹
                    copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存Bitmap到文件
     */
    public static void saveBitmap(Bitmap bmp, String filePath) throws FileNotFoundException {
//        ParcelFileDescriptor.AutoCloseOutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
//                ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_WRITE_ONLY));

        FileOutputStream outputStream = new FileOutputStream(new File(filePath));
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
    }

    public static String getPhotoSaveFilePath() {
        return getPhotoSaveFilePath(simpleDateFormat.format(System.currentTimeMillis()));
    }

    public static String getPhotoSaveFilePath(String fileName) {
        StringBuilder fullPath = new StringBuilder();
        boolean isTFCard = FileUtils.isTFlashCardExists();
        if (isTFCard) {
            fullPath.append(FileUtils.T_FLASH_PATH);
        } else {
            fullPath.append(FileUtils.getExternalStorageDirectory());
        }
        fullPath.append(FileUtils.getMainDirName());
        fullPath.append(FileSwapHelper.BASE_PHOTO);
        fullPath.append(fileName);
        fullPath.append(FileSwapHelper.BASE_PHOTO_EXT);

        String string = fullPath.toString();
        File file = new File(string);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        return string;
    }
}
