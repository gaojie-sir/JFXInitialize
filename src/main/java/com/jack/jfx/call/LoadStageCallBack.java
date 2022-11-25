package com.jack.jfx.call;

/**
 * 界面加载回调
 * @author: gj
 * @date: 2020-03-13 14:27
 **/
public interface LoadStageCallBack {
    /**
     * 界面开始加载
     * @param timeStamp
     */
    void startLoad(long timeStamp);

    /**
     * 界面加载完成
     * @param timeStamp
     */
    void loadDone(long timeStamp);
}
