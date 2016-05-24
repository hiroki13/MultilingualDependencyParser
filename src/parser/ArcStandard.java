/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import ling.Sentence;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author hiroki
 */

public class ArcStandard extends Parser {

    public ArcStandard(int beamWidth, int weightSize, boolean stag) {
        this.BEAM_WIDTH = beamWidth;
        this.STAG = stag;
        this.perceptron = new Perceptron(3, weightSize);
        this.featurizer = new Featurizer(weightSize, stag);
    }

    public ArcStandard(int beamWidth, Perceptron perceptron, boolean stag) {
        this.BEAM_WIDTH = beamWidth;
        this.STAG = stag;
        this.perceptron = perceptron;
    }

    @Override
    public void train(Sentence[] trainSents) {
        trainData = trainSents;
        oracles = new ArrayList[trainSents.length];
        OracleParser oracleParser = new OracleParser(featurizer);
        
        for (int i=0; i<trainData.length; ++i) trainIndex.add(i);
        Collections.shuffle(trainIndex);

        for(int i=0; i<trainData.length; ++i){
            if (i % 1000 == 0 && i != 0) System.out.print(String.format("%d ", i));

            Sentence sent = trainData[trainIndex.get(i)];
            State oracle = oracleParser.getOracle(sent);
            ArrayList<State> reversedOracle = oracle.reverseState();
            oracles[sent.INDEX] = reversedOracle;

            if (reversedOracle != null) {
                decode(sent, reversedOracle);
                perceptron.updateWeights();
            }
        }
    }

    @Override
    public void train() {
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
    final public ArrayList<PredictedAction> predictAction(State state, boolean[] validAction, ArrayList<Integer> feature){
        ArrayList<PredictedAction> predictedActions = new ArrayList<>();
        for (int action=0; action<3; ++action) {
            if (validAction[action])
                predictedActions.add(new PredictedAction(action, state, feature,
                                      perceptron.calcScore(feature, action)));
        }
        return predictedActions;
    }
    
    @Override
    final public ArrayList<PredictedAction> predictTestAction(State state, boolean[] validAction, ArrayList<Integer> feature){
        ArrayList<PredictedAction> predictedActions = new ArrayList<>();
        for (int action=0; action<3; ++action) {
            if (validAction[action])
                predictedActions.add(new PredictedAction(action, state, feature, calcScore(feature, action)));
        }
        return predictedActions;
    }
    
    @Override
    final public State executeAction(PredictedAction predAction){
        int action = predAction.action;
        State newState;
        if (action==0) newState = shift(predAction);
        else if (action==1) newState = left(predAction);
        else newState = right(predAction);
        return newState;
    }

    private State shift(PredictedAction action){
        State curState = (State) action.state;
        State newState = new State();
        newState.s0 = curState.b0;
        newState.b0 = curState.b0+1;
        newState.arcs = curState.arcs.clone();
        newState.tail = curState;
        newState.LAST_ACTION = 0;
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
        newState.LAST_ACTION = 1;
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
        newState.LAST_ACTION = 2;
        newState.prevState = curState;
        newState.feature = action.feature;
        newState.score = action.score;
        return newState;
    }    

}