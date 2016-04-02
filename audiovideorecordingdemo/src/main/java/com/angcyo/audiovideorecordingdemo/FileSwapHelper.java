package com.angcyo.audiovideorecordingdemo;

import com.angcyo.audiovideorecordingdemo.rencoder.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Created by robi on 2016-03-29 15:05.
 */
public class FileSwapHelper {

    public static String BASE_VIDEO = "/video4/";
    public static String BASE_EXT = ".mp4";
    private String currentFileName = "-";//用于匹配1分钟时间的文件名
    private boolean isTFCard = false;//用于判断拔插TF卡
    private SimpleDateFormat simpleDateFormat;
    private String nextFileName;//如果需要切换文件,文件完整的路径将保存在此

    public FileSwapHelper() {
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
    }

    /**
     * SD 卡拔出,和1分钟后, 重新设置文件保存路径
     */
    public boolean requestSwapFile() {
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

        if (isChanged) {
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
        } else {
            fullPath.append(FileUtils.getExternalStorageDirectory());
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
}
