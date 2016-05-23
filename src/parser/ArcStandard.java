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
    final public void decode(Sentence sentence, ArrayList<State> reversedOracle) {
        State initState = new State(sentence.N_TOKENS);
        initState.s0 = 0;
        initState.b0 = 1;
        State[] beam = new State[BEAM_WIDTH];
        beam[0] = initState;
        int i = 0;
        
        while(beam[0].s0 != 0 || beam[0].b0 < sentence.size()){
            StateComparator candidates = new StateComparator();
            ArrayList<PredictedAction> actions = new ArrayList<>();

            for (State state:beam){
                if (state == null) break;

                ArrayList feature = featurizer.featurize(sentence, state);
                ArrayList predActions = predictAction(state, getValidAction(sentence, state), feature);
                
                for (int j=0; j<predActions.size(); j++)
                    actions.add((PredictedAction) predActions.get(j));
            }
            
            for (int j=0; j<actions.size(); j++){
                candidates.addSort((PredictedAction) actions.get(j));
            }
            
            beam = getNBestStates(candidates, BEAM_WIDTH);
            
            if (i < reversedOracle.size())
                perceptron.setMaxViolationPoint(reversedOracle.get(i), beam[0]);
            i += 1;
        }
        
    }
    
    @Override
    final public State[] decode(Sentence sentence) {
        State[] beam = new State[BEAM_WIDTH];
        State initState = new State(sentence.N_TOKENS);
        initState.s0 = 0;
        initState.b0 = 1;
        beam[0] = initState;
        
        while(beam[0].s0 != 0 || beam[0].b0 < sentence.size()){
            StateComparator candidates = new StateComparator();
            ArrayList<PredictedAction> actions = new ArrayList<>();

            for (State state:beam){
                if (state == null) break;

                ArrayList feature = featurizer.featurize(sentence, state);
                ArrayList predActions = predictTestAction(state, getValidAction(sentence, state), feature);
                
                for (int j=0; j<predActions.size(); j++)
                    actions.add((PredictedAction) predActions.get(j));
            }
            
            for (int j=0; j<actions.size(); j++){
                candidates.addSort((PredictedAction) actions.get(j));
            }
            
            beam = getNBestStates(candidates, BEAM_WIDTH);            
        }
        
        return beam;
    }
    
    
    private boolean[] getValidAction(Sentence sentence, State state) {
        boolean[] validAction = {true, true, true};
        
        if (state.b0 >= sentence.size()) validAction[0] = false;

        if (state.tail != null) {
            if (state.tail.s0 <= 0)
                validAction[1] = false;
        }
        else validAction[1] = false;

        if (state.tail == null) validAction[2] = false;
        else if (state.tail.s0 == 0 && state.b0 < sentence.size()) validAction[2] = false;

        return validAction;
    }
    
    private ArrayList<PredictedAction> predictAction(State state,
                                                    boolean[] validAction,
                                                    ArrayList<Integer> feature){
        ArrayList<PredictedAction> predictedActions = new ArrayList<>();
        for (int action=0; action<3; ++action) {
            if (validAction[action])
                predictedActions.add(new PredictedAction(action, state, feature,
                                      perceptron.calcScore(feature, action)));
        }
        return predictedActions;
    }
    
    private ArrayList<PredictedAction> predictTestAction(State state,
                                                    boolean[] validAction,
                                                    ArrayList<Integer> feature){
        ArrayList<PredictedAction> predictedActions = new ArrayList<>();
        for (int action=0; action<3; ++action) {
            if (validAction[action])
                predictedActions.add(new PredictedAction(action, state, feature, calcScore(feature, action)));
        }
        return predictedActions;
    }
    
    private State[] getNBestStates(StateComparator candidates, int n){
        State[] beam = new State[n];
        final int nCands = candidates.size();
        for (int i=0; i<n; ++i){
            if (i >= nCands) break;
            beam[i] = executeAction(candidates.get(i));
        }
        return beam;
    }
    
    private State executeAction(PredictedAction predAction){
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