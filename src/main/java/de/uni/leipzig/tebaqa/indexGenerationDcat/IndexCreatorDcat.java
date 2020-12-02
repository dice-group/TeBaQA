package de.uni.leipzig.tebaqa.indexGenerationDcat;

import de.uni.leipzig.tebaqa.indexGeneration.GenerateIndexes;
import de.uni.leipzig.tebaqa.indexGeneration.ResourceIndexCreator;
import de.uni.leipzig.tebaqa.indexGeneration.WriteElasticSearchIndex;
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

public class IndexCreatorDcat {
    public static final String N_TRIPLES = "NTriples";
    public static final String TTL = "ttl";
    public static final String NT = "nt";
    public static final String OWL = "owl";
    public static final String TSV = "tsv";
    private WriteDcatIndex writeIndex;
    private static org.slf4j.Logger log = LoggerFactory.getLogger(GenerateIndexes.class);
    //private HashMap<String,Resource>nameToResource=new HashMap<>();
    private Set<String> names=new HashSet<>();
    private HashMap<String,String> nameToResourceSubject=new HashMap<>();
    private HashMap<String,String>nameToResourceObject=new HashMap<>();
    private HashMap<String,String>nameToTypes=new HashMap<>();
    private HashMap<String,String>nameToLabels=new HashMap<>();
    private HashMap<String,String>nameToProperties_subject=new HashMap<>();
    private HashMap<String,String>nameToProperties_object=new HashMap<>();

    private HashMap<String,String>uriPropToDe=new HashMap<>();
    private HashMap<String,String>uriPropToEn=new HashMap<>();
    private HashMap<String,String>uriClassToDe=new HashMap<>();
    private HashMap<String,String>uriClassToEn=new HashMap<>();
    Set<String>propNames=new HashSet<>();
    Set<String>classNames=new HashSet<>();
    public void createIndexFromFile(List<File> files, String index) {
        try {
            writeIndex = new WriteDcatIndex();
            //writeIndex.setResourceIndexes(index);
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

            //writeIndex.close();
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

            /*try {
                writeIndex.upsertResource(name,labels,types,resource_Subject,resource_Object,predicates_subject,predicates_object);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }
        names.clear();
        nameToLabels.clear();
        nameToResourceSubject.clear();
        nameToResourceObject.clear();
        nameToProperties_subject.clear();
        nameToProperties_object.clear();
        nameToTypes.clear();
        //writeIndex.commit();
    }
    public void createOntologieIndexFromFile(List<File> files,String predicateIndex,String classIndex) {
        try {
            writeIndex = new WriteDcatIndex();
            writeIndex.setOntologieIndexes(predicateIndex,classIndex);
            writeIndex.createBulkProcessor();
            for (File file : files) {
                String type = FileUtil.getFileExtension(file.getName());
                //if(type.equals(OWL))
                    indexTTLFile(file,OWL);
                //writeIndex.commit();
            }
            writeIndex.close();
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }


    private void indexTTLFile(File file, String type)
            throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
        log.info("Start parsing: " + file);
        RDFParser parser;
        RDFHandlerBase handler=null;
        if(TTL.equals(type)) {
            parser = new TurtleParser();
            handler = new IndexCreatorDcat.OnlineStatementHandler();
        }
        else if(OWL.equals(type)) {
            parser = new RDFXMLParser();
            handler=new IndexCreatorDcat.OntologieStatementHandler();
        }
        else{
            parser=new NTriplesParser();
            handler = new IndexCreatorDcat.OnlineStatementHandler();
        }
        parser.setRDFHandler(handler);
        parser.setStopAtFirstError(false);
        parser.parse(new FileInputStream(file), "");
        //parser.parse(new BZip2CompressorInputStream(new FileInputStream(file)), "");
        try {
            for (String p : propNames) {
                String propDe="";
                String propEn="";
                if(uriPropToEn.containsKey(p))propEn=uriPropToEn.get(p);
                if(uriPropToDe.containsKey(p))propDe=uriPropToDe.get(p);
                if(propDe!=""||propEn!=null)
                writeIndex.indexProperty(p, propEn, propDe);
            }
            for (String p : classNames) {
                String classDe="";
                String classEn="";
                if(uriClassToEn.containsKey(p))classEn=uriClassToEn.get(p);
                if(uriClassToDe.containsKey(p))classDe=uriClassToDe.get(p);
                if(classDe!=""||classEn!=null)
                    writeIndex.indexClass(p, classEn, classDe);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Finished parsing: " + file);
    }



    private class OntologieStatementHandler extends RDFHandlerBase {
        private Set<String> foundClasses=new HashSet<>();
        private Set<String> foundPredicates=new HashSet<>();
        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();
            if(object.equals("http://www.w3.org/2002/07/owl#Class"))
                classNames.add(subject);
            if(object.equals("http://www.w3.org/2002/07/owl#ObjectProperty"))
                propNames.add(subject);
            if(predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                Literal l = (Literal) st.getObject();
                String lang = l.getLanguage();
                if(lang.equals("en")&&propNames.contains(subject)) {
                    ArrayList<String> list = new ArrayList();
                    list.add(object);
                    uriPropToEn.put(subject, object);
                }
                else if(lang.equals("de")&&propNames.contains(subject)) {
                    ArrayList<String> list = new ArrayList();
                    list.add(object);
                    uriPropToDe.put(subject, object);
                }
                if(lang.equals("en")&&classNames.contains(subject)) {
                    ArrayList<String> list = new ArrayList();
                    list.add(object);
                    uriClassToEn.put(subject, object);
                }
                else if(lang.equals("de")&&classNames.contains(subject)) {
                    ArrayList<String> list = new ArrayList();
                    list.add(object);
                    uriClassToDe.put(subject, object);
                }



            }
        }
    }
    private class OnlineStatementHandler extends RDFHandlerBase {
        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();
            if (subject.startsWith("http") && (!(st.getObject() instanceof URI) || object.startsWith("http"))){
                if (!names.contains(subject)) names.add(subject);
                if (!names.contains(object) && st.getObject() instanceof URI && object.startsWith("http://dbpedia.org/resource/"))
                    names.add(object);
                if (predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                    //if(nameToTypes.size()%1000==0)System.out.println("Mapsize Types"+nameToTypes.size());
                    if (!nameToTypes.containsKey(subject)) nameToTypes.put(subject, object);
                    else nameToTypes.put(subject, nameToTypes.get(subject) + ",," + object);
                } else if (st.getObject() instanceof URI) {
                    //if(nameToResourceSubject.size()%1000==0)System.out.println("Mapsize Resource"+nameToResourceSubject.size());
                    if (!nameToResourceSubject.containsKey(subject)) nameToResourceSubject.put(subject, object);
                    else nameToResourceSubject.put(subject, nameToResourceSubject.get(subject) + ",," + object);
                    if (!nameToResourceObject.containsKey(object)) nameToResourceObject.put(object, subject);
                    else nameToResourceObject.put(object, nameToResourceObject.get(object) + ",," + subject);
                    if (!nameToProperties_subject.containsKey(subject)) nameToProperties_subject.put(subject, predicate);
                    else nameToProperties_subject.put(subject, nameToProperties_subject.get(subject) + ",," + predicate);
                    if (!nameToProperties_object.containsKey(subject)) nameToProperties_object.put(object, predicate);
                    else nameToProperties_object.put(subject, nameToProperties_object.get(object) + ",," + predicate);
                } else if (!predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                    //if(nameToProperties_subject.size()%1000==0)System.out.println("Mapsize Properties"+nameToProperties_subject.size());
                    if (!nameToProperties_subject.containsKey(subject)) nameToProperties_subject.put(subject, predicate);
                    else nameToProperties_subject.put(subject, nameToProperties_subject.get(subject) + ",," + predicate);
                }
                if (predicate.equals("http://www.w3.org/2000/01/rdf-schema#label") ||
                        predicate.equals("http://www.w3.org/2004/02/skos/core#prefLabel")) {
                    //if(nameToLabels.size()%1000==0)System.out.println("Mapsize Labels"+nameToLabels.size());
                    if (!nameToLabels.containsKey(subject)) nameToLabels.put(subject, object);
                    else nameToLabels.put(subject, nameToLabels.get(subject) + ",," + object);
                }
                if (nameToProperties_subject.size() + nameToProperties_object.size() + nameToLabels.size() + nameToResourceSubject.size() + nameToTypes.size() > 1000000)
                    index();


            }
        }
    }
}
