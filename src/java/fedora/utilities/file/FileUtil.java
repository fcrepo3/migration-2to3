/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.file;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import fedora.common.FaultException;

/**
 * Utility methods for working with files and directories.
 * 
 * @author Chris Wilper
 */
public abstract class FileUtil {
   
    /** Buffer size, in bytes, for reads/writes; 4096. */
    public static final int READ_BUFFER_SIZE = 4096;
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(FileUtil.class);
   
    /** System-dependent line separator. */
    private static final String CR = System.getProperty("line.separator");
   
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
     * @param source the stream to read from.
     * @param file the file to write to.
     * @throws FaultException if the operation failed due to an I/O error.
     */
    public static void writeFile(InputStream source, File file) {
        OutputStream sink = null;
        try {
            sink = new FileOutputStream(file);
            sendBytes(source, sink);
        } catch (IOException e) {
            throw new FaultException("Error opening file for writing: "
                    + file.getPath(), e);
        } finally {
            close(source);
            close(sink);
        }
    }
   
    /**
     * Writes (or overwrites) the given file (using UTF-8 encoding) with the 
     * content of the given string.
     * 
     * @param source the string to write.
     * @param file the file to write to.
     * @throws FaultException if the operation failed due to an I/O error.
     */
    public static void writeTextFile(String source, File file) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF-8"));
            writer.print(source);
        } catch (IOException e) {
            throw new FaultException("Error writing text to file: "
                    + file.getPath(), e);
        } finally {
            close(writer);
        }
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
            while ((len = source.read(buf)) > 0) {
                sink.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new FaultException("Error sending bytes from InputStream "
                    + "to OutputStream", e);
        } finally {
            close(source);
            close(sink);
        }
    }
   
    /**
     * Reads the content of a UTF-8 encoded text stream into a string.
     * 
     * The stream will be closed when this method returns, whether it
     * was ultimately successful or not.
     * 
     * @param source the source stream.
     * @return the string.
     * @throws FaultException if the operation failed due to an I/O error.
     */
    public static String readTextStream(InputStream source) {
        try {
            StringBuffer buf = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    source, "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                buf.append(line + CR);
                line = reader.readLine();
            }
            return buf.toString();
        } catch (IOException e) {
            throw new FaultException("Error reading text stream", e);
        } finally {
            close(source);
        }
    }

    /**
     * Closes a stream if it's non-null, logging a warning if an error is
     * encountered.
     * 
     * @param stream the stream to close.
     */
    public static void close(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
            LOG.warn("Unable to close stream", e);
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
