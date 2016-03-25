package com.github.libffmpeg;

public interface FFmpegSyncResponseInterface extends ResponseHandler  {

    /**
     * on Success
     * @param message complete output of the FFmpeg command
     */
    public void onSuccess(String message);

    /**
     * on Progress
     */
    public void onProgress(int percent);

    public void onMetadata(Metadata metadata);

    /**
     * on Failure
     * @param message complete output of the FFmpeg command
     */
    public void onFailure(String message);
}
