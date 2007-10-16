package fedora.utilities.cmda.analyzer;

import java.io.File;

/**
 * Interface for filtering files.
 *
 * @author cwilper@cs.cornell.edu
 */
public interface FileFilter {

    /**
     * Tells whether the file is accepted by this filter.
     *
     * @param file the file.
     * @return true if the filter accepts it, false othersise.
     */
    boolean accept(File file);

}
