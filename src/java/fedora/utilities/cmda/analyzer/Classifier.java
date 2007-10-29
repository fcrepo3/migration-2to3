package fedora.utilities.cmda.analyzer;

import fedora.server.storage.types.DigitalObject;

/**
 * Interface for classifying Fedora objects based on their content.
 *
 * @author Chris Wilper
 */
public interface Classifier {

    /**
     * Gets a content model object appropriate to the given object.
     * 
     * @param obj the object to examine.
     * @return a content model object that describe the class of object
     *         it belongs to.
     */
    DigitalObject getContentModel(DigitalObject obj);

}
