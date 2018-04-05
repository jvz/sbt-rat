<?xml version='1.0'?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method='text'/>
<xsl:template match='/'>
== Release Audit Report

Generated at: <xsl:value-of select='rat-report/@timestamp'/>

=== Summary

* Notes: <xsl:value-of select='count(descendant::type[attribute::name="notice"])'/>
* Binaries: <xsl:value-of select='count(descendant::type[attribute::name="binary"])'/>
* Archives: <xsl:value-of select='count(descendant::type[attribute::name="archive"])'/>
* Standards: <xsl:value-of select='count(descendant::type[attribute::name="standard"])'/>
* Apache Licensed: <xsl:value-of select='count(descendant::header-type[attribute::name="AL   "])'/>
* Generated Documents: <xsl:value-of select='count(descendant::header-type[attribute::name="GEN  "])'/>
    - _JavaDocs are generated, thus a license header is optional. Generated files do not require license headers._
* Unknown Licenses: <xsl:value-of select='count(descendant::header-type[attribute::name="?????"])'/>

<xsl:if test="descendant::resource[license-approval/@name='false']">
=== Files with unapproved licenses

<xsl:for-each select='descendant::resource[license-approval/@name="false"]'>
    <xsl:text>* </xsl:text><xsl:value-of select='@name'/><xsl:text>
</xsl:text>
</xsl:for-each>
</xsl:if>
<xsl:if test="descendant::resource[type/@name='archive']">
=== Archives

<xsl:for-each select='descendant::resource[type/@name="archive"]'>
    <xsl:text>* </xsl:text><xsl:value-of select='@name'/><xsl:text>
</xsl:text>
</xsl:for-each>
</xsl:if>

=== Files

Files with Apache License headers will be marked `AL`.
Binary files (which do not require any license headers) will be marked `B`.
Compressed archives will be marked `A`.
Notices, licenses etc. will be marked `N`.

<!-- &#9989; is a green checkmark and &#10060; is a red cross -->
|===
|OK? |Type |File

<xsl:for-each select='descendant::resource'>
    <xsl:text>|</xsl:text>
    <xsl:choose>
        <xsl:when test='license-approval/@name="false"'>&#10060;</xsl:when>
        <xsl:otherwise>&#9989;</xsl:otherwise>
    </xsl:choose>
    <xsl:text>
|</xsl:text>
    <xsl:choose>
        <xsl:when test='type/@name="notice"'>`N`</xsl:when>
        <xsl:when test='type/@name="archive"'>`A`</xsl:when>
        <xsl:when test='type/@name="binary"'>`B`</xsl:when>
        <xsl:when test='type/@name="standard"'>
            <xsl:value-of select='header-type/@name'/>
        </xsl:when>
        <xsl:otherwise>`!!!!!`</xsl:otherwise>
    </xsl:choose>
    <xsl:text>
|</xsl:text>
    <xsl:value-of select='@name'/>
    <xsl:text>

</xsl:text>
</xsl:for-each>
|===
<xsl:if test="descendant::resource[header-type/@name='?????']">

=== Invalid License Headers

<xsl:for-each select='descendant::resource[header-type/@name="?????"]'>
.<xsl:value-of select='@name'/>
----
<xsl:value-of select='header-sample'/>
----
</xsl:for-each>
</xsl:if>
</xsl:template>
</xsl:stylesheet>
