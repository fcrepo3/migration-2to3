package fedora.utilities.file;

import java.io.File;
import java.io.FileFilter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * An iterator that crawls a directory looking for files.
 *
 * @author Chris Wilper
 */
public class RecursiveFileIterator implements Iterator<File> {

    /** The current stack of dir entries (where we are in the tree). */
    private final Stack<DirNode> m_stack;

    /** The file/directory filter to use (null if none). */
    private final FileFilter m_filter;

    /** The next file (null when exhausted). */
    private File m_next;

    /**
     * Constructs an instance.
     *
     * @param baseDir the directory to start from.
     * @param filter the file/directory filter to use (null if none).
     * @throws IllegalArgumentException if given file is not an existing dir.
     */
    public RecursiveFileIterator(File baseDir, FileFilter filter) {
        m_filter = filter;
        m_stack = new Stack<DirNode>();
        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("No such directory: "
                    + baseDir.getPath());
        }
        if (m_filter != null && filter.accept(baseDir)) {
            m_stack.push(new DirNode(baseDir));
        }
        m_next = getNext();
    }

    //---
    // Iterator<File> implementation
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
    public File next() {
        if (m_next == null) {
            throw new NoSuchElementException("Iterator exhausted");
        } else {
            File current = m_next;
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
    // Instance helpers
    //---

    private File getNext() {
        while (m_stack.size() != 0) {
            DirNode node = m_stack.peek();
            File child = node.nextChild();
            if (child != null) {
                if (child.isDirectory()) {
                    m_stack.push(new DirNode(child));
                } else {
                    return child;
                }
            } else {
                m_stack.pop();
            }
        }
        return null;
    }

    /**
     * Holds a directory and iteration state for children.
     */
    private class DirNode {

        /** Directories and files directly beneath this directory. */
        private File[] m_children;

        /** Current iteration state, zero-based. */
        private int m_pos;

        public DirNode(File file) {
            if (m_filter == null) {
                m_children = file.listFiles();
            } else {
                Set<File> set = new HashSet<File>();
                for (File child : file.listFiles()) {
                    if (m_filter.accept(child)) {
                        set.add(child);
                    }
                }
                m_children = set.toArray(new File[set.size()]);
            }
        }

        public File nextChild() {
            if (m_pos >= m_children.length) {
                return null;
            } else {
                return m_children[m_pos++];
            }
        }
    }

}
