package fedora.utilities.cmda.analyzer;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

/**
 * A configurable <code>Classifier</code> that uses several key parts of
 * the given objects to assign content models.
 *
 * @author cwilper@cs.cornell.edu
 */
public class DefaultClassifier implements Classifier {

    /**
     * @{inheritDoc}
     */
    public DigitalObject getContentModel(DigitalObject obj) {
        return getContentModel(getSignature(obj));
    }

    private DigitalObject getContentModel(Signature signature) {
        DigitalObject obj = new BasicDigitalObject();
        return obj;
    }

    private Signature getSignature(DigitalObject obj) {
        return null;
    }

    private class Signature {
    }

}
