/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.ArrayList;
import java.util.Collections;
import ling.Sentence;

/**
 *
 * @author hiroki
 */

public class LabeledArcStandard extends Parser{

    public LabeledArcStandard(int beamWidth, int weightSize, int labelSize, boolean stag) {
        this.BEAM_WIDTH = beamWidth;
        this.LABEL_SIZE = labelSize;
        this.STAG = stag;
        this.perceptron = new Perceptron(labelSize * 2 + 1, weightSize);
        this.featurizer = new Featurizer(weightSize, stag);
    }

    public LabeledArcStandard(int beamWidth, Perceptron perceptron, boolean stag){
        this.BEAM_WIDTH = beamWidth;
        this.STAG = stag;
        this.perceptron = perceptron;
    }

    @Override
    public void train(Sentence[] trainSents){
        trainData = trainSents;
        oracles = new ArrayList[trainSents.length];
        OracleParser oracleParser = new OracleParser(featurizer, LABEL_SIZE);
        
        for (int i=0; i<trainData.length; ++i) trainIndex.add(i);
        Collections.shuffle(trainIndex);

        for(int i=0; i<trainData.length; ++i){
            if (i % 1000 == 0 && i != 0) System.out.print(String.format("%d ", i));

            Sentence sent = trainData[trainIndex.get(i)];
            State oracle = oracleParser.getLabeledOracle(sent);
            ArrayList<State> reversedOracle = oracle.reverseState();
            oracles[sent.INDEX] = reversedOracle;

            if (reversedOracle != null) {
                decode(sent, reversedOracle);
                perceptron.updateWeights();
            }
        }
    }

    @Override
    public void train(){
        Collections.shuffle(trainIndex);

        for(int i=0; i<trainData.length; ++i){
            if (i % 1000 == 0 && i != 0) System.out.print(String.format("%d ", i));

            Sentence sentence = trainData[trainIndex.get(i)];
            ArrayList<State> reversedOracle = oracles[sentence.INDEX];
            if (reversedOracle != null) {
                decode(sentence, reversedOracle);
                perceptron.updateWeights();
            }
        }
    }
        
    @Override
    final public ArrayList<PredictedAction> predictAction(State state,
                                                       boolean[] validAction,
                                                       ArrayList<Integer> feature){
        ArrayList<PredictedAction> predictedActions = new ArrayList<>();
        for (int action=0; action<3; ++action) {
            if (validAction[action]) {
                if (action == 0)
                    predictedActions.add(new PredictedAction(action, state, feature, perceptron.calcScore(feature, action)));
                else {
                    for (int l=0; l<LABEL_SIZE; ++l) {
                        int label = l + (action-1) * LABEL_SIZE + 1;
                        predictedActions.add(new PredictedAction(label, state, feature, perceptron.calcScore(feature, label)));
                    }
                }
            }
        }
        return predictedActions;
    }
    
    @Override
    final public ArrayList<PredictedAction> predictTestAction(State state,
                                                           boolean[] validAction,
                                                           ArrayList<Integer> feature){
        ArrayList<PredictedAction> predictedActions = new ArrayList<>();
        for (int action=0; action<3; ++action) {
            if (validAction[action]) {
                if (action == 0)
                    predictedActions.add(new PredictedAction(action, state, feature, calcScore(feature, action)));
                else {
                    for (int l=0; l<LABEL_SIZE; ++l) {
                        int label = l + (action-1) * LABEL_SIZE + 1;
                        predictedActions.add(new PredictedAction(label, state, feature, calcScore(feature, label)));
                    }
                }
            }
        }
        return predictedActions;
    }
    
    @Override
    final public State executeAction(PredictedAction predAction){
        int action = predAction.action;
        State state;
        if (action == 0) state = shift(predAction);
        else if (action < LABEL_SIZE + 1) state = left(predAction);
        else state = right(predAction);
        return state;
    }
    
    private State shift(PredictedAction action){
        State curState = (State) action.state;
        State newState = new State();
        newState.s0 = curState.b0;
        newState.b0 = curState.b0+1;
        newState.arcs = curState.arcs.clone();
        newState.tail = curState;
        newState.LAST_ACTION = action.action;
        newState.prevState = curState;
        newState.feature = action.feature;
        newState.score = action.score;
        return newState;
    }

    private State left(PredictedAction action){
        State curState = (State) action.state;
        State newState = new State();
        newState.arcs = curState.arcs.clone();
        newState.arcs[curState.tail.s0] = curState.s0;
        newState.s0 = curState.s0;
        newState.s0L = curState.tail.s0;
        newState.s0R = curState.s0R;
        newState.b0 = curState.b0;
        newState.tail = curState.tail.tail;
        newState.LAST_ACTION = action.action;
        newState.prevState = curState;
        newState.feature = action.feature;
        newState.score = action.score;
        return newState;        
    }
    
    private State right(PredictedAction action){
        State curState = (State) action.state;
        State newState = new State();
        newState.arcs = curState.arcs.clone();
        newState.arcs[curState.s0] = curState.tail.s0;
        newState.s0 = curState.tail.s0;
        newState.s0L = curState.tail.s0L;
        newState.s0R = curState.s0;
        newState.b0 = curState.b0;
        newState.tail = curState.tail.tail;
        newState.LAST_ACTION = action.action;
        newState.prevState = curState;
        newState.feature = action.feature;
        newState.score = action.score;
        return newState;
    }    
    
}
