package fedora.utilities.file;

import java.io.File;
import java.io.FileFilter;

/**
 * A simple file filter that accepts any file or directory not beginning 
 * with "." (dot).
 *
 * @author Chris Wilper
 */
public class NoDotFileFilter implements FileFilter {

    /**
     * Creates an instance.
     */
    public NoDotFileFilter() {
        // no-op
    }

    //---
    // FileFilter implementation
    //---

    /**
     * {@inheritDoc}
     */
    public boolean accept(File file) {
        return !file.getName().startsWith(".");
    }

}
