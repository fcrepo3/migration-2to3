package fedora.utilities.cmda.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.Properties;

import org.apache.log4j.Logger;

import fedora.common.FaultException;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.config.ConfigUtil;
import fedora.utilities.digitalobject.ObjectStore;
import fedora.utilities.digitalobject.RepoUtil;

/**
 * Utility to generate transformation rules for instance objects to
 * make them conform to content models to which they should belong.
 *
 * @author Chris Wilper
 */
public class Generator {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(Generator.class);
   
    /** Contains the base stylesheet. */
    private static final String XSLT_TEMPLATE;
   
    /** Where the original BMechs can be read from. */
    private final ObjectStore m_store;
    
    /** Where to find the other input files, and send output files. */
    private final File m_sourceDir;
    
    /** The deserializer to use when reading cModels from sourceDir. */
    private final DODeserializer m_cModelDeserializer;
    
    static {
        // read the xslt template from the jar into XSLT_TEMPLATE
        final String xsltPath =  "fedora/utilities/cmda/generator/resources/"
                + "foxml-upgrade-cmda.xslt";
        InputStream in = Generator.class.getClassLoader().getResourceAsStream(
                xsltPath);
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, "UTF-8"));
            StringBuffer buf = new StringBuffer();
            String line = reader.readLine();
            while (line != null) {
                buf.append(line + "\n");
                line = reader.readLine();
            }
            XSLT_TEMPLATE = buf.toString();
        } catch (IOException e) {
            throw new FaultException("Error reading from jar: "
                    + xsltPath, e);
        }
    }
    
    /**
     * Creates an instance.
     * 
     * @param store where the original BMechs can be read from.
     * @param sourceDir where to find the input files, and to send output files.
     * @param cModelDeserializer the deserializer to use when reading cModels
     *        from sourceDir.
     */
    public Generator(ObjectStore store, File sourceDir,
            DODeserializer cModelDeserializer) {
        m_store = store;
        m_sourceDir = sourceDir;
        m_cModelDeserializer = cModelDeserializer;
    }
    
    /**
     * Creates an instance from properties.
     * 
     * <pre>
     *   store                - where the original BMechs can be read from.
     *                          Default value is
     *                    "fedora.utilities.digitalobject.LocalRepoObjectStore"
     *   sourceDir (required) - where to find the input files, and to send
     *                          output files.
     *   cModelDeserializer   - the deserializer to use when reading cModels
     *                          from sourceDir.  Default value is
     *               "fedora.server.storage.translation.FOXML1_1DODeserializer"
     * </pre>
     *
     * @param props the properties to get configuration values from.
     */
    public Generator(Properties props) {
        m_store = (ObjectStore) ConfigUtil.construct(props,
                "objectStore",
                "fedora.utilities.digitalobject.LocalRepoObjectStore");
        m_sourceDir = ConfigUtil.getRequiredFile(props, "sourceDir");
        m_cModelDeserializer = (DODeserializer) ConfigUtil.construct(props,
                "cModelDeserializer",
                "fedora.server.storage.translation.FOXML1_1DODeserializer");
    }
   
    /**
     * Generates all necessary stylesheets and BMechs.
     */
    public void generateAll() {
        for (File file : m_sourceDir.listFiles()) {
            String[] parts = file.getName().split("\\.");
            if (parts.length == 2 && parts[0].startsWith("cmodel-")) {
                String key = parts[0].substring(7);
                generateAll(RepoUtil.readObject(m_cModelDeserializer, file),
                        key);
            }
        }
    }
    
    //---
    // Instance helpers
    //---
    
    private void generateAll(DigitalObject cModel, String key) {
        File xsltFile = new File(m_sourceDir, "cmodel-" + key
                + ".members.xslt");
        generateStylesheet(xsltFile, cModel.getPid());
        File bMechsFile = new File(m_sourceDir, "cmodel-" + key
                + ".bmechs.txt");
        if (bMechsFile.exists()) {
            generateBMechs(bMechsFile, key);
        }
    }
    
    private void generateStylesheet(File xsltFile, String cModelPID) {
        String xslt = XSLT_TEMPLATE.replaceAll(
                "info:fedora/changme:CONTENT_MODEL_PID",
                "info:fedora/" + cModelPID);
        try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(xsltFile), "UTF-8"));
            writer.print(xslt);
            writer.close();
        } catch (IOException e) {
            throw new FaultException("Error writing stylesheet: "
                    + xsltFile.getPath(), e);
        }
    }
    
    private void generateBMechs(File bMechsFile, String key) {
        // TODO: write the bmechs as necessary
        if (bMechsFile == null || key == null && m_store == null) {
            throw new IllegalArgumentException();
        }
        // Format of bMechsFile: (ignore blank lines and those starting w/#)
        //   OLD_BMECH demo:BMech1
        //   NEW_BMECH demo:GeneratedBMech1
        //   NEW_PARTS FULL_SIZE=DS1 MEDIUM_SIZE=DS2
        //   ...
        // Algorithm:
        //    for each (OLD_BMECH, NEW_BMECH, NEW_PARTS) found in bMechsFile,
        //      read OLD_BMECH from repo and write NEW_BMECH to file
        //      with NEW_PARTS.  Output filename is cmodel-key.bmech-n.xml
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
            System.out.println(Messages.GENERATOR_USAGE);
            System.exit(0);
        } else {
            if (args[0].equals("--help")) {
                System.out.println(Messages.GENERATOR_HELP);
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
                new Generator(props).generateAll();
            } catch (FileNotFoundException e) {
                LOG.error("Configuration file not found: " + args[0]);
                exitFatally();
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage());
                exitFatally();
                // CHECKSTYLE:OFF
            } catch (Throwable th) {
                // CHECKSTYLE:ON
                LOG.error("Generator failed due to an unexpected error", th);
                exitFatally();
            }
        }
    }
    
    private static void exitFatally() {
        System.exit(1);
    }
}
