/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileFilter;

import java.util.Iterator;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.types.DigitalObject;

/**
 * Provides an iterator of digital objects that crawls a given directory.
 *
 * @author Chris Wilper
 */
public class DirObjectLister
        implements ObjectLister {
    
    /** The deserializer that will be used if none is specified. */
    public static final String DEFAULT_DESERIALIZER =
            "fedora.server.storage.translation.FOXML1_0DODeserializer";

    /** The file filter that will be used if none is specified. */
    public static final String DEFAULT_FILE_FILTER =
            "fedora.utilities.cmda.analyzer.DefaultFileFilter";

    /** The directory to start at. */
    private final File m_dir;
    
    /** The file filter to use, null if none. */
    private final FileFilter m_filter;
    
    /** The deserializer to use. */
    private final DODeserializer m_deserializer;
    
    /**
     * Creates an instance.
     * 
     * @param dir the directory to start at.
     * @param filter the file filter to use, null if none.
     * @param deserializer the deserializer to use.
     */
    public DirObjectLister(File dir, FileFilter filter,
            DODeserializer deserializer) {
        m_dir = dir;
        m_filter = filter;
        m_deserializer = deserializer;
    }
    
    //---
    // ObjectLister implementation
    //---

    /**
     * {@inheritDoc}
     */
    public Iterator<DigitalObject> iterator() {
        return new DirObjectIterator(m_dir, m_filter, m_deserializer);
    }

}
