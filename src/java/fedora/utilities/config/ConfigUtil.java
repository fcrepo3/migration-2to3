/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.config;

import java.util.Properties;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

/**
 * Defines several utility methods for performing configuration-related tasks.
 * 
 * @author Chris Wilper
 */
public abstract class ConfigUtil {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(ConfigUtil.class);
    
    /**
     * Gets a required string from properties.
     * 
     * @param props properties in which to find the value.
     * @param name property name.
     * @return the value.
     * @throws IllegalArgumentException if the property isn't found.
     */
    public static String getRequiredString(Properties props, String name) {
        String value = props.getProperty(name);
        if (value == null) {
            throw new IllegalArgumentException("Required property missing: "
                    + name);
        } else {
            return value;
        }
    }

    /**
     * Gets an optional integer from properties.
     * 
     * @param props properties in which to find the value.
     * @param name property name.
     * @param defaultValue the value to return if the property isn't found.
     * @return the value.
     */
    public static int getOptionalInt(Properties props, String name,
            int defaultValue) {
        String value = props.getProperty(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not an integer: " + value);
            }
        }
    }

    /**
     * Creates an instance of a configurable class.
     * 
     * @param props properties in which to find the value, and to pass to
     *        the properties constructor, if such a constructor exists.
     * @param propName the property specifying the class to construct.
     * @param defaultClassName the class to use if the property is unspecified.
     * @return the object.
     * @throws RuntimeException if construction fails for any reason.
     */
    public static Object construct(Properties props, String propName,
            String defaultClassName) {
        String className = props.getProperty(propName);
        if (className == null) {
            className = defaultClassName;
        }
        try {
            Class clazz = Class.forName(className);
            return clazz.getConstructor(Properties.class).newInstance(props);
        } catch (NoSuchMethodException e) {
            try {
                return Class.forName(className).newInstance();
            } catch (Throwable th) {
                throw constructionFailed(className, th);
            }
        } catch (Throwable th) {
            throw constructionFailed(className, th);
        }
    }
    
    private static RuntimeException constructionFailed(String className,
            Throwable th) {
        String message = "Error constructing class: " + className;
        LOG.debug(message, th); // so we have a chance to see the full trace
        if (th instanceof InvocationTargetException) {
            Throwable cause = th.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(message, th.getCause());
            }
        } else {
            throw new RuntimeException(message, th);
        }
    }
 

}
