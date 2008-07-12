/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cma.analyzer;

/**
 * Aspects of objects that may be considered important for the purpose
 * of classification.
 *
 * @author Chris Wilper
 */
public enum Aspect {

    /**
     * The content model property, as specified in older (pre-3.0) versions 
     * of Fedora;
     * <code>OrigContentModel</code>
     */
    ORIG_CONTENT_MODEL      ("OrigContentModel"),

    /**
     * The set of BDef objects used by the object's disseminators;
     * <code>BDefPIDs</code>
     */
    BDEF_PIDS               ("BDefPIDs"),

    /**
     * The set of BMech objects used by the object's disseminators;
     * <code>BMechPIDs</code>
     */
    BMECH_PIDS              ("BMechPIDs"),

    /**
     * The set of binding key assignments used by the object's dissemintors;
     * <code>BindingKeyAssignments</code>
     */
    BINDING_KEY_ASSIGNMENTS ("BindingKeyAssignments"),

    /**
     * The set of datastream IDs within the object;
     * <code>DatastreamIDs</code>
     */
    DATASTREAM_IDS          ("DatastreamIDs"),

    /**
     * The MIME types asserted for each datastream within the object;
     * <code>MIMETypes</code>
     */
    MIME_TYPES              ("MIMETypes"),

    /**
     * The Format URIs asserted for each datastream within the object;
     * <code>FormatURIs</code>
     */
    FORMAT_URIS             ("FormatURIs");

    /** The name of this aspect. */
    private String m_name;

    private Aspect(String name) {
        m_name = name;
    }

    /**
     * Gets the name of this aspect.
     * 
     * @return the name.
     */
    public String getName() { return m_name; }

    /**
     * Gets an aspect by name.
     * 
     * @param name the name of the aspect to get.
     * @return the aspect.
     * @throws IllegalArgumentException if the name given doesn't identify
     *         a known aspect.
     */
    public static Aspect fromName(String name) {
        for (Aspect aspect : values()) {
            if (name.equalsIgnoreCase(aspect.getName())) {
                return aspect;
            }
        }
        throw new IllegalArgumentException("Unknown aspect: " + name);
    }

}
