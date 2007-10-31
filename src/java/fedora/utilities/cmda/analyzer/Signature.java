package fedora.utilities.cmda.analyzer;

import java.util.Map;
import java.util.Set;

/**
 * Describes constraints on Fedora digital objects.
 *
 * @author Chris Wilper
 */
public class Signature {

    /** See constructor for description. */
    private String m_origContentModelID;

    /** See constructor for description. */
    private Set<String> m_bDefPIDs;

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
        // no-op
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
     * @param bDefPIDs              constrains the bdefs used by old-style
     *                              disseminators.
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
            Set<String> bDefPIDs, 
            Set<String> bMechPIDs, 
            Map<String, Set<String>> bindingKeyAssignments,
            Set<String> datastreamIDs,
            Map<String, String> mimeTypes,
            Map<String, String> formatURIs) {
        m_origContentModelID = origContentModelID;
        m_bDefPIDs = bDefPIDs;
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
     * Gets the constraint on the bDefs used by old-style disseminators.
     *
     * @return the pids of the bDefs, or null if this signature does not
     *         constrain this aspect.
     */
    public Set<String> getBDefPIDs() {
        return m_bDefPIDs;
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
        }
        return m_bindingKeyAssignments.get(bMechPID);
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
        }
        return m_mimeTypes.get(datastreamID);
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
        }
        return m_formatURIs.get(datastreamID);
    }

    //---
    // Object overrides
    //---
   
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append(Aspect.ORIG_CONTENT_MODEL.getName() + "\n  ");
        if (m_origContentModelID == null) {
            out.append("any");
        } else if (m_origContentModelID.length() == 0) {
            out.append("none");
        } else {
            out.append("'" + m_origContentModelID + "'");
        }
        out.append("\n" + Aspect.BDEF_PIDS.getName() + "\n  "
                + listStrings(m_bDefPIDs));
        out.append("\n" + Aspect.BMECH_PIDS.getName() + "\n  " 
                + listStrings(m_bMechPIDs));
        out.append("\n" + Aspect.BINDING_KEY_ASSIGNMENTS.getName());
        if (m_bindingKeyAssignments == null) {
            out.append("\n  any");
        } else {
            for (String pid : m_bMechPIDs) {
                out.append("\n  for " + pid + "\n    "
                        + listStrings(m_bindingKeyAssignments.get(pid)));
            }
            if (m_bMechPIDs.size() == 0) {
                out.append("\n  none");
            }
        }
        out.append("\n" + Aspect.DATASTREAM_IDS.getName() + "\n  "
                + listStrings(m_datastreamIDs));
        appendDSRestrictions(Aspect.MIME_TYPES, m_mimeTypes, out);
        appendDSRestrictions(Aspect.FORMAT_URIS, m_formatURIs, out);
        return out.toString();
    }
    
    private static void appendDSRestrictions(Aspect aspect,
            Map<String, String> restrictions, StringBuffer out) {
        out.append("\n" + aspect.getName());
        if (restrictions == null) {
            out.append("\n  any");
        } else {
            for (String dsID : restrictions.keySet()) {
                out.append("\n  for " + dsID + ", ");
                String value = restrictions.get(dsID);
                if (value == null || value.length() == 0) {
                    out.append("none");
                } else {
                    out.append("'" + value + "'");
                }
            }
        }
    }
    
    private static String listStrings(Set<String> set) {
        if (set == null) {
            return "any";
        }
        if (set.size() == 0) {
            return "none";
        }
        StringBuffer out = new StringBuffer();
        boolean pastFirst = false;
        for (String string : set) {
            if (pastFirst) {
                out.append(", ");
            } else {
                pastFirst = true;
            }
            out.append("'" + string + "'");
        }
        return out.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Signature && this.getClass().equals(o.getClass())) {
            Signature s = (Signature) o;
            return equals(m_origContentModelID, s.getOrigContentModelID())
                    && equals(m_bDefPIDs, s.getBDefPIDs())
                    && sameBMechDetails(s)
                    && sameDatastreamDetails(s);
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return addHashCodes(m_origContentModelID, m_bDefPIDs, m_bMechPIDs, 
                m_bindingKeyAssignments, m_datastreamIDs, m_mimeTypes,
                m_formatURIs);
    }

    //---
    // Instance helpers
    //---
    
    private boolean sameBMechDetails(Signature s) {
        if (m_bMechPIDs == null) {
            return s.getBMechPIDs() == null;
        } else if (s.getBMechPIDs() == null) {
            return false;
        } else {
            for (String bMechPID : m_bMechPIDs) {
                if (!equals(getBindingKeyAssignments(bMechPID),
                        s.getBindingKeyAssignments(bMechPID))) {
                    return false;
                }
            }
            return m_bMechPIDs.equals(s.getBMechPIDs());
        }
    }
    
    private boolean sameDatastreamDetails(Signature s) {
        if (m_datastreamIDs == null) {
            return s.getDatastreamIDs() == null;
        } else if (s.getDatastreamIDs() == null) {
            return false;
        } else {
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
            return m_datastreamIDs.equals(s.getDatastreamIDs());
        }
    }

    //---
    // Static helpers
    //---

    /**
     * Gives the sum of the hash codes of the given objects.
     * Objects given as null will be skipped.
     */
    private static int addHashCodes(Object... objects) {
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
            return b == null;
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

}
