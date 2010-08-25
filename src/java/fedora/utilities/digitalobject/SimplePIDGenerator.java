package fedora.utilities.digitalobject;

import java.util.Properties;

import org.fcrepo.common.PID;

/**
 * A simple non-persistent pid generator implementation.
 *
 * @author Chris Wilper
 */
public class SimplePIDGenerator
        implements PIDGenerator {

    /**
     * The PID prefix that will be used if none is specified;
     * <code>demo:GeneratedPID</code>
     */
    public static final String DEFAULT_PID_PREFIX = "demo:GeneratedPID";

    /** The PID prefix this instance uses. */
    private final String m_pidPrefix;

    /** The current number of generated PIDs. */
    private int m_n;

    /**
     * Creates an instance.
     *
     * @param pidPrefix the PID prefix to use.
     */
    public SimplePIDGenerator(String pidPrefix) {
        m_pidPrefix = pidPrefix;
    }

    /**
     * Creates an instance from properties.
     *
     * <pre>
     *   pidPrefix (optional) - the PID prefix to use;
     *                          default is DEFAULT_PID_PREFIX.
     * </pre>
     *
     * @param props the properties.
     */
    public SimplePIDGenerator(Properties props) {
        m_pidPrefix = props.getProperty("pidPrefix", DEFAULT_PID_PREFIX);
    }

    //---
    // PIDGenerator implementation
    //---

    /**
     * {@inheritDoc}
     */
    public PID getNextPID() {
        m_n++;
        return PID.getInstance(m_pidPrefix + m_n);
    }

}
