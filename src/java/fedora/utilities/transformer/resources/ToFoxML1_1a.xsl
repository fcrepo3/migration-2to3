<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:foxml="info:fedora/fedora-system:def/foxml#" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<xsl:output method="xml" encoding="UTF-8" indent="yes"/>
	<xsl:template match="*|@*">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
		    <xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="//*[local-name()='digitalObject']">
		<xsl:copy>
			<xsl:attribute name="PID">
			<xsl:value-of select="@PID"/>
			</xsl:attribute>
			<xsl:attribute name="VERSION">
			<xsl:text>1.1</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="xsi:schemaLocation">
			<xsl:text>info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd</xsl:text>
			</xsl:attribute>
	   		<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="//*[local-name()='property']">
		<xsl:for-each select=".">
			<xsl:if test="@NAME !='info:fedora/fedora-system:def/model#contentModel'">
				<xsl:copy-of select="."/>
    		</xsl:if>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>