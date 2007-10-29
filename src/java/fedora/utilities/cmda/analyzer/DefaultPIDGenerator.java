package fedora.utilities.cmda.analyzer;

import java.util.Properties;

import fedora.common.PID;

/**
 * A simple non-persistent pid generator implementation.
 *
 * @author Chris Wilper
 */
public class DefaultPIDGenerator implements PIDGenerator {

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
     * Constructs and instance.
     * 
     * @param pidPrefix the PID prefix to use.
     */ 
    public DefaultPIDGenerator(String pidPrefix) {
        m_pidPrefix = pidPrefix;
    }

    /**
     * Constructs an instance with configuration taken from the given
     * properties.
     *
     * <p><b>Specifying the PID prefix</b>
     * <br/>
     * The <code>pidPrefix</code> property will be used, if specified.
     * Otherwise, the <code>DEFAULT_PID_PREFIX</code> will be used.</p>
     * 
     * @param props the properties from which to get the configuration.
     */
    public DefaultPIDGenerator(Properties props) {
        if (props.getProperty("pidPrefix") == null) {
            m_pidPrefix = DEFAULT_PID_PREFIX;
        } else {
            m_pidPrefix = props.getProperty("pidPrefix");
        }
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
