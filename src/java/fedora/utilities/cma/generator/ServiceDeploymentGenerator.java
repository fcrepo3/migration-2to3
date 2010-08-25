/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cma.generator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.fcrepo.common.Constants;
import org.fcrepo.common.FaultException;
import org.fcrepo.common.Models;

import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;

/**
 * Utility for generating modified copies of existing BMechs.
 *
 * @author Chris Wilper
 */
class ServiceDeploymentGenerator {

    /** System-dependent line separator. */
    private static final String CR = System.getProperty("line.separator");

    /** Current date/time, for new datastreams. */
    private static final Date NOW = new Date();

    /** The source behavior mechanism object. */
    private final DigitalObject m_oldBMech;

    /** The bDef pid expressed in the source bMech. */
    private final String m_bDefPID;

    /** The latest version of all datastreams in the source bMech. */
    private final Map<String, Datastream> m_oldDatastreams;

    /** Stylesheet for fixing bMech datastreams. */
    private final Transformer m_xmlFixer;

    /** Whether the basic content model will be explicit in the output. */
    private final boolean m_explicitBasicModel;

    /**
     * Creates an instance.
     *
     * @param oldBMech
     *        the source behavior mechanism object.
     * @param explicitBasicModel
     *        whether the basic content model should be explicit in the output.
     */
    @SuppressWarnings("unchecked")
    public ServiceDeploymentGenerator(DigitalObject oldBMech,
                                      boolean explicitBasicModel) {
        m_oldBMech = oldBMech;
        m_oldDatastreams = new HashMap<String, Datastream>();
        m_explicitBasicModel = explicitBasicModel;
        // put latest version of all original datastreams in m_oldDatastreams
        Iterator<String> dsIDs = m_oldBMech.datastreamIdIterator();
        while (dsIDs.hasNext()) {
            String dsID = dsIDs.next();
            m_oldDatastreams.put(dsID, getLatestOldDS(dsID));
        }

        m_bDefPID = getBDefPID();

        // build the transformer we'll use for this instance
        final String xsltPath =
                "fedora/utilities/cma/generator/resources/"
                        + "fix-bmech-datastream.xslt";
        InputStream in =
                Generator.class.getClassLoader().getResourceAsStream(xsltPath);
        if (in == null) {
            throw new FaultException("Resource not found: " + xsltPath);
        }
        TransformerFactory tFactory = TransformerFactory.newInstance();
        try {
            m_xmlFixer = tFactory.newTransformer(new StreamSource(in));
        } catch (TransformerConfigurationException e) {
            throw new FaultException("Error configuring transformer", e);
        }
    }

    /**
     * Generates a copy of the source bMech with a different PID and new part
     * names.
     *
     * @param newPID
     *        the PID to use for the copy.
     * @param newParts
     *        mapping of original datastream input part names to the names they
     *        should have in the copy.
     * @param cModelPID
     *        the pid of the content model the BMech is contractor for (for
     *        RELS-EXT).
     * @return the copy.
     */
    public DigitalObject generate(String newPID,
                                  Map<String, String> newParts,
                                  String cModelPID) {
        DigitalObject obj = new BasicDigitalObject();
        obj.setLabel("Generated deployment for " + cModelPID + " (copy of "
                + m_oldBMech.getPid() + ")");
        obj.setPid(newPID);

        addFixedCopy(obj, "DSINPUTSPEC", newParts);
        addFixedCopy(obj, "METHODMAP", newParts);
        addFixedCopy(obj, "WSDL", newParts);
        addRelsExt(obj, cModelPID);
        copyOtherDatastreams(obj);
        return obj;
    }

    //---
    // Instance helpers
    //---

    private String getBDefPID() {
        // the bdef pid is found in the old DSINPUTSPEC's root element,
        // <fbs:DSInputSpec bDefPID="demo:DualResImage"
        byte[] xmlContent =
                ((DatastreamXMLMetadata) m_oldDatastreams
                        .get("DSINPUTSPEC")).xmlContent;
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent));
            return doc.getDocumentElement().getAttribute("bDefPID");
        } catch (IOException e) {
            throw new FaultException("Error reading DSINPUTSPEC", e);
        } catch (ParserConfigurationException e) {
            throw new FaultException("Error reading DSINPUTSPEC", e);
        } catch (SAXException e) {
            throw new FaultException("Error reading DSINPUTSPEC", e);
        }
    }

    private Datastream getLatestOldDS(String dsID) {
        Datastream latest = null;

        for (Datastream ds : m_oldBMech.datastreams(dsID)) {
            if (latest == null || ds.DSCreateDT.after(latest.DSCreateDT)) {
                latest = ds;
            }
        }
        return latest;
    }

    private void addFixedCopy(DigitalObject obj,
                              String dsID,
                              Map<String, String> newParts) {
        DatastreamXMLMetadata ds =
                (DatastreamXMLMetadata) (m_oldDatastreams.get(dsID).copy());
        obj.addDatastreamVersion(ds, false);
        fixXML(ds, newParts);
    }

    private void addRelsExt(DigitalObject obj,
                            String cModelPID) {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata("UTF-8");
        ds.DSVersionable = true;
        ds.DatastreamID = "RELS-EXT";
        ds.DSVersionID = "RELS-EXT" + "1.0";
        ds.DSControlGrp = "X";
        ds.DSMIME = "application/rdf+xml";
        ds.DSFormatURI = Constants.RELS_EXT1_0.uri;
        ds.DSLabel = "RDF Statements about this object";
        ds.DSCreateDT = NOW;
        try {
            ds.xmlContent =
                    getRelsExtContent(obj.getPid(),
                                      m_bDefPID,
                                      cModelPID,
                                      m_explicitBasicModel).getBytes("UTF-8");
            obj.addDatastreamVersion(ds, false);
        } catch (UnsupportedEncodingException e) {
            throw new FaultException(e);
        }
    }

    private void copyOtherDatastreams(DigitalObject obj) {
        for (String dsID : m_oldDatastreams.keySet()) {

            if (!obj.datastreams(dsID).iterator().hasNext()) {
                obj.addDatastreamVersion(m_oldDatastreams.get(dsID).copy(),
                                         true);
            }
        }
    }

    private void fixXML(DatastreamXMLMetadata ds,
                        Map<String, String> newParts) {
        try {
            String xml = new String(ds.xmlContent, "UTF-8");
            for (String oldName : newParts.keySet()) {
                String newName = newParts.get(oldName);
                m_xmlFixer.setParameter("oldName", oldName);
                m_xmlFixer.setParameter("newName", newName);
                StringReader source = new StringReader(xml);
                StringWriter result = new StringWriter();
                m_xmlFixer.transform(new StreamSource(source),
                                     new StreamResult(result));
                xml = result.toString();
            }
            ds.xmlContent = xml.getBytes("UTF-8");
        } catch (TransformerException e) {
            throw new FaultException("Error transforming datastream "
                    + ds.DatastreamID, e);
        } catch (UnsupportedEncodingException e) {
            throw new FaultException(e);
        }
    }

    //---
    // Static helpers
    //---

    private static String getRelsExtContent(String pid,
                                            String bDefPID,
                                            String cModelPID,
                                            boolean explicitBasicModel) {
        StringBuffer out = new StringBuffer();
        out
                .append("<rdf:RDF xmlns:rdf=\"" + Constants.RDF.uri
                        + "\" xmlns:fedora-model=\"" + Constants.MODEL.uri
                        + "\">" + CR);
        out.append("  <rdf:Description rdf:about=\"info:fedora/" + pid + "\">"
                + CR);
        out.append("    <fedora-model:isDeploymentOf"
                + " rdf:resource=\"info:fedora/" + bDefPID + "\"/>" + CR);
        out.append("    <fedora-model:isContractorOf"
                + " rdf:resource=\"info:fedora/" + cModelPID + "\"/>" + CR);
        out.append("    <fedora-model:hasModel" + " rdf:resource=\""
                + Models.SERVICE_DEPLOYMENT_3_0.uri + "\"/>" + CR);
        if (explicitBasicModel) {
            out.append("    <fedora-model:hasModel" + " rdf:resource=\""
                    + Models.FEDORA_OBJECT_3_0.uri + "\"/>" + CR);
        }
        out.append("  </rdf:Description>" + CR);
        out.append("</rdf:RDF>");
        return out.toString();
    }

}
