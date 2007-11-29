/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fedora.common.FaultException;

/**
 * Utility methods for working with files and directories.
 * 
 * @author Chris Wilper
 */
public abstract class FileUtil {
   
    /** Buffer size, in bytes, for reads/writes; 4096. */
    public static final int READ_BUFFER_SIZE = 4096;
   
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
   
    /**
     * Gets an absolute file based on a given path, relative to a parent
     * directory or by itself, if absolute.
     * 
     * @param parentDir the parent directory.
     * @param path the path, relative to the parent or absolute.
     * @return the file.
     */
    public static File getFile(File parentDir, String path) {
        File file = new File(path);
        if (file.isAbsolute() || path.startsWith("\\")) {
            return file.getAbsoluteFile();
        }
        return new File(parentDir, path).getAbsoluteFile();
    }
   
    /**
     * Writes (or overwrites) the given file with the content of the
     * given stream.
     * 
     * @param source
     * @param file
     */
    public static void writeFile(InputStream source, File file) {
        OutputStream sink = null;
        try {
            sink = new FileOutputStream(file);
        } catch (IOException e) {
            throw new FaultException("Error opening file for writing: "
                    + file.getPath(), e);
        }
        sendBytes(source, sink);
    }
    
    /**
     * Sends all bytes from one stream to another.
     *
     * Both streams will be closed when this method returns, whether it was 
     * ultimately successful or not.
     *
     * @param  source         the stream to read from.
     * @param  sink           the stream to write to.
     * @throws FaultException if the operation failed due to an I/O error.
     */
    public static void sendBytes(InputStream source, OutputStream sink)
            throws FaultException {
        try {
            byte[] buf = new byte[READ_BUFFER_SIZE];
            int len;
            while ((len = source.read(buf)) > 0 ) {
                sink.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new FaultException("Error sending bytes from InputStream "
                    + "to OutputStream", e);
        } finally {
            //TODO: close these
            /*
            close(source);
            close(sink);
            */
        }
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
