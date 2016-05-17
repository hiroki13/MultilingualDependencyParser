/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parser;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author hiroki
 */

final public class Perceptron implements Serializable{
    final public int N_LABELS;
    public float[][] weight;
    public float[][] aweight;
    public float t = 1.0f;
    public State maxvOracleState;
    public State maxvSystemState;
    public float maxvScoreDiff;

    public Perceptron(int n_labels, int weight_size){
        this.N_LABELS = n_labels;
        this.weight = new float[n_labels][weight_size];
        this.aweight = new float[n_labels][weight_size];
        this.maxvOracleState = new State();
        this.maxvSystemState = new State();
        this.maxvScoreDiff = 100000.0f;
    }

    final public float calcScore(ArrayList<Integer> usedFeatures, int action) {
        float score = 0.0f;
        float[] w = weight[action];
        for (int i=0; i<usedFeatures.size(); ++i)
            score += w[usedFeatures.get(i)];
        return score;
    }
    
    final public void setMaxViolationPoint(State oracle, State state){
        float o_score = calcScore(oracle.feature, oracle.last_action);
        if (oracle.prev_state == null) oracle.score = o_score;
        else oracle.score = oracle.prev_state.score + o_score;

        float score_diff =  oracle.score - state.score;
        if (score_diff <= this.maxvScoreDiff){
            this.maxvOracleState = new State(oracle);
            this.maxvSystemState = new State(state);
            this.maxvScoreDiff = score_diff;
        }
    }
    
    final public void updateWeights(){
        ArrayList<State> oracle = this.maxvOracleState.reverseState();
        ArrayList<State> system = this.maxvSystemState.reverseState();
        int m, n;

        for(int i=0; i<oracle.size(); ++i) {
            State oracleState = oracle.get(i);
            State systemState = system.get(i);
            ArrayList<Integer> oracleFeature = oracleState.feature;
            ArrayList<Integer> systemFeature = systemState.feature;

            for (int j=0; j<oracleState.feature.size(); ++j) {
                m = oracleState.last_action;
                n = oracleFeature.get(j);
                this.weight[m][n] += 1.0;
                this.aweight[m][n] += t;
            }
            for (int j=0; j<systemState.feature.size(); ++j) {
                m = systemState.last_action;
                n = systemFeature.get(j);
                this.weight[m][n] -= 1.0;
                this.aweight[m][n] -= t;
            }
        }

        this.t += 1.0f;
        this.maxvOracleState = new State();
        this.maxvSystemState = new State();
        this.maxvScoreDiff = 100000.0f;
    }
}
