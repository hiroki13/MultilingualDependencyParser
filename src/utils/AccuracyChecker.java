/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import ling.Sentence;
import ling.Token;
import parser.Parser;
import parser.ArcStandard;
import parser.State;
import parser.Perceptron;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

/**
 *
 * @author hiroki
 */

final public class AccuracyChecker {
    public float correct;
    public float total;
    public Parser parser;
    public long time;
    
    public AccuracyChecker(){
        this.correct = 0.0f;
        this.total = 0.0f;
    }

    final public void checkUAS(Sentence sentence, State state){
        final int[] heads = sentence.arcs;
        final int[] arcs = state.arcs;

        for (int d=1; d<arcs.length; ++d) {            
            int h = arcs[d];
            if (h == heads[d]) correct += 1.0f;
            total += 1.0f;
        }
    }
    
    final public void test(Sentence[] testData, Parser parser){
        this.parser = new ArcStandard(parser.BEAM_WIDTH, parser.perceptron.weight[0].length, parser.STAG);
        this.parser.featurizer = parser.featurizer;
        this.parser.perceptron.weight = averagingWeights(parser);
        this.parser.BEAM_WIDTH = parser.BEAM_WIDTH;
        time = (long) 0.0;
        
        for (int i=0; i<testData.length; i++){
            Sentence sentence = testData[i];
            long time1 = System.currentTimeMillis();
            State[] beam = this.parser.decode(sentence);
            long time2 = System.currentTimeMillis();
            time += time2 - time1;

            this.checkUAS(sentence, beam[0]);
        }
    }
    
    final public void output(String fn, Sentence[] testData, Parser parser, boolean isOutput) throws IOException{
        this.parser = new ArcStandard(parser.BEAM_WIDTH, parser.perceptron.weight[0].length, parser.STAG);

        State[][] beams = new State[testData.length][parser.BEAM_WIDTH];
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fn+".conll")));
        PrintWriter scoref = new PrintWriter(new BufferedWriter(new FileWriter(fn+".parsescore")));
        
        this.parser.featurizer = parser.featurizer;
        this.parser.perceptron.weight = averagingWeights(parser);
        this.parser.BEAM_WIDTH = parser.BEAM_WIDTH;
        time = (long) 0.0;
        
        int counter = 0;
        for (int i=0; i<testData.length; i++){
            Sentence sentence = testData[i];
            long time1 = System.currentTimeMillis();
            State[] beam = this.parser.decode(sentence);
            long time2 = System.currentTimeMillis();
            time += time2 - time1;
            beams[i] = beam;
            
            final int[] arcs = beam[0].arcs;
            final float score = beam[0].score;
            final Token[] tokens = sentence.tokens;
            
            for (int j=1; j<tokens.length; j++){
                Token t = tokens[j];
                String text = String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t_\t_\t_", t.INDEX, t.O_FORM, t.O_CPOS, t.O_POS, arcs[j]);
                pw.println(text);
            }
            scoref.println(score);
            pw.println();
            
            if (counter%100 == 0 && counter != 0)
                System.out.print(counter + " ");
            counter++;
        }
        pw.close();
        scoref.close();
        ObjectOutputStream beamStream = new ObjectOutputStream(new FileOutputStream(fn+"_beam.bin"));
        beamStream.writeObject(beams);
        beamStream.close();
        
        if (isOutput) outputPerceptron(fn);
    }
    
    
    private void outputPerceptron(String fn) throws IOException{
        ObjectOutputStream objOutStream = new ObjectOutputStream(new FileOutputStream(fn + "_param.bin"));
        objOutStream.writeObject(parser.perceptron);
        objOutStream.close();
    }
    
    private float[][] averagingWeights(Parser parser){
        Perceptron p = parser.perceptron;
        float[][] w = p.weight;
        float[][] aw = p.aweight;
        float t = p.t;
        float[][] new_weight = new float[w.length][w[0].length];

        for (int i = 0; i<w.length; ++i) {
            for (int j = 0; j<w[0].length; ++j)
                new_weight[i][j] = w[i][j] - aw[i][j] / t;
        }

        return new_weight;
    }
}
