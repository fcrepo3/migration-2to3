package fedora.utilities.cmda.analyzer;

import java.util.Properties;

import fedora.common.PID;

/**
 * A simple non-persistent pid generator implementation.
 *
 * @author cwilper@cs.cornell.edu
 */
public class DefaultPIDGenerator implements PIDGenerator {

    public static final String DEFAULT_PID_PREFIX = "demo:GeneratedPID";

    private final String m_pidPrefix;

    private int m_n;

    public DefaultPIDGenerator(String pidPrefix) {
        m_pidPrefix = pidPrefix;
    }

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
