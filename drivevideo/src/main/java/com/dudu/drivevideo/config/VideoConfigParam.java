package com.dudu.drivevideo.config;

/**
 * Created by dengjun on 2016/1/26.
 * Description :
 */
public class VideoConfigParam {
    private FrontVideoConfigParam preVideoConfigParam;
    private RearVideoConfigParam rearVideoConfigParam;

    public VideoConfigParam() {
        preVideoConfigParam = new FrontVideoConfigParam();
        rearVideoConfigParam = new RearVideoConfigParam();
    }

    public FrontVideoConfigParam getPreVideoConfigParam() {
        return preVideoConfigParam;
    }

    public RearVideoConfigParam getRearVideoConfigParam() {
        return rearVideoConfigParam;
    }
}
