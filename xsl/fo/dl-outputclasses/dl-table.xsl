<?xml version='1.0' encoding='utf-8'?>
<xsl:stylesheet version="2.0"
	xmlns:xs				= "http://www.w3.org/2001/XMLSchema" 
	xmlns:xsl				= "http://www.w3.org/1999/XSL/Transform" 
	xmlns:fo				= "http://www.w3.org/1999/XSL/Format"
	xmlns:ds				= "http://www.dita-semia.org"
	exclude-result-prefixes	= "#all">
	
	
	<xsl:variable name="DL_OUTPUTCLASS_TABLE" 		as="xs:string">table</xsl:variable>
	
	
	<xsl:template match="*[contains(@class, ' topic/dl ')][tokenize(@outputclass, '\s+') = $DL_OUTPUTCLASS_TABLE]">
		<fo:block xsl:use-attribute-sets="ds:dl-table">
			
			<xsl:call-template name="commonattributes"/>
			
			<fo:table table-layout="fixed" width="100%">
				
				<fo:table-column column-number	= "1" column-width	= "proportional-column-width(35)"/>
				<fo:table-column column-number	= "2" column-width	= "proportional-column-width(65)"/>
				
				<xsl:variable name="dlhead" as="element()*" select="*[contains(@class, ' topic/dlhead ')]"/>
				<xsl:if test="exists($dlhead)">
					<fo:table-header>
						<xsl:apply-templates select="$dlhead" mode="dl-table"/>
					</fo:table-header>
				</xsl:if>
				<fo:table-body>
					<xsl:apply-templates select="node() except $dlhead" mode="dl-table"/>
				</fo:table-body>
				
			</fo:table>
			
		</fo:block>
	</xsl:template>
	
	
	<xsl:template match="*[contains(@class, ' topic/dlhead ')]" mode="dl-table">
		
		<fo:table-row xsl:use-attribute-sets="ds:dlhead-table">
			
			<xsl:call-template name="commonattributes"/>
			
			<fo:table-cell xsl:use-attribute-sets="ds:dthd-table-cell">
				<xsl:apply-templates select="*[contains(@class, ' topic/dthd ')]" mode="#current"/>
			</fo:table-cell>
			<fo:table-cell xsl:use-attribute-sets="ds:ddhd-table-cell">
				<xsl:apply-templates select="*[contains(@class, ' topic/ddhd ')]" mode="#current"/>
			</fo:table-cell>
		</fo:table-row>
		
	</xsl:template>
	
	
	<xsl:template match="*[contains(@class, ' topic/dthd ')]" mode="dl-table">
		
		<fo:block xsl:use-attribute-sets="ds:dthd-table">
			<xsl:call-template name="commonattributes"/>
			<xsl:apply-templates mode="#default"/>
		</fo:block>
		
	</xsl:template>
	
	
	<xsl:template match="*[contains(@class, ' topic/ddhd ')]" mode="dl-table">
		
		<fo:block xsl:use-attribute-sets="ds:ddhd-table">
			
			<xsl:call-template name="commonattributes"/>
			<xsl:apply-templates mode="#default"/>
		</fo:block>
		
	</xsl:template>
	
	
	<xsl:template match="*[contains(@class, ' topic/dlentry ')]" mode="dl-table">
		
		<fo:table-row xsl:use-attribute-sets="ds:dlentry-table">
			
			<xsl:call-template name="commonattributes"/>

			<fo:table-cell xsl:use-attribute-sets="ds:dt-table-cell">
				<xsl:apply-templates select="*[contains(@class, ' topic/dt ')]" mode="#current"/>
			</fo:table-cell>
			<fo:table-cell xsl:use-attribute-sets="ds:dd-table-cell">
				<xsl:apply-templates select="*[contains(@class, ' topic/dd ')]" mode="#current"/>
			</fo:table-cell>
		</fo:table-row>
		
	</xsl:template>
	
	
	<xsl:template match="*[contains(@class, ' topic/dt ')]" mode="dl-table">
		<fo:block xsl:use-attribute-sets="ds:dt-table">
			<xsl:call-template name="commonattributes"/>
			<xsl:call-template name="ds:dt-toc-id"/>
			<xsl:apply-templates mode="#default"/>
		</fo:block>
		
	</xsl:template>
	
	
	<xsl:template match="*[contains(@class, ' topic/dd ')]" mode="dl-table">
		
		<fo:block xsl:use-attribute-sets="ds:dd-table">
			<xsl:call-template name="commonattributes"/>
			<xsl:apply-templates mode="#default"/>
		</fo:block>
		
	</xsl:template>
	
		
</xsl:stylesheet>
