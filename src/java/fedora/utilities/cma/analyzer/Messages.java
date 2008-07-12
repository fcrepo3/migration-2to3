/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cma.analyzer;

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
  
    /** Help text for command-line analyzer. */
    static final String ANALYZER_HELP = BUNDLE.getString(
            "Analyzer.help");
  
    /** Usage text for command-line analyzer. */
    static final String ANALYZER_USAGE = BUNDLE.getString(
            "Analyzer.usage");
    
    private static ResourceBundle loadBundle() {
        final String className = Messages.class.getName();
        final String bundleName = className.substring(
                0, className.length() - 8) + "resources.Messages";
        return ResourceBundle.getBundle(bundleName);
    }
    
}
