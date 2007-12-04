/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fedora.common.FaultException;

import fedora.server.config.ServerConfiguration;
import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOSerializer;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.config.ConfigUtil;
import fedora.utilities.file.FileUtil;

/**
 * Non-threadsafe interface to a local repository.
 * 
 * @author Chris Wilper
 */
public class LocalRepoObjectStore
        implements ObjectStore {
    
    /** 
     * The deserializer that will be used if none is specified;
     * <code>fedora.server.storage.translation.FOXML1_0DODeserializer</code>
     */
    public static final String DEFAULT_DESERIALIZER =
            "fedora.server.storage.translation.FOXML1_0DODeserializer";
    
    /** 
     * The serializer that will be used if none is specified;
     * <code>fedora.server.storage.translation.FOXML1_1DOSerializer</code>
     */
    public static final String DEFAULT_SERIALIZER =
            "fedora.server.storage.translation.FOXML1_1DOSerializer";
    
    /** The deserializer to use. */
    private final DODeserializer m_deserializer;
   
    /** The serializer to use. */
    private final DOSerializer m_serializer;
   
    /** The base directory where Fedora objects are stored. */
    private final File m_objectStoreBase;
   
    /** The URL, username, and password info for the Fedora database. */
    private final Map<String, String> m_dbInfo;
    
    /** The connection this instance uses. */
    private final Connection m_conn;
   
    /** The prepared statement this instance uses. */
    private final PreparedStatement m_st;
   
    /**
     * Creates an instance.
     * 
     * @param fedoraHome the FEDORA_HOME directory.
     * @param jdbcJar a jar containing the appropriate jdbc driver, or null
     *                if it's already in the classpath.
     * @param deserializer the deserializer to use.
     * @param serializer the serializer to use.
     */
    public LocalRepoObjectStore(File fedoraHome, File jdbcJar,
            DODeserializer deserializer, DOSerializer serializer) {
        m_deserializer = deserializer;
        m_serializer = serializer;
        ServerConfiguration serverConfig = RepoUtil.getServerConfig(fedoraHome);
        m_objectStoreBase = RepoUtil.getObjectStoreBase(
                serverConfig, fedoraHome);
        m_dbInfo = RepoUtil.getDBInfo(serverConfig, jdbcJar);
        m_conn = RepoUtil.getConnection(m_dbInfo);
        boolean initialized = false;
        try {
            RepoUtil.buildObjectPathsIfNeeded(m_conn, m_objectStoreBase,
                    m_deserializer);
            m_st = m_conn.prepareStatement(
                    "SELECT path FROM objectPaths WHERE token = ?");
            initialized = true;
        } catch (SQLException e) {
            throw new FaultException("Error preparing statement", e);
        } finally {
            if (!initialized) {
                close();
            }
        }
    }
    
    /**
     * Creates an instance from properties.
     * 
     * <pre>
     *   fedoraHome   (required) - the Fedora home directory.
     *   jdbcJar      (optional) - a jar containing the appropriate jdbc
     *                             driver, if it's not already in the classpath.
     *   deserializer (optional) - the deserializer to use;
     *                             default is DEFAULT_DESERIALIZER.
     *   serializer   (optional) - the serializer to use;
     *                             default is DEFAULT_SERIALIZER.
     * </pre>
     * 
     * @param props the properties.
     */
    public LocalRepoObjectStore(Properties props) {
        this(new File(ConfigUtil.getRequiredString(props, "fedoraHome")),
                ConfigUtil.getOptionalFile(props, "jdbcJar", null),
                (DODeserializer) ConfigUtil.construct(props, "deserializer", 
                DEFAULT_DESERIALIZER),
                (DOSerializer) ConfigUtil.construct(props, "serializer", 
                DEFAULT_SERIALIZER));
    }
    
    //---
    // ObjectStore implementation
    //---

    /**
     * {@inheritDoc}
     */
    public DigitalObject getObject(String pid) {
        File file = getFile(pid);
        if (file == null) {
            return null;
        }
        return RepoUtil.readObject(m_deserializer, file);
    }
    
    /**
     * {@inheritDoc}
     */
    public InputStream getObjectStream(String pid) {
        File file = getFile(pid);
        if (file == null) {
            return null;
        }
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            throw new FaultException("Error reading: " + file.getPath(), e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean replaceObject(DigitalObject obj) {
        File file = getFile(obj.getPid());
        if (file == null) {
            return false;
        }
        RepoUtil.writeObject(m_serializer, obj, file);
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean replaceObject(String pid, InputStream source) {
        File file = getFile(pid);
        if (file == null) {
            return false;
        }
        FileUtil.writeFile(source, file);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        RepoUtil.close(m_st);
        RepoUtil.close(m_conn);
    }
    
    //---
    // ObjectLister implementation
    //---

    /**
     * {@inheritDoc}
     */
    public Iterator<DigitalObject> iterator() {
        return new LocalRepoObjectIterator(m_objectStoreBase, 
                RepoUtil.getConnection(m_dbInfo),
                m_deserializer.getInstance());
    }
    
    //---
    // Object overrides
    //---
   
    /**
     * {@inheritDoc}
     */
    @Override
    public void finalize() {
        close();
    }
    
    //---
    // Instance helpers
    //---
    
    private File getFile(String pid) {
        String path = getPath(pid);
        if (path == null) {
            return null;
        }
        return FileUtil.getFile(m_objectStoreBase, path);
    }
   
    private String getPath(String pid) {
        ResultSet results = null;
        try {
            m_st.setString(1, pid);
            results = m_st.executeQuery();
            if (!results.next()) {
                return null;
            }
            return results.getString(1);
        } catch (SQLException e) {
            throw new FaultException(
                    "Error querying database for object path", e);
        } finally {
            RepoUtil.close(results);
        }
    }
    
}