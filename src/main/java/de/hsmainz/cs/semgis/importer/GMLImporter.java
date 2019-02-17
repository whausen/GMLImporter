package de.hsmainz.cs.semgis.importer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceRequiredException;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.hsmainz.cs.semgis.importer.parser.ConfigParser;
import de.hsmainz.cs.semgis.importer.parser.KnownSchemaParser;;

public class GMLImporter {

	public static Map<String, List<String>> formatToOntology;

	public static void readConfig() throws IOException {
		ConfigParser parser = new ConfigParser("config2.csv");
		formatToOntology = parser.maps;

	}

	public static OntModel enrichClasses(OntModel model, OntModel model2, Boolean enrich) {
		OntModel result = ModelFactory.createOntologyModel();
		if (enrich) {
			model.add(model2);
		}

		/*ExtendedIterator<OntClass> it = model.listClasses();
		while (it.hasNext()) {
			OntClass cls = it.next();
			if (model2.containsResource(cls.asResource())) {
				System.out.println("Contained: " + cls);
				if (cls.getURI() != null) {
					OntClass cls2 = model2.getOntClass(cls.getURI());
					System.out.println("Cls2: " + cls2.getURI());
					if (cls2 != null) {
						ExtendedIterator<OntClass> it2 = cls2.listEquivalentClasses();
						while (it2.hasNext()) {
							OntClass toadd = it2.next();
							if (toadd.isUnionClass()) {
								ExtendedIterator<? extends OntResource> abs = toadd.asUnionClass().listInstances();
								StmtIterator abss = toadd.listProperties();
								while (abs.hasNext()) {
									Statement prop = (Statement) abs.next();
									System.out.println("Props: " + prop);
									OntClass intsect = (OntClass) prop.asTriple().getObject();
									System.out.println("IntsectClass: " + intsect);
								}
								while (abss.hasNext()) {
									Statement prop = abss.next();
									// result.add(prop);
									System.out.println("Props2: " + prop);
									result.add(prop);
								}
								OntClass rescls = result.createClass(cls.getURI());
								rescls.addEquivalentClass(toadd);

								abs.close();
								// cls.addEquivalentClass(toadd);
								System.out.println("ToAdd: " + toadd);
							} else {
								if (cls.isRestriction()) {
									System.out.println("Cls IsRestriction");
								}
								if (toadd.isRestriction()) {
									System.out.println("IsRestriction");
								}
								StmtIterator abs = toadd.listProperties();
								
								while (abs.hasNext()) {
									Statement prop = abs.next();
									System.out.println("Props: " + prop);
									try {
									RDFList lst=(RDFList) prop.getObject().as(RDFList.class);
									System.out.println("list: "+lst);
									for(RDFNode abc:lst.asJavaList()) {
										System.out.println("ListElemen: "+abc);
										StmtIterator abcd = abc.asResource().listProperties();
										while(abcd.hasNext()) {
											Statement abcdelem = abcd.next();
											System.out.println("Prop1: "+abcdelem);											
											StmtIterator abcde = abcdelem.getObject().asResource().listProperties();
											while(abcde.hasNext()) {
												Statement prop3elem = abcd.next();
												System.out.println("Prop2: "+prop3elem);
											}
											result.add(abcde);
										}
										result.add(abcd);
									}
									}catch(JenaException e) {
										
									}
									result.add(prop.getObject().asResource().listProperties());
									/*try {
										StmtIterator unit = prop.getObject().asResource().listProperties();
										while (unit.hasNext()) {
											Statement un = unit.next();
											System.out.println("Un: " + un);
											StmtIterator unit2 = un.getObject().asResource().listProperties();
											while (unit2.hasNext()) {
												Statement un2 = unit2.next();
												System.out.println("Un2: " + un2);
												StmtIterator unit3 = un2.getObject().asResource().listProperties();
												while (unit3.hasNext()) {
													Statement un3 = unit3.next();
													System.out.println("Un3: " + un3);
													try {
														StmtIterator unit4 = un3.getObject().asResource()
																.listProperties();
														while (unit4.hasNext()) {
															Statement un4 = unit4.next();
															System.out.println("Un4: " + un4);
															
														}
														result.add(unit4);
													} catch (ResourceRequiredException e) {
														e.printStackTrace();
													}
												}
												result.add(unit3);
											}
											result.add(unit2);
										}
										result.add(unit);
										System.out.println(prop.getObject().asResource().listProperties());
										// OntResource
										// intsect=model.getOntResource(prop.asTriple().getObject().getURI());
										// System.out.println("IntsectClass: "+intsect);
									} catch (ClassCastException e) {

									}
									result.add(prop);
								}
								OntClass rescls = result.createClass(cls.getURI());
								rescls.addEquivalentClass(toadd);

								abs.close();
								// cls.addEquivalentClass(toadd);
								System.out.println("ToAdd: " + toadd);
							}
							// try {
							// System.out.println("Profile: "+toadd.getOntModel());
							// System.out.println("UnionOf: "+toadd.getProfile().UNION_OF());
							// result.add(toadd.asUnionClass().getOntModel());
							// }catch(ConversionException e) {
							// cls2.addEquivalentClass(toadd);
							// }

						}

					}
				}

			}
		}
		model.add(result);*/
		return result;
	}

	public static void main(String[] args) throws IOException {
		readConfig();
		if (args.length < 2) {
			System.out.println("Too less arguments, exiting program");
			System.out.println("SYNTAX: programname inputfile outputfile " + formatToOntology.keySet() + "");
		} else {
			String filepath = args[0];
			String outpath = args[1];
			String fileformat = "";
			if (args.length > 2)
				fileformat = args[2];
			boolean enrich = false;
			if (args.length > 3) {
				if (args[3].toUpperCase().equals("ENRICH")) {
					enrich = true;
				}
			}
			System.out.println(filepath + " - " + outpath + " - " + fileformat);
			File file = new File(filepath);
			File outfile = new File(outpath);
			if (file.exists() && !file.isDirectory()) {
				OntModel model = ModelFactory.createOntologyModel();
				try {
					KnownSchemaParser parser;
					if (fileformat.isEmpty()) {
						parser = new KnownSchemaParser(model, true, true);
					} else {
						parser = new KnownSchemaParser(model, false, false);
					}
					XMLReader reader = XMLReaderFactory.createXMLReader();
					reader.setContentHandler(parser);
					reader.parse(new InputSource(new FileReader(file)));
					if (formatToOntology.containsKey(fileformat.toUpperCase())) {
						for (String s : formatToOntology.get(fileformat)) {
							OntModel modell = ModelFactory.createOntologyModel();
							modell.read("ontologies/" + s);
							enrichClasses(model, modell, enrich);
						}
					} else {
						KnownSchemaParser.restructureDomains(model);
					}
					parser.model.write(new FileWriter(new File(outpath)), "TTL");
					System.out.println("Finished the conversion");
				} catch (IOException | SAXException e) {
					System.out.println("There was an error reading the file: " + e.getMessage());
				}

			} else {
				System.out.println("Input file " + file.getPath() + " does not exist!");
			}
		}

	}

}