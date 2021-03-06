/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import java.io.InputStream;

import org.fcrepo.common.FaultException;

import org.fcrepo.server.storage.types.DigitalObject;

/**
 * Provides basic read/write access to a set of pre-existing digital objects.
 *
 * @author Chris Wilper
 */
public interface ObjectStore
        extends ObjectLister {

    /**
     * Gets a digital object in the store.
     *
     * @param pid the id of the object.
     * @return the object, or null if not found.
     * @throws FaultException if the object existed, but could not
     *                        be read or deserialized for any reason.
     */
    DigitalObject getObject(String pid)
            throws FaultException;

    /**
     * Gets an InputStream for a digital object in the store.
     *
     * @param pid the id of the object.
     * @return an input stream, or null if the object is not found.
     * @throws FaultException if the object existed, but could not
     *                        be read for any reason.
     */
    InputStream getObjectStream(String pid)
            throws FaultException;

    /**
     * Replaces a digital object in the store.
     *
     * @param obj the object to replace.
     * @return whether the object existed and was therefore replaced.
     * @throws FaultException if the object existed, but could not
     *                        be overwritten or serialized for any reason.
     */
    boolean replaceObject(DigitalObject obj)
            throws FaultException;

    /**
     * Replaces a digital object in the store, given a stream.
     *
     * @param pid the pid of the object to replace.
     * @param source the stream containing the serialized object.
     *               It will be closed when finished, regardless of whether
     *               this method ultimately succeeds or fails.
     * @return whether the object existed and was therefore replaced.
     * @throws FaultException if the object existed, but could not
     *                        be overwritten or serialized for any reason.
     */
    boolean replaceObject(String pid, InputStream source)
            throws FaultException;

    /**
     * Releases any resources allocated by this object.
     */
    void close();

}
