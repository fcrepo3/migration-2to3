package fedora.utilities.cmda.analyzer;

/**
 * Aspects of objects that may be considered important for the purpose
 * of classification.
 *
 * @author cwilper@cs.cornell.edu
 */
public enum Aspect {

    ORIG_CONTENT_MODEL      ("OrigContentModel"),

    BDEF_PIDS               ("BDefPIDs"),

    BMECH_PIDS              ("BMechPIDs"),

    BINDING_KEY_ASSIGNMENTS ("BindingKeyAssignments"),

    DATASTREAM_IDS          ("DatastreamIDs"),

    MIME_TYPES              ("MIMETypes"),

    FORMAT_URIS             ("FormatURIs");

    private String m_name;

    Aspect(String name) {
        m_name = name;
    }

    public String getName() { return m_name; }

    public static Aspect fromName(String name) {
        for (Aspect aspect : values()) {
            if (name.equalsIgnoreCase(aspect.getName())) {
                return aspect;
            }
        }
        throw new IllegalArgumentException("Unrecognized aspect: " + name);
    }

}
