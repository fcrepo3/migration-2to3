package fedora.utilities.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import fedora.utilities.config.ConfigUtil;
import fedora.utilities.digitalobject.ObjectStore;

/**
 * Utility to apply transformation rules to Fedora objects.
 *
 * @author Chris Wilper
 */
public class Transformer {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(Transformer.class);
   
    /** PID files this instance will run with. */
    private final List<File> m_pidFiles;
    
    /** Corresponding XSLT files this instance will use. */
    private final List<File> m_xsltFiles;
    
    /**
     * Creates an instance.
     * 
     * @param pidFiles pid files identifying objects to transform for
     *                 each associated stylesheet.
     * @param xsltFiles xslt files containing transformation rules for
     *                  each associated pid file.
     */
    public Transformer(List<File> pidFiles, List<File> xsltFiles) {
        m_pidFiles = pidFiles;
        m_xsltFiles = xsltFiles;
    }
  
    /**
     * Creates an instance from properties.
     *
     * <pre>
     *   pidFiles  (required)      - path to a file containing a list of PIDs
     *                              (one per line).  Multiple files may be
     *                              given here, in which case they should be
     *                              delimited by space.  For each pidFile given,
     *                              there must be a corresponding xsltFile.
     *   xsltFiles (required)      - path to an xslt through each object in
     *                              the corresponding list of PIDs should be
     *                              passed.  Multiple files may also be given
     *                              here, same rules as above.
     * </pre>
     *
     * @param props the properties.
     */
    public Transformer(Properties props) {
        // TODO: initialize from properties
        // m_classifier = (Classifier) ConfigUtil.construct(props,
        //     CLASSIFIER_PROPERTY, DEFAULT_CLASSIFIER);
        m_pidFiles = new ArrayList<File>();
        m_xsltFiles = new ArrayList<File>();
    }
   
    /**
     * Run the transformations.
     */
    public void transformAll(ObjectStore store, boolean dryRun) {
        // TODO: run transformation(s) as configured
    }

    //---
    // Command-line
    //---
    
    /**
     * Command-line entry point for the analyzer.
     * 
     * @param args command-line arguments.
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
            System.out.println(Messages.TRANSFORMER_USAGE);
            System.exit(0);
        } else {
            if (args[0].equals("--help")) {
                // TODO: complete transformer help in Messages.properties
                System.out.println(Messages.TRANSFORMER_HELP);
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
                Transformer transformer = new Transformer(props);
                ObjectStore store = (ObjectStore) ConfigUtil.construct(props,
                        "objectStore",
                        "fedora.utilities.digitalobject.LocalRepoObjectStore");
                boolean dryRun = ConfigUtil.getOptionalBoolean(props, "dryRun",
                        false);
                transformer.transformAll(store, dryRun);
            } catch (FileNotFoundException e) {
                LOG.error("Configuration file not found: " + args[0]);
                exitFatally();
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage());
                exitFatally();
                // CHECKSTYLE:OFF
            } catch (Throwable th) {
                // CHECKSTYLE:ON
                LOG.error("Transformation failed due to an unexpected error",
                        th);
                exitFatally();
            }
        }
    }
    
    private static void exitFatally() {
        System.exit(1);
    }
}