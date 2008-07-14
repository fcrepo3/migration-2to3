/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cma.generator;

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
import fedora.server.storage.types.DigitalObjectUtil;

import fedora.utilities.Log4J;
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
   
    /** Where the original BMechs can be read from. */
    private final ObjectStore m_store;
    
    /** Where to find the other input files, and send output files. */
    private final File m_sourceDir;
    
    /** The deserializer to use when reading cModels from sourceDir. */
    private final DODeserializer m_cModelDeserializer;
    
    /** The serializer to use when writing bMechs to sourceDir. */
    private final DOSerializer m_serializer;
   
    /** Whether the basic content model will be explicit in the output. */
    private final boolean m_explicitBasicModel;
    
    static {
        // read the xslt template from the jar into XSLT_TEMPLATE
        final String xsltBase =  "fedora/utilities/cma/generator/resources/";
        InputStream in = Generator.class.getClassLoader().getResourceAsStream(
                xsltBase + "foxml-upgrade-cma.xslt");
        XSLT_TEMPLATE = FileUtil.readTextStream(in);
    }
    
    /**
     * Creates an instance.
     * 
     * @param store where the original BMechs can be read from.
     * @param sourceDir where to find the input files, and to send output files.
     * @param cModelDeserializer the deserializer to use when reading cModels
     *        from sourceDir.
     * @param serializer the serializer to use when writing bMechs to sourceDir.
     * @param explicitBasicModel
     *        whether the basic content model should be explicit in the output.
     */
    public Generator(ObjectStore store,
                     File sourceDir,
                     DODeserializer cModelDeserializer,
                     DOSerializer serializer,
                     boolean explicitBasicModel) {
        m_store = store;
        m_sourceDir = sourceDir;
        m_cModelDeserializer = cModelDeserializer;
        m_serializer = serializer;
        m_explicitBasicModel = explicitBasicModel;
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
        m_explicitBasicModel = ConfigUtil.getOptionalBoolean(props,
                "explicitBasicModel",
                false);
    }
   
    /**
     * Generates all necessary stylesheets and SDeps.
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
        writeNoCModelStylesheet("sdeps");
        writeNoCModelStylesheet("sdefs");
        LOG.info("Generated stylesheets service deployments for " + count
                + " data object content models.");
    }
    
    //---
    // Instance helpers
    //---
    
    private void writeNoCModelStylesheet(String filePrefix) {
        File listFile = new File(m_sourceDir, filePrefix + ".txt");
        if (listFile.exists()) {
            File xsltFile = new File(m_sourceDir, filePrefix + ".xslt");
            FileUtil.writeTextFile(XSLT_TEMPLATE, xsltFile);
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
                "<xsl:param name=\"cModelPidURI\"",
                "<xsl:param name=\"cModelPidURI\" select=\"'info:fedora/"
                + cModel.getPid() + "'\"");
        FileUtil.writeTextFile(xslt, xsltFile);
        File sDepsFile = new File(m_sourceDir, "cmodel-" + key
                + ".deployments.txt");
        if (sDepsFile.exists()) {
            LOG.info("Generating service deployment object(s) for "
                    + "content model " + cModel.getPid());
            generateSDeps(sDepsFile, key, cModel.getPid());
        }
    }
    
    private void generateSDeps(File sDepsFile, String key, String cModelPID) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(sDepsFile), "UTF-8"));
            generateSDeps(reader, key, cModelPID);
            reader.close();
        } catch (IOException e) {
            throw new FaultException("Error generating service deployment(s) "
                    + "from " + sDepsFile.getPath(), e);
        }
    }
    
    private void generateSDeps(BufferedReader reader, String key,
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
                } else if (parts[0].equals("NEW_DEPLOYMENTS")) {
                    newPID = parts[1];
                } else if (parts[0].equals("NEW_PARTS")) {
                    i++;
                    File outFile = new File(m_sourceDir, "cmodel-" + key
                            + ".deployment" + i + ".xml");
                    generateSDep(oldPID, newPID,
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
   
    private void generateSDep(String oldPID, String newPID, 
            Map<String, String> newParts, File outFile, String cModelPID) {
        LOG.info("Generating service deployment " + newPID
                + " from original, " + oldPID);
        DigitalObject oldBMech = m_store.getObject(oldPID);
        if (oldBMech == null) {
            throw new FaultException("BMech not found in repository: "
                    + oldPID);
        }
        // Update MIME types and Format URIs before processing
        DigitalObjectUtil.updateLegacyDatastreams(oldBMech);
        ServiceDeploymentGenerator sDepGen =
                new ServiceDeploymentGenerator(oldBMech,
                                               m_explicitBasicModel);
        DigitalObject newSDep = sDepGen.generate(newPID, newParts, cModelPID);
        RepoUtil.writeObject(m_serializer, newSDep, outFile); 
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
        Log4J.force();
        // HACK: make DOTranslatorUtility happy
        System.setProperty("fedoraServerHost", "localhost");
        System.setProperty("fedoraServerPort", "80");
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
