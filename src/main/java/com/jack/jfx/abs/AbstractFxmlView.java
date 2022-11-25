package com.jack.jfx.abs;

import com.jack.jfx.annotation.FXMLView;
import com.jack.jfx.gui.GUIState;
import com.jack.jfx.handler.PropertyReaderHelper;
import com.jack.jfx.handler.ResourceBundleControl;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.ResourceBundle.getBundle;

/**
 * @author gj
 */
public abstract class AbstractFxmlView {

    private ObjectProperty<Object> presenterProperty;

    private Optional<ResourceBundle> bundle;

    private URL resource;

    private String controllerResources;

    private FXMLView annotation = null;

    private FXMLLoader fxmlLoader;

    private String fxmlRoot;

    public Stage getStage() {
        return stage;
    }

    private Stage stage;

    private Modality currentStageModality;

    protected Object controller;


    public FXMLLoader getFxmlLoader() {
        return fxmlLoader;
    }

    public Object getController() {
        return controller;
    }

    /**
     * Instantiates a new abstract fx.fxml view.
     */
    public AbstractFxmlView() {
        final String filePathFromPackageName = PropertyReaderHelper.determineFilePathFromPackageName(getClass());
        setFxmlRootPath(filePathFromPackageName);
        annotation = getFXMLAnnotation();
        resource = getURLResource(annotation);
        controllerResources = getControllerURLResources(annotation);
        presenterProperty = new SimpleObjectProperty<>();
        bundle = getResourceBundle(getBundleName());
        getView();
    }

    /**
     * 重新加载页面
     */
    public void reLoadView() {
        final String filePathFromPackageName = PropertyReaderHelper.determineFilePathFromPackageName(getClass());
        setFxmlRootPath(filePathFromPackageName);
        annotation = getFXMLAnnotation();
        resource = getURLResource(annotation);
        controllerResources = getControllerURLResources(annotation);
        presenterProperty = new SimpleObjectProperty<>();
        bundle = getResourceBundle(getBundleName());
        getView();
    }


    /**
     * Gets the URL resource. This will be derived from applied annotation value
     * or from naming convention.
     *
     * @param annotation the annotation as defined by inheriting class.
     * @return the URL resource
     */
    private URL getURLResource(final FXMLView annotation) {
        if (annotation != null && !"".equals(annotation.value())) {
            return getClass().getResource("/" + annotation.value());
        } else {
            return getClass().getResource(getFxmlPath());
        }
    }

    /**
     * @param annotation
     * @return
     */
    public String getControllerURLResources(final FXMLView annotation) {
        if (annotation != null && !"".equals(annotation.value())) {
            return annotation.controller();
        } else {
            return "";
        }
    }


    /**
     * Gets the {@link FXMLView} annotation from inheriting class.
     *
     * @return the FXML annotation
     */
    private FXMLView getFXMLAnnotation() {
        final Class<? extends AbstractFxmlView> theClass = this.getClass();
        final FXMLView annotation = theClass.getAnnotation(FXMLView.class);
        return annotation;
    }

    /**
     * Creates the fx for type.
     *
     * @return the object
     */
    private Object createControllerForAnnotation() {
        try {
            if ("".equals(controllerResources)) {
                return null;
            }
            try {
                Class<?> aClass = Class.forName(controllerResources);
                this.controller = aClass.newInstance();
                AbstractJavaFxApplicationSupport.addBean(aClass, controller);
                return controller;
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void setFxmlRootPath(final String path) {
        fxmlRoot = path;
    }


    /**
     * Load synchronously.
     *
     * @param resource the resource
     * @param bundle   the bundle
     * @return the FXML abs
     * @throws IllegalStateException the illegal state exception
     */
    private FXMLLoader loadSynchronously(final URL resource, final Optional<ResourceBundle> bundle) throws IllegalStateException {

        final FXMLLoader loader = new FXMLLoader(resource, bundle.orElse(null));
        loader.setControllerFactory(param -> createControllerForAnnotation());
        try {
            loader.load();
        } catch (final IOException | IllegalStateException e) {
            throw new IllegalStateException("Cannot load " + getConventionalName(), e);
        }

        return loader;
    }

    /**
     * Ensure fx.fxml abs initialized.
     */
    private void ensureFxmlLoaderInitialized() {
        if (fxmlLoader != null) {
            return;
        }
        fxmlLoader = loadSynchronously(resource, bundle);
        presenterProperty.set(fxmlLoader.getController());
    }

    /**
     * Sets up the first view using the primary {@link Stage}
     */
    protected void initFirstView() {
        Scene scene = getView().getScene() != null ? getView().getScene() : new Scene(getView());
        scene.setFill(GUIState.getSceneColor());
        GUIState.getStage().setScene(scene);
        GUIState.setScene(scene);
    }


    public void hide() {
        if (stage != null) {
            stage.hide();
        } else {
            Region root = fxmlLoader.getRoot();
            root.getScene().getWindow().hide();
        }
    }

    /**
     * Shows the FxmlView instance being the child stage of the given {@link Window}
     *
     * @param window   The owner of the FxmlView instance
     * @param modality See {@code javafx.stage.Modality}.
     */
    public void showView(Window window, Modality modality) {
        if (stage == null || currentStageModality != modality || !Objects.equals(stage.getOwner(), window)) {
            stage = createStage(modality);
            stage.initOwner(window);
        }
        stage.show();
    }

    /**
     * Shows the FxmlView instance on a top level {@link Window}
     *
     * @param modality See {@code javafx.stage.Modality}.
     */
    public void showView(Modality modality) {
        if (stage == null || currentStageModality != modality) {
            stage = createStage(modality);
        }
        stage.show();
    }

    /**
     * Shows the FxmlView instance being the child stage of the given {@link Window} and waits
     * to be closed before returning to the caller.
     *
     * @param window   The owner of the FxmlView instance
     * @param modality See {@code javafx.stage.Modality}.
     */
    public void showViewAndWait(Window window, Modality modality) {
        if (stage == null || currentStageModality != modality || !Objects.equals(stage.getOwner(), window)) {
            stage = createStage(modality);
            stage.initOwner(window);
        }
        stage.showAndWait();
    }

    /**
     * Shows the FxmlView instance on a top level {@link Window} and waits to be closed before
     * returning to the caller.
     *
     * @param modality See {@code javafx.stage.Modality}.
     */
    public void showViewAndWait(Modality modality) {
        if (stage == null || currentStageModality != modality) {
            stage = createStage(modality);
        }
        stage.showAndWait();
    }

    /**
     * 创建新的窗口
     *
     * @param modality
     * @return
     */
    private Stage createStage(Modality modality) {
        currentStageModality = modality;
        Stage stage = new Stage();
        stage.initModality(currentStageModality);
        stage.initStyle(getDefaultStyle());
        stage.setTitle(getDefaultTitle());
        List<Image> icon = getIcons();
        if (icon != null) {
            stage.getIcons().addAll(icon);
        } else {
            if (GUIState.getStage() != null) {
                List<Image> primaryStageIcons = GUIState.getStage().getIcons();
                stage.getIcons().addAll(primaryStageIcons);
            }
        }
        Scene scene = getView().getScene() != null ? getView().getScene() : new Scene(getView());
        scene.setFill(GUIState.getSceneColor());
        stage.setScene(scene);
        GUIState.setStage(stage);
        GUIState.setScene(scene);
        return stage;
    }

    /**
     * 获取图标图片
     *
     * @return
     */
    private List<Image> getIcons() {
        String[] icon = annotation.icon();
        if (icon.length == 0) {
            return null;
        }
        List<Image> images = new ArrayList<>();
        for (String imagePath : icon) {
            if ("".equals(imagePath)) {
                break;
            }
            Image image = new Image(imagePath);
            images.add(image);
        }
        return images;
    }


    /**
     * Initializes the view by loading the FXML (if not happened yet) and
     * returns the top Node (parent) specified in the FXML file.
     *
     * @return the root view as determined from {@link FXMLLoader}.
     */
    public Parent getView() {
        ensureFxmlLoaderInitialized();
        final Parent parent = fxmlLoader.getRoot();
        addCSSIfAvailable(parent);
        return parent;
    }


    /**
     * Initializes the view synchronously and invokes the consumer with the
     * created parent Node within the FX UI thread.
     *
     * @param consumer - an object interested in received the {@link Parent} as
     *                 callback
     */
    public void getView(final Consumer<Parent> consumer) {
        CompletableFuture.supplyAsync(this::getView, Platform::runLater).thenAccept(consumer);
    }

    /**
     * Scene Builder creates for each FXML document a root container. This
     * method omits the root container (e.g. {@link AnchorPane}) and gives you
     * the access to its first child.
     *
     * @return the first child of the {@link AnchorPane} or null if there are no
     * children available from this view.
     */
    public Node getViewWithoutRootContainer() {

        final ObservableList<Node> children = getView().getChildrenUnmodifiable();
        if (children.isEmpty()) {
            return null;
        }

        return children.listIterator().next();
    }

    /**
     * Adds the CSS if available.
     *
     * @param parent the parent
     */
    void addCSSIfAvailable(final Parent parent) {
        addCSSFromAnnotation(parent);
        final URL uri = getClass().getResource(getStyleSheetName());
        if (uri == null) {
            return;
        }
        final String uriToCss = uri.toExternalForm();
        parent.getStylesheets().add(uriToCss);
    }

    /**
     * Adds the CSS from annotation to parent.
     *
     * @param parent the parent
     */
    private void addCSSFromAnnotation(final Parent parent) {
        if (annotation != null && annotation.css().length > 0) {
            for (final String cssFile : annotation.css()) {
                final URL uri = getClass().getResource("/" + cssFile);
                if (uri != null) {
                    final String uriToCss = uri.toExternalForm();
                    parent.getStylesheets().add(uriToCss);
                }
            }
        }
    }

    /**
     * Gets the default title for to be shown in a (un)modal window.
     */
    String getDefaultTitle() {
        return annotation.title();
    }

    /**
     * Gets the default style for a (un)modal window.
     */
    StageStyle getDefaultStyle() {
        final StageStyle style = annotation.stageStyle();
        return style;
    }

    /**
     * Gets the style sheet name.
     *
     * @return the style sheet name
     */
    private String getStyleSheetName() {
        return fxmlRoot + getConventionalName(".css");
    }

    /**
     * In case the view was not initialized yet, the conventional fx.fxml
     * (airhacks.fx.fxml for the AirhacksView and AirhacksPresenter) are loaded and
     * the specified presenter / fx is going to be constructed and
     * returned.
     *
     * @return the corresponding fx / presenter (usually for a
     * AirhacksView the AirhacksPresenter)
     */
    public Object getPresenter() {
        ensureFxmlLoaderInitialized();
        return presenterProperty.get();
    }

    /**
     * Does not initialize the view. Only registers the Consumer and waits until
     * the the view is going to be created / the method FXMLView#getView or
     * FXMLView#getViewAsync invoked.
     *
     * @param presenterConsumer listener for the presenter construction
     */
    public void getPresenter(final Consumer<Object> presenterConsumer) {

        presenterProperty.addListener(
                (final ObservableValue<? extends Object> o, final Object oldValue, final Object newValue) -> {
                    presenterConsumer.accept(newValue);
                });
    }

    /**
     * Gets the conventional name.
     *
     * @param ending the suffix to append
     * @return the conventional name with stripped ending
     */
    private String getConventionalName(final String ending) {
        return getConventionalName() + ending;
    }

    /**
     * Gets the conventional name.
     *
     * @return the name of the view without the "View" prefix in lowerCase. For
     * AirhacksView just airhacks is going to be returned.
     */
    private String getConventionalName() {
        return stripEnding(getClass().getSimpleName().toLowerCase());
    }

    /**
     * Gets the bundle name.
     *
     * @return the bundle name
     */
    private String getBundleName() {
        String bundle = annotation.bundle();
        if (bundle == null || "".equals(bundle)) {
            final String lbundle = getClass().getPackage().getName() + "." + getConventionalName();
            return lbundle;
        }

        final String lbundle = annotation.bundle();
        return lbundle;
    }

    /**
     * Strip ending.
     *
     * @param clazz the clazz
     * @return the string
     */
    private static String stripEnding(final String clazz) {
        if (!clazz.endsWith("view")) {
            return clazz;
        }
        return clazz.substring(0, clazz.lastIndexOf("view"));
    }

    /**
     * Gets the fx.fxml file path.
     *
     * @return the relative path to the fx.fxml file derived from the FXML view.
     * e.g. The name for the AirhacksView is going to be
     * <PATH>/airhacks.fx.fxml.
     */

    final String getFxmlPath() {
        final String fxmlPath = fxmlRoot + getConventionalName(".fxml");
        return fxmlPath;
    }

    /**
     * Returns a resource bundle if available
     *
     * @param name the name of the resource bundle.
     * @return the resource bundle
     */
    private Optional<ResourceBundle> getResourceBundle(final String name) {
        try {
            return Optional.of(getBundle(name,
                    new ResourceBundleControl(getResourceBundleCharset())));
        } catch (final MissingResourceException ex) {
            return Optional.empty();
        }
    }

    /**
     * Returns the charset to use when reading resource bundles as specified in
     * the annotation.
     *
     * @return the charset
     */
    private Charset getResourceBundleCharset() {
        return Charset.forName(annotation.encoding());
    }

    /**
     * Gets the resource bundle.
     *
     * @return an existing resource bundle, or null
     */
    public Optional<ResourceBundle> getResourceBundle() {
        return bundle;
    }
}
