package de.uni.leipzig.tebaqa.indexGeneration;

import info.aduna.io.FileUtil;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class LemonTest {

        public static final String N_TRIPLES = "NTriples";
        public static final String TTL = "ttl";
        public static final String NT = "nt";
        public static final String OWL = "owl";
        public static final String TSV = "tsv";
        //private HashMap<String,Resource>nameToResource=new HashMap<>();
        private Set<String> names=new HashSet<>();
        private HashMap<String,Set<String>> refToDBpedia=new HashMap<>();
        private HashMap<String,String>senseToElemt=new HashMap<>();
        private HashMap<String,String>elementToCanForm=new HashMap<>();
        private HashMap<String,String>canFormToLit=new HashMap<>();
        public static void main(String[]args){
            LemonTest t=new LemonTest();
            t.readFromFile(new File("C:/Users/Jan/Desktop/lemon.dbpedia-master/dbpedia_en.nt"));

            HashMap<String,Set<String>> dbpediaToRep=new HashMap<>();
            for(String key:t.refToDBpedia.keySet()){
                    HashSet<String>s=new HashSet<>();
                    for(String el:t.refToDBpedia.get(key))
                        s.add(t.canFormToLit.get(t.elementToCanForm.get(t.senseToElemt.get(el))));
                    dbpediaToRep.put(key,s);
            }
            for(String key:dbpediaToRep.keySet()){
                StringBuilder out=new StringBuilder();
                dbpediaToRep.get(key).forEach(s->out.append(s+", "));
                System.out.println(key+": "+out.toString());
            }
            System.out.println();

        }
        public void readFromFile(File f) {

                    //String type = FileUtil.getFileExtension(file.getName());
                    //if (type.equals(TTL+".bz2"))
                try {
                    readTTLFile(f,NT);
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
                handler = new de.uni.leipzig.tebaqa.indexGeneration.LemonTest.OnlineStatementHandler();
            }
            else{
                parser=new NTriplesParser();
                handler = new de.uni.leipzig.tebaqa.indexGeneration.LemonTest.OnlineStatementHandler();
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
                if(predicate.equals("http://lemon-model.net/lemon#writtenRep")){
                    canFormToLit.put(subject,object);
                }
                if(predicate.equals("http://lemon-model.net/lemon#canonicalForm")){
                    elementToCanForm.put(subject,object);
                }
                if(predicate.startsWith("http://lemon-model.net/lemon#sense")){
                    senseToElemt.put(object,subject);
                }
                if(predicate.equals("http://lemon-model.net/lemon#reference")){
                    if(refToDBpedia.containsKey(object)){
                        refToDBpedia.get(object).add(subject);
                    }
                    else{
                        Set<String>rep=new HashSet<>();
                        rep.add(subject);
                        refToDBpedia.put(object,rep);
                    }

                }
            /*if(predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
                writeIndex.upsertType(subject,object);
            else if(st.getObject() instanceof URI) {
                writeIndex.upsertRelResource(subject, object, predicate);
                writeIndex.upsertConnectedResource(object,subject);
            }
            else if(predicate.equals("http://www.w3.org/2000/01/rdf-schema#label"))
                writeIndex.upsertLabel(subject,object);
            else{
                writeIndex.upsertPredicate(subject,predicate);
            }*/


            }
        }

}
