package fedora.utilities.cmda.analyzer;

import java.io.File;

import java.util.Iterator;

import fedora.server.storage.types.DigitalObject;

/**
 * An object source that crawls a directory looking for Fedora objects.
 *
 * @author cwilper@cs.cornell.edu
 */
public class DirObjectSource implements ObjectSource {

    private File m_baseDir;

    /**
     * Constructs an instance based at the given directory.
     */
    public DirObjectSource(File baseDir) {
        // TODO: also take deserializer param
        m_baseDir = baseDir;
    }

    //---
    // Iterator<DigitalObject> implementation
    //---

    public boolean hasNext() {
        // TODO: use dir info
        return false;
    }

    public DigitalObject next() {
        // TODO: return next
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
    public void close() { }

}
