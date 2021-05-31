package de.uni.leipzig.tebaqa.model;

import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.qald.QALD4_EvaluationUtils;

import java.util.ArrayList;
import java.util.List;

public class QuestionFactory {
    public static Question createInstance(IQuestion q) {
        Question question = new Question();

        question.setId(q.getId());
        question.setAnswerType(q.getAnswerType());
        question.setPseudoSparqlQuery(q.getPseudoSparqlQuery());
        question.setSparqlQuery(q.getSparqlQuery());
        question.setAggregation(Boolean.TRUE.equals(q.getAggregation()));
        question.setOnlydbo(Boolean.TRUE.equals(q.getOnlydbo()));
        question.setOutOfScope(Boolean.TRUE.equals(q.getOutOfScope()));
        question.setHybrid(Boolean.TRUE.equals(q.getHybrid()));

        question.setLanguageToQuestion(q.getLanguageToQuestion());
        question.setLanguageToKeywords(q.getLanguageToKeywords());
        question.setGoldenAnswers(q.getGoldenAnswers());
        return question;
    }

    public static List<Question> createInstances(List<IQuestion> qList) {
        ArrayList<Question> hq = new ArrayList<Question>();
        for (IQuestion q : qList) {
            hq.add(QuestionFactory.createInstance(q));
        }
        return hq;
    }
}
