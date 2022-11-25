package com.jack.jfx.handler;

/**
 * @author gj
 */
public class PropertyReaderHelper {

    private PropertyReaderHelper() {
    }

    /**
     * Determine file path from package name creates from class package instance
     * the file path equivalent. The path will be prefixed and suffixed with a
     * slash.
     *
     * @return the path equivalent to a package structure.
     */
    public static final String determineFilePathFromPackageName(final Class<?> clazz) {
        return "/" + clazz.getPackage().getName().replace('.', '/') + "/";
    }
}
