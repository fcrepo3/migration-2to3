package fedora.utilities.cmda.analyzer;

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

import fedora.server.storage.translation.DOSerializer;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.types.DigitalObject;

/**
 * Utility for analyzing a set of Fedora objects and outputting content
 * model objects and membership lists.
 *
 * @author cwilper@cs.cornell.edu
 */
public class Analyzer {

    /** The classifier that will be used if none is specified. */
    public static final String DEFAULT_CLASSIFIER
            = "fedora.utilities.cmda.analyzer.DefaultClassifier";

    /** The serializer that will be used if none is specified. */
    public static final String DEFAULT_SERIALIZER
            = "fedora.server.storage.translation.FOXMLDOSerializer";

    /** The object source that will be used if none is specified. */
    public static final String DEFAULT_OBJECT_SOURCE
            = "fedora.utilities.cmda.analyzer.DirObjectSource";

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(Analyzer.class);

    /** The classifier this instance uses. */
    private Classifier m_classifier;

    /** The output format of the content model objects. */
    private DOSerializer m_serializer;

    /** The directory the content model objects and lists will be sent to. */
    private File m_outputDir;

    /** The current number of distinct content models seen. */
    private int m_cModelCount;

    /** Map of content model to the order in which it was seen. */
    private Map<DigitalObject, Integer> m_cModelNumber;

    /** Map of content model to the PrintWriter for the list of members. */
    private Map<DigitalObject, PrintWriter> m_memberLists;

    /**
     * Constructs an analyzer.
     *
     * @param classifier the classifier to use.
     * @param serializer the serializer to use for the output content models.
     */
    public Analyzer(Classifier classifier, DOSerializer serializer) {
        m_classifier = classifier;
        m_serializer = serializer;
    }

    /**
     * Constructs an analyzer with configuration taken from the given
     * properties.
     *
     * <p><b>Specifying the Classifier</b><br/>
     * If <code>classifier</code> is specified, an instance of the class it
     * names will be constructed by passing in the given properties to its
     * Properties (or no-arg) constructor.  Otherwise, the default classifier
     * will be used.
     *
     * <p><b>Specifying the Serializer</b><br/>
     * If <code>serializer</code> is specified, an instance of the class it
     * names will be constructed by passing in the given properties to its
     * Properties (or no-arg) constructor.  Otherwise, the default serializer
     * will be used.
     *
     * @param props the properties to get configuration from.
     */
    public Analyzer(Properties props) {
        m_classifier = (Classifier) Analyzer.construct(props, "classifier",
                DEFAULT_CLASSIFIER);
        m_serializer = (DOSerializer) Analyzer.construct(props, "serializer",
                DEFAULT_SERIALIZER);
    }

    /**
     * Iterates the given objects, classifying them and sending output
     * to the given directory.
     *
     * @param objects iterator of objects to classify.
     * @param outputDir the directory to send output to.  It must not contain
     *                  any files.  If it doesn't yet exist, it will be
     *                  created.
     */
    public void classifyAll(ObjectSource objects, File outputDir) {
        clearState();
        setOutputDir(outputDir);
        try {
            while (objects.hasNext()) {
                DigitalObject object = objects.next();
                DigitalObject cModel = m_classifier.getContentModel(object);
                recordMembership(object, cModel);
            }
            serializeCModels();
        } finally {
            closeMemberLists();
        }
    }

    private void serializeCModels() {
        for (DigitalObject object : m_cModelNumber.keySet()) {
            int num = m_cModelNumber.get(object).intValue();
            File file = new File(m_outputDir, "cmodel-" + num + ".xml");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                m_serializer.getInstance().serialize(
                        object, out, "UTF-8", 
                        DOTranslationUtility.SERIALIZE_EXPORT_MIGRATE);
            } catch (Exception e) {
                throw new RuntimeException("Error writing cmodel", e);
            } finally {
                if (out != null) {
                    try { out.close(); } catch (Exception e) { }
                }
            }
        }
    }

    private void recordMembership(DigitalObject object,
            DigitalObject cModel) {
        PrintWriter writer = m_memberLists.get(cModel);
        if (writer == null) {
            m_cModelCount++;
            m_cModelNumber.put(cModel, new Integer(m_cModelCount));
            try {
                writer = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(
                                new File(m_outputDir, "cmodel-"
                                        + m_cModelCount + ".members.txt"))));
                m_memberLists.put(cModel, writer);
            } catch (IOException e) {
                throw new RuntimeException("Error writing file", e);
            }
        }
        writer.println(object.getPid());
    }

    private void closeMemberLists() {
        for (PrintWriter writer : m_memberLists.values()) {
            writer.close();
        }
        m_memberLists.clear();
    }

    private void setOutputDir(File outputDir) {
        if (!outputDir.exists()) {
            outputDir.mkdir();
            if (!outputDir.exists()) {
                throw new RuntimeException(
                        "Unable to create output directory: "
                        + outputDir.getPath());
            }
        }
        if (outputDir.listFiles().length != 0) {
            throw new RuntimeException("Output directory is not empty: "
                    + outputDir.getPath());
        }
        m_outputDir = outputDir;
    }

    private void clearState() {
        m_memberLists = new HashMap<DigitalObject, PrintWriter>();
        m_cModelNumber = new HashMap<DigitalObject, Integer>();
        m_cModelCount = 0;
    }

    /**
     * Command-line entry point for the analyzer.
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
            printUsage();
            System.exit(0);
        } else {
            if (args[0].equals("--help")) {
                printHelp();
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
                ObjectSource source = (ObjectSource) Analyzer.construct(props,
                        "objectSource", DEFAULT_OBJECT_SOURCE);
                String outputDir = getRequiredString(props, "outputDir");
                analyzer.classifyAll(source, new File(outputDir));
            } catch (FileNotFoundException e) {
                LOG.error("Configuration file not found: " + args[0]);
                System.exit(1);
            } catch (Throwable th) {
                LOG.error("Analysis failed due to the following unexpected "
                        + "error", th);
                System.exit(1);
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: 1) java -jar analyzer.jar config.properties");
        System.out.println("   Or: 2) java -jar analyzer.jar --");
        System.out.println("   Or: 3) java -jar analyzer.jar --help");
        System.out.println();
        System.out.println("Usage 1 runs analysis with configuration from the given file.");
        System.out.println("Usage 2 runs analysis with configuration from system properties.");
        System.out.println("Usage 3 prints examples and configuration details.");
    }

    private static void printHelp() {
        // TODO: print examples and configuration details
        System.out.println("You can't be helped.");
    }

    // TODO: put this util method in another class
    public static String getRequiredString(Properties props, String name) {
        String value = props.getProperty(name);
        if (value == null) {
            throw new IllegalArgumentException("Required property missing: "
                    + name);
        } else {
            return value;
        }
    }

    // TODO: put this util method in another class
    public static int getOptionalInt(Properties props, String name,
            int defaultValue) {
        String value = props.getProperty(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not an integer: " + value);
            }
        }
    }

    // TODO: put this util method in another class
    public static Object construct(Properties props, String propName,
            String defaultClassName) {
        String className = props.getProperty(propName);
        if (className == null) {
            className = defaultClassName;
        }
        try {
            Class clazz = Class.forName(className);
            return clazz.getConstructor(Properties.class).newInstance(props);
        } catch (NoSuchMethodException e) {
            try {
                return Class.forName(className).newInstance();
            } catch (Exception e2) {
                throw new RuntimeException("Error constructing "
                        + className + "()", e2);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error constructing "
                    + className + "(Properties)", e);
        }
    }

}
