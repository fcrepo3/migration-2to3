package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import fedora.server.errors.ServerException;
import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

/**
 * An object iterator that works against a local Fedora repository.
 *
 * @author Chris Wilper
 */
class LocalRepoObjectIterator
        implements Iterator<DigitalObject> {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            LocalRepoObjectIterator.class);

    /** The base dir to use when resolving relative paths. */
    private final File m_objectStoreBase;
    
    /** The database connection to use. */
    private final Connection m_conn;

    /** The deserializer to use. */
    private final DODeserializer m_deserializer;
    
    /** The result set to be iterated over. */
    private final ResultSet m_results;

    /** The next object (null when exhausted). */
    private DigitalObject m_next;

    /**
     * Constructs an instance.
     * 
     * @param objectStoreBase the base dir to use when resolving relative paths.
     * @param conn the database connection to use.
     * @param deserializer the deserializer to use.
     */
    public LocalRepoObjectIterator(File objectStoreBase, Connection conn,
            DODeserializer deserializer) {
        m_objectStoreBase = objectStoreBase;
        m_conn = conn;
        m_deserializer = deserializer;
        m_results = executeQuery();
        m_next = getNext();
    }
        
    //---
    // Iterator<DigitalObject> implementation
    //---

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return m_next != null;
    }

    /**
     * {@inheritDoc}
     */
    public DigitalObject next() {
        if (m_next == null) {
            throw new NoSuchElementException("Iterator exhausted");
        }
        DigitalObject current = m_next;
        m_next = getNext();
        return current;
    }

    /**
     * {@inheritDoc}
     */
    public void remove() { 
        throw new UnsupportedOperationException("Remove not supported");
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

    private ResultSet executeQuery() {
        try {
            Statement st = m_conn.createStatement();
            return st.executeQuery("SELECT token, path FROM objectPaths");
        } catch (SQLException e) {
            close();
            throw new RuntimeException("Error querying database", e);
        }
    }

    private DigitalObject getNext() {
        try {
            while (m_results.next()) {
                File file = getFile(m_results.getString("path"));
                Exception err = null;
                try {
                    DigitalObject obj = new BasicDigitalObject();
                    m_deserializer.deserialize(
                            new FileInputStream(file), obj, "UTF-8",
                            DOTranslationUtility.DESERIALIZE_INSTANCE);
                    return obj;
                } catch (IOException e) {
                    err = e;
                } catch (ServerException e) {
                    err = e;
                } finally {
                    if (err != null) {
                        close();
                        throw new RuntimeException("Error deserializing "
                                + file.getPath(), err);
                    }
                }
            }
        } catch (SQLException e) {
            close();
            throw new RuntimeException("Error getting next path from "
                    + "database", e);
        }
        close();
        return null;
    }
    
    private File getFile(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(m_objectStoreBase, path);
    }
    
    private void close() {
        try {
            if (!m_conn.isClosed()) {
                m_conn.close();
            }
        } catch (SQLException e) {
            LOG.warn("Error closing connection", e);
        }
    }

}
