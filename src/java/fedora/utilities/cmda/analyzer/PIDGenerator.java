package fedora.utilities.cmda.analyzer;

import fedora.common.PID;

/**
 * Interface for getting new PIDs for generated objects.
 *
 * @author Chris Wilper
 */
public interface PIDGenerator {

    /**
     * Gets a new PID.
     * 
     * @return the new pid.
     */
    PID getNextPID();

}
