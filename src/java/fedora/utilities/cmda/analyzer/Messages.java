/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cmda.analyzer;

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
    
    //---
    // Error messages
    //---

    /** Error indicating analysis failed. */
    static final String ERR_ANALYSIS_FAILED = BUNDLE.getString(
            "err.analysis_failed");
    
    /** Error indicating the aspect is unknown. */
    static final String ERR_ASPECT_UNKNOWN = BUNDLE.getString(
            "err.aspect_unknown");
    
    /** Error indicating closing a file failed. */
    static final String ERR_CLOSE_FILE_FAILED = BUNDLE.getString(
            "err.close_file_failed");
    
    /** Error indicating configuration file was not found. */
    static final String ERR_CONFIG_NOT_FOUND = BUNDLE.getString(
            "err.config_not_found");
    
    /** Error indicating directory was not empty. */
    static final String ERR_DIR_NONEMPTY = BUNDLE.getString(
            "err.dir_nonempty");
    
    /** Error indicating directory creation failed. */
    static final String ERR_MKDIR_FAILED = BUNDLE.getString(
            "err.mkdir_failed");
    
    /** Error indicating serialization failure. */
    static final String ERR_SERIALIZE_FAILED = BUNDLE.getString(
            "err.serialize_failed");
    
    /** Error indicating file write failure. */
    static final String ERR_WRITE_FILE_FAILED = BUNDLE.getString(
            "err.write_file_failed");

    private static ResourceBundle loadBundle() {
        final String className = Messages.class.getName();
        final String bundleName = className.substring(
                0, className.length() - 8) + "resources.Messages";
        return ResourceBundle.getBundle(bundleName);
    }
    
}
