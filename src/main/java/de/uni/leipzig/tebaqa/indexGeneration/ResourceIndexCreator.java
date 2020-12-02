package de.uni.leipzig.tebaqa.indexGeneration;

import info.aduna.io.FileUtil;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.semanticweb.owlapi.rio.OWLAPIRDFFormat;
import org.semanticweb.owlapi.rio.RioOWLRDFParser;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class ResourceIndexCreator {
    public static final String N_TRIPLES = "NTriples";
    public static final String TTL = "ttl";
    public static final String NT = "nt";
    public static final String OWL = "owl";
    public static final String TSV = "tsv";
    private WriteElasticSearchIndex writeIndex;
    private static org.slf4j.Logger log = LoggerFactory.getLogger(GenerateIndexes.class);
    //private HashMap<String,Resource>nameToResource=new HashMap<>();
    private Set<String>names=new HashSet<>();
    private HashMap<String,String>nameToResourceSubject=new HashMap<>();
    private HashMap<String,String>nameToResourceObject=new HashMap<>();
    private HashMap<String,String>nameToTypes=new HashMap<>();
    private HashMap<String,String>nameToLabels=new HashMap<>();
    private HashMap<String,String>nameToProperties_subject=new HashMap<>();
    private HashMap<String,String>nameToProperties_object=new HashMap<>();
    private HashMap<String,List<String>>ontToLable=new HashMap<>();
    private Set<String> foundClasses=new HashSet<>();
    private Set<String> foundPredicates=new HashSet<>();
    public void createIndexFromFile(List<File> files,String index) {
        try {
            writeIndex = new WriteElasticSearchIndex();
            writeIndex.setResourceIndexes(index);
            writeIndex.setLiteralIndexes("limboliteralsfin");
            writeIndex.createBulkProcessor();
            for (File file : files) {
                //String type = FileUtil.getFileExtension(file.getName());
                //if (type.equals(TTL+".bz2"))
                System.out.println("-------FILENAME"+file.getName()+"-----------------");
                    indexTTLFile(file,TTL);
                    index();


                //if(type.equals(NT+".bz2"))
                //    indexTTLFile(file,NT);

            }

            System.out.println("IndexCreation starts");

            writeIndex.close();
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }
    private void index(){
        for(String name:names){
            Set<String> labels=new HashSet();
            Set<String> types=new HashSet();
            Set<String> predicates_subject=new HashSet();
            Set<String> predicates_object=new HashSet();
            Set<String> resource_Subject=new HashSet();
            Set<String> resource_Object=new HashSet();
            if(nameToProperties_subject.containsKey(name))
                predicates_subject.addAll(Arrays.asList(nameToProperties_subject.get(name).split(",,")));
            if(nameToProperties_object.containsKey(name))
                predicates_object.addAll(Arrays.asList(nameToProperties_object.get(name).split(",,")));
            if(nameToResourceSubject.containsKey(name))
                resource_Subject.addAll(Arrays.asList(nameToResourceSubject.get(name).split(",,")));
            if(nameToResourceObject.containsKey(name))
                resource_Object.addAll(Arrays.asList(nameToResourceObject.get(name).split(",,")));
            if(nameToLabels.containsKey(name))
                labels.addAll(Arrays.asList(nameToLabels.get(name).split(",,")));
            if(nameToTypes.containsKey(name))
                types.addAll(Arrays.asList(nameToTypes.get(name).split(",,")));

            try {
                writeIndex.upsertResource(name,labels,types,resource_Subject,resource_Object,predicates_subject,predicates_object);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        names.clear();
        nameToLabels.clear();
        nameToResourceSubject.clear();
        nameToResourceObject.clear();
        nameToProperties_subject.clear();
        nameToProperties_object.clear();
        nameToTypes.clear();
        writeIndex.commit();
    }
    public void createOntologieIndexFromFile(List<File> files,String predicateIndex,String classIndex) {
        try {
            writeIndex = new WriteElasticSearchIndex();
            writeIndex.setOntologieIndexes(predicateIndex,classIndex);
            writeIndex.createBulkProcessor();
            for (File file : files) {
                String type = FileUtil.getFileExtension(file.getName());
                indexTTLFile(file,OWL);

            }
            for(String cl:foundClasses){
                if(ontToLable.containsKey(cl))
                    writeIndex.indexClass(cl,ontToLable.get(cl));
            }
            for(String cl:foundPredicates){
                if(ontToLable.containsKey(cl))
                    writeIndex.indexProperty(cl,ontToLable.get(cl));
            }
            writeIndex.commit();
            writeIndex.close();
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }
    public void createOntologieIndexFromUrls(List<String>resources,String predicateIndex,String classIndex) {
        try {
            writeIndex = new WriteElasticSearchIndex();
            writeIndex.setOntologieIndexes(predicateIndex,classIndex);
            writeIndex.createBulkProcessor();
            for (String urlstring : resources) {
                URL url = new URL(urlstring);
                URLConnection conn = url.openConnection();
                InputStream inputStream = conn.getInputStream();
                if (urlstring.endsWith(OWL))
                    indexTTLFileFromWeb(inputStream,OWL);
                writeIndex.commit();
            }
            writeIndex.close();
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }
    void close(){
        writeIndex.close();
    }
    public void createIndexFromUrls(List<String>resources,String index){

        try {

            for(String urlstring:resources) {
                System.out.println("Start reading File");
                URL url = new URL(urlstring);
                URLConnection conn = url.openConnection();
                InputStream inputStream = new BZip2CompressorInputStream(conn.getInputStream());
                if (urlstring.endsWith(TTL+".bz2"))
                    indexTTLFileFromWeb(inputStream,TTL);
                if(urlstring.endsWith(NT+".bz2"))
                    indexTTLFileFromWeb(inputStream,NT);
                inputStream.close();
            }
            writeIndex = new WriteElasticSearchIndex();
            writeIndex.setResourceIndexes(index);
            writeIndex.createBulkProcessor();
            System.out.println("IndexCreation starts");
            /*for(String name:nameToResource.keySet()){
                writeIndex.indexResource(nameToResource.get(name));
            }*/
            writeIndex.close();
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }
    private void indexTTLFileFromWeb(InputStream inputStream,String type)
            throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
        //log.info("Start parsing: " + file);
        RDFParser parser;
        RDFHandlerBase handler=null;
        if(TTL.equals(type)) {
            parser = new TurtleParser();
            handler = new OnlineStatementHandler();
        }
        else if(OWL.equals(type)) {
            parser = new RDFXMLParser();
            handler=new OntologieStatementHandler();
        }
        else {
            parser = new NTriplesParser();
            handler = new OnlineStatementHandler();
        }
        parser.setRDFHandler(handler);
        parser.setStopAtFirstError(false);
        parser.parse(inputStream, "");

        //log.info("Finished parsing: " + file);
    }
    private void indexTTLFile(File file, String type)
            throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
        log.info("Start parsing: " + file);
        RDFParser parser;
        RDFHandlerBase handler=null;
        if(TTL.equals(type)) {
            parser = new TurtleParser();
            handler = new OnlineStatementHandler();
        }
        else if(OWL.equals(type)) {
            parser = new NTriplesParser();
            handler=new OntologieStatementHandler();
        }
        else{
            parser=new NTriplesParser();
            handler = new OnlineStatementHandler();
        }
        parser.setRDFHandler(handler);
        parser.setStopAtFirstError(false);
        parser.parse(new InputStreamReader(new FileInputStream(file),"UTF-8"), "");
        //parser.parse(new BZip2CompressorInputStream(new FileInputStream(file)), "");

        log.info("Finished parsing: " + file);
    }



    private class OntologieStatementHandler extends RDFHandlerBase {

        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();
            if(object.equals("http://www.w3.org/2002/07/owl#Class")||
                    object.equals("http://www.w3.org/2000/01/rdf-schema#Class"))
                foundClasses.add(subject);
            if(object.equals("http://www.w3.org/2002/07/owl#ObjectProperty")||
                    object.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"))
                foundPredicates.add(subject);
            if(predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                Literal l = (Literal) st.getObject();
                String lang = l.getLanguage();
                if(lang!=null&&lang.equals("de")){
                    if (!ontToLable.containsKey(subject))
                        ontToLable.put(subject,new ArrayList<>());
                    ontToLable.get(subject).add(l.getLabel());

                }
                    /*try {
                        if(lang.equals("de")&&foundClasses.contains(subject)) {
                            ArrayList<String> list = new ArrayList();
                            list.add(object);
                            writeIndex.indexClass(subject, list);
                        }
                        else if(lang.equals("de")&&foundPredicates.contains(subject))
                            writeIndex.indexProperty(subject,object,new ArrayList<>());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/


            }
        }
    }
    private class OnlineStatementHandler extends RDFHandlerBase {
        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();

            if(!names.contains(subject))names.add(subject);
            if(!names.contains(object)&&st.getObject() instanceof URI&&object.startsWith("http://"))
                names.add(object);


            if(predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                //if(nameToTypes.size()%1000==0)System.out.println("Mapsize Types"+nameToTypes.size());
                if(!nameToTypes.containsKey(subject)) nameToTypes.put(subject,object);
                else nameToTypes.put(subject,nameToTypes.get(subject)+",,"+object);
            }
            else if(st.getObject() instanceof URI) {
                //if(nameToResourceSubject.size()%1000==0)System.out.println("Mapsize Resource"+nameToResourceSubject.size());
                if(!nameToResourceSubject.containsKey(subject)) nameToResourceSubject.put(subject,object);
                else nameToResourceSubject.put(subject,nameToResourceSubject.get(subject)+",,"+object);
                if(!nameToResourceObject.containsKey(object)) nameToResourceObject.put(object,subject);
                else nameToResourceObject.put(object,nameToResourceObject.get(object)+",,"+subject);
                if(!nameToProperties_subject.containsKey(subject))nameToProperties_subject.put(subject,predicate);
                else nameToProperties_subject.put(subject,nameToProperties_subject.get(subject)+",,"+predicate);
                if(!nameToProperties_object.containsKey(subject))nameToProperties_object.put(object,predicate);
                else nameToProperties_object.put(subject,nameToProperties_object.get(object)+",,"+predicate);
            }


            else if(!predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")){
                //if(nameToProperties_subject.size()%1000==0)System.out.println("Mapsize Properties"+nameToProperties_subject.size());
                if(!nameToProperties_subject.containsKey(subject))nameToProperties_subject.put(subject,predicate);
                else nameToProperties_subject.put(subject,nameToProperties_subject.get(subject)+",,"+predicate);
                try {
                    writeIndex.indexLiteral(subject,predicate,object);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")||
                    predicate.equals("http://www.w3.org/2004/02/skos/core#prefLabel")||
                    predicate.equals("http://schema.mobivoc.org/situationId")||
                    predicate.equals("http://linkedgeodata.org/vocabulary#label")||
                    predicate.equals("http://www.w3.org/2000/01/rdf-schema#altLabel")||
                    predicate.equals("https://portal.limbo-project.org/vocab/IndoorNavigationVocab/hasName")
            ){
                //if(nameToLabels.size()%1000==0)System.out.println("Mapsize Labels"+nameToLabels.size());
                if(!nameToLabels.containsKey(subject)) nameToLabels.put(subject,object);
                else nameToLabels.put(subject,nameToLabels.get(subject)+",,"+object);
            }
            if(nameToProperties_subject.size()+nameToProperties_object.size()+nameToLabels.size()+nameToResourceSubject.size()+nameToTypes.size()>1000000)
                index();
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
