package fedora.utilities.cmda.analyzer;

import java.io.File;
import java.io.FileFilter;

/**
 * A simple file filter implementation that accepts any file or directory
 * not beginning with "." (dot).
 *
 * @author Chris Wilper
 */
public class DefaultFileFilter implements FileFilter {

    /**
     * Creates an instance.
     */
    public DefaultFileFilter() {
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
