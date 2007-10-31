/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import fedora.server.storage.types.DigitalObject;

/**
 * Interface to a system that provides basic read/write access to a set of
 * pre-existing digital objects.
 *
 * @author Chris Wilper
 */
interface ObjectStore
        extends ObjectLister {
   
    /**
     * Gets a <code>DigitalObject</code> in the store.
     * 
     * @param pid the id of the object.
     * @return the object, or null if not found.
     */
    DigitalObject getObject(String pid);

    /**
     * Replaces a <code>DigitalObject</code> in the store.
     * 
     * @param obj the object to replace.
     * @return whether the object existed and was therefore replaced.
     */
    boolean replaceObject(DigitalObject obj);

}
