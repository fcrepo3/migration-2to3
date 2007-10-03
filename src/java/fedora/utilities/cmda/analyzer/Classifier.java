package fedora.utilities.cmda.analyzer;

import fedora.server.storage.types.DigitalObject;

/**
 * Inteface for classifying Fedora objects based on their content.
 *
 * @author cwilper@cs.cornell.edu
 */
public interface Classifier {

    /**
     * Gets a content model object appropriate to the given object.
     * 
     * @param the object to examine.
     * @return a content model object that describe the class of object
     *         it belongs to.
     */
    DigitalObject getContentModel(DigitalObject obj);

}
