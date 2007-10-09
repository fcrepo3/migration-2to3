package fedora.utilities.cmda.analyzer;

import java.io.File;
import java.io.FileInputStream;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.log4j.Logger;

import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

/**
 * An object source that crawls a directory looking for Fedora objects.
 *
 * @author cwilper@cs.cornell.edu
 */
public class DirObjectSource implements ObjectSource {

    /** The deserializer that will be used if none is specified. */
    public static final String DEFAULT_DESERIALIZER =
            "fedora.server.storage.translation.FOXMLDODeserializer";

    /** 
     * The maximum consecutive unserializable files that will be skipped
     * before aborting, if unspecified.
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
     */
    public DirObjectSource(File sourceDir, DODeserializer deserializer,
            int maxSkip) {
        m_files = new RecursiveFileIterator(sourceDir);
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
        m_files = new RecursiveFileIterator(new File(
                Analyzer.getRequiredString(props, "sourceDir")));
        m_deserializer = (DODeserializer) Analyzer.construct(props,
                "deserializer", DEFAULT_DESERIALIZER);
        m_maxSkip = Analyzer.getOptionalInt(props, "maxSkip", DEFAULT_MAXSKIP);
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
                m_deserializer.deserialize(new FileInputStream(file), obj,
                        "UTF-8", DOTranslationUtility.DESERIALIZE_INSTANCE);
                return obj;
            } catch (Exception e) {
                LOG.warn("Skipping " + file.getPath() + "; can't deserialize",
                        e);
                warnings++;
                if (warnings > m_maxSkip) {
                    throw new RuntimeException("Too many consecutive files "
                            + "could not be deserialized; aborting");
                }
            }
        }
        return null;
    }

}
