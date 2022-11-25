package com.jack.jfx.abs;

import javafx.scene.Parent;

/**
 * @author gj
 */
public abstract class BaseSplashScreen {
    /**
     * 默认不显示启动界面
     */
    protected static boolean isVisible = true;

    /**
     * 设置是否开启应用启动画面
     *
     * @param isVisible true 开启 false 不开启
     */
    public static void setVisible(boolean isVisible) {
        BaseSplashScreen.isVisible = isVisible;
    }


    /**
     * Override this to create your own splash pane parent node.
     *
     * @return A standard Node
     */
    public abstract Parent getParent();

    /**
     * Customize if the splash screen should be visible at all.
     *
     * @return true by default
     */
    public boolean visible() {
        return isVisible;
    }

}
