package fedora.utilities.cmda.analyzer;

import java.io.File;

/**
 * A simple file filter implementation that accepts any file or directory
 * not beginning with "." (dot).
 *
 * @author cwilper@cs.cornell.edu
 */
public class DefaultFileFilter implements FileFilter {

    public DefaultFileFilter() {
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
