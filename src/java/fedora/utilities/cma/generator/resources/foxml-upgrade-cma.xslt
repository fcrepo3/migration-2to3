<?xml version="1.0" encoding="utf-8"?>
<!--
  Foxml XSLT transformer 1.0 -> 1.1
  
  Input: Valid Foxml 1.0 file
  
  Parameters:
  cModelPidURI (optional) - PID URI of the CModel object that defines
  the content model of the object being transformed. If there is none, then 
  leave undefined.
  
  createdDate (optional) - This stylesheet may need to create a RELS-EXT datastream
  if none already exists.  If so, the default behaviour is to leave the datastream
  created date undefined (and Fedora will assign one upon ingest).  This parameter may
  be used to define an explicit created date if that behaviour is undesirable.
  
  explicitBasicModel(optional) - If defined, every transformed object will explicitly
  declare that it is a member of the FedoraObject-3.0 model.  If undefined, then the 
  repository gets to choose.  By default, Fedora 3.0 assumes that all objects ae in
  the FedoraObject-3.0 model.  Future versions may not.
  
  Output: Valid Foxml 1.1 file, with the following changes:
  - VERSION attribute added, set to 1.1
  - No schema location
  - Old contentModel and fType object-level properties removed
  - Disseminators removed
  - Relationship to cModelPidURI added to latest RELS-EXT version
    (RELS-EXT will be created if needed)
  - MIMETYPE and FORMAT_URI updated for system-defined datastreams:
    DC, RELS-EXT, RELS-INT, POLICY, METHODMAP, DSINPUTSPEC, WSDL
  
  11/30/07 Aaron Birkland (birkland@cs.cornell.edu)
-->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:foxml="info:fedora/fedora-system:def/foxml#">

  <xsl:param name="cModelPidURI" />

  <xsl:param name="createdDate" />

  <xsl:param name="explicitBasicModel" />

  <!-- 
    Find maximal created date of RELS-EXT, if there is one.
    Note: This is safe for XSL 1.0.  XSLT 2.0 has a max() function
    that is less verbose. 
  -->
  <xsl:variable name="maxCreatedDate">
    <xsl:for-each
      select="//foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/@CREATED">
      <xsl:sort data-type="text" order="descending" />
      <xsl:if test="position()=1">
        <xsl:value-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="fedoraModel">
    <xsl:for-each
      select="//foxml:property[@NAME='http://www.w3.org/1999/02/22-rdf-syntax-ns#type']">
      <xsl:choose>
        <xsl:when test="@VALUE='FedoraBMechObject'">
          <xsl:value-of
            select="'info:fedora/fedora-system:ServiceDeployment-3.0'" />
        </xsl:when>
        <xsl:when test="@VALUE='FedoraBDefObject'">
          <xsl:value-of
            select="'info:fedora/fedora-system:ServiceDefinition-3.0'" />
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
  </xsl:variable>

  <!-- By default, copy everything unscathed unless we want do do something to it -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <!-- Get rid of all disseminators -->
  <xsl:template match="foxml:disseminator" />

  <!-- Get rid of old content model property -->
  <xsl:template
    match="foxml:property[@NAME='info:fedora/fedora-system:def/model#contentModel']" />

  <!-- Get rid of old fType property -->
  <xsl:template
    match="foxml:property[@NAME='http://www.w3.org/1999/02/22-rdf-syntax-ns#type']" />

  <!-- Add missing/required items from the object -->
  <xsl:template match="/foxml:digitalObject">
    <xsl:copy>

      <!-- Append the required VERSION attribute -->
      <xsl:attribute name="VERSION">1.1</xsl:attribute>

      <!-- Append the original PID -->
      <xsl:attribute name="PID">
        <xsl:value-of select="@PID" />
      </xsl:attribute>

      <!-- Pass element children down the chain -->
      <xsl:apply-templates select="node()"/>

      <!-- Lastly, add RELS-EXT if it doesn't exist and needs a hasModel rel -->
      <xsl:choose>

        <!-- If RELS-EXT exists, do nothing -->
        <xsl:when test="//foxml:datastream[@ID='RELS-EXT']"/>

        <!--
          If the object is a basic Fedora object, has no assigned model,
          and  bcasic model declarations are implicit, then there is no need
          to create a RELS-EXT
        -->
        <xsl:when
          test="$fedoraModel = '' and $cModelPidURI = '' and $explicitBasicModel = ''"/>

        <!-- Otherwise, add a RELS-EXT -->
        <xsl:otherwise>
          <foxml:datastream ID="RELS-EXT" STATE="A" CONTROL_GROUP="X" VERSIONABLE="true">
            <foxml:datastreamVersion>
              <xsl:attribute name="ID">RELS-EXT.0</xsl:attribute>
              <xsl:attribute name="MIMETYPE">application/rdf+xml</xsl:attribute>
              <xsl:attribute name="FORMAT_URI">info:fedora/fedora-system:FedoraRELSExt-1.0</xsl:attribute>
              <xsl:if test="$createdDate != ''">
                <xsl:attribute name="CREATED">
                <xsl:value-of select="$createdDate"/>
              </xsl:attribute>
              </xsl:if>
              <xsl:attribute name="LABEL">RDF Statements about this object</xsl:attribute>
              <foxml:xmlContent>
                <rdf:RDF
                  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                  xmlns:fedora-model="info:fedora/fedora-system:def/model#">
                  <rdf:Description>
                    <xsl:attribute
                      name="rdf:about">
                      <xsl:text>info:fedora/</xsl:text>
                      <xsl:value-of select="@PID"/>
                    </xsl:attribute>
                    <xsl:call-template name="printCModelRels"/>
                  </rdf:Description>
                </rdf:RDF>
              </foxml:xmlContent>
            </foxml:datastreamVersion>
          </foxml:datastream>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:copy>
  </xsl:template>

  <!-- Update DC for all objects -->
  <xsl:template match="//foxml:datastream[@ID='DC']/foxml:datastreamVersion">
    <xsl:copy>
      <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
      <!-- Force expected MIMETYPE and FORMAT_URI values -->
      <xsl:attribute name="MIMETYPE">text/xml</xsl:attribute>
      <xsl:attribute name="FORMAT_URI">http://www.openarchives.org/OAI/2.0/oai_dc/</xsl:attribute>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- Update RELS-EXT for all objects -->
  <xsl:template match="//foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion">
    <xsl:copy>
      <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
      <!-- Force expected MIMETYPE and FORMAT_URI values -->
      <xsl:attribute name="MIMETYPE">application/rdf+xml</xsl:attribute>
      <xsl:attribute name="FORMAT_URI">info:fedora/fedora-system:FedoraRELSExt-1.0</xsl:attribute>
      <xsl:choose>
        <xsl:when test="@CREATED = $maxCreatedDate">
          <!-- Latest version; add hasModel relationship(s) as needed -->
          <xsl:call-template name="addCModelRels"/>
        </xsl:when>
        <xsl:otherwise>
          <!-- Not latest version; copy element children unchanged --> 
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:copy>
  </xsl:template>

  <!-- Update RELS-INT for all objects -->
  <xsl:template match="//foxml:datastream[@ID='RELS-INT']/foxml:datastreamVersion">
    <xsl:copy>
      <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
      <!-- Force expected MIMETYPE and FORMAT_URI values -->
      <xsl:attribute name="MIMETYPE">application/rdf+xml</xsl:attribute>
      <xsl:attribute name="FORMAT_URI">info:fedora/fedora-system:FedoraRELSInt-1.0</xsl:attribute>
      <!-- Copy element children unchanged --> 
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- Update POLICY for all objects -->
  <xsl:template match="//foxml:datastream[@ID='POLICY']/foxml:datastreamVersion">
    <xsl:copy>
      <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
      <!-- Force expected MIMETYPE and FORMAT_URI values -->
      <xsl:attribute name="MIMETYPE">text/xml</xsl:attribute>
      <xsl:attribute name="FORMAT_URI">urn:oasis:names:tc:xacml:1.0:policy</xsl:attribute>
      <!-- Copy element children unchanged --> 
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- Update METHODMAP for SDefs and SDeps -->
  <xsl:template match="//foxml:datastream[@ID='METHODMAP']/foxml:datastreamVersion">
    <xsl:copy>
      <xsl:choose>
        <xsl:when
          test="$fedoraModel = 'info:fedora/fedora-system:ServiceDefinition-3.0'">
          <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
          <!-- Force expected MIMETYPE and FORMAT_URI values -->
          <xsl:attribute name="MIMETYPE">text/xml</xsl:attribute>
          <xsl:attribute name="FORMAT_URI">info:fedora/fedora-system:FedoraSDefMethodMap-1.0</xsl:attribute>
        </xsl:when>
        <xsl:when
          test="$fedoraModel = 'info:fedora/fedora-system:ServiceDeployment-3.0'">
          <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
          <!-- Force expected MIMETYPE and FORMAT_URI values -->
          <xsl:attribute name="MIMETYPE">text/xml</xsl:attribute>
          <xsl:attribute name="FORMAT_URI">info:fedora/fedora-system:FedoraSDepMethodMap-1.0</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <!-- Not an SDef or SDep, leave attributes unchanged --> 
          <xsl:apply-templates select="@*"/>
        </xsl:otherwise>
      </xsl:choose>
      <!-- Copy element children unchanged --> 
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- Update DSINPUTSPEC for SDeps -->
  <xsl:template match="//foxml:datastream[@ID='DSINPUTSPEC']/foxml:datastreamVersion">
    <xsl:copy>
      <xsl:choose>
        <xsl:when
          test="$fedoraModel = 'info:fedora/fedora-system:ServiceDeployment-3.0'">
          <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
          <!-- Force expected MIMETYPE and FORMAT_URI values -->
          <xsl:attribute name="MIMETYPE">text/xml</xsl:attribute>
          <xsl:attribute name="FORMAT_URI">info:fedora/fedora-system:FedoraDSInputSpec-1.0</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <!-- Not an SDep, leave attributes unchanged --> 
          <xsl:apply-templates select="@*"/>
        </xsl:otherwise>
      </xsl:choose>
      <!-- Copy element children unchanged --> 
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- Update WSDL for SDeps -->
  <xsl:template match="//foxml:datastream[@ID='WSDL']/foxml:datastreamVersion">
    <xsl:copy>
      <xsl:choose>
        <xsl:when
          test="$fedoraModel = 'info:fedora/fedora-system:ServiceDeployment-3.0'">
          <xsl:copy-of select="@*[name() != 'MIMETYPE' and name() != 'FORMAT_URI']"/>
          <!-- Force expected MIMETYPE and FORMAT_URI values -->
          <xsl:attribute name="MIMETYPE">text/xml</xsl:attribute>
          <xsl:attribute name="FORMAT_URI">http://schemas.xmlsoap.org/wsdl/</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <!-- Not an SDep, leave attributes unchanged --> 
          <xsl:apply-templates select="@*"/>
        </xsl:otherwise>
      </xsl:choose>
      <!-- Copy element children unchanged --> 
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
    
  <!-- 
    Recursively drills down into a RELS-EXT datastreamVersion, and adds a 
    relationship to the CModel to the rdf inside
  -->
  <xsl:template name="addCModelRels">
    <xsl:copy>
      <xsl:for-each select="node()">
        <xsl:choose>
          <xsl:when test="name(self::node()) = 'rdf:Description'"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <xsl:copy>
              <xsl:apply-templates select="@*"/>
              <xsl:call-template name="printCModelRels"/>
              <xsl:apply-templates select="node()"/>
            </xsl:copy>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="addCModelRels"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

  <xsl:template name="printCModelRels">
    <!-- If we're in a user-defined model, assert it -->
    <xsl:if test="$cModelPidURI != ''">
      <xsl:element name="fedora-model:hasModel"
        xmlns:fedora-model="info:fedora/fedora-system:def/model#">
        <xsl:attribute name="rdf:resource">
          <xsl:value-of select="$cModelPidURI"/>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>

    <!-- If we're in a non-basic fedora model, assert it -->
    <xsl:if test="$fedoraModel != ''">
      <xsl:element name="fedora-model:hasModel"
        xmlns:fedora-model="info:fedora/fedora-system:def/model#">
        <xsl:attribute name="rdf:resource">
          <xsl:value-of select="$fedoraModel"/>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>

    <!--  If we need to declare the basic model explicitly, do so -->
    <xsl:if test="$explicitBasicModel != ''">
      <xsl:element name="fedora-model:hasModel"
        xmlns:fedora-model="info:fedora/fedora-system:def/model#">
        <xsl:attribute name="rdf:resource">
          <xsl:value-of select="'info:fedora/fedora-system:FedoraObject-3.0'"/>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
