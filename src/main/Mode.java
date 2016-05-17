/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import utils.OptionParser;
import utils.Reader;
import utils.AccuracyChecker;
import ling.Sentence;
import parser.Parser;
import parser.ArcStandard;
import parser.Perceptron;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import ling.Dict;
import ling.Token;
import tagger.Supertagger;

/**
 *
 * @author hiroki
 */

final public class Mode {
    final OptionParser optionparser;
    final String selectedparser, modeselect;    
    final String TRAIN_FILE, TEST_FILE, OUTPUT_FILE, MODEL_FILE;    
    final int ITERATION, BEAM_WIDTH, WEIGHT_SIZE, WINDOW_SIZE, DATA_SIZE;
    final boolean STAG;
    Sentence[] trainData, testData;

    public Mode(String[] args) {
        optionparser = new OptionParser(args);

        assert optionparser.isExsist("parser"): "Enter -parser arcstandard/supertagger";
        assert optionparser.isExsist("mode"): "Enter -mode train/test";        
        selectedparser = optionparser.getString("parser");
        modeselect = optionparser.getString("mode");

        assert optionparser.isExsist("train"): "Enter -train filename";
        TRAIN_FILE = optionparser.getString("train");
        TEST_FILE = optionparser.getString("test");
        OUTPUT_FILE = optionparser.getString("output", "output");
        MODEL_FILE = optionparser.getString("model", "model");

        ITERATION = optionparser.getInt("iter", 10);
        BEAM_WIDTH = optionparser.getInt("beam", 1);
        WEIGHT_SIZE = optionparser.getInt("weight_size", 50000);
        WINDOW_SIZE = optionparser.getInt("window_size", 7);
        DATA_SIZE = optionparser.getInt("data_size", 100000);
        STAG = optionparser.isExsist("stag");
    }
    
    public void setup() throws Exception{
        boolean isStag = false;
        Reader reader = new Reader();

        if (!"arcstandard".equals(selectedparser)) isStag = true;

        System.out.println("Loeading files...");
        trainData = reader.read(TRAIN_FILE, DATA_SIZE, isStag, false);
        testData = reader.read(TEST_FILE, DATA_SIZE, isStag, true);

        System.out.println(String.format("\tTrain Sents: %d", trainData.length));
        if (testData != null)
            System.out.println(String.format("\tTest Sents: %d", testData.length));

        if ("arcstandard".equals(selectedparser)) {
            Dict a = Token.vocabStag;
            System.out.println(String.format("\nSupertags: %d", Token.vocabStag.values.size()));
            Parser parser = new ArcStandard(BEAM_WIDTH, WEIGHT_SIZE, STAG);
            if ("train".equals(modeselect)) train(parser);
            else test();            
        }
        else {
            Supertagger supertagger = new Supertagger(BEAM_WIDTH, Sentence.stagDict.size(), WEIGHT_SIZE, WINDOW_SIZE);
            System.out.println(String.format("\nSupertags: %d", Sentence.stagDict.size()));
            if ("train".equals(modeselect)) train(supertagger);
            else test();                        
        }

    }

    private void train(Parser parser) throws Exception {
        System.out.println("\nLearning START");
    
        for(int i=0; i<ITERATION; ++i) {
            System.out.println(String.format("\nIteration %d: ", i+1));
            long time1 = System.currentTimeMillis();

            if (i == 0) parser.train(trainData);
            else parser.train();
                
            long time2 = System.currentTimeMillis();
            System.out.println("\n\tTraining Time: " + (time2 - time1) + " ms");

            if (testData == null) continue;

            AccuracyChecker checker = new AccuracyChecker();
            checker.test(testData, parser);                
            double time = testData.length / ((double) checker.time);

            System.out.println(String.format("\tUAS:%.7s  CORRECT:%d  TOTAL:%d  TIME:%.6s sents/sec",
                                            checker.correct/checker.total,
                                            (int) checker.correct,
                                            (int) checker.total,
                                            time * 1000));

            if (i+1 == ITERATION)
                checker.output(OUTPUT_FILE, testData, parser, true);
        }
    }

    private void train(Supertagger supertagger) throws Exception{
        System.out.println(String.format("\nBEAM:%d  WINDOW:%d", BEAM_WIDTH, WINDOW_SIZE));
        System.out.println("\nLearning START");
            
        for(int i=0; i<ITERATION; ++i) {
            System.out.println(String.format("\nIteration %d: ", i+1));

            long time1 = System.currentTimeMillis();
            if (i == 0) supertagger.train(trainData);
            else supertagger.train();                
            long time2 = System.currentTimeMillis();
            System.out.println("\n\tTraining Time: " + (time2 - time1) + " ms");
            
            time1 = System.currentTimeMillis();
            supertagger.test(testData);
            time2 = System.currentTimeMillis();
            System.out.println("\n\tTest Time: " + (time2 - time1) + " ms");
        }
    }

    private void test() throws IOException, ClassNotFoundException{
        Reader reader = new Reader();
        
        System.out.println("Model Loaded...");
        ObjectInputStream perceptronStream = new ObjectInputStream(new FileInputStream(MODEL_FILE + "_param.bin"));      
        Perceptron perceptron = (Perceptron) perceptronStream.readObject();
        perceptronStream.close();
        System.out.println("Model Loading Completed\n");

        ArcStandard parser = new ArcStandard(BEAM_WIDTH, perceptron, STAG);
        parser.BEAM_WIDTH = BEAM_WIDTH;

        System.out.println("Parsing START");                
        AccuracyChecker checker = new AccuracyChecker();

        checker.output(OUTPUT_FILE, testData, parser, false);                

        double time = testData.length / ((double) checker.time);

        System.out.println(String.format("\tTIME:%.6s sent./sec.", time*1000));            
    }    
}
