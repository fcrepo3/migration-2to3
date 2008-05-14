/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.utilities.cmda.generator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

import org.xml.sax.SAXException;

import org.w3c.dom.Document;

import fedora.common.Constants;
import fedora.common.FaultException;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;

/**
 * Utility for generating modified copies of existing BMechs.
 * 
 * @author Chris Wilper
 */
class BMechGenerator {
    
    /** System-dependent line separator. */
    private static final String CR = System.getProperty("line.separator");
    
    /** The source behavior mechanism object. */
    private final DigitalObject m_oldBMech;
    
    /** The bDef pid expressed in the source bMech. */
    private final String m_bDefPID;
    
    /** The latest version of all datastreams in the source bMech. */
    private final Map<String, Datastream> m_oldDatastreams;
   
    /** Stylesheet for fixing bMech datastreams. */
    private final Transformer m_xmlFixer;
   
    /**
     * Creates an instance.
     * 
     * @param oldBMech the source behavior mechanism object.
     */
    @SuppressWarnings("unchecked")
    public BMechGenerator(DigitalObject oldBMech) {
        m_oldBMech = oldBMech;
        m_oldDatastreams = new HashMap<String, Datastream>();
        
        // put latest version of all original datastreams in m_oldDatastreams
        Iterator<String> dsIDs = m_oldBMech.datastreamIdIterator();
        while (dsIDs.hasNext()) {
            String dsID = dsIDs.next();
            m_oldDatastreams.put(dsID, getLatestOldDS(dsID));
        }
        
        m_bDefPID = getBDefPID();
        
        // build the transformer we'll use for this instance
        final String xsltPath =  "fedora/utilities/cmda/generator/resources/"
                + "fix-bmech-datastream.xslt";
        InputStream in = Generator.class.getClassLoader().getResourceAsStream(
                xsltPath);
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
     * Generates a copy of the source bMech with a different PID and
     * new part names.
     * 
     * @param newPID the PID to use for the copy.
     * @param newParts mapping of original datastream input part names to 
     *                 the names they should have in the copy.
     * @param cModelPID the pid of the content model the BMech is contractor
     *                  for (for RELS-EXT).
     * @return the copy.
     */
    public DigitalObject generate(String newPID, Map<String, String> newParts,
            String cModelPID) {
        DigitalObject obj = new BasicDigitalObject();
        obj.setLabel("Generated BMech for " + cModelPID + " (copy of "
                + m_oldBMech.getPid() + ")");
        obj.addFedoraObjectType(DigitalObject.FEDORA_BMECH_OBJECT);
        obj.setPid(newPID);
        addFixedCopy(obj, "DSINPUTSPEC", newParts);
        addFixedCopy(obj, "METHODMAP", newParts);
        addFixedCopy(obj, "WSDL", newParts);
        addRelsExt(obj, m_bDefPID, cModelPID);
        copyOtherDatastreams(obj);
        return obj;
    }
    
    //---
    // Instance helpers
    //---
    
    private String getBDefPID() {
        // the bdef pid is found in DSINPUTSPEC's root element,
        // <fbs:DSInputSpec bDefPID="demo:DualResImage"
        byte[] xmlContent =
            ((DatastreamXMLMetadata) m_oldDatastreams.get("DSINPUTSPEC"))
            .xmlContent;
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
    
    @SuppressWarnings("unchecked")
    private Datastream getLatestOldDS(String dsID) {
        Datastream latest = null;
        List<Datastream> versions = m_oldBMech.datastreams(dsID);
        for (Datastream ds : versions) {
            if (latest == null || ds.DSCreateDT.after(latest.DSCreateDT)) {
                latest = ds;
            }
        }
        return latest;
    }
    
    @SuppressWarnings("unchecked")
    private void addFixedCopy(DigitalObject obj, String dsID,
            Map<String, String> newParts) {
        DatastreamXMLMetadata ds =
            (DatastreamXMLMetadata) (m_oldDatastreams.get(dsID).copy());
        obj.datastreams(dsID).add(ds);
        fixXML(ds, newParts);
    }
    
    @SuppressWarnings("unchecked")
    private void addRelsExt(DigitalObject obj, String bDefPID,
            String cModelPID) {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata("UTF-8");
        ds.DSVersionable = true;
        ds.DatastreamID = "RELS-EXT";
        ds.DSVersionID = "RELS-EXT" + "1.0";
        ds.DSControlGrp = "X";
        ds.DSMIME = "application/rdf+xml";
        ds.DSLabel = "Relationships";
        ds.DSCreateDT = new Date();
        try {
            ds.xmlContent = getRelsExtDSContent(obj.getPid(), bDefPID,
                    cModelPID)
                    .getBytes("UTF-8");
            obj.datastreams("RELS-EXT").add(ds);
        } catch (UnsupportedEncodingException e) {
            throw new FaultException(e);
        }
    }
   
    @SuppressWarnings("unchecked")
    private void copyOtherDatastreams(DigitalObject obj) {
        for (String dsID : m_oldDatastreams.keySet()) {
            List<Datastream> dsList = obj.datastreams(dsID);
            if (dsList.size() == 0) {
                dsList.add(m_oldDatastreams.get(dsID).copy());
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

    private static String getRelsExtDSContent(String pid, String bDefPID,
            String cModelPID) {
        StringBuffer out = new StringBuffer();
        out.append("<rdf:RDF xmlns:rdf=\"" + Constants.RDF.uri 
                + "\" xmlns:fedora-model=\"" + Constants.MODEL.uri + "\">"
                + CR);
        out.append("  <rdf:Description rdf:about=\"info:fedora/" + pid
                + "\">" + CR);
        out.append("    <fedora-model:" + Constants.MODEL.HAS_BDEF.localName
                + " rdf:resource=\"info:fedora/" + bDefPID + "\"/>" + CR);
        out.append("    <fedora-model:"
                + Constants.MODEL.IS_CONTRACTOR.localName
                + " rdf:resource=\"info:fedora/" + cModelPID + "\"/>" + CR);
        out.append("  </rdf:Description>");
        out.append("</rdf:RDF>");
        return out.toString();
    }

}
