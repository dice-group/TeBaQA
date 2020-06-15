package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import de.uni.leipzig.tebaqa.controller.PipelineControllerTripleTemplates;
import org.aksw.qa.commons.load.Dataset;

import java.util.ArrayList;
import java.util.List;

public class PipelineProvider {
    private static PipelineController pipeline;
    private static PipelineControllerTripleTemplates pipelinett;

    //do not instantiate
    private PipelineProvider() {
    }

    public static PipelineController getQAPipeline() {
        if (pipeline == null) {
            List<Dataset> trainDatasets = new ArrayList<>();
            trainDatasets.add(Dataset.QALD7_Train_Multilingual);
            trainDatasets.add(Dataset.QALD8_Train_Multilingual);
            trainDatasets.add(Dataset.QALD7_Test_Multilingual);
            trainDatasets.add(Dataset.QALD8_Test_Multilingual);
            pipeline = new PipelineController(trainDatasets);
        }
        return pipeline;
    }
    public static PipelineController getQAPipelinecalculateModelPipeline() {
        if (pipeline == null) {
            List<Dataset> trainDatasets = new ArrayList<>();
            trainDatasets.add(Dataset.QALD7_Train_Multilingual);
            trainDatasets.add(Dataset.QALD8_Train_Multilingual);
            trainDatasets.add(Dataset.QALD7_Test_Multilingual);
            trainDatasets.add(Dataset.QALD8_Test_Multilingual);
            List<Dataset> testDatasets = new ArrayList<>();
            //trainDatasets.add(Dataset.QALD7_Test_Multilingual);
            testDatasets.add(Dataset.QALD7_Train_Multilingual);
            testDatasets.add(Dataset.QALD8_Train_Multilingual);
            testDatasets.add(Dataset.QALD8_Test_Multilingual);
            pipeline = new PipelineController(trainDatasets,testDatasets);
        }
        return pipeline;
    }
    public static PipelineControllerTripleTemplates getQAPipelineTripleTemplates() {
        if (pipeline == null) {
            List<Dataset> trainDatasets = new ArrayList<>();
            trainDatasets.add(Dataset.QALD7_Train_Multilingual);
            trainDatasets.add(Dataset.QALD8_Train_Multilingual);
            pipelinett = new PipelineControllerTripleTemplates(trainDatasets);
        }
        return pipelinett;
    }
}
