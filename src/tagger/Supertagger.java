/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tagger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import ling.Sentence;
import ling.Token;
import static org.apache.commons.math3.linear.MatrixUtils.createRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author hiroki
 */

final public class Supertagger {
    final private int BEAM_WIDTH;
    final private int N_LABELS;
    final private boolean IS_HASH;
    final private MatrixPerceptron perceptron;
    final private SequentialFeaturizer featurizer;
    final private ArrayList<Integer> trainIndex = new ArrayList<>();
    private Sentence[] trainData;
    private double[][] weight;
    private int[][][] cacheFeatureID;
    private Hypothesis[] cacheOracleHypo;

    float total = 0.0f;
    float correct = 0.0f;
    
    public Supertagger(int beamWidth, int numLabels, int weightSize, int windowSize, boolean isHash) {
        BEAM_WIDTH = beamWidth;
        N_LABELS = numLabels;
        IS_HASH = isHash;
        perceptron = new MatrixPerceptron(numLabels, weightSize);
        featurizer = new SequentialFeaturizer(weightSize, windowSize, IS_HASH);
    }
    
    final public void train() {
        Collections.shuffle(trainIndex);
        
        total = 0.0f;
        correct = 0.0f;

        for(int i=0; i<trainData.length; ++i){
            if (i % 1000 == 0 && i != 0) System.out.print(String.format("%d ", i));

            int index = trainIndex.get(i);
            Sentence sent = trainData[index];
            int[][] featureID = cacheFeatureID[index];
            Hypothesis oracleHypo = cacheOracleHypo[index];
            Hypothesis[] reversedOracleHypo = oracleHypo.reverse(sent.N_TOKENS);
            Hypothesis[] beam = setMaxViolationPoint(sent, reversedOracleHypo, featureID);
            perceptron.updateWeights();
            checkAccuracy(oracleHypo, beam[0]);
        }
        
        System.out.println(String.format("\n\tacc:%f  crr:%f  ttl:%f", correct / total, correct, total));
    }
    
    final public void train(Sentence[] trainSents) {
        trainData = trainSents;
        cacheFeatureID = new int[trainData.length][][];
        cacheOracleHypo = new Hypothesis[trainData.length];

        for (int i=0; i<trainData.length; ++i) trainIndex.add(i);
        Collections.shuffle(trainIndex);
        
        total = 0.0f;
        correct = 0.0f;

        for(int i=0; i<trainData.length; ++i){
            if (i % 1000 == 0 && i != 0) System.out.print(String.format("%d ", i));

            int index = trainIndex.get(i);
            Sentence sent = trainData[index];
            int[][] featureID = getFeatureID(sent);
            Hypothesis oracleHypo = getOracleHypo(sent, featureID);
            Hypothesis[] rOracleHypo = oracleHypo.reverse(sent.N_TOKENS);
            Hypothesis[] beam = setMaxViolationPoint(sent, rOracleHypo, featureID);
            perceptron.updateWeights();
            checkAccuracy(oracleHypo, beam[0]);
            cacheFeatureID[index] = featureID;
            cacheOracleHypo[index] = oracleHypo;
        }
        
        System.out.println(String.format("\n\tacc:%f  crr:%f  ttl:%f", correct / total, correct, total));
    }
    
    final public void test(Sentence[] testSents, String outputFileName) throws IOException {
        averageWeight();
        Hypothesis[] results = new Hypothesis[testSents.length];

        total = 0.0f;
        correct = 0.0f;

        for(int i=0; i<testSents.length; ++i) {
            if (i % 1000 == 0 && i != 0) System.out.print(String.format("%d ", i));

            Sentence sent = testSents[i];
            Hypothesis[] beam = decode(sent);
            checkAccuracy(sent, beam[0]);
            results[i] = beam[0];
        }
        
        System.out.println(String.format("\n\tacc:%f  crr:%f  ttl:%f", correct / total, correct, total));
        
        if (outputFileName != null) outputResultingText(outputFileName, testSents, results);
    }
    
    private Hypothesis[] decode(Sentence sent) {
        Hypothesis[] beam = new Hypothesis[BEAM_WIDTH];
        beam[0] = new Hypothesis();

        for (int t=0; t<sent.N_TOKENS; ++t) {
            beam = beamSearch(t, beam, sent);
        }
        
        return beam;
        
    }

    private void checkAccuracy(Hypothesis oracle, Hypothesis system) {
        Hypothesis oh = oracle;
        Hypothesis sh = system;
        while(oh.label > -1) {
            if (oh.label == sh.label) correct += 1.0;
            total += 1.0;
                
            oh = oh.prevHypo;
            sh = sh.prevHypo;
        }
    }

    private void checkAccuracy(Sentence sent, Hypothesis system) {
        Hypothesis[] sh = system.reverse(sent.N_TOKENS);
        for (int t=0; t<sent.N_TOKENS; ++t) {
            int label = sh[t].label;
            if (label == sent.stags[t]) correct += 1.0;
            total += 1.0;
        }
    }
    
    private void outputResultingText(String fn, Sentence[] sents, Hypothesis[] results) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fn + ".conll")));

        for (int i=0; i<results.length; ++i) {
            Sentence sent = sents[i];
            Hypothesis[] h = results[i].reverse(sent.N_TOKENS);

            for (int j=0; j<h.length; ++j) {
                Token t = sent.tokens[j];
                String stag = Sentence.stagDict.getKey(h[j].label);
                String text = String.format("%d\t%s\t_\t%s\t%s\t%s\t%d\t%s\t_\t_", t.INDEX, t.O_FORM, t.O_CPOS, t.O_POS, stag, t.O_HEAD, t.O_LABEL);
                pw.println(text);
            }
            pw.println();
        }
        pw.close();
    }

    private Hypothesis getOracleHypo(Sentence sent, int[][] featureID) {
        return updateHypo(0, sent, featureID, new Hypothesis());
    }
    
    private Hypothesis updateHypo(int t, Sentence sent, int[][] featureID, Hypothesis prevHypo) {
        if (t == sent.N_TOKENS) return prevHypo;

        int label = sent.stags[t];
        int[] featureID_t = featureID[t];
        int[] markovFeatureID = featurizer.featurize(sent, t, prevHypo.label, prevHypo.prevHypo.label);
        double score = getScore(featureID_t, label) + getScore(markovFeatureID, label);

        return updateHypo(t + 1, sent, featureID, new Hypothesis(label, score, featureID_t, markovFeatureID, prevHypo));
    }
    
    private double getScore(int[] features, int label) {
        return perceptron.getScore(features, label);
    }

    private Hypothesis[] setMaxViolationPoint(Sentence sent, Hypothesis[] oracleHypo, int[][] featureID) {
        Hypothesis[] beam = new Hypothesis[BEAM_WIDTH];
        beam[0] = new Hypothesis();

        for (int t=0; t<sent.N_TOKENS; ++t) {
            beam = beamSearch(t, beam, sent, featureID);
            perceptron.setMaxViolationPoint(t, oracleHypo[t], beam[0]);
        }
        
        return beam;
    }
    
    private Hypothesis[] beamSearch(int t, Hypothesis[] beam, Sentence sent, int[][] featureID) {
        HypoComparator orderedHypoQueue = new HypoComparator(BEAM_WIDTH);

        int[] featureID_t = featureID[t];
        RealVector featureScore = perceptron.getScore(featureID_t);
        int[][] markovFeature = null;
        String[] markovStrFeature = null;
        if (IS_HASH)
            markovFeature = featurizer.getMarkovFeature(sent, t);
        else
            markovStrFeature = featurizer.getStrMarkovFeature(sent, t);

        for (int k=0; k<BEAM_WIDTH; ++k) {
            Hypothesis hypo = beam[k];
                
            if (hypo == null) break;
                
            int[] markovFeatureID;
            if (IS_HASH)
                markovFeatureID = featurizer.getFeatureID(markovFeature, hypo.label, hypo.prevHypo.label);
            else
                markovFeatureID = featurizer.getFeatureID(markovStrFeature, hypo.label, hypo.prevHypo.label, false);
            RealVector markovFeatureScore = perceptron.getScore(markovFeatureID);
            double[] score = featureScore.add(markovFeatureScore).toArray();
                
            for (int n=0; n<N_LABELS; ++n)
                orderedHypoQueue.addSort(n, score[n], featureID_t, markovFeatureID, hypo);
        }
            
        for (int k=0; k<BEAM_WIDTH; ++k) {
            beam[k] = orderedHypoQueue.get(k);
            if (k == orderedHypoQueue.size()-1) break;
        }
        
        return beam;
    }
    
    private Hypothesis[] beamSearch(int t, Hypothesis[] beam, Sentence sent) {
        HypoComparator orderedHypoQueue = new HypoComparator(BEAM_WIDTH);

        int[] featureID_t;
        if (IS_HASH)
            featureID_t = featurizer.getFeatureID(featurizer.getFeature(sent, t));
        else
            featureID_t = featurizer.getFeatureID(featurizer.getStrFeature(sent, t), false);

        int[][] markovFeature = null;
        String[] markovStrFeature = null;
        if (IS_HASH)
            markovFeature = featurizer.getMarkovFeature(sent, t);
        else
            markovStrFeature = featurizer.getStrMarkovFeature(sent, t);
        
        RealVector featureScore = getScore(featureID_t);

        for (int k=0; k<BEAM_WIDTH; ++k) {
            Hypothesis hypo = beam[k];
                
            if (hypo == null) break;
                
            int[] markovFeatureID;
            if (IS_HASH)
                markovFeatureID = featurizer.getFeatureID(markovFeature, hypo.label, hypo.prevHypo.label);
            else
                markovFeatureID = featurizer.getFeatureID(markovStrFeature, hypo.label, hypo.prevHypo.label, true);
            RealVector markovFeatureScore = getScore(markovFeatureID);
            double[] score = featureScore.add(markovFeatureScore).toArray();
                
            for (int n=0; n<N_LABELS; ++n)
                orderedHypoQueue.addSort(n, score[n], featureID_t, markovFeatureID, hypo);
        }
            
        for (int k=0; k<BEAM_WIDTH; ++k) {
            beam[k] = orderedHypoQueue.get(k);
            if (k == orderedHypoQueue.size()-1) break;
        }
        
        return beam;
    }
    
    private RealVector getScore(int[] featureID) {
        RealVector score = createRealVector(new double[N_LABELS]);
        for (int i=0; i<featureID.length; ++i)
            score = score.add(createRealVector(weight[featureID[i]]));
        return score;
    }
    
    private int[][] getFeatureID(Sentence sent) {
        int[][] featureID = new int[sent.N_TOKENS][];
        for (int t=0; t<sent.N_TOKENS; ++t)
            featureID[t] = featurizer.featurize(sent, t);
        return featureID;
    }
    
    private void averageWeight() {
        double t = perceptron.t;
        double[][] w = perceptron.weight;
        double[][] aw = perceptron.aweight;

        this.weight = new double[w.length][w[0].length];

        for (int i = 0; i<w.length; i++) {
            for (int j = 0; j<w[i].length; j++) {
                this.weight[i][j] = w[i][j] - aw[i][j] /t;
            }
        }
        
    }
}
