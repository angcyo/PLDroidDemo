package com.angcyo.audiovideorecordingdemo;

import android.support.annotation.IntDef;
import android.util.Log;

import com.angcyo.audiovideorecordingdemo.rencoder.FileUtils;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by robi on 2016-03-29 15:05.
 */
public class FileSwapHelper {

    public static final int CARD_DF = 0;
    public static final int CARD_TF = 1;
    public static String BASE_VIDEO = "/video4/";
    public static String BASE_PHOTO = "/photos/";
    public static String BASE_EXT = ".mp4";
    public static String BASE_PHOTO_EXT = ".png";
    private String currentFileName = "-";//用于匹配1分钟时间的文件名
    private boolean isTFCard = false;//用于判断拔插TF卡
    private SimpleDateFormat simpleDateFormat;
    private String nextFileName;//如果需要切换文件,文件完整的路径将保存在此

    public FileSwapHelper() {
        simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
    }

    /**
     * SD 卡拔出,和1分钟后, 重新设置文件保存路径
     */
    public boolean requestSwapFile() {
        return requestSwapFile(false);
    }

    public boolean requestSwapFile(boolean force) {
        //SD 卡可读写
        String fileName = getFileName();
        boolean isChanged = false;

        //文件名不想当,视为1分钟保存一个新的文件,第一次保存放在内部存储,之后保存判断TF卡,减少对TF卡的存在判断
        if (!currentFileName.equalsIgnoreCase(fileName)) {
            isChanged = true;
        } else {
            boolean flashCardExists = FileUtils.isTFlashCardExists();
            boolean oldCard = isTFCard;
            isTFCard = flashCardExists;
            //TF卡拔插
            if (oldCard != flashCardExists) {
                isChanged = true;
            }
        }

        if (isChanged || force) {
            nextFileName = getSaveFilePath(fileName);
            return true;
        }

        return false;
    }

    public String getNextFileName() {
        return nextFileName;
    }

    private String getFileName() {
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return format;
    }

    private String getSaveFilePath(String fileName) {
        currentFileName = fileName;
        StringBuilder fullPath = new StringBuilder();
        if (isTFCard) {
            fullPath.append(FileUtils.T_FLASH_PATH);
            //检查TF卡剩余空间容量,并清理
            checkSpace(CARD_TF);
        } else {
            fullPath.append(FileUtils.getExternalStorageDirectory());
            //检查内置卡剩余空间容量,并清理
            checkSpace(CARD_DF);
        }
        fullPath.append(FileUtils.getMainDirName());
        fullPath.append(BASE_VIDEO);
        fullPath.append(fileName);
        fullPath.append(BASE_EXT);

        String string = fullPath.toString();
        File file = new File(string);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        return string;
    }

    /**
     * 检查剩余空间
     */
    private void checkSpace(@Card int card) {
        StringBuilder fullPath = new StringBuilder();
        String checkPath;
        if (card == CARD_TF) {
            //TF卡
            checkPath = FileUtils.T_FLASH_PATH;
        } else {
            //内置卡
            checkPath = FileUtils.getExternalStorageDirectory();
        }
        fullPath.append(checkPath);
        fullPath.append(FileUtils.getMainDirName());
        fullPath.append(BASE_VIDEO);

        if (checkCardSpace(checkPath)) {
            File file = new File(fullPath.toString());
            String[] fileNames = file.list();
            if (fileNames.length < 1) {
                return;
            }
            List<String> fileNameLists = Arrays.asList(fileNames);
            Collections.sort(fileNameLists);

            for (int i = 0; i < fileNameLists.size() && checkCardSpace(checkPath); i++) {
                //清理视频
                String removeFileName = fileNameLists.get(i);
                File removeFile = new File(file, removeFileName);
                try {
                    removeFile.delete();
                    Log.e("angcyo-->", "删除文件 " + removeFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("angcyo-->", "删除文件失败 " + removeFile.getAbsolutePath());
                }
            }

//            do {
//                //清理视频
//                String removeFileName = fileNameLists.remove(0);
//                File removeFile = new File(file, removeFileName);
//                removeFile.delete();
//
//            } while (checkCardSpace(checkPath));
        }
    }

    private boolean checkCardSpace(String filePath) {
        File dir = new File(filePath);
        double totalSpace = dir.getTotalSpace();//总大小
        double freeSpace = dir.getFreeSpace();//剩余大小
        if (freeSpace < totalSpace * 0.2) {
//            Log.e("angcyo-->", filePath + " 剩余空间不足20%，开始清理空间...");
//            FileUtils.clearLostDirFolder();
            return true;
        }
        return false;
    }

//    private void checkTFlashCardSpace() {
//        File dir = new File(FileUtils.T_FLASH_PATH);
//        double totalSpace = dir.getTotalSpace();//总大小
//        double freeSpace = dir.getFreeSpace();//剩余大小
//        if (freeSpace < totalSpace * 0.2) {
//            Log.e("angcyo-->", "剩余空间不足20%，开始清理空间...");
//            FileUtils.clearLostDirFolder();
//        }
//    }

    @IntDef({CARD_DF, CARD_TF})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Card {
    }
}
