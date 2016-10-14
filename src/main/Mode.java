/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import utils.OptionParser;
import utils.Reader;
import ling.Token;
import ling.Sentence;
import tagger.Supertagger;
import parser.Parser;
import parser.ArcStandard;
import parser.LabeledArcStandard;
import parser.Perceptron;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import ling.Dict;

/**
 *
 * @author hiroki
 */

public class Mode {
    final OptionParser optionparser;
    final String selectedparser, modeselect;    
    final String TRAIN_FILE, TEST_FILE, OUTPUT_FILE, MODEL_FILE;    
    final int ITERATION, BEAM_WIDTH, WEIGHT_SIZE, WINDOW_SIZE, DATA_SIZE, N_SPLIT, STAG_ID;
    final boolean PARSER, IS_STAG, USE_GOLD_STAG, LABELED, IS_HASH;
    Sentence[] trainData, testData;

    public Mode(String[] args) {
        optionparser = new OptionParser(args);

        assert optionparser.isExsist("parser"): "Enter -parser arcstandard/supertagger";
        assert optionparser.isExsist("mode"): "Enter -mode train/test/jack";
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
        N_SPLIT = optionparser.getInt("split", 5);
        STAG_ID = optionparser.getInt("stag_id", 1);
        IS_STAG = optionparser.isExsist("stag") || !("arcstandard".equals(selectedparser));
        USE_GOLD_STAG = optionparser.isExsist("gold_stag");
        LABELED = optionparser.isExsist("labeled");
        IS_HASH = optionparser.isExsist("hash");
        PARSER = "arcstandard".equals(selectedparser);
    }
    
    public void setup() throws Exception{
        Reader reader = new Reader();

        Sentence.PARSER = PARSER;
        Sentence.STAG_ID = STAG_ID;
        Token.IS_GOLD_STAG = USE_GOLD_STAG;

        System.out.println("Loeading files...");
        trainData = reader.read(TRAIN_FILE, DATA_SIZE, PARSER, false);
        testData = reader.read(TEST_FILE, DATA_SIZE, PARSER, true);

        System.out.println(String.format("\tTrain Sents: %d", trainData.length));
        if (testData != null)
            System.out.println(String.format("\tTest Sents: %d", testData.length));
        
        if ("jack".equals(modeselect)) {
            System.out.println(String.format("\nStag ID: %d  Stags: %d", STAG_ID, Sentence.stagDict.size()));
            jackknife();
        }
        else if ("arcstandard".equals(selectedparser)) {
            int labelSize = Token.vocabLabel.values.size();
            System.out.println(String.format("\nStagID: %d  Stags: %d  Labels: %d", STAG_ID, Token.vocabStag.values.size(), labelSize));

            Parser parser;
            if (LABELED) {
                parser = new LabeledArcStandard(BEAM_WIDTH, WEIGHT_SIZE, labelSize, IS_STAG);
                System.out.println("\nLABELED DEPENDENCY PARSER");
            }
            else {
                parser = new ArcStandard(BEAM_WIDTH, WEIGHT_SIZE, IS_STAG);
                System.out.println("\nUNLABELED DEPENDENCY PARSER");
            }

            if ("train".equals(modeselect)) train(parser);
            else test();            
        }
        else {
            Supertagger supertagger = new Supertagger(BEAM_WIDTH, Sentence.stagDict.size(), WEIGHT_SIZE, WINDOW_SIZE, IS_HASH);
            System.out.println(String.format("\nStag ID:%d  Stags: %d", STAG_ID, Sentence.stagDict.size()));
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
            
            if (i+1 == ITERATION) parser.test(testData, OUTPUT_FILE);
            else parser.test(testData, null);

            double time = testData.length / ((double) parser.time);
            System.out.println(String.format("\tUAS:%.7s  CORRECT:%d  TOTAL:%d  TIME:%.6s sents/sec",
                                            parser.correct/parser.total,
                                            (int) parser.correct,
                                            (int) parser.total,
                                            time * 1000));
        }
    }

    private void train(Supertagger supertagger) throws Exception{
        System.out.println(String.format("\nBEAM:%d  WINDOW:%d", BEAM_WIDTH, WINDOW_SIZE));
        System.out.println("\nLearning START");
        Dict d = Sentence.stagDict;
            
        for(int i=0; i<ITERATION; ++i) {
            System.out.println(String.format("\nIteration %d: ", i+1));

            long time1 = System.currentTimeMillis();
            if (i == 0) supertagger.train(trainData);
            else supertagger.train();                
            long time2 = System.currentTimeMillis();
            System.out.println("\n\tTraining Time: " + (time2 - time1) + " ms");
            
            time1 = System.currentTimeMillis();
            if (i == ITERATION - 1) supertagger.test(testData, OUTPUT_FILE);
            else supertagger.test(testData, null);
            time2 = System.currentTimeMillis();
            System.out.println("\n\tTest Time: " + (time2 - time1) + " ms");
        }
    }

    private void jackknife() throws Exception{
        System.out.println(String.format("\nBEAM:%d  WINDOW:%d", BEAM_WIDTH, WINDOW_SIZE));
        System.out.println("\nLearning START");

        Sentence[][] splitDataSet = splitData(trainData);
        Sentence[][] trainDataSet = new Sentence[N_SPLIT][];
        
        for (int i=0; i<N_SPLIT; ++i) {
            Sentence[] tmpTrainData = new Sentence[trainData.length - splitDataSet[i].length];

            int prevSentLen = 0;            
            for (int j=0; j<N_SPLIT; ++j) {
                if (i == j) continue;
                Sentence[] tmpData = splitDataSet[j];
                
                for (int k=0; k<tmpData.length; ++k)
                    tmpTrainData[prevSentLen + k] = tmpData[k];
                prevSentLen += tmpData.length;
            }
            
            trainDataSet[i] = tmpTrainData;
        }

        for (int k=0; k<N_SPLIT; ++k) {
            System.out.println(String.format("\nJacknife %d: ", k+1));
            Supertagger supertagger = new Supertagger(BEAM_WIDTH, Sentence.stagDict.size(), WEIGHT_SIZE, WINDOW_SIZE, IS_HASH);

            for(int i=0; i<ITERATION; ++i) {
                System.out.println(String.format("\nIteration %d: ", i+1));

                long time1 = System.currentTimeMillis();
                if (i == 0) supertagger.train(trainDataSet[k]);
                else supertagger.train();                
                long time2 = System.currentTimeMillis();
                System.out.println("\n\tTraining Time: " + (time2 - time1) + " ms");
            
                time1 = System.currentTimeMillis();
                if (i == ITERATION - 1) supertagger.test(splitDataSet[k], OUTPUT_FILE + k);
                else supertagger.test(splitDataSet[k], null);
                time2 = System.currentTimeMillis();
                System.out.println("\n\tTest Time: " + (time2 - time1) + " ms");
            }
        }
    }
    
    private Sentence[][] splitData(Sentence[] data) {
        int mod = data.length % N_SPLIT;
        int split = data.length / N_SPLIT;

        Sentence[][] splitData = new Sentence[N_SPLIT][];
        
        for (int i=0; i<N_SPLIT; ++i) {
            if (i == N_SPLIT-1 && mod > 0)
                splitData[i] = Arrays.copyOfRange(data, i * split, data.length);
            else
                splitData[i] = Arrays.copyOfRange(data, i * split, (i+1) * split);
        }

        return splitData;
    }

    private void test() throws IOException, ClassNotFoundException{
        System.out.println("Model Loaded...");
        ObjectInputStream perceptronStream = new ObjectInputStream(new FileInputStream(MODEL_FILE + "_param.bin"));
        Perceptron perceptron = (Perceptron) perceptronStream.readObject();
        perceptronStream.close();
        System.out.println("Model Loading Completed\n");

        ArcStandard parser = new ArcStandard(BEAM_WIDTH, perceptron, IS_STAG);
        parser.BEAM_WIDTH = BEAM_WIDTH;
    }    
}
