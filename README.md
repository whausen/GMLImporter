# GMLImporter

[![Build Status](https://travis-ci.com/i3mainz/GMLImporter.svg?branch=master)](https://travis-ci.com/i3mainz/GMLImporter)

A Java program converting GML to TTL to integrate it with a local ontology.

This is the java implementation of this approach. If you are looking for the JavaScript implementation please go to:
https://github.com/i3mainz/semgistestbench/blob/master/xplanungmapview.html


The GMLImporter converts a GML file to a TTL file using the following approach:

## Converting namespaces

Every GML should contain namespaces in its root element as can be seen below.
```xml
<?xml version="1.0" encoding="utf-8" standalone="yes"?>
<!--Erzeugt mit KIT (www.kit.edu) GML-Toolbox, Erstellungsdatum: 04/24/17-->
<xplan:XPlanAuszug xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://www.xplanung.de/xplangml/5/0 ../../Version%205.0/Schema/XPlanung-Operationen.xsd"
 xmlns:xplan="http://www.xplanung.de/xplangml/5/0" xmlns:wfs="http://www.opengis.net/wfs"
 xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema"
 xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:adv="http://www.adv-online.de/nas"
 gml:id="GML_3aebe595-a72f-43b9-8dd3-c3c757edd558">
```
Namespaces can be converted to prefixes in ttl as follows:
```ttl
@prefix xplan: <http://www.xplanung.de/xplangml/5/0> .
...
```
## Data Content Location
Data Content of a GML file is presented depending on the GML dialect in a subelement of the XML DOM tree (commonly the second level but it varies)
In this description we call this element the DC-Start Element.
However, the beginning of DC-Start Element can usually be narrowed down to a set of specific tags like:
```xml
 <gml:featureMember>
 <wfs:member>
 ```
 We do not convert those tags into RDF, as they are usually irrelevant and can be replaced by classes of the [GeoSPARQL](http://www.opengeospatial.org/standards/geosparql) vocabulary.
 ```ttl
geo:SpatialObject rdf:type owl:Class .
geo:Feature rdf:type owl:Class .
geo:Geometry rdf:type owl:Class .
geo:Feature rdfs:subClassOf geo:SpatialObject . 
geo:Geometry rdfs:subClassOf geo:SpatialObject .
 ```
## Conversion Rules
 We show our conversion rules on the following simple example which is a feature that can be found as a child of the DC-Start Element:
 ```xml
 <xplan:BP_AnpflanzungBindungErhaltung gml:id="GML_02979472-c921-4b72-8a47-135eb32eede3">
      <gml:boundedBy>
        <gml:Envelope srsName="EPSG:31467">
          <gml:lowerCorner>3480102.593 5889803.7</gml:lowerCorner>
          <gml:upperCorner>3480102.593 5889803.7</gml:upperCorner>
        </gml:Envelope>
      </gml:boundedBy>
      <xplan:ebene>0</xplan:ebene>
      <xplan:gehoertZuBereich xlink:href="#GML_7af470e9-0167-43ae-823d-56e4241eab9d" />
      <xplan:rechtscharakter>9998</xplan:rechtscharakter>
      <xplan:position>
        <gml:Point srsName="EPSG:31467" gml:id="GML_cd5cba8f-f9ca-4255-888a-7417995b4e9d">
          <gml:pos>3480102.593 5889803.7</gml:pos>
        </gml:Point>
      </xplan:position>
      <xplan:flaechenschluss>false</xplan:flaechenschluss>
      <xplan:massnahme>1000</xplan:massnahme>
      <xplan:gegenstand>1000</xplan:gegenstand>
      <xplan:kronendurchmesser uom="m">1</xplan:kronendurchmesser>
      <xplan:istAusgleich>false</xplan:istAusgleich>
    </xplan:BP_AnpflanzungBindungErhaltung>
 ```
### **Creating owl:Class** 
We call any child of DC-Start an individual element. Any node name of an individual element is converted to an owl:Class 
```ttl
xplan:BP_AnpflanzungBindungErhaltung rdf:type owl:Class. 
```
### **Creating Individuals** 
Any individual element is converted to an Individual and given the type of the element node name. If no GML id is provided, a UUID will be generated as the Individual name.
```ttl
xplan:GML_02979472-c921-4b72-8a47-135eb32eede3 rdf:type xplan:BP_AnpflanzungBindungErhaltung . 
```
Children of the individual element are considered as properties of the respective individual. 
We can distinguish owl:DatatypeProperties and owl:ObjectProperties depending on if a DOM subtree exists under the respective element or if the element contains an xlink property.
### **Datatype Properties from DOM leaf elements**
```xml
<xplan:ebene>0</xplan:ebene>
```
Is converted to a owl:DatatypeProperty, and a Literaltype is detected according to the content. (xsd:integer,xsd:double,xsd:boolean,xsd:string,xsd:date)
Optionally the data property can be enriched with a detected domain or range. If a local ontology is already present, this is not needed.
```ttl
xplan:ebene rdf:type owl:DatatypeProperty .
xplan:ebene rdfs:domain xplan:BP_AnpflanzungBindungErhaltung .
xplan:ebene rdfs:range xsd:integer .
xplan:GML_02979472-c921-4b72-8a47-135eb32eede3 xplan:ebene "0"^^xsd:integer .
``` 
### **ObjectProperty from xlink element**
```xml
<xplan:gehoertZuBereich xlink:href="#GML_7af470e9-0167-43ae-823d-56e4241eab9d" />
```
An xlink element points to a different Individual and is therefore converted to:
```ttl
xplan:gehoertZuBereich rdf:type owl:ObjectProperty .
xplan:gehoertZuBereich rdfs:domain xplan:BP_AnpflanzungBindungErhaltung .
xplan:GML_02979472-c921-4b72-8a47-135eb32eede3 xplan:gehoertZuBereich xplan:GML_7af470e9-0167-43ae-823d-56e4241eab9d . 
xplan:GML_7af470e9-0167-43ae-823d-56e4241eab9d rdf:type owl:NamedInvididual . 
```
In case the linked Individual is also defined in the same file, it will be imported as well, in case it is not included it is at least present as an owl:NamedIndividual type.
### **ObjectProperty from DOM subtree**
```xml
<gml:boundedBy>
    <gml:Envelope srsName="EPSG:31467">
      <gml:lowerCorner>3480102.593 5889803.7</gml:lowerCorner>
      <gml:upperCorner>3480102.593 5889803.7</gml:upperCorner>
    </gml:Envelope>
</gml:boundedBy>
```
In the case shown above an owl:ObjectProperty and a new Invididual including a UUID as name is created. 
The class is extracted from the next child element in the DOM tree.
```ttl
xplan:GML_02979472-c921-4b72-8a47-135eb32eede3 gml:boundedBy xplan:718a9ac3-a2fc-4aeb-b0c4-03e34ab9c837 .
xplan:718a9ac3-a2fc-4aeb-b0c4-03e34ab9c837 rdf:type gml:Envelope .
```
### **Representing geometries**:
```xml
      <xplan:position>
        <gml:Point srsName="EPSG:31467" gml:id="GML_cd5cba8f-f9ca-4255-888a-7417995b4e9d">
          <gml:pos>3480102.593 5889803.7</gml:pos>
        </gml:Point>
      </xplan:position>
```
Geometries are commonly represented in a DOM subtree in the GML namespace of what we would identify as an owl:ObjectProperty during the course of the algorithm.
To represent geometries in RDF, we choose the GeoSPARQL vocabulary and would represent the above geometry as follows:
```ttl
xplan:position rdf:type owl:ObjectProperty .
xplan:position rdfs:range geo:gmlLiteral .
xplan:position rdfs:domain xplan:BP_AnpflanzungBindungErhaltung .
xplan:GML_02979472-c921-4b72-8a47-135eb32eede3 xplan:position xplan:GML_cd5cba8f-f9ca-4255-888a-7417995b4e9d .
xplan:GML_cd5cba8f-f9ca-4255-888a-7417995b4e9d rdf:type gml:Point .
xplan:GML_cd5cba8f-f9ca-4255-888a-7417995b4e9d geo:asGML "<gml:Point srsName="EPSG:31467" gml:id="GML_cd5cba8f-f9ca-4255-888a-7417995b4e9d">
          <gml:pos>3480102.593 5889803.7</gml:pos>
        </gml:Point>"^^geo:gmlLiteral .
```
Ideally, the property would need to be renamed to geo:hasGeometry in order to conform to the GeoSPARQL standard, yet we have not considered renaming the property as we would like to export correct GML files from TTL in the future.
#### Adding other dataformats:
Currently we use the [Java Topology Suite (JTS)](https://projects.eclipse.org/projects/locationtech.jts) to create WKT representations of the parsed GML and adding them to the ttl representation.
```ttl
xplan:GML_cd5cba8f-f9ca-4255-888a-7417995b4e9d geo:asWKT "POINT(3480102.593 5889803.7)"^^geo:wktLiteral .
```
In our JavaScript implementation we add unofficial geo:asGeoJSON and geo:asKML representations as well.
### **Representing properties of DataProperty elements**:
```xml
<xplan:kronendurchmesser uom="m">1</xplan:kronendurchmesser>
```
Currently there is no support for importing properties as shown above, but it could be a TODO to implement this in the following way:
```ttl
xplan:GML_02979472-c921-4b72-8a47-135eb32eede3 xplan:kronendurchmesser xplan:1d51e242-2d2e-4eb9-8920-f830d94b2dc7 .
xplan:1d51e242-2d2e-4eb9-8920-f830d94b2dc7 rdf:type xplan:Kronendurchmesser .
xplan:1d51e242-2d2e-4eb9-8920-f830d94b2dc7 xplan:uom "m"^^xsd:string .
xplan:1d51e242-2d2e-4eb9-8920-f830d94b2dc7 xplan:value "1"^^xsd:integer .
```
Here, we would create an ObjectProperty including a value property and possible other properties describing the respective element better.
