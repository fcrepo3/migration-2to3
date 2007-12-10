package fedora.utilities.digitalobject;

import java.io.File;

import java.util.Iterator;
import java.util.NoSuchElementException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import fedora.common.FaultException;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.file.FileUtil;

/**
 * An object iterator that works against a local Fedora repository.
 *
 * @author Chris Wilper
 */
class LocalRepoObjectIterator
        implements Iterator<DigitalObject> {
   
    /** The query to get all token, path pairs. */
    private static final String QUERY = "SELECT token, path FROM objectPaths";
    
    /** The default fetch size to use when running the query. */
    private static final int DEFAULT_FETCH_SIZE = 1000;
    
    /** The fetch size to use when running the query, if MySQL is being used. */
    private static final int MYSQL_FETCH_SIZE = Integer.MIN_VALUE;
    
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
        RepoUtil.close(m_conn);
    }

    //---
    // Instance helpers
    //---

    private ResultSet executeQuery() {
        try {
            m_conn.setAutoCommit(false);
            Statement st = m_conn.createStatement(
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            int fetchSize = DEFAULT_FETCH_SIZE;
            if (RepoUtil.isMySQL(m_conn)) {
                fetchSize = MYSQL_FETCH_SIZE;
            }
            st.setFetchSize(fetchSize);
            LOG.info("Executing query (fetchSize=" + fetchSize + "): "
                    + QUERY);
            return st.executeQuery(QUERY);
        } catch (SQLException e) {
            RepoUtil.close(m_conn);
            throw new FaultException("Error querying database", e);
        }
    }

    private DigitalObject getNext() {
        try {
            while (m_results.next()) {
                File file = FileUtil.getFile(m_objectStoreBase,
                        m_results.getString("path"));
                DigitalObject obj = null;
                try {
                    obj = RepoUtil.readObject(m_deserializer, file);
                    return obj;
                } finally {
                    if (obj == null) {
                        RepoUtil.close(m_conn);
                    }
                }
            }
        } catch (SQLException e) {
            RepoUtil.close(m_conn);
            throw new FaultException("Error getting next path from "
                    + "database", e);
        }
        RepoUtil.close(m_conn);
        return null;
    }
    
}
