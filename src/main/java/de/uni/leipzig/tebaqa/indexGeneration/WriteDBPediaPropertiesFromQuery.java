package de.uni.leipzig.tebaqa.indexGeneration;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.helper.DBpediaPropertiesProvider;
import moa.recommender.rc.utils.Hash;
import org.apache.jena.rdf.model.Resource;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.query.algebra.Str;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.turtle.TurtleParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static de.uni.leipzig.tebaqa.helper.DBpediaPropertiesProvider.getDBpediaPropertiesAndLabels;

public class WriteDBPediaPropertiesFromQuery {
    private static HashMap<String,List<String>>properties=new HashMap<>();
    private static HashMap<String,List<String>>foaf=new HashMap<>();
    public static void main(String[]args){
        try {
            //HashMap<String,List<String>>synonyms=getRelationSynonyms();
            getRelationLabelsFromFile();
            HashMap<Resource,String> props=DBpediaPropertiesProvider.getDBpediaPropertiesAndLabels();
            HashMap<String,List<String>>relations=new HashMap();
            /*props.keySet().forEach(s->{if(synonyms.containsKey(props.get(s).substring(0,props.get(s).indexOf("@"))))
                relations.put(s,synonyms.get(props.get(s).substring(0,props.get(s).indexOf("@"))));});
            */
            props.keySet().forEach(resource->{
                if(properties.containsKey(resource.getLocalName()))relations.put(resource.toString(),properties.get(resource.getLocalName()));
            });
            WriteElasticSearchIndex index=new WriteElasticSearchIndex();
            index.generateOntIndex("propertydbpedia2");
            index.createBulkProcessor();
            for(Resource key:props.keySet())
                index.indexProperty(key.toString(),props.get(key).substring(0,props.get(key).indexOf("@")),relations.get(key.toString()));
            for(String key:foaf.keySet())
                index.indexProperty(key,foaf.get(key).get(0),foaf.get(key));
            index.commit();
            index.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private static HashMap<String,List<String>>readSynFile(){
        File file=new File("C:/Users/Jan/Desktop/words.txt");
        try {
            FileReader fr= new FileReader(file);
            BufferedReader br=new BufferedReader(fr);
            //String line = br.readLine();
            String line=br.readLine();
            HashMap<String,List<String>>synonyms=new HashMap();
            while (line!=null){
                String[]syn=line.split(",");
                String key=syn[0];
                List<String>synonym=new ArrayList<>();
                synonym.addAll(Arrays.asList(syn));
                synonym.remove(0);
                synonyms.put(key,synonym);
                line=br.readLine();
            }
            return synonyms;


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    private static HashMap<String,List<String>> getRelationSynonyms() {
            //Path currentDir = Paths.get(".");
            //Path path = currentDir.resolve("resources/dbpedia-relation-paraphrases.txt");
        Path path = Paths.get("src/main/resources/dbpedia-relation-paraphrases.txt");
            HashMap<String,List<String>>relations=new HashMap<>();
        Stream<String> lines = null;
        try {
            lines = Files.lines(path);


        lines.forEach(line-> {
            String rel = line.split("\\t")[0];
            String dat = line.split("\\t")[1];
            if(!relations.containsKey(rel)){
                List<String>data=new ArrayList<>();
                data.add(dat);
                relations.put(rel,data);
            }
            else relations.get(rel).add(dat);
        });
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return relations;
    }
    private static void getRelationLabelsFromFile(){
        RDFParser parser;
        RDFHandlerBase handler=null;
        parser = new TurtleParser();
        handler = new WriteDBPediaPropertiesFromQuery.OntologieStatementHandler();


        parser.setRDFHandler(handler);
        parser.setStopAtFirstError(false);
        try {
            parser.parse(new FileInputStream(new File("ttlFiles/dbpedia_3Eng_property.ttl.txt")), "");
            parser.parse(new FileInputStream(new File("ttlFiles/propertydbpediasyn.ttl")), "");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RDFParseException e) {
            e.printStackTrace();
        } catch (RDFHandlerException e) {
            e.printStackTrace();
        }
        //parser.parse(new BZip2CompressorInputStream(new FileInputStream(file)), "");

    }





    private static class OntologieStatementHandler extends RDFHandlerBase {

        @Override
        public void handleStatement(Statement st) {
            String subject;
            if(st.getSubject().stringValue().startsWith("http://xmlns.com/foaf/0.1/")) {
                if(foaf.containsKey(st.getSubject().stringValue()))
                    foaf.get(st.getSubject().stringValue()).add(st.getObject().stringValue());
                else {
                    List<String> labels = new ArrayList<>();
                    labels.add(st.getObject().stringValue());
                    foaf.put(st.getSubject().stringValue(), labels);
                }
            }
            if(st.getSubject().stringValue().startsWith("http://dbpedia.org/ontology/"))
                subject= st.getSubject().stringValue().replace("http://dbpedia.org/ontology/","");
            else subject=st.getSubject().stringValue().replace("http://dbpedia.org/property/","");
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();
            if(properties.containsKey(subject)){
                properties.get(subject).add(object);
            }
            else {
                List<String>labels=new ArrayList<>();
                labels.add(object);
                properties.put(subject,labels);
            }

        }
    }




}
