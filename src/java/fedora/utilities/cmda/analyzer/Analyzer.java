package fedora.utilities.cmda.analyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
     * Iterates the given objects, classifying them and sending output
     * to the given directory.
     *
     * @param objects iterator of objects to classify.
     * @param outputDir the directory to send output to.  It must not contain
     *                  any files.  If it doesn't yet exist, it will be
     *                  created.
     */
    public void classifyAll(Iterator<DigitalObject> objects,
            File outputDir) {
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
        LOG.warn("Not implemented");
        // TODO: use system props to pass these in (simplifies parsing)
    }

}
