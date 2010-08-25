/* The contents of this file are subject to the license and copyright terms
 * detailed in the license sourceDirectory at the root of the source tree (also
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.digitalobject;

import java.io.File;
import java.io.FileFilter;

import java.util.Iterator;
import java.util.Properties;

import org.fcrepo.server.storage.translation.DODeserializer;
import org.fcrepo.server.storage.types.DigitalObject;

import fedora.utilities.config.ConfigUtil;

/**
 * Provides an iterator of digital objects that crawls a given directory.
 *
 * @author Chris Wilper
 */
public class DirObjectLister
        implements ObjectLister {

    /**
     * The deserializer that will be used if none is specified;
     * <code>org.fcrepo.server.storage.translation.FOXML1_0DODeserializer</code>
     */
    public static final String DEFAULT_DESERIALIZER =
            "org.fcrepo.server.storage.translation.FOXML1_0DODeserializer";

    /**
     * The file filter that will be used if none is specified;
     * <code>fedora.utilities.file.NoDotFileFilter</code>
     */
    public static final String DEFAULT_FILE_FILTER =
            "fedora.utilities.file.NoDotFileFilter";

    /** The directory to start at. */
    private final File m_sourceDir;

    /** The file filter to use, null if none. */
    private final FileFilter m_filter;

    /** The deserializer to use. */
    private final DODeserializer m_deserializer;

    /**
     * Creates an instance.
     *
     * @param sourceDir the directory to start at.
     * @param filter the file filter to use, null if none.
     * @param deserializer the deserializer to use.
     */
    public DirObjectLister(File sourceDir, FileFilter filter,
            DODeserializer deserializer) {
        m_sourceDir = sourceDir;
        m_filter = filter;
        m_deserializer = deserializer;
    }

    /**
     * Creates an instance from properties.
     *
     * <pre>
     *   sourceDir    (required) - the directory to start at.
     *   fileFilter   (optional) - the file filter to use;
     *                             default is DEFAULT_FILE_FILTER.
     *   deserializer (optional) - the deserializer to use;
     *                             default is DEFAULT_DESERIALIZER.
     * </pre>
     *
     * @param props the properties.
     */
    public DirObjectLister(Properties props) {
        m_sourceDir = new File(ConfigUtil.getRequiredString(props,
                "sourceDir"));
        m_filter = (FileFilter) ConfigUtil.construct(props,
                "fileFilter", DEFAULT_FILE_FILTER);
        m_deserializer = (DODeserializer) ConfigUtil.construct(props,
                "deserializer", DEFAULT_DESERIALIZER);
    }

    //---
    // ObjectLister implementation
    //---

    /**
     * {@inheritDoc}
     */
    public Iterator<DigitalObject> iterator() {
        return new DirObjectIterator(m_sourceDir, m_filter, m_deserializer);
    }

}
