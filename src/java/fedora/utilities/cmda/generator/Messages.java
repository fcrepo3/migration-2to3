/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cmda.generator;

import java.util.ResourceBundle;

/**
 * Text message constants for this package.
 *
 * @author Chris Wilper
 */
abstract class Messages {
   
    /** The <code>ResourceBundle</code> containing all messages. */
    public static final ResourceBundle BUNDLE = loadBundle();
    
    //---
    // Informative messages
    //---
  
    /** Help text for command-line transformer. */
    static final String GENERATOR_HELP = BUNDLE.getString(
            "Generator.help");
  
    /** Usage text for command-line transformer. */
    static final String GENERATOR_USAGE = BUNDLE.getString(
            "Generator.usage");
    
    private static ResourceBundle loadBundle() {
        final String className = Messages.class.getName();
        final String bundleName = className.substring(
                0, className.length() - 8) + "resources.Messages";
        return ResourceBundle.getBundle(bundleName);
    }
    
}
