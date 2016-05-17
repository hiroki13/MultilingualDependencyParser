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

public class ArcStandard extends Parser{
    public ArcStandard(int beamWidth, int weightSize, boolean stag){
        this.BEAM_WIDTH = beamWidth;
        this.STAG = stag;
        this.perceptron = new Perceptron(3, weightSize);
        this.featurizer = new Featurizer(weightSize, stag);
    }

    public ArcStandard(int beamWidth, Perceptron perceptron, boolean stag){
        this.BEAM_WIDTH = beamWidth;
        this.STAG = stag;
        this.perceptron = perceptron;
    }

    @Override
    public void train(Sentence[] trainSents){
        trainData = trainSents;
        oracles = new ArrayList[trainSents.length];
        
        for (int i=0; i<trainData.length; ++i) trainIndex.add(i);
        Collections.shuffle(trainIndex);

        for(int i=0; i<trainData.length; ++i){
            if (i % 1000 == 0 && i != 0) System.out.print(String.format("%d ", i));

            Sentence sent = trainData[trainIndex.get(i)];
            Oracle oracle = new Oracle(perceptron, featurizer, sent);
            ArrayList<State> reversed_oracle = oracle.state.reverseState();
            oracles[sent.INDEX] = reversed_oracle;

            if (reversed_oracle != null) {
                decode(sent, reversed_oracle);
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
            ArrayList<State> reversed_oracle = oracles[sentence.INDEX];
            if (reversed_oracle != null) {
                decode(sentence, reversed_oracle);
                perceptron.updateWeights();
            }
        }
    }

    @Override
    final public void decode(Sentence sentence, ArrayList<State> r_oracle){
        State init_state = new State(sentence.N_TOKENS);
        init_state.s0 = 0;
        init_state.b0 = 1;
        State[] beam = new State[BEAM_WIDTH];
        beam[0] = init_state;
        int i = 0;
        
        while(beam[0].s0 != 0 || beam[0].b0 < sentence.size()){
            StateComparator candidates = new StateComparator();
            ArrayList<PredictedAction> actions = new ArrayList<>();

            for (State state:beam){
                if (state == null) break;
                boolean[] valid_action = getValidAction(sentence, state);

                ArrayList feature = featurizer.featurize(sentence, state);
                ArrayList pred_actions = predictAction(state, valid_action, feature);
                
                for (int j=0; j<pred_actions.size(); j++)
                    actions.add((PredictedAction) pred_actions.get(j));
            }
            
            for (int j=0; j<actions.size(); j++){
                candidates.addSort((PredictedAction) actions.get(j));
            }
            
            beam = getNBestStates(candidates, BEAM_WIDTH);
            
            if (i < r_oracle.size())
                perceptron.setMaxViolationPoint(r_oracle.get(i), beam[0]);
            i += 1;
        }
        
    }
    
    @Override
    final public State[] decode(Sentence sentence){
        State[] beam = new State[BEAM_WIDTH];
        State init_state = new State(sentence.N_TOKENS);
        init_state.s0 = 0;
        init_state.b0 = 1;
        beam[0] = init_state;
        
        while(beam[0].s0 != 0 || beam[0].b0 < sentence.size()){
            StateComparator candidates = new StateComparator();
            ArrayList<PredictedAction> actions = new ArrayList<>();

            for (State state:beam){
                if (state == null) break;
                boolean[] valid_action = getValidAction(sentence, state);

                ArrayList feature = featurizer.featurize(sentence, state);
                ArrayList pred_actions = predictAction(state, valid_action, feature);
                
                for (int j=0; j<pred_actions.size(); j++)
                    actions.add((PredictedAction) pred_actions.get(j));
            }
            
            for (int j=0; j<actions.size(); j++){
                candidates.addSort((PredictedAction) actions.get(j));
            }
            
            beam = getNBestStates(candidates, BEAM_WIDTH);            
        }
        
        return beam;
    }
    
    
    private boolean[] getValidAction(Sentence sentence, State state){
        boolean[] valid_action = {true, true, true};
        
        if (state.b0 >= sentence.size())
            valid_action[0] = false;
        if (state.tail != null) {
            if (state.tail.s0 <= 0)
                valid_action[1] = false;
        }
        else
            valid_action[1] = false;

        if (state.tail == null)
            valid_action[2] = false;
        else if (state.tail.s0 == 0 && state.b0 < sentence.size())
            valid_action[2] = false;

        return valid_action;
    }
    
    private ArrayList<PredictedAction> predictAction(State state,
                                                    boolean[] valid_action,
                                                    ArrayList<Integer> feature){
        ArrayList<PredictedAction> predicted_actions = new ArrayList<>();
        for (int action=0; action<3; ++action) {
            if (valid_action[action])
                predicted_actions.add(new PredictedAction(action, state, feature,
                                      perceptron.calcScore(feature, action)));
        }
        return predicted_actions;
    }
    
    private State[] getNBestStates(StateComparator candidates, int n){
        State[] beam = new State[n];
        final int n_cands = candidates.size();
        for (int i=0; i<n; ++i){
            if (i >= n_cands) break;
            PredictedAction pred_action = candidates.get(i);
            beam[i] = executeAction(pred_action);
        }
        return beam;
    }
    
    private State executeAction(PredictedAction pred_action){
        int action = pred_action.action;
        State new_state;
        if (action==0) new_state = shift(pred_action);
        else if (action==1) new_state = left(pred_action);
        else new_state = right(pred_action);
        return new_state;
    }
    
    private State shift(PredictedAction action){
        State cur_state = (State) action.state;
        State new_state = new State();
        new_state.s0 = cur_state.b0;
        new_state.b0 = cur_state.b0+1;
        new_state.arcs = cur_state.arcs.clone();
        new_state.tail = cur_state;
        new_state.last_action = 0;
        new_state.prev_state = cur_state;
        new_state.feature = action.feature;
        new_state.score = action.score;
        if (cur_state.transition == null) new_state.transition = "0";
        else new_state.transition = cur_state.transition + "0";
        return new_state;
    }

    private State left(PredictedAction action){
        State cur_state = (State) action.state;
        State new_state = new State();
        new_state.arcs = cur_state.arcs.clone();
        new_state.arcs[cur_state.tail.s0] = cur_state.s0;
        new_state.s0 = cur_state.s0;
        new_state.s0L = cur_state.tail.s0;
        new_state.s0R = cur_state.s0R;
        new_state.b0 = cur_state.b0;
        new_state.tail = cur_state.tail.tail;
        new_state.last_action = 1;
        new_state.prev_state = cur_state;
        new_state.feature = action.feature;
        new_state.score = action.score;
        if (cur_state.transition == null) new_state.transition = "1";
        else new_state.transition = cur_state.transition + "1";
        return new_state;        
    }
    
    private State right(PredictedAction action){
        State cur_state = (State) action.state;
        State new_state = new State();
        new_state.arcs = cur_state.arcs.clone();
        new_state.arcs[cur_state.s0] = cur_state.tail.s0;
        new_state.s0 = cur_state.tail.s0;
        new_state.s0L = cur_state.tail.s0L;
        new_state.s0R = cur_state.s0;
        new_state.b0 = cur_state.b0;
        new_state.tail = cur_state.tail.tail;
        new_state.last_action = 2;
        new_state.prev_state = cur_state;
        new_state.feature = action.feature;
        new_state.score = action.score;
        if (cur_state.transition == null) new_state.transition = "2";
        else new_state.transition = cur_state.transition + "2";
        return new_state;
    }    

}