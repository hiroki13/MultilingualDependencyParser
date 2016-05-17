/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.ArrayList;

/**
 *
 * @author hiroki
 */
public class PredictedAction{
    final int action;
    final State state;
    final ArrayList<Integer> feature;
    int used_stag_id;
    final float score;
    final String transition;
    ArrayList<PredictedAction> action_set;
    
    public PredictedAction(int action, State state, ArrayList<Integer> feature,
                            int used_stag_id, float score){
        this.action = action;
        this.state = state;
        this.feature = feature;
        this.used_stag_id = used_stag_id;
        this.score = score + state.score;
        if (state.transition == null) this.transition = String.valueOf(action);
        else this.transition = state.transition + action;
        this.action_set = new ArrayList<>();
        this.action_set.add(this);
    }
    
    public PredictedAction(int action, State state, ArrayList<Integer> feature,
                            float score){
        this.action = action;
        this.state = state;
        this.feature = feature;
        this.score = score + state.score;
        if (state.transition == null) this.transition = String.valueOf(action);
        else this.transition = state.transition + action;
    }
    
}
