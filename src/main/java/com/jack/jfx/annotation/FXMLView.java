package com.jack.jfx.annotation;

import javafx.stage.StageStyle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author gj
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FXMLView {

    /**
     * Value refers to a relative path from where to load a certain fx.fxml file.
     *
     * @return the relative file path of a views fx.fxml file.
     */
    String value() default "";

    /**
     * Css files to be used together with this view.
     *
     * @return the string[] listing all css files.
     */
    String[] css() default {};

    /**
     * Resource bundle to be used with this view.
     *
     * @return the string of such resource bundle.
     */
    String bundle() default "";

    /**
     * The encoding that will be sued when reading the {@link #bundle()} file.
     * The default encoding is ISO-8859-1.
     *
     * @return the encoding to use when reading the resource bundle
     */
    String encoding() default "UTF-8";

    /**
     * The default title for this view for modal.
     *
     * @return The default title string.
     */
    String title() default "提示";

    /**
     * The style to be applied to the underlying stage
     * when using this view as a modal window.
     */
    StageStyle stageStyle() default StageStyle.TRANSPARENT;

    /**
     * The fx for this fxml fx path
     *
     * @return
     */
    String controller() default "";

    /**
     * The icon for this Stage icon path
     *
     * @return
     */
    String[] icon() default {};
}
