package fedora.utilities.digitalobject;

import org.fcrepo.common.PID;

/**
 * Gets new PIDs.
 *
 * @author Chris Wilper
 */
public interface PIDGenerator {

    /**
     * Gets a new PID.
     *
     * @return the pid.
     */
    PID getNextPID();

}
