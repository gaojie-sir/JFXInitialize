package com.jack.jfx.abs;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: gj
 * @date: 2020-03-13 10:30
 **/
public abstract class BaseAlertWindow extends StackPane {
    /**
     * 弹窗窗口
     */
    private Stage stage;

    public BaseAlertWindow() {
        initFxPlatform();
    }

    public Stage getStage() {
        return stage;
    }

    private void initFxPlatform() {
        if (!Platform.isFxApplicationThread()) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                new JFXPanel();
                System.out.println("FX线程未启动");
                stage = new Stage();
                countDownLatch.countDown();
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Logger.getGlobal().log(Level.SEVERE, null, e);
            }
        } else {
            stage = new Stage();
            System.out.println("FX线程已启动");
        }
    }


    public void showAndWaitDialog() {
        if (!Platform.isFxApplicationThread()) {
            SwingUtilities.invokeLater(() -> {
                new JFXPanel();
                System.out.println("FX线程未启动");
                Platform.runLater(() -> {
                    stage.showAndWait();
                });
            });
        } else {
            System.out.println("FX线程已启动");
            Platform.runLater(() -> {
                stage.showAndWait();
            });
        }
    }
}
