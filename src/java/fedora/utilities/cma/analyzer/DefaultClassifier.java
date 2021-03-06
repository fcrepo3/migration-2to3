/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cma.analyzer;

import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import org.fcrepo.common.Constants;
import org.fcrepo.common.FaultException;
import org.fcrepo.common.Models;

import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.DSBinding;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.storage.types.Disseminator;

import fedora.utilities.config.ConfigUtil;
import fedora.utilities.digitalobject.PIDGenerator;

import static fedora.utilities.cma.analyzer.Constants.CR;
import static fedora.utilities.cma.analyzer.Constants.UTF8;

/**
 * A classifier that can use several key aspects of the given objects to assign
 * content models.
 *
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class DefaultClassifier
        implements Classifier {

    /**
     * The PID generator that will be used if none is specified;
     * <code>fedora.utilities.digitalobject.SimplePIDGenerator</code>
     */
    public static final String DEFAULT_PID_GENERATOR =
            "fedora.utilities.digitalobject.SimplePIDGenerator";

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(DefaultClassifier.class);

    /** Control group to use for Inline XML datastreams. */
    private static final String INLINE_XML = "X";

    /** The text/xml MIME type. */
    private static final String TEXT_XML = "text/xml";

    /** The application/rdf+xml MIME type. */
    private static final String RDF_XML = "application/rdf+xml";

    /** Label for DS-COMPOSITE-MODEL datastreams. */
    private static final String DS_COMPOSITE_MODEL_LABEL =
            "Datastream Composite Model";

    /** Label for RELS-EXT datastreams. */
    private static final String RELS_EXT_LABEL =
            "RDF Statements about this object";

    /** Current date/time, for new datastreams. */
    private static final Date NOW = new Date();

    /** Whether the basic content model will be explicit in the output. */
    private final boolean m_explicitBasicModel;

    /** Aspects used by this instance for the purpose of classification. */
    private Set<Aspect> m_aspects;

    /** Specific datastream IDs to be ignored during classification. */
    private final Set<String> m_ignoreDatastreamIDs;

    /** The PID generator used by this instance. */
    private final PIDGenerator m_pidGen;

    /** Map of content models used for each signature. */
    private final Map<Signature, DigitalObject> m_contentModels;

    /** Map of member signatures used for each content model (keyed by PID). */
    private final Map<String, Signature> m_memberSignatures;

    /**
     * Constructs an instance that uses the given aspects for the purpose of
     * classification, and the given generator for the purpose of assigning pids
     * to generated content models.
     *
     * @param ignoreAspects
     *        aspects to ignore for classification. NOTE: BDefPIDs, BMechPIDs,
     *        and BindingKeyAssignments are required and will NOT be ignored,
     *        even if specified here. Also, MIMETypes and FormatURIs will be
     *        ignored automatically if DatastreamIDs is ignored.
     * @param ignoreDatastreamIDs
     *        specific datastreams to ignore during classification.
     *        This has no effect if DatastreamIDs is already specified as an
     *        ignoreAspect.
     * @param pidGen
     *        the pid generator to use.
     * @param explicitBasicModel
     *        If true, will explicitly declare content models as having the
     *        basic FedoraObject-3.0 model.
     */
    public DefaultClassifier(Set<Aspect> ignoreAspects,
                             Set<String> ignoreDatastreamIDs,
                             PIDGenerator pidGen,
                             boolean explicitBasicModel) {
        setAspects(ignoreAspects);
        m_ignoreDatastreamIDs = ignoreDatastreamIDs;
        m_pidGen = pidGen;
        m_contentModels = new HashMap<Signature, DigitalObject>();
        m_memberSignatures = new HashMap<String, Signature>();
        m_explicitBasicModel = explicitBasicModel;
    }

    /**
     * Constructs an instance using the configuration from the given properties.
     * <p>
     * <b>Specifying Aspects</b> <br/> By default, all aspects are considered
     * for the purpose of classification. To ignore one or more aspects, the
     * <code>ignoreAspects
     * </code> property should be used. The value, if
     * specified, should contain a space-delimited list of any of the following:
     * <ul>
     * <li> OrigContentModel</li>
     * <li> DatastreamIDs (will cause MIMETypes and FormatURIs to be ignored)
     * </li>
     * <li> MIMETypes</li>
     * <li> FormatURIs</li>
     * </ul>
     * <p>
     * <b>Ignoring Specific Datastreams</b> <br/> By default, all datastreams
     * are considered for the purpose of classification. To ignore one or
     * more datastreams, the <code>ignoreDatastreamIDs</code> property
     * should be used. The value, if specified, should contain a
     * space-delimited list of datastream IDs to ignore.  This has no effect
     * if DatastreamIDs is already specified as an ignoreAspect.
     * <p>
     * <b>Specifying the PIDGenerator</b> <br/> By default, a built-in PID
     * generator will be used that generates PIDs of the form:
     * <code>changeme:CModel#</code>, where # is incremented for each new
     * PID. If a property is found named <code>pidGen</code>, the value
     * specifies the PIDGenerator class to use, and the class must have a
     * constructor that accepts a Properties object for configuration.
     *
     * @param props
     *        the properties to get the configuration from.
     */
    public DefaultClassifier(Properties props) {
        setAspects(getIgnoreAspects(props));
        m_ignoreDatastreamIDs = getIgnoreDatastreamIDs(props);
        if (props.get("pidPrefix") == null) {
            props.put("pidPrefix", "changeme:CModel");
        }
        m_pidGen =
                (PIDGenerator) ConfigUtil.construct(props,
                                                    "pidGen",
                                                    DEFAULT_PID_GENERATOR);
        m_contentModels = new HashMap<Signature, DigitalObject>();
        m_memberSignatures = new HashMap<String, Signature>();
        m_explicitBasicModel =
                ConfigUtil.getOptionalBoolean(props,
                                              "explicitBasicModel",
                                              false);
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

    /**
     * {@inheritDoc}
     */
    public String getBMechDirectives(String cModelPID) {
        return getBMechDirectives(m_memberSignatures.get(cModelPID), cModelPID);
    }

    //---
    // Instance helpers
    //---

    private String getBMechDirectives(Signature memberSignature,
                                      String cModelPID) {
        Set<String> bMechPIDs = memberSignature.getBMechPIDs();
        if (bMechPIDs == null || bMechPIDs.size() == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        for (String origPID : bMechPIDs) {
            out.append("OLD_BMECH " + origPID + CR);
            i++;
            out.append("NEW_DEPLOYMENTS " + cModelPID + "-SDep" + i + CR);
            out.append("NEW_PARTS");
            Set<String> assignments =
                    memberSignature.getBindingKeyAssignments(origPID);
            for (String assignment : assignments) {
                out.append(" " + assignment);
            }
            out.append(CR + CR);
        }
        return out.toString();
    }

    private void setAspects(Set<Aspect> ignoreAspects) {
        m_aspects = new HashSet<Aspect>();
        Aspect[] allAspects = Aspect.values();
        for (int i = 0; i < allAspects.length; i++) {
            m_aspects.add(allAspects[i]);
        }
        for (Aspect aspect : ignoreAspects) {
            if (aspect == Aspect.ORIG_CONTENT_MODEL
                    || aspect == Aspect.DATASTREAM_IDS
                    || aspect == Aspect.MIME_TYPES
                    || aspect == Aspect.FORMAT_URIS) {
                LOG.info("Ignoring aspect: " + aspect.getName());
                m_aspects.remove(aspect);
            } else {
                LOG.warn("NOT ignoring required aspect: " + aspect.getName());
            }
        }
    }

    private DigitalObject getContentModel(Signature signature) {
        if (m_contentModels.containsKey(signature)) {
            return m_contentModels.get(signature);
        }
        DigitalObject cModelObj = new BasicDigitalObject();
        cModelObj.setLabel("Generated CModel");
        cModelObj.setPid(m_pidGen.getNextPID().toString());
        addRelsExt(cModelObj, signature, m_explicitBasicModel);
        addInlineDS(cModelObj,
                    "DS-COMPOSITE-MODEL",
                    TEXT_XML,
                    Constants.DS_COMPOSITE_MODEL1_0.uri,
                    DS_COMPOSITE_MODEL_LABEL,
                    getDSCompositeModelContent(signature));
        addInlineDS(cModelObj,
                    "CLASS-DESCRIPTION",
                    TEXT_XML,
                    null,
                    "Technical description of the class of objects assigned to"
                            + " this content model",
                    "<class-description>" + CR + signature.toString() + CR
                            + "</class-description>");
        m_contentModels.put(signature, cModelObj);
        m_memberSignatures.put(cModelObj.getPid(), signature);
        return cModelObj;
    }

    private Signature getSignature(DigitalObject obj) {
        Set<String> dsIDs = new HashSet<String>();
        if (m_aspects.contains(Aspect.DATASTREAM_IDS)) {
            dsIDs.addAll(getDatastreamIDs(obj));
        }
        dsIDs.removeAll(m_ignoreDatastreamIDs);
        Map<String, Set<String>> assignments = getBindingKeyAssignments(obj);

        addBoundDatastreams(assignments, dsIDs);

        return new Signature(m_aspects.contains(Aspect.ORIG_CONTENT_MODEL)
                                     ? obj.getExtProperty(
                                             Constants.MODEL.CONTENT_MODEL.uri)
                                     : null,
                             m_aspects.contains(Aspect.BDEF_PIDS)
                                     ? getBDefPIDs(obj)
                                     : null,
                             m_aspects.contains(Aspect.BMECH_PIDS)
                                     ? getBMechPIDs(obj)
                                     : null,
                             assignments,
                             dsIDs,
                             m_aspects.contains(Aspect.MIME_TYPES)
                                     ? getMIMETypes(obj, dsIDs)
                                     : null,
                             m_aspects.contains(Aspect.FORMAT_URIS)
                                     ? getFormatURIs(obj, dsIDs)
                                     : null);
    }

    //---
    // Static helpers
    //---

    private static void addBoundDatastreams(Map<String,
                                            Set<String>> assignments,
                                            Set<String> dsIDs) {
        // make sure required datastreams include those indicated by
        // binding key assignments
        for (Set<String> mappings : assignments.values()) {
            for (String mapping : mappings) {
                String[] parts = mapping.split("=");
                dsIDs.add(parts[1]);
            }
        }
    }

    private static Set<Aspect> getIgnoreAspects(Properties props) {
        Set<Aspect> ignoreAspects = new HashSet<Aspect>();
        String ignorePropVal = props.getProperty("ignoreAspects");
        if (ignorePropVal != null) {
            LOG.info("Configuration specifies ignoreAspects: " + ignorePropVal);
            for (String name : ignorePropVal.trim().split("\\s+")) {
                ignoreAspects.add(Aspect.fromName(name));
            }
        }
        return ignoreAspects;
    }

    private static Set<String> getIgnoreDatastreamIDs(Properties props) {
        Set<String> ignoreDatastreamIDs = new HashSet<String>();
        String ignorePropVal = props.getProperty("ignoreDatastreamIDs");
        if (ignorePropVal != null) {
            LOG.info("Configuration specifies ignoreDatastreamIDs: "
                    + ignorePropVal);
            for (String id : ignorePropVal.trim().split("\\s+")) {
                ignoreDatastreamIDs.add(id);
            }
        }
        return ignoreDatastreamIDs;
    }


    @SuppressWarnings("unchecked")
    private static Set<String> getBDefPIDs(DigitalObject obj) {
        Set<String> set = new HashSet<String>();
        Iterator dissIDs = obj.disseminatorIdIterator();
        while (dissIDs.hasNext()) {
            Disseminator diss =
                    getLatestDissVersion(obj, (String) dissIDs.next());
            set.add(diss.bDefID);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getBMechPIDs(DigitalObject obj) {
        Set<String> set = new HashSet<String>();
        Iterator dissIDs = obj.disseminatorIdIterator();
        while (dissIDs.hasNext()) {
            Disseminator diss =
                    getLatestDissVersion(obj, (String) dissIDs.next());
            set.add(diss.sDepID);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> getBindingKeyAssignments(
                DigitalObject obj) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Iterator dissIDs = obj.disseminatorIdIterator();
        while (dissIDs.hasNext()) {
            Disseminator diss =
                    getLatestDissVersion(obj, (String) dissIDs.next());
            map.put(diss.sDepID, getBindingKeyAssignments(diss));
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
    private static Map<String, String> getMIMETypes(DigitalObject obj,
                                                    Set<String> dsIDs) {
        Map<String, String> map = new HashMap<String, String>();
        for (String dsID : dsIDs) {
            map.put(dsID, getLatestDSVersion(obj, dsID).DSMIME);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getFormatURIs(DigitalObject obj,
                                                     Set<String> dsIDs) {
        Map<String, String> map = new HashMap<String, String>();
        for (String dsID : dsIDs) {
            map.put(dsID, getLatestDSVersion(obj, dsID).DSFormatURI);
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
            if (latest == null
                    || latest.dissCreateDT.getTime() < diss.dissCreateDT
                            .getTime()) {
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
            if (latest == null
                    || latest.DSCreateDT.getTime() < ds.DSCreateDT.getTime()) {
                latest = ds;
            }
        }
        return latest;
    }

    private static void addRelsExt(DigitalObject cModelObj,
                                   Signature signature,
                                   boolean explicitBasicModel) {
        addInlineDS(cModelObj,
                    "RELS-EXT",
                    RDF_XML,
                    Constants.RELS_EXT1_0.uri,
                    RELS_EXT_LABEL,
                    getRelsExtContent(signature,
                                      cModelObj.getPid(),
                                      explicitBasicModel));
    }

    @SuppressWarnings("unchecked")
    private static void addInlineDS(DigitalObject obj,
                                    String dsID,
                                    String mimeType,
                                    String formatURI,
                                    String dsLabel,
                                    String xml) {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata(UTF8);
        ds.DSVersionable = true;
        ds.DatastreamID = dsID;
        ds.DSVersionID = dsID + "1.0";
        ds.DSControlGrp = INLINE_XML;
        ds.DSLabel = dsLabel;
        ds.DSCreateDT = NOW;
        ds.DSMIME = mimeType;
        ds.DSFormatURI = formatURI;

        try {
            ds.xmlContent = xml.getBytes(UTF8);
            obj.addDatastreamVersion(ds, false);
        } catch (UnsupportedEncodingException e) {
            throw new FaultException(e);
        }
    }

    private static String getRelsExtContent(Signature signature,
                                            String pid,
                                            boolean explicitBasicModel) {
        StringBuilder out = new StringBuilder();
        out.append("<rdf:RDF xmlns:rdf=\"" + Constants.RDF.uri
                + "\" xmlns:fedora-model=\"" + Constants.MODEL.uri
                + "\">" + CR);
        out.append("  <rdf:Description rdf:about=\"info:fedora/" + pid + "\">"
                + CR);
        if (signature.getBDefPIDs() != null) {
            for (String bDefPID : signature.getBDefPIDs()) {
                out.append("    <fedora-model:hasService"
                        + " rdf:resource=\"info:fedora/" + bDefPID + "\"/>"
                        + CR);
            }
        }

        out.append("    <fedora-model:hasModel" + " rdf:resource=\""
                + Models.CONTENT_MODEL_3_0.uri + "\"/>" + CR);

        if (explicitBasicModel) {
            out.append("    <fedora-model:hasModel" + " rdf:resource=\""
                    + Models.FEDORA_OBJECT_3_0.uri + "\"/>" + CR);
        }
        out.append("  </rdf:Description>");
        out.append("</rdf:RDF>");
        return out.toString();
    }

    private static String getDSCompositeModelContent(Signature signature) {
        StringBuilder out = new StringBuilder();
        out.append("<dsCompositeModel xmlns=\""
                + Constants.DS_COMPOSITE_MODEL.uri + "\">" + CR);
        if (signature.getDatastreamIDs() != null) {
            for (String dsID : signature.getDatastreamIDs()) {
                out.append("  <dsTypeModel ID=\"" + dsID + "\">" + CR);
                String mimeType = signature.getMIMEType(dsID);
                String formatURI = signature.getFormatURI(dsID);
                if (mimeType != null || formatURI != null) {
                    out.append("    <form");
                    if (mimeType != null) {
                        out.append(" MIME=\"" + mimeType + "\"");
                    }
                    if (formatURI != null) {
                        out.append(" FORMAT_URI=\"" + formatURI + "\"");
                    }
                    out.append("/>" + CR);
                }
                out.append("  </dsTypeModel>" + CR);
            }
        }
        out.append("</dsCompositeModel>");
        return out.toString();
    }

}