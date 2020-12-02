package de.uni.leipzig.tebaqa.indexGenerationDcat;

import de.uni.leipzig.tebaqa.indexGeneration.ResourceIndexCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class GenerateIndexes {
    public static void main(String[]args){

        try {
        Properties prop = new Properties();
        InputStream input = new FileInputStream("src/main/resources/entityLinking.properties");
        prop.load(input);

        String resourceIndex = "resource_dcat";

        String propertyIndex = "property_dcat";
        String classIndex = "class_dcat";
        IndexCreatorDcat ic = new IndexCreatorDcat();
        File resources=new File("dct/opal-2019-06-24_2020-04-14_12-06-49.nq");
        File property=new File("dct/dcat2+german-labels.rdf");
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
