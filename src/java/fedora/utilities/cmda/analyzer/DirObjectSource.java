package fedora.utilities.cmda.analyzer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;

import java.util.NoSuchElementException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.config.ConfigUtil;
import fedora.utilities.file.RecursiveFileIterator;

import static fedora.utilities.cmda.analyzer.Constants.CHAR_ENCODING;

/**
 * An object source that crawls a directory looking for Fedora objects.
 *
 * @author Chris Wilper
 */
public class DirObjectSource implements ObjectSource {

    /** The deserializer that will be used if none is specified. */
    public static final String DEFAULT_DESERIALIZER =
            "fedora.server.storage.translation.FOXML1_0DODeserializer";

    /** The file filter that will be used if none is specified. */
    public static final String DEFAULT_FILE_FILTER =
            "fedora.utilities.cmda.analyzer.DefaultFileFilter";

    /** 
     * The maximum consecutive XML files with deserialization errors that
     * will be skipped before aborting, if unspecified.
     */
    public static final int DEFAULT_MAXSKIP = 50;

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(DirObjectSource.class);

    /** A file iterator starting at the source directory. */
    private final RecursiveFileIterator m_files;

    /** The deserializer this instance uses. */
    private final DODeserializer m_deserializer;

    /**
     * The maximum consecutive unserializable files that will be skipped
     * before aborting by this instance.
     */
    private final int m_maxSkip;

    /** The next object (null when exhausted). */
    private DigitalObject m_next;

    /**
     * Constructs an instance.
     * 
     * @param sourceDir directory to start from.
     * @param filter the file filter to use, null if none.
     * @param deserializer the deserializer to use.
     * @param maxSkip the max consecutive unserializable files skipped before
     *        aborting.
     */
    public DirObjectSource(File sourceDir, FileFilter filter, 
            DODeserializer deserializer, int maxSkip) {
        m_files = new RecursiveFileIterator(sourceDir, filter);
        m_deserializer = deserializer;
        m_maxSkip = maxSkip;
        m_next = getNext();
    }

    /**
     * Constructs an instance with configuration taken from the given
     * properties.
     *
     * <p><b>Specifying the Source Directory (Required)</b>
     * <br/>
     * The <code>sourceDir</code> property must name the directory to start
     * from.  The directory must be specified and exist or an
     * <code>IllegalArgumentException</code> will be thrown.</p>
     *
     * <p><b>Specifying the File Filter</b>
     * <br/>
     * If <code>fileFilter</code> is specified, an instance of the class it
     * names will be constructed by passing in the given properties to its
     * Properties (or no-arg) constructor.  Otherwise, the
     * <code>DEFAULT_FILE_FILTER</code> will be used.</p>
     *
     * <p><b>Specifying the Deserializer</b>
     * <br/>
     * If <code>deserializer</code> is specified, an instance of the class it
     * names will be constructed by passing in the given properties to its
     * Properties (or no-arg) constructor.  Otherwise, the
     * <code>DEFAULT_DESERIALIZER</code> will be used.</p>
     *
     * <p><b>Specifying the Maximum Skips</b>
     * <br/>
     * If <code>maxSkip</code> is specified, it must be an integer or an
     * <code>IllegalArgumentException</code> will be thrown.  If unspecified,
     * the <code>DEFAULT_MAXSKIP</code> value will be used.</p>
     *
     * @param props the properties to get the configuration from.
     */
    public DirObjectSource(Properties props) {
        FileFilter filter = (FileFilter) ConfigUtil.construct(props,
                "fileFilter", DEFAULT_FILE_FILTER);
        m_files = new RecursiveFileIterator(new File(
                ConfigUtil.getRequiredString(props, "sourceDir")), filter);
        m_deserializer = (DODeserializer) ConfigUtil.construct(props,
                "deserializer", DEFAULT_DESERIALIZER);
        m_maxSkip = ConfigUtil.getOptionalInt(props, "maxSkip",
                DEFAULT_MAXSKIP);
        m_next = getNext();
    }

    //---
    // Iterator<DigitalObject> implementation
    //---

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return m_next != null;
    }

    /**
     * {@inheritDoc}
     */
    public DigitalObject next() {
        if (m_next == null) {
            throw new NoSuchElementException("Iterator exhausted");
        } else {
            DigitalObject current = m_next;
            m_next = getNext();
            return current;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove() { 
        throw new UnsupportedOperationException("Remove not supported");
    }

    //---
    // ObjectSource implementation
    //---

    /**
     * {@inheritDoc}
     */
    public void close() { }

    //---
    // Instance helpers
    //---

    private DigitalObject getNext() {
        int warnings = 0;
        while (m_files.hasNext()) {
            File file = m_files.next();
            try {
                DigitalObject obj = new BasicDigitalObject();
                m_deserializer.deserialize(
                        new FileInputStream(file), obj, CHAR_ENCODING,
                        DOTranslationUtility.DESERIALIZE_INSTANCE);
                return obj;
            } catch (Exception e) {
                if (isXML(file)) {
                    LOG.warn("Skipping " + file.getPath() + "; can't "
                            + "deserialize", e);
                    warnings++;
                    if (warnings > m_maxSkip) {
                        throw new RuntimeException("Too many consecutive "
                                + "files could not be deserialized; aborting");
                    }
                } else {
                    LOG.debug("Skipping " + file.getPath() + "; not XML");
                }
            }
        }
        return null;
    }

    //---
    // Static helpers
    //---

    private boolean isXML(File file) {
        try {
            DocumentBuilderFactory factory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            parser.parse(file);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
