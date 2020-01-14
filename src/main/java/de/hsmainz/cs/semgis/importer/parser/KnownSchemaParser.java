package de.hsmainz.cs.semgis.importer.parser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;


import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.XSD;

/**
 * Converts any GML file in its corresponding namespace to RDF, with optionally
 * making assumptions about ranges and domains.
 * 
 * @author timo.homburg
 *
 */
public class KnownSchemaParser implements ContentHandler {

	public OntModel model;

	public static final String NSGEO = "http://www.opengis.net/ont/geosparql#";
	public static final String NSSF = "http://www.opengis.net/ont/sf#";
	public static final String GML = "gml";
	public static final String WKT = "asWKT";
	public static final String ASGML = "asGML";
	public static final String WKTLiteral = "wktLiteral";
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String GMLLiteral = "gmlLiteral";
	
	public static final String hasGeometry= "hasGeometry";
	
	public static final String XMLSchema="http://www.w3.org/2001/XMLSchema#";
	
	public static final String RDFSchema="http://www.w3.org/2000/01/rdf-schema#";
	
	public static final String Envelope="Envelope";
	
	public static final String Corner="Corner";

	private static final String TRUE = "true";

	private static final String FALSE = "false";

	private static final String POINT = "Point";

	private static final String HTTP = "http://";
	
	private static final String HTTP2 = "http";
	
	private static final String gmlid = "gml:id";
	
	private static final String HTTPS = "https://";
	
	private static final String HTTPS2 = "https";

	private static Set<String> featureMembers = new TreeSet<String>(
			Arrays.asList(new String[] { "featureMember", "member", "cityObjectMember" }));

	private static final String XLINKHREF = "xlink:href";

	private static final String CODESPACE = "codeSpace";

	private final Map<String, OntResource> knownMappings;

	private Individual currentIndividual;
	
	private Individual lastlinkedIndividual;

	private String currentType;

	private Map<String, String> currentRestrictions;

	private final List<String> openedTags, openedTags2;

	private Boolean featureMember, inClass, envelope;

	private final StringBuilder multipleChildrenBuffer;

	private final StringBuilder gmlStrBuilder;

	private String codeSpace = "";

	private final Stack<String> stack, stack2;

	private final Stack<Map<String, String>> restrictionStack;

	private Integer outertagCounter;

	private Integer splitterCountThreshold = 0;

	private final StringBuilder attbuilder;

	private final SimpleDateFormat parserSDF1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");


	private final SimpleDateFormat parserSDF2 = new SimpleDateFormat("yyyy-MM-dd");


	private Boolean range = false, domain = false;

	private final StringBuilder literalBuffer=new StringBuilder();

	private boolean alreadyHandled;

	private OntClass codelist;

	private Attributes attributes;
	
	private String uuid=UUID.randomUUID().toString(),stringAttribute="";

	private String indnamespace;

	public KnownSchemaParser(OntModel model, Boolean range, Boolean domain,String indnamespace) throws IOException {
		this.model = model;
		this.codelist = model.createClass("http://semgis.de/geodata#Codelist");
		this.outertagCounter = 0;
		this.envelope = false;
		this.knownMappings = new TreeMap<String, OntResource>();
		this.openedTags = new LinkedList<String>();
		this.openedTags2 = new LinkedList<String>();
		this.featureMember = false;
		this.inClass = false;
		this.multipleChildrenBuffer = new StringBuilder();
		this.gmlStrBuilder = new StringBuilder();
		this.attbuilder = new StringBuilder();
		this.currentRestrictions = new TreeMap<String, String>();
		this.stack = new Stack<String>();
		this.indnamespace=indnamespace;
		this.stack2 = new Stack<String>();
		this.range = range;
		this.domain = domain;
		this.restrictionStack = new Stack<Map<String, String>>();
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		// TODO Auto-generated method stub
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		this.attributes = attributes;
		//System.out.println(localName);
		alreadyHandled = false;
		if (featureMember) {
			System.out.println(stack);
			System.out.println(stack2);
			System.out.println(openedTags);
			System.out.println(openedTags2);
			String uriString = uri + "#" + localName;
			uriString = uriString.replace("##", "#");
			if (!knownMappings.containsKey(uriString) && openedTags.size() % 2 != 1) {
				knownMappings.put(uriString, model.createClass(uriString));
			} else if (!knownMappings.containsKey(uriString)) {
				knownMappings.put(uriString, model.createOntProperty(uriString));
			}
			// System.out.println("Add: "+uriString);
			this.openedTags.add(uriString);
			this.openedTags2.add(qName);
			// System.out.println("OpenedTags: "+openedTags);
			String value;
			if (attributes.getValue(gmlid) == null) {
				value = UUID.randomUUID().toString();
			} else {
				value = attributes.getValue(gmlid);
			}
			if (attributes.getValue(CODESPACE) != null)
				this.codeSpace = attributes.getValue(CODESPACE);
			String indid = (uri + "#" + value).replace("##", "#");
			if (knownMappings.get(uriString) != null && knownMappings.get(uriString).isClass()
					&& (!inClass || openedTags.size() > 2)) {
				this.inClass = true;
				if (openedTags.size() % 2 != 0) {
					int count=StringUtils.countMatches(indid, HTTP2);
					if(count>1) {
						indid=indid.substring(indid.lastIndexOf(HTTP2));
						if(!indnamespace.isEmpty()) {
							indid=indnamespace+indid.substring(indid.lastIndexOf('/')+1);
						}
					}
					//System.out.println(indid);
					this.currentIndividual = model.createIndividual(indid, model.createOntResource(indid));
					this.currentIndividual.setRDFType(model.createClass(uriString));
					this.currentType = uriString;
					if (uriString.contains("Envelop")) {
						this.envelope = true;
						this.multipleChildrenBuffer.delete(0, this.multipleChildrenBuffer.length());
						this.attbuilder.delete(0, this.attbuilder.length());
						attbuilder.append("<");
						attbuilder.append(qName);
						attbuilder.append(" xmlns:gml=\"").append(uri).append("\"");
						for (int i = 0; i < attributes.getLength(); i++) {
							attbuilder.append(" ").append(attributes.getLocalName(i)).append("=\"")
									.append(attributes.getValue(i) + "\"");
						}
						attbuilder.append(">");
						this.multipleChildrenBuffer.append(attbuilder.toString());
					}

					// if(stack.isEmpty())
					stack.push(this.currentIndividual.toString());
					stack2.push(qName);
					//System.out.println(stack);
					//System.out.println(stack2);
					if (attributes.getLength() > 0)
						this.currentIndividual.addLabel(value, "en");
					if (attributes.getLength() > 1) {
						for (int i = 0; i < attributes.getLength(); i++) {
							Literal liter = this.determineLiteralType(attributes.getValue(i));
							this.currentIndividual.addProperty(
									model.createDatatypeProperty(uri + "#" + attributes.getQName(i)), liter);
							if (domain)
								model.createDatatypeProperty(uri + "#" + attributes.getQName(i))
										.addDomain(this.currentIndividual.getRDFType());
							if (range)
								model.createDatatypeProperty(uri + "#" + attributes.getQName(i))
										.addRange(model.getResource(liter.getDatatypeURI()));
						}
					}
					this.restrictionStack.push(this.currentRestrictions);
				}

			} else {
				// System.out.println("Property: "+qName);
			}

			if (attributes.getValue(XLINKHREF) != null) {
				String linkString;
				if (!attributes.getValue(XLINKHREF).startsWith(HTTP) && !attributes.getValue(XLINKHREF).startsWith(HTTPS)) {
					linkString = uri + attributes.getValue(XLINKHREF);
				} else {
					linkString = attributes.getValue(XLINKHREF);
				}
				Individual propInd;
				if (this.model.getIndividual(linkString) == null) {
					int count=StringUtils.countMatches(linkString, HTTP2);
					if(count>1) {
						String indid2=linkString.substring(linkString.lastIndexOf(HTTP2));
						propInd = model.createIndividual(indid2, this.model.createOntResource(linkString));
					}else {
						propInd = model.createIndividual(linkString, this.model.createOntResource(linkString));
					}
					this.currentIndividual
							.addProperty(model.createObjectProperty(openedTags.get(openedTags.size() - 1)), propInd);
					this.lastlinkedIndividual=this.currentIndividual;
					if (domain)
						model.getObjectProperty(openedTags.get(openedTags.size() - 1))
								.addDomain(this.currentIndividual.getRDFType());
				} else {
					propInd = model.getIndividual(linkString);
					this.currentIndividual
							.addProperty(model.createObjectProperty(openedTags.get(openedTags.size() - 1)), propInd);
					this.lastlinkedIndividual=this.currentIndividual;
					if (domain)
						model.getObjectProperty(openedTags.get(openedTags.size() - 1))
								.addDomain(this.currentIndividual.getRDFType());
					if (range)
						model.getObjectProperty(openedTags.get(openedTags.size() - 1)).addRange(propInd.getRDFType());
					model.createAllValuesFromRestriction(this.currentIndividual.getRDFType().getURI(),
							model.getObjectProperty(openedTags.get(openedTags.size() - 1)), propInd.getRDFType());
				}
			}
		}
		if (featureMembers.contains(localName)) {
			this.featureMember = true;
			outertagCounter++;
			splitterCountThreshold++;
			if (splitterCountThreshold == 1000) {
				splitterCountThreshold = 0;
			}
		}
		literalBuffer.delete(0,literalBuffer.length());

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// if(outertagCounter>beginProcessing){
		String literal = new String(ch, start, length);
		literalBuffer.append(literal.trim());
		 if (featureMember && openedTags.size() > 1 && !literal.trim().isEmpty() ) {
			 if(openedTags.get(openedTags.size() - 1).contains("value")) {
					//this.stringAttributeBool=false;
					System.out.println("StringAttribute Adding: "+this.stringAttribute+" - "+literal);
					currentIndividual.addProperty(model.createDatatypeProperty(this.stringAttribute),
							this.determineLiteralType(literal));
					this.stringAttribute="";
					this.alreadyHandled=true;
				}else if (knownMappings.get(openedTags.get(openedTags.size() - 1)) != null
					&& knownMappings.get(openedTags.get(openedTags.size() - 1)).isObjectProperty()
					&& this.currentRestrictions.containsKey(openedTags.get(openedTags.size() - 1))
					&& !this.currentRestrictions.get(openedTags.get(openedTags.size() - 1))
							.contains(XMLSchema)
					&& StringUtils.isNumeric(literal)) {
				this.currentIndividual.addProperty(model.getObjectProperty(openedTags.get(openedTags.size() - 1)),
						model.getIndividual(
								this.currentRestrictions.get(openedTags.get(openedTags.size() - 1)) + "_" + literal));
				alreadyHandled = true;
			} else if (openedTags.get(openedTags.size() - 1).contains(Corner)
					&& stack2.lastElement().contains(Envelope)) {
				multipleChildrenBuffer.append("<").append(openedTags2.get(openedTags2.size() - 1)).append(">")
						.append(literal).append("</").append(openedTags2.get(openedTags2.size() - 1)).append(">");
				alreadyHandled = true;
			} else if (openedTags.get(openedTags.size() - 1).contains("posList") || openedTags.get(openedTags.size() - 1).contains("coordinates")
					|| openedTags.get(openedTags.size() - 1).contains("pos")) {
				String wktlit = this.currentIndividual.getRDFType().getLocalName() + "(" + literal + ")";
				this.gmlStrBuilder.delete(0, gmlStrBuilder.length());
				gmlStrBuilder.append("<").append(stack2.lastElement()).append(" xmlns:gml=\"")
						.append(stack.lastElement().substring(0, stack.lastElement().lastIndexOf('#'))).append("\"><")
						.append(openedTags2.get(openedTags2.size() - 1)).append(">").append(literal).append("</")
						.append(openedTags2.get(openedTags2.size() - 1)).append("></").append(stack2.lastElement())
						.append(">");
				String gmlStr = gmlStrBuilder.toString();
				// System.out.println("gmlStr: "+gmlStr);
				if(this.lastlinkedIndividual!=null) {
					this.lastlinkedIndividual.addProperty(this.model.createObjectProperty(NSGEO + hasGeometry),this.currentIndividual);
					this.lastlinkedIndividual=null;
				}
				this.currentIndividual.addProperty(this.model.createDatatypeProperty(NSGEO + ASGML),
						this.model.createTypedLiteral(gmlStr, NSGEO + GMLLiteral));
				if (!wktlit.contains(POINT))
					wktlit = formatWKTString(wktlit, ' ', 2);
				try {
					wktlit=wktlit.replace(","," ");
					//com.vividsolutions.jts.geom.Geometry geom = (com.vividsolutions.jts.geom.Geometry) wktreader
					//		.read(wktlit);
					if(this.lastlinkedIndividual!=null) {
						this.lastlinkedIndividual.addProperty(this.model.createObjectProperty(NSGEO + hasGeometry),this.currentIndividual);
						this.lastlinkedIndividual=null;
					}
					this.currentIndividual.addProperty(this.model.createDatatypeProperty(NSGEO + WKT),
							this.model.createTypedLiteral(wktlit, NSGEO + WKTLiteral));
				} catch (Exception e) {
					e.printStackTrace();
				}
				alreadyHandled = true;
			}

		}
		/*if(!this.stringAttribute.isEmpty()) {
			this.stringAttributeBool=true;
		}else {
			this.stringAttributeBool=false;
		}*/
	}

	public static String formatWKTString(String str, char c, int n) {
		// System.out.println(str);
		int pos = str.indexOf(c, 0);
		if (StringUtils.countMatches(str, " ") <= 1)
			return str;
		StringBuilder builder = new StringBuilder();
		boolean second = true;
		int lastpos = 0;
		while (pos != -1) {
			pos = str.indexOf(c, pos + 1);
			if (!second)
				second = true;
			else if (pos != -1) {
				second = false;
				builder.append(str.substring(lastpos, pos)).append(",");
				lastpos = pos;
			}
		}
		// System.out.println("Lastpos: "+lastpos+" - "+str.length());
		builder.delete(builder.length() - 1, builder.length());
		builder.append(str.substring(lastpos, str.length()));
		return builder.toString();
	}

	private Map<String, String> restrictedTypes(String classType) {
		Map<String, String> restrictedTypes = new TreeMap<String, String>();
		Queue<String> superClasses = new LinkedList<String>();
		ExtendedIterator<OntClass> supers = this.model.getOntClass(classType).listSuperClasses(true);
		while (supers.hasNext()) {
			OntClass superClass = supers.next();

			if (superClass.isRestriction() && superClass.asRestriction().isAllValuesFromRestriction()) {
				OntProperty prop = superClass.asRestriction().getOnProperty();
				restrictedTypes.put(prop.getURI(),
						((OntClass) superClass.asRestriction().asAllValuesFromRestriction().getAllValuesFrom())
								.getURI());
			} else if (superClass.isClass() && superClass.getURI() != null) {
				// System.out.println("New SuperClass: "+superClass.toString());
				superClasses.add(superClass.toString());
			}
		}
		supers.close();
		if (!superClasses.isEmpty()) {
			while (!superClasses.isEmpty()) {
				String cls = superClasses.poll();
				supers = this.model.getOntClass(cls).listSuperClasses(true);
				while (supers.hasNext()) {
					OntClass superClass = supers.next();
					if (superClass.isRestriction() && superClass.asRestriction().isAllValuesFromRestriction()) {
						OntProperty prop = superClass.asRestriction().getOnProperty();
						restrictedTypes.put(prop.getURI(),
								((OntClass) superClass.asRestriction().asAllValuesFromRestriction().getAllValuesFrom())
										.getURI());
					} else if (superClass.isClass() && superClass.getURI() != null) {
						// System.out.println("New SuperClass: "+superClass.toString());
						superClasses.add(superClass.getURI());
					}
				}
				supers.close();
			}
		}
		return restrictedTypes;
	}

	private Literal determineLiteralType(String literal) {
		try {
			if (StringUtils.isNumeric(literal) && !literal.contains(".")) {
				return this.model.createTypedLiteral(Integer.valueOf(literal), XSD.xint.getURI());
			} else if (StringUtils.isNumeric(literal) && literal.contains(".")) {
				Double d = Double.valueOf(literal);
				return this.model.createTypedLiteral(d, XSD.xdouble.getURI());
			}
		} catch (Exception e) {

		}
		// System.out.println("DETERMINE LITERAL TYPE - "+literal);
		try {
			Date date = parserSDF1.parse(literal);
			// System.out.println("DETERMINE LITERAL TYPE DATE? "+date);
			if (date != null) {
				String dateStr = parserSDF1.format(date);
				return this.model.createTypedLiteral(dateStr, XSD.dateTime.getURI());
			}
		} catch (Exception e) {

		}
		try {
			Date date = parserSDF2.parse(literal);
			// System.out.println("DETERMINE LITERAL TYPE DATE? "+date);
			if (date != null) {
				
				String dateStr = parserSDF1.format(date);
				return this.model.createTypedLiteral(dateStr, XSD.date.getURI());
			}
		} catch (Exception e) {

		}
		if (TRUE.equals(literal) || FALSE.equals(literal)) {
			return this.model.createTypedLiteral(Boolean.valueOf(literal), XSD.xboolean.getURI());
		} else {
			return this.model.createTypedLiteral(literal, XSD.xstring.getURI());
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String comb = uri + "#" + localName;
		comb = comb.replace("##", "#");
		String literal = literalBuffer.toString();
		if (!alreadyHandled && !openedTags.isEmpty()) {
			if (!literal.isEmpty()) {
				if (knownMappings.get(openedTags.get(openedTags.size() - 1)) != null
						&& knownMappings.get(openedTags.get(openedTags.size() - 1)).isAnnotationProperty()) {
					this.currentIndividual.addProperty(
							model.createAnnotationProperty(openedTags.get(openedTags.size() - 1)),
							this.model.createOntResource(literal));
					alreadyHandled=true;
				} else if (knownMappings.get(openedTags.get(openedTags.size() - 1)) != null
						&& knownMappings.get(openedTags.get(openedTags.size() - 1)).isDatatypeProperty()) {
					if (!codeSpace.isEmpty()) {							
								if(literal.contains(HTTP) || literal.contains(HTTPS)) {
									this.currentIndividual.addProperty(model.createObjectProperty(openedTags.get(openedTags.size() - 1)),codelist.createIndividual(literal));
								}else {
									this.currentIndividual.addProperty(model.createObjectProperty(openedTags.get(openedTags.size() - 1)),codelist.createIndividual(codeSpace + literal));
								}
								
					} else {
						Literal liter = this.determineLiteralType(literal);
						this.currentIndividual.addProperty(
								model.createDatatypeProperty(openedTags.get(openedTags.size() - 1)), liter);
						if (domain)
							model.getDatatypeProperty(openedTags.get(openedTags.size() - 1))
									.addDomain(this.currentIndividual.getRDFType());
						if (range)
							model.getDatatypeProperty(openedTags.get(openedTags.size() - 1))
									.addRange(model.getResource(liter.getDatatypeURI()));
						model.createAllValuesFromRestriction(this.currentIndividual.getRDFType().getURI(),
								model.getDatatypeProperty(openedTags.get(openedTags.size() - 1)),
								model.getResource(liter.getDatatypeURI()));
					}
					alreadyHandled=true;
				} else if (knownMappings.get(openedTags.get(openedTags.size() - 1)) != null
						&& knownMappings.get(openedTags.get(openedTags.size() - 1)).isProperty()) {
					if (!codeSpace.isEmpty()) {
						if(literal.contains(HTTP) || literal.contains(HTTPS)) {
							this.currentIndividual.addProperty(model.createObjectProperty(openedTags.get(openedTags.size() - 1)),codelist.createIndividual(literal));
						}else {
							this.currentIndividual.addProperty(model.createObjectProperty(openedTags.get(openedTags.size() - 1)),codelist.createIndividual(codeSpace + literal));
						}
					} else {
						this.currentIndividual.addProperty(
								model.createDatatypeProperty(openedTags.get(openedTags.size() - 1)),
								this.determineLiteralType(literal));
					}
					alreadyHandled=true;
				}
			}
		}
		if(openedTags.size() % 2 == 0 && !alreadyHandled && !stack.isEmpty() && !openedTags.isEmpty() 
				 && literal.isEmpty() && attributes.getLength()>1) {
				OntClass cls=model.createClass(uuid);
				Individual ind=cls.createIndividual(UUID.randomUUID().toString());
				//System.out.println((openedTags.get(openedTags.size() - 1)));
				//System.out.println(openedTags.get(openedTags.size() - 1)+" Literal is empty "+attributes.getLength());
				int i=0;
				while(i<attributes.getLength()) {
					//System.out.println(openedTags.get(openedTags.size() - 1)+" - "
				//+attributes.getLocalName(i)+" - "+attributes.getValue(i));
					ind.addProperty(model.createDatatypeProperty(attributes.getLocalName(i)),
							determineLiteralType(attributes.getValue(i)));
					i++;
				}
				this.currentIndividual.addProperty(
						model.createObjectProperty(openedTags.get(openedTags.size() - 1)),
						ind);
				this.lastlinkedIndividual=this.currentIndividual;
		}
		// System.out.println("Remove: "+uri+localName+ this.openedTags.contains(comb)+"
		// - "+this.openedTags.size());
		this.openedTags.remove(comb);
		// System.out.println("After removal: "+this.openedTags.size());
		this.openedTags2.remove(qName);

		// if(outertagCounter>beginProcessing){
		// System.out.println("OpenedTags: "+openedTags);
		if (openedTags.size() % 2 == 0 && !stack.isEmpty())

		{
			if (localName.contains("Envelop")) {
				this.envelope = false;
				this.multipleChildrenBuffer.append("</").append(qName).append(">");
				if(this.lastlinkedIndividual!=null)
					this.lastlinkedIndividual.addProperty(this.model.createObjectProperty(NSGEO + hasGeometry),this.currentIndividual);
				this.currentIndividual.addProperty(this.model.createDatatypeProperty(NSGEO + ASGML),
						this.model.createTypedLiteral(multipleChildrenBuffer.toString(), GMLLiteral));
			}
			String lastElement = stack.pop();
			//System.out.println(lastElement);
			//System.out.println(stack.toString());
			//System.out.println(stack2.toString());
			stack2.pop();
			restrictionStack.pop();
			if (!stack.isEmpty()) {
				int count=StringUtils.countMatches(stack.lastElement(), HTTP2);
				if(count>1) {
					String indid=stack.lastElement().substring(stack.lastElement().lastIndexOf(HTTP2));
					this.currentIndividual = this.model.getIndividual(stack.lastElement());
				}else {
					this.currentIndividual = this.model.getIndividual(stack.lastElement());
				}			
				// System.out.println("endElement addProperty: "+lastElement);
				this.currentIndividual.addProperty(
						this.model.createObjectProperty(this.openedTags.get(this.openedTags.size() - 1)),
						this.model.getIndividual(lastElement));
				if (domain)
					model.getObjectProperty(this.openedTags.get(this.openedTags.size() - 1))
							.addDomain(this.currentIndividual.getRDFType());
			}
			if (!restrictionStack.isEmpty())
				this.currentRestrictions = restrictionStack.lastElement();
			alreadyHandled=true;
		}
	
		if (featureMembers.contains(localName)) {
			System.out.println("FINISHED=============================================");
			System.out.println(lastlinkedIndividual);
			System.out.println(literalBuffer);
			this.featureMember = false;
		}
		/*if(localName.contains("stringAttribute")) {
			stringAttributeBool=false;
		}*/
		if ((uri + "#" + localName).equals(currentType)) {
			this.inClass = false;
		}
		this.codeSpace = "";
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {

	}

	public static void restructureDomains(OntModel model) {
		ExtendedIterator<ObjectProperty> objprops = model.listObjectProperties();
		while (objprops.hasNext()) {
			ObjectProperty prop=objprops.next();
			ExtendedIterator<? extends OntResource> domains = prop.listDomain();
			List<RDFNode> elements = new LinkedList<RDFNode>();
			List<RDFNode> elements2 = new LinkedList<RDFNode>();
			Boolean first = true, second = true;
			if (domains.hasNext())
				while (domains.hasNext()) {
					if (second && !first) {
						second = false;
					}
					if (first) {
						first = false;
					}
					OntResource curdom = domains.next();
					if (!curdom.getURI().contains(RDFSchema)
							&& !curdom.getURI().contains(OWL.NS))
						elements.add(curdom);
				}
			if (!second)
				prop.setDomain(
						model.createUnionClass(null, model.createList(elements.toArray(new RDFNode[elements.size()]))));
			domains.close();
			first = true;
			second = true;
			ExtendedIterator<? extends OntResource> ranges = prop.listRange();
			while (ranges.hasNext()) {
				if (second && !first) {
					second = false;
				}
				if (first) {
					first = false;
				}
				OntResource curran = ranges.next();
				if (!curran.getURI().contains(RDFSchema)
						&& !curran.getURI().contains(OWL.NS))
					elements2.add(curran);
			}
			if (!second)
				prop.setRange(model.createUnionClass(null,
						model.createList(elements2.toArray(new RDFNode[elements2.size()]))));
			ranges.close();
		}
		ExtendedIterator<DatatypeProperty> dataprops = model.listDatatypeProperties();
		while (dataprops.hasNext()) {
			DatatypeProperty prop2 = dataprops.next();
			ExtendedIterator<? extends OntResource> domains2 = prop2.listDomain();
			List<RDFNode> doms = new LinkedList<RDFNode>();
			List<RDFNode> rgs = new LinkedList<RDFNode>();
			Boolean first = true, second = true;
			while (domains2.hasNext()) {
				if (second && !first) {
					second = false;
				}
				if (first) {
					first = false;
				}
				OntResource curdom = domains2.next();
				if (curdom.getURI() != null && !curdom.getURI().contains(RDFSchema)
						&& !curdom.getURI().contains(OWL.NS))
					doms.add(curdom);
				// domains.add(domains2.next());
			}
			if (!second)
				prop2.setDomain(model.createUnionClass(null, model.createList(doms.toArray(new RDFNode[doms.size()]))));
			domains2.close();
			first = true;
			second = true;
			ExtendedIterator<? extends OntResource> ranges2 = prop2.listRange();
			while (ranges2.hasNext()) {
				if (second && !first) {
					second = false;
				}
				if (first) {
					first = false;
				}
				OntResource curran = ranges2.next();
				if (!curran.getURI().contains(RDFSchema)
						&& !curran.getURI().contains(OWL.NS))
					rgs.add(curran);
			}
			if (!second)
				prop2.setRange(model.createUnionClass(null, model.createList(rgs.toArray(new RDFNode[rgs.size()]))));
			ranges2.close();
		}
		dataprops.close();
	}


	@Override
	public void setDocumentLocator(Locator locator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// TODO Auto-generated method stub

	}

}