package com.jack.jfx.gui;

import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.*;

/**
 * this VM.
 * @author gj
 */
public enum GUIState {


    /**
     * ex
     */
    INSTANCE;
    private static Color sceneColor = Color.TRANSPARENT;

    private static Scene scene;

    private static Stage stage;

    private static String title;

    private static HostServices hostServices;

    private static SystemTray systemTray;

    public static String getTitle() {
        return title;
    }

    public static Scene getScene() {
        return scene;
    }

    public static Stage getStage() {
        return stage;
    }

    public static void setScene(final Scene scene) {
        GUIState.scene = scene;
    }

    public static void setStage(final Stage stage) {
        GUIState.stage = stage;
    }

    public static void setTitle(final String title) {
        GUIState.title = title;
    }

    public static HostServices getHostServices() {
        return hostServices;
    }

    public static void setHostServices(HostServices hostServices) {
        GUIState.hostServices = hostServices;
    }

    public static SystemTray getSystemTray() {
        return systemTray;
    }

    public static void setSystemTray(SystemTray systemTray) {
        GUIState.systemTray = systemTray;
    }

    public static Color getSceneColor() {
        return sceneColor;
    }

    public static void setSceneColor(Color sceneColor) {
        GUIState.sceneColor = sceneColor;
    }}
