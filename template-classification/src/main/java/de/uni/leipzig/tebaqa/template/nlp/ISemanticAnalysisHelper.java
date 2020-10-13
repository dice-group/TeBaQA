package de.uni.leipzig.tebaqa.template.nlp;

import de.uni.leipzig.tebaqa.template.model.Cluster;
import de.uni.leipzig.tebaqa.template.model.QueryTemplateMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ISemanticAnalysisHelper {
    HashMap<String, String> getPosTags(String text);

    Map<String, QueryTemplateMapping> mapGraphToTemplates(List<Cluster> queryClusters);
}
