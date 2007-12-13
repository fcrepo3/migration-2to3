<?xml version="1.0" encoding="utf-8"?>
<!--
    Foxml XSLT transformer 1.0 -> 1.1
    
    Input: Valid foxml 1.0 file
    
    Parameters:
        CModelPidURI (required) - PID URI of the CModel object that defines
        the content model of the object being transformed.  For testing/evaluation
        purposes, if no value is supplied, a dummy value will be used,
        
    Output: Valid foxml 1.1 file, with the following changes:
        - VERSION attribute added, set to 1.1
        - No schema location
        - Old content model property removed
        - Disseminators are removed 
            - (they are expected to be defined in the related CModel object)
        - CModel relationship added to latest RELS-EXT version
            - If no RELS-EXT, one is added
            
    11/30/07 Aaron Birkland (birkland@cs.cornell.edu)
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:foxml="info:fedora/fedora-system:def/foxml#">

    <!-- 
        The PID of the CModel object is expected as a parameter to this 
        transform.  If it is not given, then just use a dummy PID.
    -->
    <xsl:param name="CModelPidURI" select="'info:fedora/changme:CONTENT_MODEL_PID'"/>

    <xsl:param name="CreatedDate" select="'2007-12-07T00:00:00Z'"/>

    <!-- 
        Find maximal created date of RELS-EXT, if there is one.
        Note: This is safe for XSL 1.0.  XSLT 2.0 has a max() function
        that is less verbose. 
    -->
    <xsl:variable name="maxCreatedDate">
        <xsl:for-each select="//foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/@CREATED">
            <xsl:sort data-type="text" order="descending"/>
            <xsl:if test="position()=1">
                <xsl:value-of select="."/>
            </xsl:if>
        </xsl:for-each>
    </xsl:variable>

    <!-- By default, copy everything unscathed unless we want do do something to it -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Get rid of all disseminators -->
    <xsl:template match="foxml:disseminator"/>
    
    <!-- Get rid of old content model property -->
    <xsl:template match="foxml:property[@NAME='info:fedora/fedora-system:def/model#contentModel']"/>

    <!-- Add missing/required items from the object -->
    <xsl:template match="/foxml:digitalObject">
        <xsl:copy>

            <!-- Append the required VERSION attribute -->
            <xsl:attribute name="VERSION">1.1</xsl:attribute>
           
            <!-- Append the original PID --> 
            <xsl:attribute name="PID">
              <xsl:value-of select="@PID"/>
            </xsl:attribute>
            
            <!-- Pass element children down the chain -->
            <xsl:apply-templates select="node()"/>

            <!-- Lastly,  RELS-EXT if it doesn't exist -->
            <xsl:choose>

                <!-- If RELS-EXT exists, do nothing -->
                <xsl:when test="//foxml:datastream[@ID='RELS-EXT']"/>

                <!-- Otherwise, add a RELS-EXT -->
                <xsl:otherwise>
                    <foxml:datastream ID="RELS-EXT" STATE="A" CONTROL_GROUP="X" VERSIONABLE="true">
                        <foxml:datastreamVersion ID="RELS-EXT.0" MIMETYPE="text/xml">
                            <xsl:attribute name="ID">RELS-EXT.0</xsl:attribute>
                            <xsl:attribute name="MIMETYPE">text/xml</xsl:attribute>
                            <xsl:attribute name="CREATED">
                                <xsl:value-of select="$CreatedDate"/>
                            </xsl:attribute>
                            <foxml:contentDigest TYPE="DISABLED" DIGEST="none"/>
                            <foxml:xmlContent>
                                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                    xmlns:fedora-model="info:fedora/fedora-system:def/model#">
                                    <rdf:Description>
                                        <xsl:attribute name="rdf:about">
                                            <xsl:value-of select="@PID"/>
                                        </xsl:attribute>
                                        <xsl:element name="fedora-model:hasFormalContentModel"
                                            namespace="info:fedora/fedora-system:def/model#">
                                            <xsl:attribute name="rdf:resource">
                                                <xsl:value-of select="$CModelPidURI"/>
                                            </xsl:attribute>
                                        </xsl:element>
                                    </rdf:Description>
                                </rdf:RDF>
                            </foxml:xmlContent>
                        </foxml:datastreamVersion>
                    </foxml:datastream>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:copy>
    </xsl:template>

    <!-- Add the CMDA contract relationship to current RELS-EXT, if it exists -->
    <xsl:template
        match="/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion">
        <xsl:choose>

            <!-- Only add therelationship to the latest RELS-EXT -->
            <xsl:when test="@CREATED = $maxCreatedDate">
                <xsl:call-template name="addCmodelRel"/>
            </xsl:when>

            <!-- Otherwise, for older versions, pass through -->
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- 
         Recursively drills down into a RELS-EXT datastreamVersion, and adds a 
         relationship to the CModel to the rdf inside
    -->
    <xsl:template name="addCmodelRel">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:for-each select="node()">
                <xsl:choose>
                    <xsl:when test="name(self::node()) = 'rdf:Description'"
                        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                        <xsl:copy>
                            <xsl:apply-templates select="@*"/>
                            <xsl:element name="fedora-model:hasContentModel"
                                xmlns:fedora-model="info:fedora/fedora-system:def/model#">
                                <xsl:attribute name="rdf:resource">
                                    <xsl:value-of select="$CModelPidURI"/>
                                </xsl:attribute>
                            </xsl:element>
                            <xsl:apply-templates select="node()"/>
                        </xsl:copy>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="addCmodelRel"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
