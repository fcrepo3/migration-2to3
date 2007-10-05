package fedora.utilities.cmda.analyzer;

import java.util.Map;
import java.util.Set;

/**
 * Describes constraints on Fedora digital objects.
 *
 * @author cwilper@cs.cornell.edu
 */
public class Signature {

    /** See constructor for description. */
    private String m_origContentModelID;

    /** See constructor for description. */
    private Set<String> m_bMechPIDs;

    /** See constructor for description. */
    private Map<String, Set<String>> m_bindingKeyAssignments;

    /** See constructor for description. */
    private Set<String> m_datastreamIDs;

    /** See constructor for description. */
    private Map<String, String> m_mimeTypes;

    /** See constructor for description. */
    private Map<String, String> m_formatURIs;

    /**
     * Constructs a signature with no constraints.
     */
    public Signature() {
    }

    /**
     * Constructs a signature with the given constraints.
     *
     * <p>Any parameter may be given as <code>null</code>, indicating that the
     * signature does not constrain that aspect.  If a parameter has any 
     * values, the signature fully describes that aspect.  In other words,
     * for an object to have the signature, that aspect of the object must
     * exactly match the values given in the parameter (no more, no less).
     *
     * @param origContentModelID    constrains the original content model id.
     * @param bMechPIDs             constrains the bmechs used by old-style
     *                              disseminators.
     * @param bindingKeyAssignments constrains the binding keys used by
     *                              old-style disseminators.  The key is the
     *                              bMech pid and the value is the set of
     *                              binding keys in the disseminator that
     *                              uses that bmech.
     * @param datastreamIDs         constrains the datastream ids.
     * @param mimeTypes             constrains the mime types of each
     *                              datastream.  The key is the datastream
     *                              id and the value is the mime type it must
     *                              have.
     * @param formatURIs            constrains the format uris of each
     *                              datastream.  They key is the datastream
     *                              id and the value is the format uri it must
     *                              have.
     */
    public Signature(String origContentModelID,
            Set<String> bMechPIDs, 
            Map<String, Set<String>> bindingKeyAssignments,
            Set<String> datastreamIDs,
            Map<String, String> mimeTypes,
            Map<String, String> formatURIs) {
        m_origContentModelID = origContentModelID;
        m_bMechPIDs = bMechPIDs;
        m_bindingKeyAssignments = bindingKeyAssignments;
        m_datastreamIDs = datastreamIDs;
        m_mimeTypes = mimeTypes;
        m_formatURIs = formatURIs;
    }

    /**
     * Gets the constraint on the original content model id.
     *
     * @return the value the content model id must have, or null if this
     *         signature does not constrain this aspect.
     */
    public String getOrigContentModelID() {
        return m_origContentModelID;
    }

    /**
     * Gets the constraint on the bMechs used by old-style disseminators.
     *
     * @return the pids of the bMechs, or null if this signature does not
     *         constrain this aspect.
     */
    public Set<String> getBMechPIDs() {
        return m_bMechPIDs;
    }

    /**
     * Gets the constraint on the binding key assignments of an old-style
     * disseminator.
     *
     * <p>Each binding key assignment has the form: <code>key=id</code>, where
     * <em>key</em> is the binding key and <em>id</em> is the datastream
     * id assigned to that binding key.
     *
     * @param bMechPID the bMech of the old-style disseminator whose binding
     *                 key assignment constraints should be returned.
     * @return the binding key assignments of the associated disseminator, or
     *         null if this signature does not constrain this aspect.
     */
    public Set<String> getBindingKeyAssignments(String bMechPID) {
        if (m_bindingKeyAssignments == null) {
            return null;
        } else {
            return m_bindingKeyAssignments.get(bMechPID);
        }
    }

    /**
     * Gets the constraint on the datastream ids.
     *
     * @return the datastream ids, or null if this signature does not
     *         constrain this aspect.
     */
    public Set<String> getDatastreamIDs() {
        return m_datastreamIDs;
    }

    /**
     * Gets the constraint on the mime type of a datastream.
     *
     * @param datastreamID the datastream whose mime type constraint should
     *                     be returned.
     * @return the mime type, or null if this signature does not constrain
     *         this aspect.
     */
    public String getMIMEType(String datastreamID) {
        if (m_mimeTypes == null) {
            return null;
        } else {
            return m_mimeTypes.get(datastreamID);
        }
    }

    /**
     * Gets the constraint on the format URI of a datastream.
     *
     * @param datastreamID the datastream whose format URI constraint should
     *                     be returned.
     * @return the format URI, or null if this signature does not constrain
     *         this aspect.
     */
    public String getFormatURI(String datastreamID) {
        if (m_formatURIs == null) {
            return null;
        } else {
            return m_formatURIs.get(datastreamID);
        }
    }

    //---
    // Object overrides
    //---

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Signature && this.getClass().equals(o.getClass())) {
            Signature s = (Signature) o;
            if (!equals(m_origContentModelID, s.getOrigContentModelID())) {
                return false;
            }
            if (!equals(m_bMechPIDs, s.getBMechPIDs())) {
                return false;
            }
            if (m_bMechPIDs != null) {
                for (String bMechPID : m_bMechPIDs) {
                    if (!equals(getBindingKeyAssignments(bMechPID),
                            s.getBindingKeyAssignments(bMechPID))) {
                        return false;
                    }
                }
            }
            if (!equals(m_datastreamIDs, s.getDatastreamIDs())) {
                return false;
            }
            if (m_datastreamIDs != null) {
                for (String datastreamID : m_datastreamIDs) {
                    if (!equals(getMIMEType(datastreamID),
                            s.getMIMEType(datastreamID))) {
                        return false;
                    }
                    if (!equals(getFormatURI(datastreamID),
                            s.getFormatURI(datastreamID))) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return addHashCodes(m_origContentModelID, m_bMechPIDs, 
                m_bindingKeyAssignments, m_datastreamIDs, m_mimeTypes,
                m_formatURIs);
    }

    //---
    // Static helpers
    //---

    /**
     * Gives the sum of the hash codes of the given objects.
     * Objects given as null will be skipped.
     */
    private int addHashCodes(Object... objects) {
        int sum = 0;
        for (Object o : objects) {
            if (o != null) {
                sum += o.hashCode();
            }
        }
        return sum;
    }

    /**
     * Tells whether the given objects are equal.
     * In order to be equal, if one object is null, both must be null.
     */
    private static boolean equals(Object a, Object b) {
        if (a == null) {
            if (b == null) {
                return true;
            } else {
                return false;
            }
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

}
