package de.hsmainz.cs.semgis.webservice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.hsmainz.cs.semgis.importer.GMLImporter;

@Path("/service")
public class GMLConverterService {
	
		@POST
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces({"text/ttl"})
		@Path("/convert")
	    public String queryService(@FormDataParam("file") InputStream uploadedInputStream,
				@FormDataParam("file") FormDataContentDisposition fileDetail,
				@QueryParam("format") String format) { 
			final String dir = System.getProperty("user.dir");
	        System.out.println("current dir = " + dir); 
	        try {
				String theString = IOUtils.toString(uploadedInputStream, "UTF-8");
				OntModel model=GMLImporter.processFile(fileDetail.getType(), theString, true, false, "");
				StringWriter writer=new StringWriter();
				model.write(writer, "TTL");
				System.out.println("Finished the conversion");
				return writer.toString();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	        return "Conversion failed";		
		}
		
		@GET
		@Produces(MediaType.TEXT_PLAIN)
		@Consumes(MediaType.TEXT_PLAIN)
		@Path("/getclasshierarchy")
	    public static String getClassHierarchyTree(@QueryParam("user") String user,@QueryParam("graph") String graph){
	    	Model model=null;//BrokerOntologyConnection.getInstance(user,graph);
	        final Queue<List<Resource>> queue = new LinkedList<>();
	        final List<Resource> thingPath = new ArrayList<>();
	        thingPath.add( model.getResource("http://www.opengis.net/ont/geosparql#Feature"));
	        queue.offer( thingPath );
	        // Get the paths, and display them
	        final List<List<Resource>> paths = BFS( model, queue, 6 ,null);
	        //printTree(paths);
	        return jsonExport(paths);
	    }
		
		private static String jsonExport(List<List<Resource>> hierarchy){
	    	int i=0,parent,level;
	    	String parentString="http://www.opengis.net/ont/geosparql#SpatialObject";
	    	Collections.reverse(hierarchy);
	    	StringBuilder builder=new StringBuilder();
	    	builder.append("{ \"plugins\": [\"search\", \"types\",\"sort\"],\"search\": {}, \"core\": { \"data\" :[");
	    	Set<String> idlist=new TreeSet<String>();
    		builder.append("{ \"id\" : \"http://www.w3.org/2002/07/owl#Thing\", \"parent\" : \"#\", \"text\" : \"owl:Thing\" },\n");
    		builder.append("{ \"id\" : \"http://www.opengis.net/ont/geosparql#SpatialObject\", \"parent\" : \"http://www.w3.org/2002/07/owl#Thing\", \"text\" : \"SpatialObject\" },\n");
    		idlist.add("http://www.w3.org/2002/07/owl#Thing");
    		idlist.add("http://www.opengis.net/ont/geosparql#SpatialObject");
	    	for(List<Resource> lres:hierarchy){
	    		parent=0;
	    		level=lres.size();
	    		for(Resource res:lres){
	    			if(res==null || res.getURI()==null)
	    				continue;
	    			if(!idlist.contains(res.getURI())){
	    				builder.append("{ \"id\" : \""+res.getURI()
	    						/*+"_"+level*/+
	    						"\", \"parent\" : \""+parentString/*+"_"+(level-1)*/
	    						+"\", \"text\" : \""+URLDecoder.decode(res.getLocalName())+"\" },\n");
	    				idlist.add(res.getURI());
	    			}
	    			parentString=res.getURI();
    				parent++;
	    		}
	    		//BrokerOntologyConnection.log.debug("Idlist: "+idlist);
	    	}
	    	builder.delete(builder.length()-2, builder.length());
	    	builder.append("]},");
	    	builder.append("\"types\" : {\n\"file\" : {\n\"icon\" : \"../"+
	    			"class.png"+"\"\n},\"default\" : {\n\"icon\" : \"../"+
	    			"class.png"+"\",\n\"valid_children\" : [\"default\"]\n}\n}\n}");
	    	return builder.toString();
	    }
		
		private static List<List<Resource>> BFS( final Model model, final Queue<List<Resource>> queue, final int depth ,final String[] include) {
	        final List<List<Resource>> results = new ArrayList<>();
	        Map<Integer,Set<String>> workedResources=new TreeMap<Integer,Set<String>>();
	        Set<String> seenResources=new TreeSet<String>();
	        while ( !queue.isEmpty() ) {

	            final List<Resource> path = queue.poll();
	            results.add( path );
	            if ( path.size() < depth ) {
	            	if(!workedResources.containsKey(path.size())){
		            	workedResources.put(path.size(), new TreeSet<String>());
	            	}
	                final Resource last = path.get( path.size() - 1 );
	                String lastlast=null;
	                if(path.size()>1)
	                	lastlast = path.get( path.size() - 2 ).toString();
	            	workedResources.get(path.size()).add(last.toString());
	            	//BrokerOntologyConnection.log.debug("WorkedResources: "+workedResources);
	                final StmtIterator stmt = model.listStatements( null, RDFS.subClassOf, last );
	                while ( stmt.hasNext() ) {
	                    final List<Resource> extPath = new ArrayList<>( path );
	                    Statement statement=stmt.next();                   
	                    Resource curres=statement.getSubject().asResource();
	                    if(!workedResources.containsKey(path.size()-1) || !curres.equals(last)){
	                    	
	                    	if(seenResources.contains(last.toString())){
	                    		//BrokerOntologyConnection.log.debug("I have seen "+last.toString()+" before");
	                    	}
	                    	if(workedResources.containsKey(path.size()-1))
	                    	if(!extPath.contains(curres)){
	                    		extPath.add( curres );
	                    		queue.offer( extPath );
	                    	}

		                    seenResources.add(last.toString());
	                    }
	                    
	                }
	            }
	        }
	        return results;
	    }
		
}
