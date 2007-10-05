package fedora.utilities.cmda.analyzer;

import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.DSBinding;

/**
 * A configurable <code>Classifier</code> that uses several key aspects of
 * the given objects to assign content models.
 *
 * @author cwilper@cs.cornell.edu
 */
public class DefaultClassifier implements Classifier {

    private static final String ENCODING = "UTF-8";

    private static final String CMODEL_DS_ID = "DS_COMPOSITE_MODEL";

    private static final String CMODEL_DS_VERSION_ID = CMODEL_DS_ID + "1.0";

    private static final String CMODEL_DS_CONTROL_GROUP = "X";

    private static final String CMODEL_DS_LABEL = "Datastream Composite Model";

    private static final String CMODEL_DS_MIMETYPE = "text/xml";

    private Map<Signature, DigitalObject> m_contentModels;

    private Set<Aspect> m_aspects;

    /**
     * Constructs an instance that uses the given aspects for the purpose of
     * classification.
     */
    public DefaultClassifier(Set<Aspect> aspects) {
        m_aspects = aspects;
        m_contentModels = new HashMap<Signature, DigitalObject>();
    }

    /**
     * Constructs an instance using the configuration from the given
     * properties.
     *
     * By default, no aspects are considered for the purpose of
     * classification.  If a property is found that specifies 
     * "use.AspectName", the aspect name must be one of those defined
     * in the <code>Aspect</code> enum, and the value must be "true" or
     * "false".
     */
    public DefaultClassifier(Properties config) {
        m_aspects = new HashSet<Aspect>();
        m_contentModels = new HashMap<Signature, DigitalObject>();
        for (Aspect aspect : Aspect.values()) {
            String propName = "use." + aspect.getName();
            String value = config.getProperty(propName);
            if (value != null) {
                if (value.equalsIgnoreCase("true")) {
                    m_aspects.add(aspect);
                } else if (!value.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("Boolean property "
                            + "must have value of true or false: " + propName);
                }
            }
        }
    }

    //---
    // Classifier implementation
    //---

    /**
     * @{inheritDoc}
     */
    public DigitalObject getContentModel(DigitalObject obj) {
        return getContentModel(getSignature(obj));
    }

    //---
    // Instance helpers
    //---

    private boolean using(Aspect aspect) {
        return m_aspects.contains(aspect);
    }

    @SuppressWarnings("unchecked")
    private DigitalObject getContentModel(Signature signature) {
        if (m_contentModels.containsKey(signature)) {
            return m_contentModels.get(signature);
        } else {
            DigitalObject cModelObj = new BasicDigitalObject();
            cModelObj.datastreams(CMODEL_DS_ID).add(getCModelDS(signature));
            m_contentModels.put(signature, cModelObj);
            return cModelObj;
        }
    }

    private Signature getSignature(DigitalObject obj) {
        return new Signature(getOrigContentModelConstraint(obj),
                getBMechPIDConstraints(obj),
                getBindingKeyAssignmentConstraints(obj),
                getDatastreamIDConstraints(obj),
                getMIMETypeConstraints(obj),
                getFormatURIConstraints(obj));
    }

    private String getOrigContentModelConstraint(DigitalObject obj) {
        if (using(Aspect.ORIG_CONTENT_MODEL)) {
            return obj.getContentModelId();
        } else {
            return null;
        }
    }

    private Set<String> getBMechPIDConstraints(DigitalObject obj) {
        if (using(Aspect.BMECH_PIDS)) {
            return getBMechPIDs(obj);
        } else {
            return null;
        }
    }

    private Set<String> getBMechPIDs(DigitalObject obj) {
        Set<String> set = new HashSet<String>();
        Iterator dissIDs = obj.disseminatorIdIterator();
        while (dissIDs.hasNext()) {
            Disseminator diss = getLatestDissVersion(obj, (String) dissIDs.next());
            set.add(diss.bMechID);
        }
        return set;
    }

    private Map<String, Set<String>> getBindingKeyAssignmentConstraints(
            DigitalObject obj) {
        if (using(Aspect.BINDING_KEY_ASSIGNMENTS)) {
            Map<String, Set<String>> map = new HashMap<String, Set<String>>();
            Iterator dissIDs = obj.disseminatorIdIterator();
            while (dissIDs.hasNext()) {
                Disseminator diss = getLatestDissVersion(obj, (String) dissIDs.next());
                map.put(diss.bMechID, getBindingKeyAssignments(diss));
            }
            return map;
        } else {
            return null;
        }
    }

    private Set<String> getBindingKeyAssignments(Disseminator diss) {
        Set<String> set = new HashSet<String>();
        for (DSBinding binding : diss.dsBindMap.dsBindings) {
            set.add(binding.bindKeyName + "=" + binding.datastreamID);
        }
        return set;
    }

    private Set<String> getDatastreamIDConstraints(DigitalObject obj) {
        if (using(Aspect.DATASTREAM_IDS)) {
            // TODO: return the info from the object
            return null;
        } else {
            return null;
        }
    }

    private Map<String, String> getMIMETypeConstraints(DigitalObject obj) {
        if (using(Aspect.MIME_TYPES)) {
            // TODO: return the info from the object
            return null;
        } else {
            return null;
        }
    }

    private Map<String, String> getFormatURIConstraints(DigitalObject obj) {
        if (using(Aspect.FORMAT_URIS)) {
            // TODO: return the info from the object
            return null;
        } else {
            return null;
        }
    }

    private Datastream getCModelDS(Signature signature) {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata(ENCODING);
        ds.DatastreamID = CMODEL_DS_ID;
        ds.DSVersionID = CMODEL_DS_VERSION_ID;
        ds.DSControlGrp = CMODEL_DS_CONTROL_GROUP;
        ds.DSLabel = CMODEL_DS_LABEL;
        ds.DSMIME = CMODEL_DS_MIMETYPE;
        try {
            ds.xmlContent = getCModelDSContent(signature).getBytes(ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Bad char encoding: " + ENCODING, e);
        }
        return ds;
    }

    private String getCModelDSContent(Signature signature) {
        // TODO: express signature in DS_COMPOSITE_MODEL format
        return null;
    }

    //---
    // Static helpers
    //---

    private static Disseminator getLatestDissVersion(DigitalObject obj, String dissID) {
        Disseminator latest = null;
        Iterator disses = obj.disseminators(dissID).iterator();
        while (disses.hasNext()) {
            Disseminator diss = (Disseminator) disses.next();
            if (latest == null || latest.dissCreateDT.getTime() < diss.dissCreateDT.getTime()) {
                latest = diss;
            }
        }
        return latest;
    }
}
