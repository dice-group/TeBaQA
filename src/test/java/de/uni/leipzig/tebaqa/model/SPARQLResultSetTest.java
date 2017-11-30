package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SPARQLResultSetTest {

    @Test
    public void testRemoveDateType() {
        List<String> resultSet = new ArrayList<>();
        resultSet.add("1997-08-31^^http://www.w3.org/2001/XMLSchema#date");
        SPARQLResultSet sparqlResultSet = new SPARQLResultSet(resultSet, SemanticAnalysisHelper.DATE_ANSWER_TYPE);
        List<String> actual = sparqlResultSet.getResultSet();
        assertTrue(actual.size() == 1);
        assertEquals("1997-08-31", actual.get(0));
    }

    @Test
    public void testRemoveNmberType() {
        List<String> resultSet = new ArrayList<>();
        resultSet.add("1225^^http://www.w3.org/2001/XMLSchema#positiveInteger");
        SPARQLResultSet sparqlResultSet = new SPARQLResultSet(resultSet, SemanticAnalysisHelper.DATE_ANSWER_TYPE);
        List<String> actual = sparqlResultSet.getResultSet();
        assertTrue(actual.size() == 1);
        assertEquals("1225", actual.get(0));
    }
}
