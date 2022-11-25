package com.jack.jfx.util;

import com.jack.jfx.abs.BaseAlertWindow;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Alert;

import javax.swing.*;

/**
 * @author: gj
 * @date: 2020-03-13 10:27
 **/
public class SingleApplication {
    private BaseAlertWindow baseAlertWindow;

    public SingleApplication(BaseAlertWindow baseAlertWindow) {
        this.baseAlertWindow = baseAlertWindow;
    }

    public SingleApplication() {
    }

    public boolean checkApplicationRun(String fileName) {
        FileLock instance = FileLock.getInstance();
        instance.setLockFileName(fileName);
        boolean isStarted = instance.tryApplicationStarted();
        if (isStarted) {
            if (baseAlertWindow == null) {
                showAlert("程序已启动");
            } else {
                baseAlertWindow.showAndWaitDialog();
            }
            return true;
        }
        return false;
    }

    public boolean checkApplicationRun() {
        return checkApplicationRun(null);
    }

    public void showAlert(String msg) {
        if (!Platform.isFxApplicationThread()) {
            SwingUtilities.invokeLater(() -> {
                new JFXPanel();
                System.out.println("FX线程未启动");
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, msg);
                    alert.showAndWait();
                });
            });
        } else {
            System.out.println("FX线程已启动");
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, msg);
                alert.showAndWait();
            });
        }
    }
}
