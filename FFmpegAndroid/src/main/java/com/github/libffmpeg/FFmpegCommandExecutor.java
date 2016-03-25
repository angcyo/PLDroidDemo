package com.github.libffmpeg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;


public class FFmpegCommandExecutor {

    private final String cmd;
    private final long timeout;
    private final FFmpegSyncResponseInterface ffmpegExecuteResponseHandler;
    private final ShellCommand shellCommand;

    private long startTime;
    private Process process;
    private String output = "";
    private Metadata metadata;

    private volatile boolean isCancelled;

    public FFmpegCommandExecutor(String cmd, long timeout, FFmpegSyncResponseInterface ffmpegExecuteResponseHandler) {
        this.cmd = cmd;
        this.timeout = timeout;
        this.ffmpegExecuteResponseHandler = ffmpegExecuteResponseHandler;
        this.shellCommand = new ShellCommand();
        this.metadata = new Metadata();
    }

    public synchronized void cancelProcess() {
        isCancelled = true;

        Util.destroyProcess(process);
    }

    public void execute() {
        startTime = System.currentTimeMillis();
        ffmpegExecuteResponseHandler.onStart();

        CommandResult commandResult = executeInternal();

        onFinish(commandResult);
    }

    public boolean isProcessCompleted() {
        return Util.isProcessCompleted(process);
    }

    private CommandResult executeInternal() {
        if (isCancelled) {
            return new CommandResult(false, "Cancelled").markCancelled();
        }

        try {
            process = shellCommand.run(cmd);
            if (process == null) {
                return CommandResult.getDummyFailureResponse();
            }
            checkAndUpdateProcess();
            return CommandResult.getOutputFromProcess(process);
        } catch (Exception e) {
            if (isCancelled) {
                return CommandResult.getDummyFailureResponse();
            }

            Log.e("Error running FFmpeg", e);

            return new CommandResult(false, e.getMessage());
        } finally {
            Util.destroyProcess(process);
            process = null;
        }
    }

    private synchronized void onFinish(CommandResult commandResult) {
        if (isCancelled) {
            ffmpegExecuteResponseHandler.onFinish(commandResult.success, true);
            return;
        }

        output += commandResult.output;
        if (commandResult.success) {
            ffmpegExecuteResponseHandler.onSuccess(output);
        } else {
            ffmpegExecuteResponseHandler.onFailure(output);
        }
        ffmpegExecuteResponseHandler.onFinish(commandResult.success, false);
    }

    private void checkAndUpdateProcess() throws TimeoutException, InterruptedException {
        boolean metadataNotified = false;

        while (!Util.isProcessCompleted(process)) {
            // checking if process is completed
            if (Util.isProcessCompleted(process)) {
                return;
            }

            // Handling timeout
            if (timeout != Long.MAX_VALUE && System.currentTimeMillis() > startTime + timeout) {
                throw new TimeoutException("FFmpeg timed out");
            }

            try {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    output += line + "\n";

                    if (metadata.getFps() < 0) {
                        double fps = Util.parseFpsIfPresent(line);

                        if (fps >= 0) {
                            metadata.setFps(fps);
                        }
                    }

                    if (metadata.getDuration() < 0) {
                        long duration = Util.parseDurationIfPresent(line);
                        if (duration > 0) {
                            metadata.setDuration(duration);
                        }
                    }

                    if (!metadataNotified && metadata.getFps() > 0 && metadata.getDuration() > 0) {
                        ffmpegExecuteResponseHandler.onMetadata(metadata);
                        metadataNotified = true;
                    }

                    long processTime = Util.getProcessTime(line);

                    if (processTime >= 0 && metadata.getDuration() > 0) {
                        ffmpegExecuteResponseHandler.onProgress((int) (100 * (processTime / (double) metadata.getDuration())));
                    }
                }
            } catch (IOException e) {
                Log.e("checkAndUpdateProcess", e);
            }
        }
    }
}
