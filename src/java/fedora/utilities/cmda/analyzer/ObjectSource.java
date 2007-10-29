package fedora.utilities.cmda.analyzer;

import java.util.Iterator;

import fedora.server.storage.types.DigitalObject;

/**
 * Interface for iterating a set of Fedora objects.
 *
 * @author Chris Wilper
 */
public interface ObjectSource extends Iterator<DigitalObject> {

    /**
     * Closes the object source.
     */
    void close();

}
