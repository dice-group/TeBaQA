package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import org.aksw.qa.commons.load.Dataset;

import java.util.ArrayList;
import java.util.List;

public class PipelineProvider {
    private static PipelineController pipeline;

    //do not instantiate
    private PipelineProvider() {
    }

    public static PipelineController getQAPipeline() {
        if (pipeline == null) {
            List<Dataset> trainDatasets = new ArrayList<>();
            trainDatasets.add(Dataset.QALD7_Train_Multilingual);
            trainDatasets.add(Dataset.QALD8_Train_Multilingual);
            pipeline = new PipelineController(trainDatasets);
        }
        return pipeline;
    }
}
