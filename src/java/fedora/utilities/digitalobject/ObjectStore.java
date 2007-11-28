/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import fedora.common.FaultException;

import fedora.server.storage.types.DigitalObject;

/**
 * Provides basic read/write access to a set of pre-existing digital objects.
 *
 * @author Chris Wilper
 */
public interface ObjectStore
        extends ObjectLister {
   
    /**
     * Gets a <code>DigitalObject</code> in the store.
     * 
     * @param pid the id of the object.
     * @return the object, or null if not found.
     * @throws FaultException if the object existed, but could not
     *                        be read or deserialized for any reason.
     */
    DigitalObject getObject(String pid)
            throws FaultException;

    /**
     * Replaces a <code>DigitalObject</code> in the store.
     * 
     * @param obj the object to replace.
     * @return whether the object existed and was therefore replaced.
     * @throws FaultException if the object existed, but could not
     *                        be overwritten or serialized for any reason.
     */
    boolean replaceObject(DigitalObject obj)
            throws FaultException;
    
    /**
     * Releases any resources allocated by this object.
     */
    void close();

}
