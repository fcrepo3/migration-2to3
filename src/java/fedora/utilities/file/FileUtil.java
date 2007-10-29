/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.file;

import java.io.File;

/**
 * Utility methods for working with files and directories.
 * 
 * @author Chris Wilper
 */
public abstract class FileUtil {
  
    /**
     * Removes all files (and optionally, directories) within the given
     * directory.
     * 
     * @param dir the directory.
     * @param recursive whether subdirectories should be removed recursively.
     * @return whether deletion of all files/directories was successful.
     */
    public static boolean clearDirectory(File dir, boolean recursive) {
        File[] files = dir.listFiles();
        return clearDirectories(files, recursive) && clearFiles(files);
    }
    
    private static boolean clearDirectories(File[] files, boolean recursive) {
        for (File file : files) {
            if (file.isDirectory() && recursive) {
                if (!clearDirectory(file, true)) {
                    return false;
                }
                if (!file.delete()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private static boolean clearFiles(File[] files) {
        for (File file : files) {
            if (!file.isDirectory() && !file.delete()) {
                return false;
            }
        }
        return true;
    }

}
