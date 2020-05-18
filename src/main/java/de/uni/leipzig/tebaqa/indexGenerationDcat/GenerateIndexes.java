package de.uni.leipzig.tebaqa.indexGenerationDcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GenerateIndexes {
    public static void main(String[]args){

        try {
        Properties prop = new Properties();
        InputStream input = new FileInputStream("src/main/resources/application.properties");
        prop.load(input);


        String propertyIndex = prop.getProperty("property_index");
        String classIndex =prop.getProperty("class_index");

        IndexCreatorDcat ic = new IndexCreatorDcat();
        //not used at the moment
        File resources=new File(prop.getProperty("resource_file"));
        File property=new File(prop.getProperty("ontology_file"));
        List<File> listOfFiles = new ArrayList<File>();
        listOfFiles.add(resources);
        List<File> listOfPropFiles = new ArrayList<File>();
        listOfPropFiles.add(property);
        List<File> listOResFiles = new ArrayList<File>();
        listOResFiles.add(resources);
        ic.createOntologieIndexFromFile(listOfPropFiles, propertyIndex, classIndex);

        //ic.createIndexFromFile(listOfFiles,resourceIndex)




    } catch (
    IOException e) {
        System.out.println("Error while creating index. Maybe the index is corrupt now.");
        e.printStackTrace();
    }
}
}
