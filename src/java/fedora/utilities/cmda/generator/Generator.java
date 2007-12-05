package fedora.utilities.cmda.generator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import fedora.common.FaultException;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.config.ConfigUtil;
import fedora.utilities.digitalobject.ObjectStore;
import fedora.utilities.digitalobject.RepoUtil;
import fedora.utilities.file.FileUtil;

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
        XSLT_TEMPLATE = FileUtil.readTextStream(in);
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
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(bMechsFile), "UTF-8"));
            generateBMechs(reader, key);
            reader.close();
        } catch (IOException e) {
            throw new FaultException("Error generating BMech(s) from "
                    + bMechsFile.getPath(), e);
        }
    }
    
    private void generateBMechs(BufferedReader reader, String key)
            throws IOException {
        String line = reader.readLine();
        String oldBMech = null;
        String newBMech = null;
        int i = 0;
        while (line != null) {
            line = line.trim();
            if (line.length() > 0 && !line.startsWith("#")) {
                String[] parts = line.split(" ");
                if (parts[0].equals("OLD_BMECH")) {
                    oldBMech = parts[1];
                } else if (parts[0].equals("NEW_BMECH")) {
                    newBMech = parts[1];
                } else if (parts[0].equals("NEW_PARTS")) {
                    i++;
                    File outFile = new File(m_sourceDir, "cmodel-" + key
                            + ".bmech" + i + ".xml");
                    generateBMech(oldBMech, newBMech,
                            parseNewParts(parts), outFile);
                }
            }
            line = reader.readLine();
        }
    }
    
    private Map<String, String> parseNewParts(String[] parts) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 1; i < parts.length; i++) {
            String[] newPart = parts[i].split("=");
            map.put(newPart[0], newPart[1]);
        }
        return map;
    }
   
    private void generateBMech(String oldPID, String newPID, 
            Map<String, String> newParts, File outFile) 
            throws IOException {
        // FIXME: This is just a test; it's not functionally correct.
        //        What should happen is this:
        //        - deserialize the object, change the pid,
        //        - read, change, and write the necessary datastream XML,
        //        - then serialize to the file using m_serializer.
        String src = FileUtil.readTextStream(m_store.getObjectStream(oldPID));
        String dst = src.replaceAll(oldPID, newPID);
        for (String oldPart : newParts.keySet()) {
            String newPart = newParts.get(oldPart);
            dst = dst.replaceAll(oldPart, newPart);
        }
        FileUtil.writeFile(new ByteArrayInputStream(dst.getBytes("UTF-8")),
                outFile);
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
