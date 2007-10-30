package fedora.utilities.cmda.analyzer;

import java.io.UnsupportedEncodingException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DSBinding;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Disseminator;

import fedora.utilities.config.ConfigUtil;

import static fedora.utilities.cmda.analyzer.Constants.CHAR_ENCODING;

/**
 * A classifier that can use several key aspects of the given objects to
 * assign content models.
 *
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class DefaultClassifier implements Classifier {

    /** The PID generator that will be used if none is specified. */
    public static final String DEFAULT_PID_GENERATOR
            = "fedora.utilities.cmda.analyzer.DefaultPIDGenerator";

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            DefaultClassifier.class);

    /** Control group to use for Inline XML datastreams. */
    private static final String INLINE_DS_CONTROL_GROUP = "X";

    /** MIME type to use for Inline XML datastreams. */
    private static final String INLINE_DS_MIME_TYPE = "text/xml";

    /** Datastream version ID suffix. */
    private static final String DS_VERSION_ID_SUFFIX = "1.0";

    /** Datastream ID for composite model datastreams. */
    private static final String COMP_MODEL_DS_ID = "DS-COMPOSITE-MODEL";

    /** Label for composite model datastreams. */
    private static final String COMP_MODEL_DS_LABEL = "DS Composite Model";

    /** Datastream ID for RELS-EXT datastreams. */
    private static final String RELS_EXT_DS_ID = "RELS-EXT";

    /** Label for RELS-EXT datastreams. */
    private static final String RELS_EXT_DS_LABEL = "Relationships";

    /** Aspects used by this instance for the purpose of classification. */
    private Set<Aspect> m_aspects;

    /** The PID generator used by this instance. */
    private PIDGenerator m_pidGen;

    /** Map of content models used for each signature. */
    private Map<Signature, DigitalObject> m_contentModels;

    /**
     * Constructs an instance that uses the given aspects for the purpose of
     * classification, and the given generator for the purpose of assigning
     * pids to generated content models.
     * 
     * @param aspects aspects to use for classification.
     * @param pidGen the pid generator to use.
     */
    public DefaultClassifier(Set<Aspect> aspects, PIDGenerator pidGen) {
        setAspects(aspects);
        m_pidGen = pidGen;
        m_contentModels = new HashMap<Signature, DigitalObject>();
    }

    /**
     * Constructs an instance using the configuration from the given
     * properties.
     *
     * <p><b>Specifying Aspects</b>
     * <br/>
     * By default, all aspects are considered for the purpose of
     * classification.  If a property is found of the form
     * <code>use.AspectName</code>, the aspect name must be one of those
     * defined in the <code>{@link Aspect}</code> enum, and the value must 
     * be "true" or "false".</p>
     *
     * <p><b>Specifying the PIDGenerator</b>
     * <br/>
     * By default, a built-in PID generator will be used that generates
     * PIDs of the form: <code>demo:GeneratedPID#</code>, where #
     * is incremented for each new PID.  If a property is found named
     * <code>pidGenerator</code>, the value specifies the PIDGenerator
     * class to use, and the class must have a constructor that accepts
     * a Properties object for configuration.
     *
     * @param props the properties to get the configuration from.
     */
    public DefaultClassifier(Properties props) {
        setAspects(getAspectsFromProperties(props));
        m_pidGen = (PIDGenerator) ConfigUtil.construct(props, "pidGen",
                DEFAULT_PID_GENERATOR);
        m_contentModels = new HashMap<Signature, DigitalObject>();
    }

    //---
    // Classifier implementation
    //---

    /**
     * {@inheritDoc}
     */
    public DigitalObject getContentModel(DigitalObject obj) {
        return getContentModel(getSignature(obj));
    }

    //---
    // Instance helpers
    //---

    private void setAspects(Set<Aspect> aspects) {
        for (Aspect aspect : aspects) {
            logExplicitUse(aspect);
        }
        addImplicit(aspects, Aspect.BINDING_KEY_ASSIGNMENTS, Aspect.BMECH_PIDS);
        addImplicit(aspects, Aspect.BMECH_PIDS, Aspect.BDEF_PIDS);
        addImplicit(aspects, Aspect.MIME_TYPES, Aspect.DATASTREAM_IDS);
        addImplicit(aspects, Aspect.FORMAT_URIS, Aspect.DATASTREAM_IDS);
        m_aspects = aspects;
    }
    
    private DigitalObject getContentModel(Signature signature) {
        if (m_contentModels.containsKey(signature)) {
            return m_contentModels.get(signature);
        }
        DigitalObject cModelObj = new BasicDigitalObject();
        cModelObj.addFedoraObjectType(DigitalObject
                .FEDORA_CONTENT_MODEL_OBJECT);
        cModelObj.setPid(m_pidGen.getNextPID().toString());
        addRelsExtDSIfNeeded(cModelObj, signature);
        addCompModelDSIfNeeded(cModelObj, signature);
        
        // TODO: put in another method
        addInlineDS(cModelObj, "CLASS-DESCRIPTION",
                "Technical description of the class of objects assigned to"
                + " this content model", "<description>\n" 
                + signature.toString() + "\n</description>");
            
        m_contentModels.put(signature, cModelObj);
        return cModelObj;
    }

    private Signature getSignature(DigitalObject obj) {
        return new Signature(
                m_aspects.contains(Aspect.ORIG_CONTENT_MODEL)
                        ? obj.getContentModelId() : null,
                m_aspects.contains(Aspect.BDEF_PIDS)
                        ? getBDefPIDs(obj) : null,
                m_aspects.contains(Aspect.BMECH_PIDS)
                        ? getBMechPIDs(obj) : null,
                m_aspects.contains(Aspect.BINDING_KEY_ASSIGNMENTS)
                        ? getBindingKeyAssignments(obj) : null,
                m_aspects.contains(Aspect.DATASTREAM_IDS)
                        ? getDatastreamIDs(obj) : null,
                m_aspects.contains(Aspect.MIME_TYPES)
                        ? getMIMETypes(obj) : null,
                m_aspects.contains(Aspect.FORMAT_URIS)
                        ? getFormatURIs(obj) : null);
    }

    //---
    // Static helpers
    //---

    private static void addImplicit(Set<Aspect> aspects, Aspect cause,
            Aspect implied) {
        if (aspects.contains(cause) && !aspects.contains(implied)) {
            logImplicitUse(implied, cause);
            aspects.add(implied);
        }
    }

    private static void logExplicitUse(Aspect explicit) {
        LOG.info("Using '" + explicit.getName() + "' for "
                + "classification");
    }

    private static void logImplicitUse(Aspect implicit, Aspect impliedBy) {
        LOG.info("Using '" + implicit.getName() + "' for "
                + "classification (implied by '" + impliedBy.getName() + "')");
    }

    private static Set<Aspect> getAspectsFromProperties(Properties props) {
        Set<Aspect> aspects = new HashSet<Aspect>();
        aspects.addAll(Arrays.asList(Aspect.values()));
        for (Aspect aspect : Aspect.values()) {
            String name = "use." + aspect.getName();
            String value = props.getProperty(name);
            if (value != null) {
                if (value.equalsIgnoreCase("false")) {
                    LOG.info("Ignoring aspect (if possible): " 
                            + aspect.getName());
                    aspects.remove(aspect);
                } else if (!value.equalsIgnoreCase("true")) {
                    throw new IllegalArgumentException("Boolean property "
                            + "must have value of true or false: " + name);
                }
            }
        }
        return aspects;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getBDefPIDs(DigitalObject obj) {
        Set<String> set = new HashSet<String>();
        Iterator dissIDs = obj.disseminatorIdIterator();
        while (dissIDs.hasNext()) {
            Disseminator diss = getLatestDissVersion(obj,
                    (String) dissIDs.next());
            set.add(diss.bDefID);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getBMechPIDs(DigitalObject obj) {
        Set<String> set = new HashSet<String>();
        Iterator dissIDs = obj.disseminatorIdIterator();
        while (dissIDs.hasNext()) {
            Disseminator diss = getLatestDissVersion(obj, 
                    (String) dissIDs.next());
            set.add(diss.bMechID);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> getBindingKeyAssignments(
            DigitalObject obj) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Iterator dissIDs = obj.disseminatorIdIterator();
        while (dissIDs.hasNext()) {
            Disseminator diss = getLatestDissVersion(obj, 
                    (String) dissIDs.next());
            map.put(diss.bMechID, getBindingKeyAssignments(diss));
        }
        return map;
    }

    private static Set<String> getBindingKeyAssignments(Disseminator diss) {
        Set<String> set = new HashSet<String>();
        for (DSBinding binding : diss.dsBindMap.dsBindings) {
            set.add(binding.bindKeyName + "=" + binding.datastreamID);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getDatastreamIDs(DigitalObject obj) {
        Set<String> set = new HashSet<String>();
        Iterator dsIDs = obj.datastreamIdIterator();
        while (dsIDs.hasNext()) {
            set.add((String) dsIDs.next());
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getMIMETypes(DigitalObject obj) {
        Map<String, String> map = new HashMap<String, String>();
        Iterator dsIDs = obj.datastreamIdIterator();
        while (dsIDs.hasNext()) {
            String dsID = (String) dsIDs.next();
            Datastream ds = getLatestDSVersion(obj, dsID);
            map.put(dsID, ds.DSMIME);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getFormatURIs(DigitalObject obj) {
        Map<String, String> map = new HashMap<String, String>();
        Iterator dsIDs = obj.datastreamIdIterator();
        while (dsIDs.hasNext()) {
            String dsID = (String) dsIDs.next();
            Datastream ds = getLatestDSVersion(obj, dsID);
            map.put(dsID, ds.DSFormatURI);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Disseminator getLatestDissVersion(DigitalObject obj,
            String dissID) {
        Disseminator latest = null;
        Iterator disses = obj.disseminators(dissID).iterator();
        while (disses.hasNext()) {
            Disseminator diss = (Disseminator) disses.next();
            if (latest == null || latest.dissCreateDT.getTime()
                    < diss.dissCreateDT.getTime()) {
                latest = diss;
            }
        }
        return latest;
    }

    @SuppressWarnings("unchecked")
    private static Datastream getLatestDSVersion(DigitalObject obj,
            String dsID) {
        Datastream latest = null;
        Iterator dses = obj.datastreams(dsID).iterator();
        while (dses.hasNext()) {
            Datastream ds = (Datastream) dses.next();
            if (latest == null || latest.DSCreateDT.getTime()
                    < ds.DSCreateDT.getTime()) {
                latest = ds;
            }
        }
        return latest;
    }

    private static void addRelsExtDSIfNeeded(DigitalObject cModelObj,
            Signature signature) {
        if (signature.getBDefPIDs() != null
                && signature.getBDefPIDs().size() > 0) {
            addInlineDS(cModelObj, RELS_EXT_DS_ID, RELS_EXT_DS_LABEL,
                    getRelsExtDSContent(signature, cModelObj.getPid()));
        }
    }

    private static void addCompModelDSIfNeeded(DigitalObject cModelObj,
            Signature signature) {
        if (signature.getDatastreamIDs() != null
                && signature.getDatastreamIDs().size() > 0) {
            addInlineDS(cModelObj, COMP_MODEL_DS_ID, COMP_MODEL_DS_LABEL,
                    getCompModelDSContent(signature));
        }
    }

    @SuppressWarnings("unchecked")
    private static void addInlineDS(DigitalObject obj, String dsID,
            String dsLabel, String xml) {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata(CHAR_ENCODING);
        ds.DatastreamID = dsID;
        ds.DSVersionID = dsID + DS_VERSION_ID_SUFFIX;
        ds.DSControlGrp = INLINE_DS_CONTROL_GROUP;
        ds.DSMIME = INLINE_DS_MIME_TYPE;
        ds.DSLabel = dsLabel;
        try {
            ds.xmlContent = xml.getBytes(CHAR_ENCODING);
            obj.datastreams(dsID).add(ds);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getRelsExtDSContent(Signature signature,
            String pid) {
        StringBuffer out = new StringBuffer();
        out.append("<rdf:RDF xmlns:rdf=\"" + Constants.RDF.uri 
                + "\" xmlns:rel=\"" + Constants.RELS_EXT.uri + "\">\n");
        out.append("  <rdf:Description rdf:about=\"info:fedora/" + pid
                + "\">\n");
        if (signature.getBDefPIDs() != null) {
            for (String bDefPID : signature.getBDefPIDs()) {
                out.append("    <rel:" + Constants.RELS_EXT
                        .HAS_FORMAL_CONTENT_MODEL.localName
                        + " rdf:resource=\"info:fedora/" + bDefPID + "\"/>\n");
            }
        }
        out.append("  </rdf:Description>");
        out.append("</rdf:RDF>");
        return out.toString();
    }

    private static String getCompModelDSContent(Signature signature) {
        StringBuffer out = new StringBuffer();
        out.append("<dsCompositeModel>\n");
        if (signature.getDatastreamIDs() != null) {
            for (String dsID : signature.getDatastreamIDs()) {
                out.append("  <dsTypeModel ID=\"" + dsID + "\">\n");
                String mimeType = signature.getMIMEType(dsID);
                String formatURI = signature.getFormatURI(dsID);
                if (mimeType != null || formatURI != null) {
                    out.append("    <form");
                    if (mimeType != null) {
                        out.append(" MIME=\"" + mimeType + "\"");
                    }
                    if (formatURI != null) {
                        out.append(" FORMAT_URIS=\"" + mimeType + "\"");
                    }
                    out.append("/>\n");
                }
                out.append("  </dsTypeModel>\n");
            }
        }
        out.append("</dsCompositeModel>");
        return out.toString();
    }

}