package com.jack.jfx.abs;


import com.jack.jfx.annotation.Autowired;
import com.jack.jfx.annotation.HFXApplication;
import com.jack.jfx.call.LoadStageCallBack;
import com.jack.jfx.gui.GUIState;
import com.jack.jfx.handler.ComponentLoader;
import com.jack.jfx.helper.CssHelper;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * FX启动类父类
 *
 * @author gj
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractJavaFxApplicationSupport extends Application {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Class<? extends Application> appClasses;

    private static String[] savedArgs = new String[0];

    private static Class<? extends AbstractFxmlView> savedInitialView;

    private static BaseSplashScreen splashScreen;

    private static List<Image> icons = new ArrayList<>();

    private final List<Image> defaultIcons = new ArrayList<>();

    private volatile static ConcurrentMap<Class, Object> appContext = new ConcurrentHashMap<>();

    private static LoadStageCallBack loadStageCallBack;

    public static Stage getStage() {
        return GUIState.getStage();
    }

    public static Scene getScene() {
        return GUIState.getScene();
    }

    public static HostServices getAppHostServices() {
        return GUIState.getHostServices();
    }

    public static SystemTray getSystemTray() {
        return GUIState.getSystemTray();
    }


    private static ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("HFX-BootStrap-Thread");
            return thread;
        }
    });

    public static void addBean(Class<?> tClass, Object object) {
        appContext.put(tClass, object);
        System.out.println("\033[32m[" + SIMPLE_DATE_FORMAT.format(new Date()) + "]-" + "\033[32m" + "HFX Initialize Load Bean" + "->\033[36m" + "[hashCode:" + object.hashCode() + "]" + "->\033[38m" + "[" + object + "]");
    }

    public static <T> T getBean(Class<T> tClass) {
        T bean = null;
        try {
            bean = (T) appContext.get(tClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }


    /**
     * 显示错误页面
     *
     * @param throwable
     */
    public static void showAlert(Throwable throwable) {
        Platform.runLater(() -> showErrorAlert(throwable));
    }


    @Override
    public void start(final Stage stage) throws Exception {
        if (appClasses == null) {
            appClasses = this.getClass();
        }
        GUIState.setStage(stage);
        GUIState.setHostServices(this.getHostServices());
        //加载启动界面
        final Stage splashStage = new Stage();
        if (splashScreen != null) {
            if (splashScreen.visible()) {
                Parent parent = splashScreen.getParent();
                if (parent != null) {
                    final Scene splashScene = new Scene(parent);
                    splashStage.setScene(splashScene);
                    splashStage.initStyle(StageStyle.TRANSPARENT);
                    splashStage.getIcons().addAll(defaultIcons);
                    splashStage.show();
                }
            }
        }
        executorService.execute(() -> {
            System.out.println("Load JAVAFX......");
            long startTimeStamp = System.currentTimeMillis();
            if (loadStageCallBack != null) {
                loadStageCallBack.startLoad(System.currentTimeMillis());
            }
            Platform.runLater(() -> {
                System.out.println("Load FXML......");
                //加载对象
                ComponentLoader.loadClasses("com.hnf");
                //依赖注入
                for (Object value : appContext.values()) {
                    autoWried(value);
                }
                showInitialView(savedInitialView);
                cssAutoParser();
                //加载完毕，隐藏启动界面
                if (splashScreen != null) {
                    if (splashScreen.visible()) {
                        splashStage.hide();
                        splashStage.setScene(null);
                    }
                }
                if (loadStageCallBack != null) {
                    loadStageCallBack.loadDone(System.currentTimeMillis());
                }
                long endTimeStamp = System.currentTimeMillis();
                System.out.println("\033[32mHNF HFX Initialize Loading Completed:" + (endTimeStamp - startTimeStamp) + "ms\033[38m");
            });
        });
    }


    /**
     * css配置加载
     */
    public void cssAutoParser() {
        HFXApplication hfxApplication = appClasses.getAnnotation(HFXApplication.class);
        if (hfxApplication == null) {
            return;
        }
        String css = hfxApplication.css();
        boolean enableCssAuto = hfxApplication.enableCssAuto();
        System.out.println("\033[32m[" + SIMPLE_DATE_FORMAT.format(new Date()) + "]-" + "\033[32m" + "HFX Application Config" + "->\033[36m" + "[css]" + "->\033[38m" + "[" + css + "]");
        System.out.println("\033[32m[" + SIMPLE_DATE_FORMAT.format(new Date()) + "]-" + "\033[32m" + "HFX Application Config" + "->\033[36m" + "[enableCssAuto]" + "->\033[38m" + "[" + enableCssAuto + "]");
        //开启css自动装载
        CssHelper.loadCss(css, enableCssAuto);
    }

    /**
     * 局部刷新
     *
     * @param pane    父布局
     * @param newView 实现了AbstractFxmlView的子布局
     */
    public static AbstractFxmlView partialRefresh(Pane pane, Class<? extends AbstractFxmlView> newView) {
        AbstractFxmlView view = getBean(newView);
        if (view == null) {
            view = reloadPartialRefresh(pane, newView);
            return view;
        }
        if (view != null) {
            refreshChildren(pane, view.getView());
        }
        return view;
    }


    /**
     * 局部刷新--重新加载局部
     *
     * @param pane    父布局
     * @param newView 实现了AbstractFxmlView的子布局
     */
    public static AbstractFxmlView reloadPartialRefresh(Pane pane, Class<? extends AbstractFxmlView> newView) {
        AbstractFxmlView view = reloadFxmlView(newView);
        if (view != null) {
            refreshChildren(pane, view.getView());
        }
        return view;
    }

    /**
     * 重新加载FXMLView
     *
     * @param newView
     * @return
     */
    public static AbstractFxmlView reloadFxmlView(Class<? extends AbstractFxmlView> newView) {
        if (newView == null) {
            return null;
        }
        AbstractFxmlView view = null;
        try {
            view = newView.newInstance();
            addBean(newView, view);
            //重新注入
            for (Object value : appContext.values()) {
                autoWried(value);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return view;
    }


    /**
     * 子界面刷新
     *
     * @param pane
     * @param parent
     */
    private static void refreshChildren(Pane pane, Parent parent) {
        ObservableList<Node> children = pane.getChildren();
        if (children != null) {
            children.clear();
        }
        children.add(parent);
    }


    /**
     * Show view.
     *
     * @param newView the new view
     */
    public static void showInitialView(final Class<? extends AbstractFxmlView> newView) {
        try {
            AbstractFxmlView bean = getBean(newView);
            bean.initFirstView();
            GUIState.getStage().getIcons().addAll(icons);
            GUIState.getStage().show();
        } catch (Throwable t) {
            showErrorAlert(t);
        }
    }


    /**
     * Show error alert that close app.
     *
     * @param throwable cause of error
     */
    private static void showErrorAlert(Throwable throwable) {
        Alert alert = new Alert(AlertType.ERROR, "Oops! An unrecoverable error occurred.\n" +
                "Please contact your software vendor.\n\n" +
                "The application will stop now.\n\n" +
                "Error: " + throwable.getMessage());
        alert.showAndWait().ifPresent(response -> Platform.exit());
    }


    /**
     * Sets the title. Allows to overwrite values applied during construction at
     * a later time.
     *
     * @param title the new title
     */
    protected static void setTitle(final String title) {
        GUIState.getStage().setTitle(title);
    }


    /**
     * Launch app.
     *
     * @param appClass     the app class
     * @param view         the view
     * @param splashScreen the splash screen
     * @param args         the args
     */
    private static void launch(final Class<? extends Application> appClass,
                               final Class<? extends AbstractFxmlView> view, final BaseSplashScreen splashScreen,
                               final String[] args, final LoadStageCallBack loadStageCk) {
        savedInitialView = view;
        savedArgs = args;
        loadStageCallBack = loadStageCk;
        appClasses = appClass;
        if (splashScreen != null) {
            AbstractJavaFxApplicationSupport.splashScreen = splashScreen;
        }
        if (SystemTray.isSupported()) {
            GUIState.setSystemTray(SystemTray.getSystemTray());
        }
        Application.launch(appClass, args);
    }

    /**
     * 依赖注入
     *
     * @param object
     */
    public static void autoWried(Object object) {
        Class<?> aClass = object.getClass();
        Field[] declaredFields = aClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            Autowired annotation = declaredField.getAnnotation(Autowired.class);
            if (annotation != null) {
                Class<?> type = declaredField.getType();
                Object bean = getBean(type);
                try {
                    declaredField.set(object, bean);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 加载启动界面
     *
     * @param appClass     启动类
     * @param view         主界面
     * @param splashScreen 启动界面
     * @param args
     */
    public static void launchApp(final Class<? extends Application> appClass,
                                 final Class<? extends AbstractFxmlView> view, final Class<? extends BaseSplashScreen> splashScreen,
                                 final String[] args, final LoadStageCallBack loadStageCb) {
        try {
            if (splashScreen == null) {
                launch(appClass, view, null, args, loadStageCb);
            } else {
                launch(appClass, view, splashScreen.newInstance(), args, loadStageCb);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载启动界面,不设置界面加载回调
     *
     * @param appClass     启动类
     * @param view         主界面
     * @param splashScreen 启动界面
     * @param args
     */
    public static void launchApp(final Class<? extends Application> appClass,
                                 final Class<? extends AbstractFxmlView> view, final Class<? extends BaseSplashScreen> splashScreen,
                                 final String[] args) {
        launchApp(appClass, view, splashScreen, args, null);
    }

    /**
     * 不加载启动界面
     *
     * @param appClass 启动器类
     * @param view     主界面
     * @param args
     */
    public static void launchApp(final Class<? extends Application> appClass,
                                 final Class<? extends AbstractFxmlView> view,
                                 final String[] args, final LoadStageCallBack loadStageCallBack) {
        launch(appClass, view, null, args, loadStageCallBack);
    }

    /**
     * 不加载启动界面,不设置界面加载回调
     *
     * @param appClass 启动器类
     * @param view     主界面
     * @param args
     */
    public static void launchApp(final Class<? extends Application> appClass,
                                 final Class<? extends AbstractFxmlView> view,
                                 final String[] args) {
        launch(appClass, view, null, args, null);
    }
}
