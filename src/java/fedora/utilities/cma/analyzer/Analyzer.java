/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cma.analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.fcrepo.common.Constants;
import org.fcrepo.common.FaultException;

import org.fcrepo.server.storage.translation.DOSerializer;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.storage.types.DigitalObjectUtil;

import fedora.utilities.Log4J;
import fedora.utilities.config.ConfigUtil;
import fedora.utilities.digitalobject.ObjectLister;
import fedora.utilities.digitalobject.RepoUtil;
import fedora.utilities.file.FileUtil;

import static fedora.utilities.cma.analyzer.Constants.UTF8;

/**
 * Utility for analyzing a set of Fedora objects and outputting content model
 * objects and membership lists.
 *
 * @author Chris Wilper
 */
public class Analyzer {

    //---
    // Property names
    //---

    /**
     * The property indicating which classifier to use; <code>classifier</code>
     */
    public static final String CLASSIFIER_PROPERTY = "classifier";

    /**
     * The property indicating which object lister to use;
     * <code>objectLister</code>
     */
    public static final String OBJECT_LISTER_PROPERTY = "objectLister";

    /**
     * The property indicating which output directory to use:
     * <code>outputDir</code>
     */
    public static final String OUTPUT_DIR_PROPERTY = "outputDir";

    /**
     * The property indicating whether to clear the output directory if it
     * contains files prior to running classification;
     * <code>clearOutputDir</code>
     */
    public static final String CLEAR_OUTPUT_DIR_PROPERTY = "clearOutputDir";

    /**
     * The property indicating which serializer to use; <code>serializer</code>
     */
    public static final String SERIALIZER_PROPERTY = "serializer";

    //---
    // Property defaults
    //---

    /**
     * The classifier that will be used if none is specified;
     * <code>fedora.utilities.cma.analyzer.DefaultClassifier</code>
     */
    public static final String DEFAULT_CLASSIFIER =
            "fedora.utilities.cma.analyzer.DefaultClassifier";

    /**
     * The object lister that will be used if none is specified;
     * <code>fedora.utilities.digitalobject.LocalRepoObjectStore</code>
     */
    public static final String DEFAULT_OBJECT_LISTER =
            "fedora.utilities.digitalobject.LocalRepoObjectStore";

    /**
     * The serializer that will be used if none is specified;
     * <code>org.fcrepo.server.storage.translation.FOXML1_1DOSerializer</code>
     */
    public static final String DEFAULT_SERIALIZER =
            "org.fcrepo.server.storage.translation.FOXML1_1DOSerializer";

    //---
    // Private constants
    //---

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(Analyzer.class);

    /** Prefix for generated content model object filenames. */
    private static final String CMODEL_PREFIX = "cmodel-";

    /** Suffix for generated content model object filenames. */
    private static final String CMODEL_SUFFIX = ".xml";

    /** Prefix for content model membership list filenames. */
    private static final String MEMBER_PREFIX = "cmodel-";

    /** Suffix for content model membership list filenames. */
    private static final String MEMBER_SUFFIX = ".members.txt";

    //---
    // Instance variables
    //---

    /** The classifier this instance uses. */
    private final Classifier m_classifier;

    /** The output format of the content model objects. */
    private final DOSerializer m_serializer;

    /** The directory the content model objects and lists will be sent to. */
    private File m_outputDir;

    /** The current number of distinct content models seen. */
    private int m_cModelCount;

    /** Map of content model to the order in which it was seen. */
    private Map<DigitalObject, Integer> m_cModelNumber;

    /** Map of content model to the PrintWriter for the list of members. */
    private Map<DigitalObject, PrintWriter> m_memberLists;

    //---
    // Constructors
    //---

    /**
     * Creates an instance.
     *
     * @param classifier
     *        the classifier to use.
     * @param serializer
     *        the serializer to use for output objects.
     */
    public Analyzer(Classifier classifier, DOSerializer serializer) {
        m_classifier = classifier;
        m_serializer = serializer;
    }

    /**
     * Creates an instance from properties.
     *
     * <pre>
     *   classifier (optional) - the classifier to use;
     *                           default is DEFAULT_CLASSIFIER.
     *   serializer (optional) - the serializer to use for output objects;
     *                           default is DEFAULT_SERIALIZER.
     * </pre>
     *
     * @param props
     *        the properties.
     */
    public Analyzer(Properties props) {
        m_classifier =
                (Classifier) ConfigUtil.construct(props,
                                                  CLASSIFIER_PROPERTY,
                                                  DEFAULT_CLASSIFIER);
        m_serializer =
                (DOSerializer) ConfigUtil.construct(props,
                                                    SERIALIZER_PROPERTY,
                                                    DEFAULT_SERIALIZER);
    }

    //---
    // Public interface
    //---

    /**
     * Iterates the given objects, classifying them and sending output to the
     * given directory.
     *
     * @param lister
     *        provides the list of objects to classify.
     * @param outputDir
     *        the directory to send output to. It must not contain any files. If
     *        it doesn't yet exist, it will be created.
     * @param clearOutputDir
     *        if the output directory contains files, and this is true, they
     *        will be automatically deleted before classification begins.
     */
    @SuppressWarnings("deprecation")
    public void classifyAll(ObjectLister lister,
                            File outputDir,
                            boolean clearOutputDir) {
        clearState();
        setOutputDir(outputDir, clearOutputDir);
        LOG.info("Classification started.");
        int objectCount = 0;
        PrintWriter noCModelWriter;
        PrintWriter sDepWriter;
        PrintWriter sDefWriter;
        try {
            noCModelWriter = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                new File(outputDir, "nocmodel.txt")),
                            UTF8));
            noCModelWriter.println("# The following objects will be upgraded "
                    + "with no content model");
            sDepWriter = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    new File(outputDir, "sdeps.txt")),
                                    UTF8));
            sDepWriter.println("# The following Behavior Mechanism objects"
                    + " will be upgraded into Service Deployments");
            sDefWriter = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    new File(outputDir, "sdefs.txt")),
                            UTF8));
            sDefWriter.println("# The following Behavior Definition objects"
                    + " will be upgraded into Service Definitions");
        } catch (IOException e) {
            throw new FaultException("Error opening file for writing", e);
        }
        try {
            for (DigitalObject object : lister) {
                // Update MIME types and Format URIs before processing
                DigitalObjectUtil.updateLegacyDatastreams(object);
                String ftype = object.getExtProperty(Constants.RDF.TYPE.uri);
                if (Constants.MODEL.DATA_OBJECT.looselyMatches(ftype, false)) {
                    DigitalObject cModel = m_classifier.getContentModel(object);
                    if (cModel == null) {
                        noCModelWriter.println(object.getPid());
                    } else {
                        recordMembership(object, cModel);
                    }
                } else if (Constants.MODEL.BMECH_OBJECT.looselyMatches(ftype,
                                                                       false)) {
                    sDepWriter.println(object.getPid());
                } else if (Constants.MODEL.BDEF_OBJECT.looselyMatches(ftype,
                                                                      false)) {
                    sDefWriter.println(object.getPid());
                }
                objectCount++;
            }
            serializeCModels();
            writeBMechDirectives();
        } finally {
            noCModelWriter.close();
            sDepWriter.close();
            sDefWriter.close();
            closeMemberLists();
            LOG.info("Classification finished.");
            LOG.info("Total objects analyzed: " + objectCount);
            LOG.info("Total content models generated: " + m_cModelCount);
            LOG.info("Output is in directory: " + outputDir.getPath());
        }
    }

    //---
    // Instance helpers
    //---

    private void serializeCModels() {
        for (DigitalObject obj : m_cModelNumber.keySet()) {
            int num = m_cModelNumber.get(obj).intValue();
            String cModelFilename = CMODEL_PREFIX + num + CMODEL_SUFFIX;
            File file = new File(m_outputDir, cModelFilename);
            LOG.info("Serializing content model " + obj.getPid());
            RepoUtil.writeObject(m_serializer, obj, file);
        }
    }

    private void writeBMechDirectives() {
        for (DigitalObject obj : m_cModelNumber.keySet()) {
            int num = m_cModelNumber.get(obj).intValue();
            String directives = m_classifier.getBMechDirectives(obj.getPid());
            if (directives != null) {
                LOG.info("Writing deployment directives for content model "
                        + obj.getPid());
                File file = new File(m_outputDir,
                                     CMODEL_PREFIX + num + ".deployments.txt");
                try {
                    PrintWriter writer =
                            new PrintWriter(
                                    new OutputStreamWriter(
                                            new FileOutputStream(file),
                                            UTF8));
                    writer.println("# The following BMechs will be copied and "
                            + "written as FOXML1.1 service deployments");
                    writer.println("# with new PIDs and part names as given "
                            + "below");
                    writer.println(directives);
                    writer.close();
                } catch (IOException e) {
                    throw new FaultException(
                            "Error writing deployment directives: "
                            + file.getPath(), e);
                }
            }
        }
    }

    private void recordMembership(DigitalObject object, DigitalObject cModel) {
        PrintWriter writer = m_memberLists.get(cModel);
        if (writer == null) {
            m_cModelCount++;
            m_cModelNumber.put(cModel, new Integer(m_cModelCount));
            File file =
                    new File(m_outputDir, MEMBER_PREFIX + m_cModelCount
                            + MEMBER_SUFFIX);
            try {
                writer = new PrintWriter(
                        new OutputStreamWriter(new FileOutputStream(file)));
                m_memberLists.put(cModel, writer);
                writer.println("# The following objects will be assigned to "
                        + "cmodel-" + m_cModelCount);
                printHeader(writer, cModel);
            } catch (IOException e) {
                throw new FaultException("Error writing file: "
                        + file.getPath(), e);
            }
        }
        writer.println(object.getPid());
    }

    private static void printHeader(PrintWriter writer, DigitalObject cModel) {

        DatastreamXMLMetadata ds =
                (DatastreamXMLMetadata) cModel.datastreams("CLASS-DESCRIPTION")
                        .iterator().next();
        try {
            String xml = new String(ds.xmlContent, UTF8);
            writer.println("# " + xml.replaceAll("\\n", "\r\n# "));
        } catch (IOException e) {
            LOG.warn("Unsupported encoding: " + UTF8);
        }
    }

    private void closeMemberLists() {
        for (PrintWriter writer : m_memberLists.values()) {
            writer.close();
        }
        m_memberLists.clear();
    }

    private void setOutputDir(File outputDir, boolean clearOutputDir) {
        if (!outputDir.exists()) {
            outputDir.mkdir();
            if (!outputDir.exists()) {
                throw new FaultException("Failed to create directory: "
                        + outputDir.getPath());
            }
        }
        if (outputDir.listFiles().length != 0) {
            if (clearOutputDir) {
                FileUtil.clearDirectory(outputDir, true);
            } else {
                throw new FaultException("Directory not empty: "
                        + outputDir.getPath());
            }
        }
        m_outputDir = outputDir;
    }

    private void clearState() {
        m_memberLists = new HashMap<DigitalObject, PrintWriter>();
        m_cModelNumber = new HashMap<DigitalObject, Integer>();
        m_cModelCount = 0;
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
        if (args.length != 1) {
            System.out.println(Messages.ANALYZER_USAGE);
            System.exit(0);
        } else {
            if (args[0].equals("--help")) {
                System.out.println(Messages.ANALYZER_HELP);
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
                Analyzer analyzer = new Analyzer(props);
                ObjectLister lister =
                        (ObjectLister) ConfigUtil
                                .construct(props,
                                           OBJECT_LISTER_PROPERTY,
                                           DEFAULT_OBJECT_LISTER);
                String outputDir =
                        ConfigUtil
                                .getRequiredString(props, OUTPUT_DIR_PROPERTY);
                analyzer.classifyAll(lister, new File(outputDir), ConfigUtil
                        .getOptionalBoolean(props,
                                            CLEAR_OUTPUT_DIR_PROPERTY,
                                            false));
            } catch (FileNotFoundException e) {
                LOG.error("Configuration file not found: " + args[0]);
                exitFatally();
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage());
                exitFatally();
                // CHECKSTYLE:OFF
            } catch (Throwable th) {
                // CHECKSTYLE:ON
                LOG.error("Analysis failed due to an unexpected error", th);
                exitFatally();
            }
        }
    }

    private static void exitFatally() {
        System.exit(1);
    }
}
