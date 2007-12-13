<?xml version="1.0" encoding="utf-8"?>
<!--
    Foxml XSLT transformer 1.0 -> 1.1 (No Content Model)
    
    Input: Valid foxml 1.0 file
    
    Output: Valid foxml 1.1 file, with the following changes:
        - VERSION attribute added, set to 1.1
        - No schema location
        - Old content model property removed
        - Disseminators are removed 
            - (they are expected to be defined in the related CModel object)
            
    Note: This stylesheet is a copy of foxml-upgrade-cmda, but does not
          assign a content model.
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:foxml="info:fedora/fedora-system:def/foxml#">

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

        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
