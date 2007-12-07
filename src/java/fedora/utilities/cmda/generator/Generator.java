package fedora.utilities.cmda.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import fedora.common.FaultException;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOSerializer;
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
   
    /** Contains the base stylesheet for upgrading + setting new cmodel. */
    private static final String XSLT_TEMPLATE;
   
    /** Contains the base stylesheet for upgrading (no cmodel). */
    private static final String XSLT_NOCMODEL;
   
    /** Where the original BMechs can be read from. */
    private final ObjectStore m_store;
    
    /** Where to find the other input files, and send output files. */
    private final File m_sourceDir;
    
    /** The deserializer to use when reading cModels from sourceDir. */
    private final DODeserializer m_cModelDeserializer;
    
    /** The serializer to use when writing bMechs to sourceDir. */
    private final DOSerializer m_serializer;
    
    static {
        // read the xslt template from the jar into XSLT_TEMPLATE
        final String xsltBase =  "fedora/utilities/cmda/generator/resources/";
        InputStream in = Generator.class.getClassLoader().getResourceAsStream(
                xsltBase + "foxml-upgrade-cmda.xslt");
        XSLT_TEMPLATE = FileUtil.readTextStream(in);
        in = Generator.class.getClassLoader().getResourceAsStream(
                xsltBase + "foxml-upgrade-nocmodel.xslt");
        XSLT_NOCMODEL = FileUtil.readTextStream(in);
    }
    
    /**
     * Creates an instance.
     * 
     * @param store where the original BMechs can be read from.
     * @param sourceDir where to find the input files, and to send output files.
     * @param cModelDeserializer the deserializer to use when reading cModels
     *        from sourceDir.
     * @param serializer the serializer to use when writing bMechs to sourceDir.
     */
    public Generator(ObjectStore store, File sourceDir,
            DODeserializer cModelDeserializer, DOSerializer serializer) {
        m_store = store;
        m_sourceDir = sourceDir;
        m_cModelDeserializer = cModelDeserializer;
        m_serializer = serializer;
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
     *   serializer           - the serializer to use when writing bMechs
     *                          to sourceDir.  Default value is
     *                 "fedora.server.storage.translation.FOXML1_1DOSerializer"
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
        m_serializer = (DOSerializer) ConfigUtil.construct(props,
                "serializer",
                "fedora.server.storage.translation.FOXML1_1DOSerializer");
    }
   
    /**
     * Generates all necessary stylesheets and BMechs.
     */
    public void generateAll() {
        int count = 0;
        for (File file : m_sourceDir.listFiles()) {
            String[] parts = file.getName().split("\\.");
            if (parts.length == 2 && parts[0].startsWith("cmodel-")) {
                String key = parts[0].substring(7);
                generateAll(RepoUtil.readObject(m_cModelDeserializer, file),
                        key);
                count++;
            }
        }
        writeNoCModelStylesheet("nocmodel");
        writeNoCModelStylesheet("bmechs");
        writeNoCModelStylesheet("bdefs");
        LOG.info("Generated stylesheets and bMechs for " + count
                + " data object content models.");
    }
    
    //---
    // Instance helpers
    //---
    
    private void writeNoCModelStylesheet(String filePrefix) {
        File listFile = new File(m_sourceDir, filePrefix + ".txt");
        if (listFile.exists()) {
            File xsltFile = new File(m_sourceDir, filePrefix + ".xslt");
            FileUtil.writeTextFile(XSLT_NOCMODEL, xsltFile);
            LOG.info("Wrote stylesheet for " + listFile.getName() + ", "
                    + xsltFile.getName());
        }
    }
    
    private void generateAll(DigitalObject cModel, String key) {
        LOG.info("Writing stylesheet for objects with content model "
                + cModel.getPid());
        File xsltFile = new File(m_sourceDir, "cmodel-" + key
                + ".members.xslt");
        String xslt = XSLT_TEMPLATE.replaceAll(
                "info:fedora/changme:CONTENT_MODEL_PID",
                "info:fedora/" + cModel.getPid());
        FileUtil.writeTextFile(xslt, xsltFile);
        File bMechsFile = new File(m_sourceDir, "cmodel-" + key
                + ".bmechs.txt");
        if (bMechsFile.exists()) {
            LOG.info("Generating bMech object(s) for content model "
                    + cModel.getPid());
            generateBMechs(bMechsFile, key, cModel.getPid());
        }
    }
    
    private void generateBMechs(File bMechsFile, String key, String cModelPID) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(bMechsFile), "UTF-8"));
            generateBMechs(reader, key, cModelPID);
            reader.close();
        } catch (IOException e) {
            throw new FaultException("Error generating BMech(s) from "
                    + bMechsFile.getPath(), e);
        }
    }
    
    private void generateBMechs(BufferedReader reader, String key,
            String cModelPID)
            throws IOException {
        String line = reader.readLine();
        String oldPID = null;
        String newPID = null;
        int i = 0;
        while (line != null) {
            line = line.trim();
            if (line.length() > 0 && !line.startsWith("#")) {
                String[] parts = line.split(" ");
                if (parts[0].equals("OLD_BMECH")) {
                    oldPID = parts[1];
                } else if (parts[0].equals("NEW_BMECH")) {
                    newPID = parts[1];
                } else if (parts[0].equals("NEW_PARTS")) {
                    i++;
                    File outFile = new File(m_sourceDir, "cmodel-" + key
                            + ".bmech" + i + ".xml");
                    generateBMech(oldPID, newPID,
                            parseNewParts(parts), outFile, cModelPID);
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
            Map<String, String> newParts, File outFile, String cModelPID) {
        LOG.info("Generating bMech " + newPID + " from original, " + oldPID);
        DigitalObject oldBMech = m_store.getObject(oldPID);
        if (oldBMech == null) {
            throw new FaultException("BMech not found in repository: "
                    + oldPID);
        }
        BMechGenerator bMechGen = new BMechGenerator(oldBMech);
        DigitalObject newBMech = bMechGen.generate(newPID, newParts, cModelPID);
        RepoUtil.writeObject(m_serializer, newBMech, outFile); 
    }
    
    //---
    // Command-line
    //---
    
    /**
     * Command-line entry point for the generator.
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
