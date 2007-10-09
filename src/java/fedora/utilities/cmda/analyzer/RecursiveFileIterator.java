package fedora.utilities.cmda.analyzer;

import java.io.File;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * An iterator that crawls a directory looking for files.
 *
 * @author cwilper@cs.cornell.edu
 */
public class RecursiveFileIterator implements Iterator<File> {

    /** The current stack of dir entries (where we are in the tree). */
    private final Stack<DirNode> m_stack;

    /** The next file (null when exhausted). */
    private File m_next;

    /**
     * Constructs an instance.
     *
     * @param baseDir the directory to start from.
     * @throws IllegalArgumentException if given file is not an existing dir.
     */
    public RecursiveFileIterator(File baseDir) {
        m_stack = new Stack<DirNode>();
        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("No such directory: "
                    + baseDir.getPath());
        }
        m_stack.push(new DirNode(baseDir));
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

        private File[] m_children;

        private int m_pos;

        public DirNode(File file) {
            m_children = file.listFiles();
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
