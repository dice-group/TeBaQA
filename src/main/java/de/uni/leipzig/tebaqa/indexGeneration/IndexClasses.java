package de.uni.leipzig.tebaqa.indexGeneration;

import com.google.common.collect.Lists;
import org.apache.jena.rdf.model.Resource;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class IndexClasses {
    private static HashMap<String, Set<String>> classes=new HashMap<>();
    public static void main(String[]args){
        getClassLabelsFromFile();
        try {
            WriteElasticSearchIndex index=new WriteElasticSearchIndex();
        index.generateOntIndex("classdbpedia");
        index.createBulkProcessor();

        for(String key:classes.keySet()) {
                index.indexClass(key, Lists.newArrayList(classes.get(key)));
            }
            index.commit();
            index.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    private static void getClassLabelsFromFile(){
        RDFParser parser;
        RDFHandlerBase handler=null;
        parser = new NTriplesParser();
        handler = new IndexClasses.OntologieStatementHandler();


        parser.setRDFHandler(handler);
        parser.setStopAtFirstError(false);
        try {
            parser.parse(new FileInputStream(new File("classFiles/dbpedia_2016-10.nt")), "");
            parser.parse(new FileInputStream(new File("classFiles/dbpedia_3Eng_class.ttl.txt")), "");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RDFParseException e) {
            e.printStackTrace();
        } catch (RDFHandlerException e) {
            e.printStackTrace();
        }
        //parser.parse(new BZip2CompressorInputStream(new FileInputStream(file)), "");
        System.out.println();
    }





    private static class OntologieStatementHandler extends RDFHandlerBase {

        @Override
        public void handleStatement(Statement st) {
            String subject;
            if(st.getObject().stringValue().equals("http://www.w3.org/2002/07/owl#Class"))
                classes.put(st.getSubject().toString(),new HashSet<>());
            else if(st.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#label")
                &&classes.containsKey(st.getSubject().toString())){
                Literal l = (Literal) st.getObject();
                String lang = l.getLanguage();
                if(lang==null||lang.equals("en"))
                    classes.get(st.getSubject().toString()).add(l.getLabel());
            }

        }
    }
}
