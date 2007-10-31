package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import fedora.server.errors.ServerException;
import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.file.RecursiveFileIterator;

/**
 * An object iterator that crawls a given directory.
 *
 * @author Chris Wilper
 */
class DirObjectIterator
        implements Iterator<DigitalObject> {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(DirObjectIterator.class);

    /** A file iterator starting at the source directory. */
    private final RecursiveFileIterator m_files;

    /** The deserializer this instance uses. */
    private final DODeserializer m_deserializer;

    /** The next object (null when exhausted). */
    private DigitalObject m_next;

    /**
     * Constructs an instance.
     * 
     * @param sourceDir directory to start from.
     * @param filter the file filter to use, null if none.
     * @param deserializer the deserializer to use.
     */
    public DirObjectIterator(File sourceDir, FileFilter filter, 
            DODeserializer deserializer) {
        m_files = new RecursiveFileIterator(sourceDir, filter);
        m_deserializer = deserializer;
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
        }
        DigitalObject current = m_next;
        m_next = getNext();
        return current;
    }

    /**
     * {@inheritDoc}
     */
    public void remove() { 
        throw new UnsupportedOperationException("Remove not supported");
    }

    //---
    // Instance helpers
    //---

    private DigitalObject getNext() {
        while (m_files.hasNext()) {
            File file = m_files.next();
            Exception err = null;
            try {
                DigitalObject obj = new BasicDigitalObject();
                m_deserializer.deserialize(
                        new FileInputStream(file), obj, "UTF-8",
                        DOTranslationUtility.DESERIALIZE_INSTANCE);
                return obj;
            } catch (IOException e) {
                err = e;
            } catch (ServerException e) {
                err = e;
            } finally {
                if (err != null) {
                    if (isXML(file)) {
                        LOG.warn("Skipping " + file.getPath() + "; can't "
                                + "deserialize", err);
                    } else {
                        LOG.debug("Skipping " + file.getPath() + "; not XML");
                    }
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
        } catch (IOException e) {
            return false;
        } catch (ParserConfigurationException e) {
            return false;
        } catch (SAXException e) {
            return false;
        }
    }

}
