package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.KeyWordController;
import de.uni.leipzig.tebaqa.controller.PipelineController;
import org.aksw.qa.commons.load.Dataset;

import java.util.ArrayList;
import java.util.List;

public class KeyWordPipelineProvider {
    private static KeyWordController keyWordPipeline;

    public static KeyWordController getKeyWordPipeline() {
        if (keyWordPipeline == null) {
            keyWordPipeline = new KeyWordController();
        }
        return keyWordPipeline;
    }
}
