package fedora.utilities.digitalobject;

import fedora.common.PID;

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
