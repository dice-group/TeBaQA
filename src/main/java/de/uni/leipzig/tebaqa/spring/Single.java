package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.controller.PipelineController;

import java.util.ArrayList;
import java.util.List;

import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipeline;

public class Single {
    public static void main(String[]args){
        PipelineController qaPipeline=getQAPipeline();
        List<String> q=new ArrayList<String>();
        q.add("What is Donald Trump's main business?");
        q.add("What is the last work of Dan Brown?");
        q.add("What other books have been written by the author of The Fault in Our Stars?");
        q.add("When was the last episode of the TV series Friends aired?");
        q.add("What is the original title of the interpretation of dreams?");
        q.add("With how many countries Iran has borders?");
        q.add("Where is Sungkyunkwan University?");
        q.add("What is the smallest city by area in Germany?");
        q.add("Who are the founders of  BlaBlaCar?");
        q.add("Which beer brewing comapnies are located in North-Rhine Westphalia?");
        q.add("how many foreigners speak German?");
        q.add("Who is the current federal minister of finance in Germany?");
        q.add("Where is the birthplace of Goethe?");
        q.add("Which species does an elephant belong?");
        //q.forEach(s->qaPipeline.answerQuestion(s));
        qaPipeline.answerQuestion("When was the last episode of the TV series Friends aired?");
    }
}
