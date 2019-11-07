package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import org.junit.Test;

import java.util.HashMap;

public class QueryMappingFactoryLabelsTest {

    @Test
    public void generateMapping(){
        QueryTemplateMapping t= new QueryTemplateMapping(0,0);
        //String query="QueryTemplateMapping(int numberOfClasses, int numberOfProperties)";
        QueryMappingFactoryLabels qmf = new QueryMappingFactoryLabels("Wie viele Bahnhöfe gibt es in der Region München?", "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX vrank:<http://purl.org/voc/vrank#>\n" +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT DISTINCT ?h WHERE {\n" +
                " <https://portal.limbo-project.org/bahnsteig/3631-6> a <https://portal.limbo-project.org/bahnhof/3631>.\n" +
                " <https://portal.limbo-project.org/bahnsteig/3631-6> <http://linkedgeodata.org/vocabulary#height> ?h.\n" +
                "}", new SemanticAnalysisHelper());
        String queryPattern= qmf.getQueryPattern();
        t.addSelectTemplate(queryPattern,"PREFIX owl:<http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX vrank:<http://purl.org/voc/vrank#>\n" +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT DISTINCT ?h WHERE {\n" +
                " <https://portal.limbo-project.org/bahnsteig/3631-6> a <https://portal.limbo-project.org/bahnhof/3631>.\n" +
                " <https://portal.limbo-project.org/bahnsteig/3631-6> <http://linkedgeodata.org/vocabulary#height> ?h.\n" +
                "}");
        qmf.generateQueries(new HashMap<>(),"",false);
    }
}
