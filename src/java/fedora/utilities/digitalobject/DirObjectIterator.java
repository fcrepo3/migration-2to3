package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileFilter;

import java.util.Iterator;
import java.util.NoSuchElementException;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.file.RecursiveFileIterator;

/**
 * An object iterator that crawls a given directory.
 *
 * @author Chris Wilper
 */
class DirObjectIterator
        implements Iterator<DigitalObject> {

    /** A file iterator starting at the source directory. */
    private final RecursiveFileIterator m_files;

    /** The deserializer this instance uses. */
    private final DODeserializer m_deserializer;

    /** The next object (null when exhausted). */
    private DigitalObject m_next;

    /** The file associated with the most last object returned by next(). */
    private File m_currentFile;

    /** The file associated with the next object to be returned by next(). */
    private File m_nextFile;

    /**
     * Constructs an instance.
     * 
     * @param sourceDir directory to start from.
     * @param filter the file filter to use, null if none.
     * @param deserializer the deserializer to use.
     */
    public DirObjectIterator(File sourceDir, FileFilter filter, 
            DODeserializer deserializer) {
        m_files = new RecursiveFileIterator(sourceDir, filter);
        m_deserializer = deserializer;
        m_next = getNext();
    }
    
    /**
     * Gets the most recently encountered file.
     * 
     * @return the file, or null if next() hasn't yet been called or
     *         the iterator is empty.
     */
    public File currentFile() {
        return m_currentFile;
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
        m_currentFile = m_nextFile;
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
    // Instance helpers
    //---

    private DigitalObject getNext() {
        while (m_files.hasNext()) {
            m_nextFile = m_files.next();
            return RepoUtil.readObject(m_deserializer, m_nextFile);
        }
        return null;
    }

}
