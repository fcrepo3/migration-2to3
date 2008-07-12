<?xml version="1.0" encoding="utf-8"?>
<!--
  Input      : DSINPUTSPEC, METHODMAP, or WSDL datastream content.
  
  Parameters : oldName - the part name to search for
               newName - the value to replace it with
               
  Output     : The same datastream, with key occurances of oldName replaced
               by newName.
            
  Author     : Chris Wilper
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output omit-xml-declaration="yes"/>

  <xsl:param name="oldName"/>
  <xsl:param name="newName"/>
  
  <xsl:variable name="oldNameInParens">
    <xsl:value-of select="concat('(', concat($oldName, ')'))"/>
  </xsl:variable>

  <xsl:variable name="newNameInParens">
    <xsl:value-of select="concat('(', concat($newName, ')'))"/>
  </xsl:variable>

  <!-- By default, copy everything unless we want do do something to it -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- DSINPUTSPEC changes -->
  
  <xsl:template match="fbs:DSInput[@wsdlMsgPartName=$oldName]"
      xmlns:fbs="http://fedora.comm.nsdlib.org/service/bindspec">
    <fbs:DSInput>
      <xsl:attribute name="DSMax">
        <xsl:value-of select="@DSMax"/>
      </xsl:attribute>
      <xsl:attribute name="DSMin">
        <xsl:value-of select="@DSMin"/>
      </xsl:attribute>
      <xsl:attribute name="DSOrdinality">
        <xsl:value-of select="@DSOrdinality"/>
      </xsl:attribute>
      <xsl:attribute name="wsdlMsgPartName">
        <xsl:value-of select="$newName"/>
      </xsl:attribute>
      <xsl:apply-templates select="node()"/>
    </fbs:DSInput>
  </xsl:template>

  <!-- METHODMAP changes -->
  
  <xsl:template match="fmm:DatastreamInputParm[@parmName=$oldName]"
      xmlns:fmm="http://fedora.comm.nsdlib.org/service/methodmap">
    <fmm:DatastreamInputParm>
      <xsl:attribute name="parmName">
        <xsl:value-of select="$newName"/>
      </xsl:attribute>
      <xsl:attribute name="passBy">
        <xsl:value-of select="@passBy"/>
      </xsl:attribute>
      <xsl:attribute name="required">
        <xsl:value-of select="@required"/>
      </xsl:attribute>
    </fmm:DatastreamInputParm>
  </xsl:template>

  <!-- WSDL changes -->
  
  <xsl:template match="wsdl:part[@name=$oldName]"
      xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">
    <wsdl:part>
      <xsl:attribute name="name">
        <xsl:value-of select="$newName"/>
      </xsl:attribute>
      <xsl:attribute name="type">
        <xsl:value-of select="@type"/>
      </xsl:attribute>
    </wsdl:part>
  </xsl:template>

  <xsl:template match="http:operation[contains(@location, $oldNameInParens)]"
      xmlns:http="http://schemas.xmlsoap.org/wsdl/http/">
    <http:operation>
      <xsl:attribute name="location">
        <xsl:value-of select="substring-before(@location, $oldNameInParens)"/>
        <xsl:value-of select="$newNameInParens"/>
        <xsl:value-of select="substring-after(@location, $oldNameInParens)"/>
      </xsl:attribute>
    </http:operation>
  </xsl:template>

</xsl:stylesheet>
