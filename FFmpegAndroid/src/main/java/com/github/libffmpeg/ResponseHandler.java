package com.github.libffmpeg;

abstract interface ResponseHandler {

    /**
     * on Start
     */
    public void onStart();

    /**
     * on Finish
     */
    public void onFinish(boolean success, boolean isCancelled);

}
