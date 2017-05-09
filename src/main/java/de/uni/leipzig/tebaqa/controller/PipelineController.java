package de.uni.leipzig.tebaqa.controller;

import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.QALD_Loader;
import org.apache.log4j.Logger;

import java.util.List;

public class PipelineController {
    private static Logger log = Logger.getLogger(PipelineController.class);

    private Dataset dataset;

    public static void main(String args[]) {
        PipelineController controller = new PipelineController();
        log.info("Configuring controller");

        Dataset dataset = Dataset.QALD1_Test_dbpedia;
        controller.setDataset(dataset);
        log.info("Dataset: " + dataset);

        log.info("Running controller");
        controller.run();
    }

    private void run() {
        List<IQuestion> load = QALD_Loader.load(dataset);
        List<HAWKQuestion> questions = HAWKQuestionFactory.createInstances(load);
    }

    private void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
}
