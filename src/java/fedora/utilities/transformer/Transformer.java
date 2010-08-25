
package fedora.utilities.transformer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Properties;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import org.fcrepo.common.FaultException;

import fedora.utilities.Log4J;
import fedora.utilities.config.ConfigUtil;
import fedora.utilities.digitalobject.ObjectStore;
import fedora.utilities.file.FileUtil;

/**
 * Utility to apply transformation rules to Fedora objects.
 *
 * @author Chris Wilper
 */
public class Transformer {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(Transformer.class);

    /** Option to make the basic content model explicit in the output. */
    private static final String EXPLICIT_BASIC_MODEL = "explicitBasicModel";

    /** PID files this instance will run with. */
    private final List<File> m_pidFiles;

    /** Corresponding XSLT files this instance will use. */
    private final List<File> m_xsltFiles;

    /** Whether the basic content model will be made explicit in the output. */
    private final boolean m_explicitBasicModel;

    /**
     * Creates an instance.
     *
     * @param pidFiles
     *        pid files identifying objects to transform for each associated
     *        stylesheet.
     * @param xsltFiles
     *        xslt files containing transformation rules for each associated pid
     *        file.
     * @param explicitBasicModel
     *        whether to make the basic content model explicit in the output.
     * @throws IllegalArgumentException
     *         if pidFiles or xsltFiles are empty, a file listed doesn't exist,
     *         or the number of pidFiles and xsltFiles don't match.
     */
    public Transformer(List<File> pidFiles,
                       List<File> xsltFiles,
                       boolean explicitBasicModel) {
        m_pidFiles = pidFiles;
        m_xsltFiles = xsltFiles;
        m_explicitBasicModel = explicitBasicModel;
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
     * @param props
     *        the properties.
     * @throws IllegalArgumentException
     *         if a required parameter is unspecified, one of the specified
     *         files doesn't exist, or the number of pidFiles and xsltFiles
     *         don't match.
     */
    public Transformer(Properties props) {
        m_pidFiles = ConfigUtil.getRequiredFiles(props, "pidFiles");
        m_xsltFiles = ConfigUtil.getRequiredFiles(props, "xsltFiles");
        m_explicitBasicModel =
                ConfigUtil.getOptionalBoolean(props,
                                              EXPLICIT_BASIC_MODEL,
                                              false);
        validateFiles();
    }

    /**
     * Run all transformations.
     *
     * @param store
     *        the store to read from/write to.
     * @param dryRun
     *        if false, transformation should not overwrite original.
     * @throws FaultException
     *         if transformation cannot complete for any reason.
     */
    public void transformAll(ObjectStore store, boolean dryRun)
            throws FaultException {
        LOG.info("Will transform " + m_pidFiles.size() + " batch(es) of "
                + "objects");
        int total = 0;
        for (int i = 0; i < m_pidFiles.size(); i++) {
            File pidFile = m_pidFiles.get(i);
            File xsltFile = m_xsltFiles.get(i);
            LOG.info("Transforming objects in " + pidFile.getName() + " with "
                    + xsltFile.getName());
            int batchCount =
                    transformBatch(xsltFile,
                                   pidFile,
                                   store,
                                   m_explicitBasicModel,
                                   dryRun);
            LOG.info("Finished transforming batch of " + batchCount
                    + " objects");
            total += batchCount;
        }
        LOG.info("Finished transforming all " + total + " objects.");
        if (dryRun) {
            LOG.info("NOTE: This was a dry run (no changes written).");
        } else {
            LOG.info("NOTE: This was NOT a dry run (all changes written).");
        }
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
     * @param xsltFile
     *        the stylesheet to use for transforming the batch.
     * @param pidFile
     *        a text file containing a list of pids, one per line.
     * @param store
     *        the store to read from/write to.
     * @param dryRun
     *        if false, transformation should not overwrite original.
     * @return the number of transformations done.
     * @throws FaultException
     *         if transformation cannot complete for any reason.
     */
    private static int transformBatch(File xsltFile,
                                      File pidFile,
                                      ObjectStore store,
                                      boolean explicitBasicModel,
                                      boolean dryRun) {
        BufferedReader pids = null;
        String pidLine = null;
        int numTransformed = 0;
        try {
            TransformerFactory tfactory = TransformerFactory.newInstance();
            javax.xml.transform.Transformer vtransformer =
                    tfactory.newTransformer(new StreamSource(xsltFile));

            if (explicitBasicModel) {
                vtransformer.setParameter(EXPLICIT_BASIC_MODEL, "'true'");
            }
            pids = new BufferedReader(new FileReader(pidFile));
            while ((pidLine = pids.readLine()) != null) {
                pidLine = pidLine.trim();
                if (pidLine.length() == 0 || pidLine.startsWith("#")) {
                    continue;
                }
                transformOne(vtransformer, pidLine, store, dryRun);
                numTransformed++;
            }
            return numTransformed;
        } catch (IOException e) {
            throw new FaultException("Error reading from pid file: "
                    + pidFile.getName(), e);
        } catch (TransformerConfigurationException e) {
            throw new FaultException("Error processing XSLT file: "
                    + xsltFile.getName(), e);
        } catch (TransformerException e) {
            throw new FaultException("Error transforming object " + pidLine
                    + "using XSLT file: " + xsltFile.getName(), e);
        } finally {
            FileUtil.close(pids);
        }
    }

    /**
     * Transform one object with the indicated xsltFile.
     *
     * @param xsltTransformer
     *        the compiled form of the stylesheet to use for transforming the
     *        object.
     * @param pid
     *        the pid of the object to transform.
     * @param store
     *        the store to read from/write to.
     * @param dryRun
     *        if false, transformation should not overwrite original.
     * @return the number of transformations done.
     * @throws TransformerException
     */
    private static int transformOne(javax.xml.transform.Transformer
                                            xsltTransformer,
                                    String pid,
                                    ObjectStore store,
                                    boolean dryRun)
            throws TransformerException {
        InputStream str = store.getObjectStream(pid);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult res = new StreamResult(out);
        xsltTransformer.transform(new StreamSource(str), res);
        if (!dryRun) {
            LOG.info("Transformed and replaced " + pid);
            store.replaceObject(pid,
                                new ByteArrayInputStream(out.toByteArray()));
        } else {
            LOG.info("Transformed " + pid);
        }
        return 0;
    }

    //---
    // Command-line
    //---

    /**
     * Command-line entry point for the analyzer.
     *
     * @param args
     *        command-line arguments.
     */
    public static void main(String[] args) {
        Log4J.force();
        // HACK: make DOTranslatorUtility happy
        System.setProperty("fedoraServerHost", "localhost");
        System.setProperty("fedoraServerPort", "80");
        transformAllFromProperties(getTransformationProperties(args));
    }

    private static Properties getTransformationProperties(String[] args) {
        Properties props = new Properties();
        if (args.length == 0) {
            System.out.println(Messages.TRANSFORMER_USAGE);
            System.exit(0);
        } else if (args[0].equals("--help")) {
            System.out.println(Messages.TRANSFORMER_HELP);
            System.exit(0);
        }
        if (args[0].equals("--")) {
            props = System.getProperties();
        } else if (args.length == 1 && !args[0].startsWith("-")) {
            props = getPropertiesFromFile(new File(args[0]));
        } else {
            // args specified on Command Line
            int argPtr = 0;
            String home = System.getenv("FEDORA_HOME");
            props.setProperty("fedoraHome", home);
            while (argPtr < args.length) {
                argPtr += processArg(args, argPtr, props);
            }
        }
        return props;
    }

    private static Properties getPropertiesFromFile(File configFile) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            LOG.error("Configuration file not found: " + configFile.getPath());
            exitFatally();
        } catch (IOException e) {
            LOG.error("Error reading configuration file: "
                    + configFile.getPath());
            exitFatally();
        }
        return props;
    }

    private static void transformAllFromProperties(Properties props) {
        String sourceDirString = props.getProperty("sourceDir");
        if (sourceDirString != null && sourceDirString.trim().length() > 0) {
            inferPathsFromSourceDir(new File(sourceDirString.trim()),
                                    props);
        }
        try {
            Transformer transformer = new Transformer(props);
            if (props.getProperty("clearObjectPaths") == null) {
                props.setProperty("clearObjectPaths", "false");
            }
            ObjectStore store = (ObjectStore) ConfigUtil.construct(props,
                    "objectStore",
                    "fedora.utilities.digitalobject.LocalRepoObjectStore");
            boolean dryRun =
                    ConfigUtil.getOptionalBoolean(props, "dryRun", false);
            transformer.transformAll(store, dryRun);
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            exitFatally();
            // CHECKSTYLE:OFF
        } catch (Throwable th) {
            // CHECKSTYLE:ON
            LOG.error("Transformation failed due to an unexpected error", th);
            exitFatally();
        }
    }

    // Sets the xsltFiles and pidFiles properties based on files in sourceDir
    private static void inferPathsFromSourceDir(File sourceDir,
                                                Properties props) {
        String pidFiles = "";
        String xsltFiles = "";
        for (File file : sourceDir.listFiles()) {
            if (file.getName().endsWith(".xslt")
                    || file.getName().endsWith(".xsl")) {
                String path = file.getPath();
                int i = path.lastIndexOf(".");
                String pathPrefix = path.substring(0, i);
                File pidFile = getPidFile(pathPrefix);
                if (pidFile != null) {
                    if (xsltFiles.length() > 0) {
                        xsltFiles += " ";
                    }
                    xsltFiles += path;
                    if (pidFiles.length() > 0) {
                        pidFiles += " ";
                    }
                    pidFiles += pidFile.getPath();
                }
            }
        }
        props.setProperty("xsltFiles", xsltFiles);
        props.setProperty("pidFiles", pidFiles);
    }

    // gets a file with name pathPrefix.txt, pathPrefix.list,
    // or simply pathPrefix if such a file exists, otherwise returns null.
    private static File getPidFile(String pathPrefix) {
        File pidFile = new File(pathPrefix + ".txt");
        if (!pidFile.exists()) {
            pidFile = new File(pathPrefix + ".list");
        }
        if (!pidFile.exists()) {
            pidFile = new File(pathPrefix);
        }
        if (!pidFile.exists()) {
            return null;
        }
        return pidFile;
    }

    private static int processArg(String[] args, int argPtr, Properties props) {
        if (args[argPtr].equalsIgnoreCase("-dryrun")) {
            props.setProperty("dryRun", "true");
            return (1);
        }
        if (args[argPtr].equals("-xsl")) {
            props.setProperty("xsltFiles", args[argPtr + 1]);
            return (2);
        }
        if (args[argPtr].equals("-jdbcJar")) {
            props.setProperty("jdbcJar", args[argPtr + 1]);
            return (2);
        }
        if (args[argPtr].equals("-fedoraHome")) {
            props.setProperty("fedoraHome", args[argPtr + 1]);
            return (2);
        }
        if (!args[argPtr].startsWith("-")) {
            props.setProperty("pidFiles", args[argPtr]);
            return (1);
        }
        return 0;
    }

    private static void exitFatally() {
        System.exit(1);
    }
}