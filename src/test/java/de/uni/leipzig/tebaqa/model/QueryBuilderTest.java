package de.uni.leipzig.tebaqa.model;

import org.aksw.qa.commons.datastructure.Question;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class QueryBuilderTest {

    @Test
    public void testBuildQuery() throws Exception {
        Cluster cluster = new Cluster(" {\"1\" @\"p\" \"2\"}");
        List<Cluster> clusters = new ArrayList<>();
        Question question = new Question();
        Map<String, String> questionText = new HashMap<>();
        questionText.put("en", "What is the timezone in San Pedro de Atacama?");
        question.setLanguageToQuestion(questionText);
        cluster.addQuestion(question);
        clusters.add(cluster);
        QueryBuilder queryBuilder = new QueryBuilder(clusters);
        //assertEquals("SELECT DISTINCT ?d WHERE { <http://dbpedia.org/resource/Boris_Becker> <http://dbpedia.org/ontology/activeYearsEndDate> ?d . }", query);
    }

}