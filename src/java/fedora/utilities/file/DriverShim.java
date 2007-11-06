/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.utilities.file;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.Properties;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;

/**
 * Allows one to load a JDBC driver at runtime. java.sql.DriverManager will
 * refuse to use a driver not loaded by the system ClassLoader. The workaround
 * for this is to use a shim class that implements java.sql.Driver. This shim
 * class will do nothing but call the methods of an instance of a JDBC driver
 * that is loaded dynamically. This works because DriverShim is loaded by the
 * system class loader, and DriverManager doesn't care that it invokes a class
 * that wasn't. Note that we must perform the registration on the instance
 * ourselves. See the utility method, loadAndRegister and the command-line test
 * below. Adapted from http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
 * 
 * @author Chris Wilper
 */
public class DriverShim
        implements Driver {

    /** The JDBC driver we're wrapping. */
    private Driver m_driver;

    /**
     * Constructs a DriverShim over the given driver in order to make it look
     * like it came from this classloader.
     * 
     * @param driver the driver.
     */
    public DriverShim(Driver driver) {
        m_driver = driver;
    }

    /**
     * Loads the driver from the given jar file, then registers it with the
     * driver manager.
     * 
     * @param driverJarFile the driver jar file.
     * @param driverClassName the driver class name.
     * @throws ClassNotFoundException if the class can't be found.
     */
    public static final void loadAndRegister(File driverJarFile,
            String driverClassName)
            throws ClassNotFoundException {
        try {
            loadAndRegister(new URL("jar:" + driverJarFile.toURI() + "!/"),
                    driverClassName);
        } catch (MalformedURLException wontHappen) {
            throw new RuntimeException(wontHappen);
        }
    }

    /**
     * Loads the driver from the given URL pointing to the jar file, then
     * registers it with the driver manager.
     * 
     * @param driverURL the URL to the jar file.
     * @param driverClassName the driver class name
     * @throws ClassNotFoundException if the class can't be found.
     */
    public static final void loadAndRegister(URL driverURL,
            String driverClassName) 
            throws ClassNotFoundException {
        try {
            URLClassLoader urlCL = new URLClassLoader(new URL[] {driverURL});
            Driver driver = (Driver) Class.forName(
                    driverClassName, true, urlCL).newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //---
    // Driver implementation
    //---

    /**
     * {@inheritDoc}
     */
    public boolean acceptsURL(String u) throws SQLException {
        return m_driver.acceptsURL(u);
    }

    /**
     * {@inheritDoc}
     */
    public Connection connect(String u, Properties p) throws SQLException {
        return m_driver.connect(u, p);
    }

    /**
     * {@inheritDoc}
     */
    public int getMajorVersion() {
        return m_driver.getMajorVersion();
    }

    /**
     * {@inheritDoc}
     */
    public int getMinorVersion() {
        return m_driver.getMinorVersion();
    }

    /**
     * {@inheritDoc}
     */
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p)
            throws SQLException {
        return m_driver.getPropertyInfo(u, p);
    }

    /**
     * {@inheritDoc}
     */
    public boolean jdbcCompliant() {
        return m_driver.jdbcCompliant();
    }

}