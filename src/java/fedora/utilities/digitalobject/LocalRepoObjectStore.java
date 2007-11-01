/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
 * @author Chris Wilper
 */
public class LocalRepoObjectStore
        implements ObjectStore {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            LocalRepoObjectStore.class);
   
    /** The base directory where Fedora objects are stored. */
    private final File m_objectStoreBase;
   
    /** The URL, username, and password info for the Fedora database. */
    private final Map<String, String> m_dbInfo;
   
    /**
     * Creates an instance.
     * 
     * @param fedoraHome the FEDORA_HOME directory.
     */
    public LocalRepoObjectStore(File fedoraHome) {
        ServerConfiguration serverConfig = getServerConfig(fedoraHome);
        m_objectStoreBase = getObjectStoreBase(serverConfig, fedoraHome);
        m_dbInfo = getDBInfo(serverConfig);
    }
       
    //---
    // ObjectStore implementation
    //---

    /**
     * {@inheritDoc}
     */
    public DigitalObject getObject(String pid) {
        // TODO: implement this
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean replaceObject(DigitalObject obj) {
        // TODO: implement this
        return false;
    }
    
    //---
    // ObjectLister implementation
    //---

    /**
     * {@inheritDoc}
     */
    public Iterator<DigitalObject> iterator() {
        // TODO Auto-generated method stub
        return null;
    }
    
    //---
    // Instance helpers
    //---
    
    private Connection getConnection() {
        String url = m_dbInfo.get("jdbcURL");
        try {
            return DriverManager.getConnection(url,
                    m_dbInfo.get("dbUsername"),
                    m_dbInfo.get("dbPassword"));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to the database at "
                    + url + " -- make sure it's running and the username "
                    + "and password are correct.");
        }
    }
    
    //---
    // Static helpers
    //---
    
    private static Map<String, String> getDBInfo(
            ServerConfiguration serverConfig) {
        
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

        initJDBC(getRequiredParam(storeConfig, "jdbcDriverClass"));

        Map<String, String> map = new HashMap<String, String>();
        map.put("jdbcURL", getRequiredParam(storeConfig, "jdbcURL"));
        map.put("dbUsername", getRequiredParam(storeConfig, "dbUsername"));
        map.put("dbPassword", getRequiredParam(storeConfig, "dbPassword"));
        return map;
    }
   
    private static File getObjectStoreBase(ServerConfiguration serverConfig,
            File fedoraHome) {
        ModuleConfiguration llConfig = getRequiredModuleConfig(serverConfig,
                "fedora.server.storage.lowlevel.ILowlevelStorage",
                "fedora.server.storage.lowlevel.DefaultLowlevelStorageModule");
        return getRequiredFileParam(llConfig, "object_store_base", fedoraHome);
    }
    
    private static void initJDBC(String jdbcDriverClass) {
        try {
            Class.forName(jdbcDriverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to initialize database --"
                    + " the JDBC driver " + jdbcDriverClass 
                    + " is not in the CLASSPATH");
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