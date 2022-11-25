package com.jack.jfx.helper;

import com.jack.jfx.gui.GUIState;
import javafx.scene.Scene;

/**
 * @author gj
 * @date 2021/4/26 10:47
 */
public class CssHelper {
    private static CssParser cssParser;

    /**
     * 加载首次css文件
     *
     * @param cssPath
     */
    public static void loadCss(String cssPath,boolean autoFormatCss) {
        if (cssParser == null) {
            cssParser = new CssParser(cssPath);
            Scene scene = GUIState.getScene();
            if (scene == null) {
                return;
            }
            if (!autoFormatCss){
                scene.getStylesheets().clear();
                scene.getStylesheets().add(cssPath);
                return;
            }
            try {
                String newCssPath = cssParser.parseCss(false);
                scene.getStylesheets().clear();
                scene.getStylesheets().add(newCssPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cssParser.setCssUpdateCallBack(newCss -> {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(newCss);
            });
        }
    }

    /**
     * 更新css变量属性
     *
     * @param varKey
     * @param varVal
     */
    public static void updateCssVar(String varKey, String varVal) {
        if (cssParser == null) {
            return;
        }
        cssParser.putVariable(varKey, varVal);
    }


}
