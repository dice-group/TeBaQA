package de.uni.leipzig.tebaqa.indexGeneration;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;

import java.io.*;
import java.util.*;

public class Collect {
    public static final String N_TRIPLES = "NTriples";
    public static final String TTL = "ttl";
    public static final String NT = "nt";
    public static final String OWL = "owl";
    public static final String TSV = "tsv";
    //private HashMap<String,Resource>nameToResource=new HashMap<>();
    private Set<String> resources=new HashSet<>();
    private Set<String> newresources=new HashSet<>();
    public static void main(String[]args){
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("src/main/resources/entityLinking.properties");
            prop.load(input);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Collect c = new Collect();
        List<File> listOfFiles = new ArrayList<File>();
        String folderWithTtlFiles = prop.getProperty("folder_with_ttl_files");
        for (File file : new File(folderWithTtlFiles).listFiles()) {
            if (file.getName().endsWith("ttl") || file.getName().endsWith("nt")||file.getName().endsWith("owl")) {
                listOfFiles.add(file);
            }
        }
        c.readFromFile(listOfFiles);


        System.out.println();

    }
    public void readFromFile(List<File> files) {

        //String type = FileUtil.getFileExtension(file.getName());
        //if (type.equals(TTL+".bz2"))
        try {
            for (File file : files) {
                readTTLFile(file, TTL);
            }
        } catch (RDFParseException e1) {
            e1.printStackTrace();
        } catch (RDFHandlerException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }


        //if(type.equals(NT+".bz2"))
        //    indexTTLFile(file,NT);



        System.out.println("IndexCreation starts");


    }



    private void readTTLFile(File file, String type)
            throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
        RDFParser parser;
        RDFHandlerBase handler=null;
        if(TTL.equals(type)) {
            parser = new TurtleParser();
            handler = new de.uni.leipzig.tebaqa.indexGeneration.Collect.OnlineStatementHandler();
        }
        else{
            parser=new NTriplesParser();
            handler = new de.uni.leipzig.tebaqa.indexGeneration.Collect.OnlineStatementHandler();
        }
        parser.setRDFHandler(handler);
        parser.setStopAtFirstError(false);
        parser.parse(new FileInputStream(file), "");
        //parser.parse(new BZip2CompressorInputStream(new FileInputStream(file)), "");
    }




    private class OnlineStatementHandler extends RDFHandlerBase {
        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();
            if(!resources.contains(subject)){
                QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", "describe <"+subject+">");
                Model m=qe.execDescribe();
                StmtIterator i=m.listStatements();
                resources.add(subject);
                while(i.hasNext()){
                    org.apache.jena.rdf.model.Statement s=i.nextStatement();
                    if(!s.getPredicate().toString().equals("http://dbpedia.org/ontology/wikiPageWikiLink")) {
                        if(!resources.contains(s.getSubject().toString()))newresources.add(s.getSubject().toString());
                        if(!resources.contains(s.getObject().toString())&&s.getObject().isURIResource())newresources.add(s.getObject().toString());
                    }

                }
                m.close();
            }
            if(st.getSubject() instanceof URI&&!resources.contains(object)){
                QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", "describe <"+object+">");
                Model m=qe.execDescribe();
                StmtIterator i=m.listStatements();
                resources.add(object);
                while(i.hasNext()){
                    org.apache.jena.rdf.model.Statement s=i.nextStatement();
                    if(!s.getPredicate().toString().equals("http://dbpedia.org/ontology/wikiPageWikiLink")) {
                        if(!resources.contains(s.getSubject().toString()))newresources.add(s.getSubject().toString());
                        if(!resources.contains(s.getObject().toString())&&s.getObject().isURIResource())newresources.add(s.getObject().toString());
                    }
                }
                m.close();
            }
            System.out.println("Old: "+resources.size());
            System.out.println("New: "+newresources.size());


        }
    }
}
