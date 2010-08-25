/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import org.fcrepo.server.storage.types.DigitalObject;

/**
 * Provides an iterator over a set of <code>DigitalObject</code>s.
 *
 * @author Chris Wilper
 */
public interface ObjectLister
        extends Iterable<DigitalObject> {

    // no additional methods

}
