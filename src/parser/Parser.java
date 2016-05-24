/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import ling.Sentence;
import java.util.ArrayList;
import ling.Token;

/**
 *
 * @author hiroki
 */

public class Parser {
    public int BEAM_WIDTH, LABEL_SIZE;
    public boolean STAG;
    public float[][] weight;
    public Perceptron perceptron;
    public Featurizer featurizer;
    public Sentence[] trainData;
    public ArrayList<Integer> trainIndex = new ArrayList<>();
    public ArrayList<State>[] oracles;
    public int n;

    public float correct;
    public float total;
    public long time;

    public Parser(){}

    public void train(Sentence[] sents){}    

    public void train(){}    

    public void decode(Sentence sentence, ArrayList<State> reversedOracle){
        State initState = new State(sentence.N_TOKENS);
        initState.s0 = 0;
        initState.b0 = 1;
        State[] beam = new State[BEAM_WIDTH];
        beam[0] = initState;
        int i = 0;
        
        while(beam[0].s0 != 0 || beam[0].b0 < sentence.size()){
            this.n = 0;
            PredictedAction[] queue = new PredictedAction[BEAM_WIDTH];

            for (State state:beam){
                if (state == null) break;

                ArrayList feature = featurizer.featurize(sentence, state);
                ArrayList<PredictedAction> predActions = predictAction(state, getValidAction(sentence, state), feature);
                
                for (int j=0; j<predActions.size(); j++)
                    queue = addSort(queue, predActions.get(j));
            }
            
            beam = executeActions(queue);

            if (i < reversedOracle.size())
                perceptron.setMaxViolationPoint(reversedOracle.get(i), beam[0]);
            i += 1;
        }
        
    }
    
    public State[] decode(Sentence sentence){
        State[] beam = new State[BEAM_WIDTH];
        State initState = new State(sentence.N_TOKENS);
        initState.s0 = 0;
        initState.b0 = 1;
        beam[0] = initState;
        
        while(beam[0].s0 != 0 || beam[0].b0 < sentence.size()){
            this.n = 0;
            PredictedAction[] queue = new PredictedAction[BEAM_WIDTH];

            for (State state:beam){
                if (state == null) break;

                ArrayList feature = featurizer.featurize(sentence, state);
                ArrayList<PredictedAction> predActions = predictTestAction(state, getValidAction(sentence, state), feature);
                
                for (int j=0; j<predActions.size(); j++)
                    queue = addSort(queue, predActions.get(j));
            }
            
            beam = executeActions(queue);
        }
        
        return beam;
    }

    private boolean[] getValidAction(Sentence sentence, State state){
        boolean[] validAction = {true, true, true};

        // Shift
        if (state.b0 >= sentence.size()) validAction[0] = false;

        // Left-Arc
        if (state.tail != null) {
            if (state.tail.s0 <= 0) validAction[1] = false;
        }
        else validAction[1] = false;

        //Right-Arc
        if (state.tail == null) validAction[2] = false;
        else if (state.tail.s0 == 0 && state.b0 < sentence.size()) validAction[2] = false;

        return validAction;
    }
    
    public ArrayList<PredictedAction> predictAction(State state, boolean[] validAction, ArrayList<Integer> feature){
        return new ArrayList<>();
    }

    public ArrayList<PredictedAction> predictTestAction(State state, boolean[] validAction, ArrayList<Integer> feature){
        return new ArrayList<>();
    }
    
    private State[] executeActions(PredictedAction[] queue) {
        State[] beam = new State[BEAM_WIDTH];
        for (int i=0; i<this.n; ++i)
            beam[i] = executeAction(queue[i]);
        return beam;
    }
    
    public State executeAction(PredictedAction predAction){
        return new State();
    }
    
    private PredictedAction[] addSort(PredictedAction[] queue, PredictedAction action) {
        if (this.n < BEAM_WIDTH) { 
            queue[this.n++] = action;
            queue = arrangeOrder(queue);
        }
        else{
            if (queue[BEAM_WIDTH-1].score < action.score) {
                queue[BEAM_WIDTH-1] = action;
                queue = arrangeOrder(queue);
            }
        }
        return queue;
    }

    private PredictedAction[] arrangeOrder(PredictedAction[] queue) {
        if (this.n == 1) return queue;
        for (int i=this.n-2; i>-1; i--) {
            if (queue[i].score < queue[i+1].score) {
                PredictedAction tmp = queue[i];
                queue[i] = queue[i+1];
                queue[i+1] = tmp;
            }
            else return queue;
        }
        return queue;
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
    
    final public void test(Sentence[] testData, String outputFileName) throws IOException {
        total = 0.0f;
        correct = 0.0f;
        time = (long) 0.0;

        averageWeight();
        State[] results = new State[testData.length];
        
        for (int i=0; i<testData.length; i++){
            Sentence sentence = testData[i];
            long time1 = System.currentTimeMillis();
            State[] beam = decode(sentence);
            long time2 = System.currentTimeMillis();
            time += time2 - time1;
            
            results[i] = beam[0];
            checkUAS(sentence, beam[0]);
        }
        
        if (outputFileName != null) output(outputFileName, testData, results);
    }

    final public void output(String fn, Sentence[] testData, State[] results) throws IOException{
        PrintWriter sf = new PrintWriter(new BufferedWriter(new FileWriter(fn + ".system.conll")));
        PrintWriter gf = new PrintWriter(new BufferedWriter(new FileWriter(fn + ".gold.conll")));

        for (int i=0; i<testData.length; i++){
            Sentence sent = testData[i];
            State result = results[i];
            int[] arcs = result.arcs;
            String[] labeledArcs = null;
            if (LABEL_SIZE > 0) labeledArcs = getLabeledArcs(arcs.length, result);
 
            for (int j=1; j<sent.N_TOKENS; ++j) {
                Token t = sent.tokens[j];
                String sText, gText;
                if (labeledArcs == null)
                    sText = String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t_\t_\t_", t.INDEX, t.O_FORM, t.O_CPOS, t.O_POS, arcs[j]);
                else
                    sText = String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t%s\t_\t_", t.INDEX, t.O_FORM, t.O_CPOS, t.O_POS, arcs[j], labeledArcs[j]);
                gText = String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t%s\t_\t_", t.INDEX, t.O_FORM, t.O_CPOS, t.O_POS, t.O_HEAD, t.O_LABEL);
                sf.println(sText);
                gf.println(gText);
            }
            sf.println();
            gf.println();
        }
        sf.close();
        gf.close();
    }
    
    private String[] getLabeledArcs(int nTokens, State state) {
        String[] labeledArcs = new String[nTokens];
        ArrayList<State> states = state.reverseState();
        
        for (int i=0; i<states.size(); ++i) {
            State s = states.get(i);
            if (s.LAST_ACTION == 0) {}
            else if (s.LAST_ACTION < LABEL_SIZE)
                labeledArcs[s.prevState.tail.s0] = Token.vocabLabel.getKey(s.LAST_ACTION - 1);
            else
                labeledArcs[s.prevState.s0] = Token.vocabLabel.getKey((s.LAST_ACTION - 1) % LABEL_SIZE);
        }
        return labeledArcs;
    }
    
    final public float calcScore(ArrayList<Integer> usedFeatures, int action) {
        float score = 0.0f;
        float[] w = weight[action];
        for (int i=0; i<usedFeatures.size(); ++i)
            score += w[usedFeatures.get(i)];
        return score;
    }
    
    final public void averageWeight(){
        float[][] w = perceptron.weight;
        float[][] aw = perceptron.aweight;
        float t = perceptron.t;

        weight = new float[w.length][w[0].length];

        for (int i = 0; i<w.length; ++i) {
            for (int j = 0; j<w[0].length; ++j)
                weight[i][j] = w[i][j] - aw[i][j] / t;
        }
    }

}
    
    

