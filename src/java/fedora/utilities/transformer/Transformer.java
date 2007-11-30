package fedora.utilities.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import fedora.common.FaultException;

import fedora.utilities.config.ConfigUtil;
import fedora.utilities.digitalobject.ObjectStore;

/**
 * Utility to apply transformation rules to Fedora objects.
 *
 * @author Chris Wilper
 */
public class Transformer {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(Transformer.class);
   
    /** PID files this instance will run with. */
    private final List<File> m_pidFiles;
    
    /** Corresponding XSLT files this instance will use. */
    private final List<File> m_xsltFiles;
    
    /**
     * Creates an instance.
     * 
     * @param pidFiles pid files identifying objects to transform for
     *                 each associated stylesheet.
     * @param xsltFiles xslt files containing transformation rules for
     *                  each associated pid file.
     * @throws IllegalArgumentException if pidFiles or xsltFiles are empty,
     *         a file listed doesn't exist, or the number of pidFiles and
     *         xsltFiles don't match.
     */
    public Transformer(List<File> pidFiles, List<File> xsltFiles) {
        m_pidFiles = pidFiles;
        m_xsltFiles = xsltFiles;
        validateFiles();
    }
  
    /**
     * Creates an instance from properties.
     *
     * <pre>
     *   pidFiles  (required) - space-delimited path(s) to one or more files,
     *                          each containing a list of PIDs (one per line).
     *                          For each path given, a corresponding xslt file
     *                          must be given in xsltFiles.
     *   xsltFiles (required) - space-delimited path(s) to one or more files,
     *                          each consisting of an XSLT stylesheet through
     *                          which the corresponding pid list should be
     *                          passed.
     * </pre>
     *
     * @param props the properties.
     * @throws IllegalArgumentException if a required parameter is
     *         unspecified, one of the specified files doesn't exist,
     *         or the number of pidFiles and xsltFiles don't match.
     */
    public Transformer(Properties props) {
        m_pidFiles = ConfigUtil.getRequiredFiles(props, "pidFiles");
        m_xsltFiles = ConfigUtil.getRequiredFiles(props, "xsltFiles");
        validateFiles();
    }
   
    /**
     * Run all transformations.
     * 
     * @param store the store to read from/write to.
     * @param dryRun if false, transformation should not overwrite original.
     * @throws FaultException if transformation cannot complete for any reason.
     */
    public void transformAll(ObjectStore store, boolean dryRun)
            throws FaultException {
        LOG.info("Will transform " + m_pidFiles.size() + " batch(es) of "
                + "objects");
        int total = 0;
        for (int i = 0; i < m_pidFiles.size(); i++) {
            File pidFile = m_pidFiles.get(i);
            File xsltFile = m_xsltFiles.get(i);
            LOG.info("Transforming objects from " + pidFile.getPath()
                    + " with " + xsltFile.getPath());
            int batchCount = transformBatch(xsltFile, pidFile, store, dryRun);
            LOG.info("Finished transforming batch of " + batchCount
                    + "objects");
            total += batchCount;
        }
        LOG.info("Transformation complete.  Transformed " + total
                + " objects.");
    }
  
    //---
    // Instance helpers
    //---
    
    private void validateFiles() {
        if (m_pidFiles == null || m_xsltFiles == null) {
            throw new IllegalArgumentException("pidFiles and xsltFiles "
                    + "must both be given as non-null");
        }
        if (m_pidFiles.size() == 0 || m_xsltFiles.size() == 0) {
            throw new IllegalArgumentException("pidFiles and xsltFiles "
                    + "must be given as non-empty");
        }
        if (m_pidFiles.size() != m_xsltFiles.size()) {
            throw new IllegalArgumentException("Number of pidFiles ("
                    + m_pidFiles.size() + ") must match number of "
                    + "xsltFiles (" + m_xsltFiles.size() + ")");
        }
        ensureReadable(m_pidFiles);
        ensureReadable(m_xsltFiles);
    }
    
    //---
    // Static helpers
    //---
    
    private static void ensureReadable(List<File> files) {
        for (File file : files) {
            if (!file.canRead()) {
                throw new IllegalArgumentException("File does not exist "
                        + "or cannot be read: " + file.getPath());
            }
        }
    }

    /**
     * Transform all objects in pidFile with the indicated xsltFile.
     * 
     * @param xsltFile the stylesheet to use for transforming the batch.
     * @param pidFile a text file containing a list of pids, one per line.
     * @param store the store to read from/write to.
     * @param dryRun if false, transformation should not overwrite original.
     * @return the number of transformations done.
     * @throws FaultException if transformation cannot complete for any reason.
     */
    private static int transformBatch(File xsltFile, File pidFile,
            ObjectStore store, boolean dryRun) {
        return 0;
        // TODO: implement, throwing FaultException(msg, e) in event of failure
        // NOTE:
        // - When reading pidlist files, blank lines and those beginning
        //   with # should be ignored.
        // - If dryrun, don't send output to store.replaceObject;
        //   just make sure the transformation succeeds
    }
    
    //---
    // Command-line
    //---
    
    /**
     * Command-line entry point for the analyzer.
     * 
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        // HACK: make DOTranslatorUtility happy
        System.setProperty("fedoraServerHost", "localhost");
        System.setProperty("fedoraServerPort", "80");
        // HACK: make commons-logging happy
        final String pfx = "org.apache.commons.logging.";
        if (System.getProperty(pfx + "LogFactory") == null) {
            System.setProperty(pfx + "LogFactory", pfx + "impl.Log4jFactory");
            System.setProperty(pfx + "Log", pfx + "impl.Log4JLogger");
        }
        if (args.length != 1) {
            System.out.println(Messages.TRANSFORMER_USAGE);
            System.exit(0);
        } else {
            if (args[0].equals("--help")) {
                // TODO: complete transformer help in Messages.properties
                System.out.println(Messages.TRANSFORMER_HELP);
                System.exit(0);
            }
            try {
                Properties props;
                if (args[0].equals("--")) {
                    props = System.getProperties();
                } else {
                    props = new Properties();
                    props.load(new FileInputStream(args[0]));
                }
                Transformer transformer = new Transformer(props);
                ObjectStore store = (ObjectStore) ConfigUtil.construct(props,
                        "objectStore",
                        "fedora.utilities.digitalobject.LocalRepoObjectStore");
                boolean dryRun = ConfigUtil.getOptionalBoolean(props, "dryRun",
                        false);
                transformer.transformAll(store, dryRun);
            } catch (FileNotFoundException e) {
                LOG.error("Configuration file not found: " + args[0]);
                exitFatally();
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage());
                exitFatally();
                // CHECKSTYLE:OFF
            } catch (Throwable th) {
                // CHECKSTYLE:ON
                LOG.error("Transformation failed due to an unexpected error",
                        th);
                exitFatally();
            }
        }
    }
    
    private static void exitFatally() {
        System.exit(1);
    }
}