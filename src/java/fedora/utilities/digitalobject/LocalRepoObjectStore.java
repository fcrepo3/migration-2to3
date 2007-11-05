/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import java.io.File;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import java.sql.Connection;

import fedora.server.config.ServerConfiguration;
import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.config.ConfigUtil;

/**
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
    
    /** The deserializer to use. */
    private final DODeserializer m_deserializer;
   
    /** The base directory where Fedora objects are stored. */
    private final File m_objectStoreBase;
   
    /** The URL, username, and password info for the Fedora database. */
    private final Map<String, String> m_dbInfo;
   
    /**
     * Creates an instance.
     * 
     * @param fedoraHome the FEDORA_HOME directory.
     * @param jdbcJar a jar containing the appropriate jdbc driver, or null
     *                if it's already in the classpath.
     * @param deserializer the deserializer to use.
     */
    public LocalRepoObjectStore(File fedoraHome, File jdbcJar,
            DODeserializer deserializer) {
        m_deserializer = deserializer;
        ServerConfiguration serverConfig = RepoUtil.getServerConfig(fedoraHome);
        m_objectStoreBase = RepoUtil.getObjectStoreBase(
                serverConfig, fedoraHome);
        m_dbInfo = RepoUtil.getDBInfo(serverConfig, jdbcJar);
        Connection conn = RepoUtil.getConnection(m_dbInfo);
        try {
            RepoUtil.buildObjectPathsIfNeeded(conn, m_objectStoreBase,
                    m_deserializer);
        } finally {
            RepoUtil.close(conn);
        }
    }
    
    /**
     * Creates an instance from properties.
     * 
     * <pre>
     *   fedoraHome   (required) - the Fedora home directory.
     *   jdbcJar      (optional) - a jar containing the appropriate jdbc
     *                             driver, if it's not already in the classpath.
     *   deserializer (required) - the deserializer to use;
     *                             default is DEFAULT_DESERIALIZER.
     * </pre>
     * 
     * @param props the properties.
     */
    public LocalRepoObjectStore(Properties props) {
        this(new File(ConfigUtil.getRequiredString(props, "fedoraHome")),
                ConfigUtil.getOptionalFile(props, "jdbcJar", null),
                (DODeserializer) ConfigUtil.construct(props, "deserializer", 
                DEFAULT_DESERIALIZER));
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
        return new LocalRepoObjectIterator(m_objectStoreBase, 
                RepoUtil.getConnection(m_dbInfo),
                m_deserializer.getInstance());
    }
    
}