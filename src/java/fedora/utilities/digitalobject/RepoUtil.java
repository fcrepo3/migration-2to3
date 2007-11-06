/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import fedora.server.config.Configuration;
import fedora.server.config.DatastoreConfiguration;
import fedora.server.config.ModuleConfiguration;
import fedora.server.config.Parameter;
import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;
import fedora.server.storage.translation.DODeserializer;

import fedora.utilities.file.DriverShim;

/**
 * Utility methods for working with a local Fedora repository.
 * 
 * @author Chris Wilper
 */
abstract class RepoUtil {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(RepoUtil.class);

    /**
     * Gets a new database connection.
     * 
     * @param dbInfo the jdbcURL, dbUsername, and dbPassword.
     * @return the connection.
     */
    public static Connection getConnection(Map<String, String> dbInfo) {
        String url = dbInfo.get("jdbcURL");
        try {
            return DriverManager.getConnection(url, dbInfo.get("dbUsername"),
                    dbInfo.get("dbPassword"));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to the database at "
                    + url + " -- make sure it's running and the username "
                    + "and password are correct.");
        }
    }

    /**
     * Populates the <code>objectPaths</code> table if it's empty.
     * 
     * @param conn the connection to use.
     * @param objectStoreBase the base directory where Fedora objects are
     *        stored.
     * @param deserializer the deserializer to use.
     */
    public static void buildObjectPathsIfNeeded(Connection conn,
        File objectStoreBase, DODeserializer deserializer) {
        int count = countObjectPaths(conn);
        if (count == 0) {
            LOG.info("objectPaths table is empty");
            buildObjectPaths(conn, objectStoreBase, deserializer);
        } else {
            LOG.info("objectPaths table has " + count + " rows");
        }
    }

    /**
     * Gets database connection information from the server configuration
     * and ensures the JDBC driver is registered with the 
     * <code>DriverManager</code>.
     * 
     * @param serverConfig server configuration.
     * @param jdbcJar the JDBC jar file containing the driver to register,
     *        or null if the jar is already expected to be in the classpath.
     * @return a map containing jdbcURL, dbUsername, and dbPassword values.
     */
    public static Map<String, String> getDBInfo(
            ServerConfiguration serverConfig, File jdbcJar) {

        // get ConnectionPoolManager's defaultPoolName
        ModuleConfiguration cpmConfig =
                getRequiredModuleConfig(serverConfig,
                        "fedora.server.storage.ConnectionPoolManager",
                        "fedora.server.storage.ConnectionPoolManagerImpl");
        String defaultPoolName = getRequiredParam(cpmConfig, "defaultPoolName");

        // get needed values from that datastore
        DatastoreConfiguration storeConfig =
                serverConfig.getDatastoreConfiguration(defaultPoolName);
        if (storeConfig == null) {
            throw new RuntimeException("Cannot find datastore configuration: "
                    + defaultPoolName);
        }

        initJDBC(getRequiredParam(storeConfig, "jdbcDriverClass"), jdbcJar);

        Map<String, String> map = new HashMap<String, String>();
        map.put("jdbcURL", getRequiredParam(storeConfig, "jdbcURL"));
        map.put("dbUsername", getRequiredParam(storeConfig, "dbUsername"));
        map.put("dbPassword", getRequiredParam(storeConfig, "dbPassword"));
        return map;
    }

    /**
     * Gets the object store base directory from the server configuration.
     * 
     * @param serverConfig the server configuration.
     * @param fedoraHome the directory to use to make the path absolute
     *        if it's specified as relative to FEDORA_HOME.
     * @return the directory.
     */
    public static File getObjectStoreBase(ServerConfiguration serverConfig,
            File fedoraHome) {
        ModuleConfiguration llConfig =
                getRequiredModuleConfig(serverConfig,
                "fedora.server.storage.lowlevel.ILowlevelStorage",
                "fedora.server.storage.lowlevel.DefaultLowlevelStorageModule");
        return getRequiredFileParam(llConfig, "object_store_base", fedoraHome);
    }

    /**
     * Reads the server configuration at 
     * <code>FEDORA_HOME/server/config/fedora.fcfg</code>.
     * 
     * @param fedoraHome the fedora home directory.
     * @return the server configuration.
     */
    public static ServerConfiguration getServerConfig(File fedoraHome) {
        File fcfg = new File(fedoraHome, "server/config/fedora.fcfg");
        if (!fcfg.exists()) {
            throw new IllegalArgumentException("No such file: "
                    + fcfg.getPath());
        }
        try {
            return new ServerConfigurationParser(new FileInputStream(fcfg))
                    .parse();
        } catch (IOException e) {
            throw new RuntimeException("Error parsing " + fcfg.getPath());
        }
    }

    /**
     * Closes the connection if it's not null and not already closed.
     * 
     * @param conn the connection.
     */
    public static void close(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            LOG.warn("Error closing connection", e);
        }
    }

    /**
     * Closes the statement if it's not null.
     * 
     * @param st the statement.
     */
    public static void close(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException e) {
            LOG.warn("Error closing statement", e);
        }
    }

    private static ModuleConfiguration getRequiredModuleConfig(
            ServerConfiguration serverConfig, String role, 
            String expectedImpl) {
        ModuleConfiguration moduleConfig =
                serverConfig.getModuleConfiguration(role);
        if (moduleConfig == null) {
            throw new RuntimeException("Cannot find configuration for module "
                    + role + " in fcfg");
        }
        if (!moduleConfig.getClassName().equals(expectedImpl)) {
            LOG.warn("Expected " + expectedImpl + " as implementing class "
                    + "for module " + role + " in fcfg");
        }
        return moduleConfig;
    }

    private static File getRequiredFileParam(Configuration config, String name,
            File fedoraHome) {
        String path = getRequiredParam(config, name);
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(fedoraHome, path);
    }

    private static String getRequiredParam(Configuration config, String name) {
        Parameter param = config.getParameter(name);
        if (param == null) {
            throw new RuntimeException("Required fcfg parameter missing: "
                    + name);
        }
        return param.getValue();
    }

    private static void buildObjectPaths(Connection conn,
            File objectStoreBase, DODeserializer deserializer) {
        LOG.info("Building objectPaths table from objects at "
                + objectStoreBase.getPath());
        DirObjectIterator iter = new DirObjectIterator(objectStoreBase, null,
                deserializer);
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO objecPaths (token, path) "
                    + "VALUES (%, %)");
            while (iter.hasNext()) {
                int count = 0;
                while (iter.hasNext() && count < 5000) {
                }
            }
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
        // TODO: implement
    }

    private static int countObjectPaths(Connection conn) {
        Statement st = null;
        try {
            st = conn.createStatement();
            ResultSet results =
                    st.executeQuery("SELECT COUNT(*) FROM objectPaths");
            results.next();
            return results.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error counting objectPaths", e);
        } finally {
            close(st);
        }
    }

    private static void initJDBC(String jdbcDriverClass, File jdbcJar) {
        if (jdbcJar == null) {
            try {
                Class.forName(jdbcDriverClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Cannot find jdbc driver "
                        + jdbcDriverClass + " in classpath (try specifying "
                        + "jdbcJar=/your/jdbc/driver.jar)");
            }
        } else {
            try {
                DriverShim.loadAndRegister(jdbcJar, jdbcDriverClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Cannot find jdbc driver "
                        + jdbcDriverClass + " in file: " + jdbcJar.getPath());
            }
        }
    }

}
