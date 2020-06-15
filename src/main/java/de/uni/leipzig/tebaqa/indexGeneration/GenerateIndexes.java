package de.uni.leipzig.tebaqa.indexGeneration;

import info.aduna.io.FileUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class GenerateIndexes {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(GenerateIndexes.class);



    public static void main(String args[]) {
        if (args.length > 0) {
            log.error("TripleIndexCreator works without parameters. Please use agdistis.properties File");
            return;
        }
        try {
            Properties prop = new Properties();
            InputStream input = new FileInputStream("src/main/resources/entityLinking.properties");
            prop.load(input);

            String resourceIndex = prop.getProperty("resource_index");
            log.info("The resource index will be here: " + resourceIndex);

            String propertyIndex = prop.getProperty("property_index");
            log.info("The resource index will be here: " + propertyIndex);
            String classIndex = prop.getProperty("class_index");
            log.info("The resource index will be here: " + classIndex);
            ResourceIndexCreator ic = new ResourceIndexCreator();
            if(Boolean.valueOf(prop.getProperty("generateOntologyIndex"))&&!Boolean.valueOf(prop.getProperty("extract_from_url"))) {
                List<File> listOfFiles = new ArrayList<File>();
                    String folderWithTtlFiles = prop.getProperty("folder_with_owl_files");
                    log.info("The resource index will be here: " + resourceIndex);
                    for (File file : new File(folderWithTtlFiles).listFiles()) {
                        if (file.getName().endsWith("ttl") || file.getName().endsWith("nt")||file.getName().endsWith("owl")) {
                            listOfFiles.add(file);
                        }
                    }
                    ic.createOntologieIndexFromFile(listOfFiles, propertyIndex, classIndex);

            }
            else if (Boolean.valueOf(prop.getProperty("generateOntologyIndex"))){
                List<String>urls= Arrays.asList(prop.getProperty("owl_urls").split(","));
                ic.createOntologieIndexFromUrls(urls,propertyIndex,classIndex);
            }
            if(Boolean.valueOf(prop.getProperty("generateResourceIndex"))&&!Boolean.valueOf(prop.getProperty("extract_from_url"))) {
                List<File> listOfFiles = new ArrayList<File>();
                String folderWithTtlFiles = prop.getProperty("folder_with_ttl_files");
                log.info("The resource index will be here: " + resourceIndex);
                for (File file : new File(folderWithTtlFiles).listFiles()) {
                    if (file.getName().endsWith("ttl") || file.getName().endsWith("nt")||file.getName().endsWith("owl")) {
                        listOfFiles.add(file);
                    }
                }
                ic.createIndexFromFile(listOfFiles,resourceIndex);
                //ic.writeIndexFromFTP(baseURI,useElasticsearch);

            }
            else if(Boolean.valueOf(prop.getProperty("generateResourceIndex"))){
                List<String>urls= Arrays.asList(prop.getProperty("urls").split(","));
                ic.createIndexFromUrls(urls,resourceIndex);
            }

            ic.close();


        } catch (IOException e) {
            log.error("Error while creating index. Maybe the index is corrupt now.", e);
        }
    }


}
