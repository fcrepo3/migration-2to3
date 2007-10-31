package fedora.utilities.cmda.analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import fedora.server.config.Configuration;
import fedora.server.config.DatastoreConfiguration;
import fedora.server.config.ModuleConfiguration;
import fedora.server.config.Parameter;
import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;
import fedora.server.storage.types.DigitalObject;

/**
 * An object source that uses a local Fedora repository to iterate
 * digital objects.
 *
 * @author Chris Wilper
 */
public class RepositoryObjectSource
        implements ObjectSource {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            RepositoryObjectSource.class);
   
    /**
     * Constructs an instance.
     * 
     * @param fedoraHome the Fedora home directory.
     */
    public RepositoryObjectSource(File fedoraHome) {
        init(fedoraHome);
    }
   
    //---
    // Iterator<DigitalObject> implementation
    //---

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        // TODO: implement this
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public DigitalObject next() {
        // TODO: implement this
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
    }

    //---
    // ObjectSource implementation
    //---

    /**
     * {@inheritDoc}
     */
    public void close() {
        // TODO: implement this
    }
    
    //---
    // Instance helpers
    //---
    
    private void init(File fedoraHome) {
        // make sure fedoraHome exists and read server config file
        if (!fedoraHome.isDirectory()) {
            throw new IllegalArgumentException("No such directory: "
                    + fedoraHome.getPath());
        }
        ServerConfiguration serverConfig = getServerConfig(fedoraHome);

        // get LLStore's object_store_base
        ModuleConfiguration llConfig = getRequiredModuleConfig(serverConfig,
                "fedora.server.storage.lowlevel.ILowlevelStorage",
                "fedora.server.storage.lowlevel.DefaultLowlevelStorageModule");
        File objectStoreBase = getRequiredFileParam(llConfig,
                "object_store_base", fedoraHome);
       
        // get ConnectionPoolManager's defaultPoolName
        ModuleConfiguration cpmConfig = getRequiredModuleConfig(serverConfig,
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

        // initialize a connection to the database
        Connection conn = initConnection(
                getRequiredParam(storeConfig, "jdbcDriverClass"),
                getRequiredParam(storeConfig, "jdbcURL"),
                getRequiredParam(storeConfig, "dbUsername"),
                getRequiredParam(storeConfig, "dbPassword"));

        // finish initialization
        init(conn, objectStoreBase);
    }
    
    private void init(Connection conn, File objectStoreBase) {
        
    }
    
    private static Connection initConnection(String jdbcDriverClass,
            String jdbcURL, String dbUsername, String dbPassword) {
        try {
            Class.forName(jdbcDriverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to initialize database --"
                    + " the JDBC driver " + jdbcDriverClass 
                    + " is not in the CLASSPATH");
        }
        try {
            return DriverManager.getConnection(jdbcURL, dbUsername, dbPassword);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to the database at "
                    + jdbcURL + " -- make sure it's running and the username "
                    + "and password are correct.");
        }
    }
    
    private static ModuleConfiguration getRequiredModuleConfig(
            ServerConfiguration serverConfig, String role,
            String expectedImpl) {
        ModuleConfiguration moduleConfig = serverConfig.getModuleConfiguration(
                role);
        if (moduleConfig == null) {
            throw new RuntimeException("Cannot find configuration for module "
                    + role + " in fcfg");
        }
        if (moduleConfig.getClassName().equals(expectedImpl)) {
            LOG.warn("Expected " + expectedImpl + " as implementing class "
                    + "for module " + role + " in fcfg");
        }
        return moduleConfig;
    }
    
    private static File getRequiredFileParam(Configuration config,
            String name, File fedoraHome) {
        String path = getRequiredParam(config, name);
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(fedoraHome, path);
    }
    
    private static String getRequiredParam(Configuration config,
            String name) {
        Parameter param = config.getParameter(name);
        if (param == null) {
            throw new RuntimeException("Required fcfg parameter missing: "
                    + name);
        }
        return param.getValue();
    }

    private static ServerConfiguration getServerConfig(File fedoraHome) {
        File fcfg = new File(fedoraHome, "server/fedora.fcfg");
        if (!fcfg.exists()) {
            throw new IllegalArgumentException("No such file: "
                    + fcfg.getPath());
        }
        try {
            return new ServerConfigurationParser(
                    new FileInputStream(fcfg)).parse();
        } catch (IOException e) {
            throw new RuntimeException("Error parsing " + fcfg.getPath());
        }
    }
}
