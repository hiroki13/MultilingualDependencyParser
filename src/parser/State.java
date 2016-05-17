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

final public class State implements Serializable{
    public int n_tokens;
    public int s0, b0, s0L, s0R;
    public int[] arcs;
    public int last_action;
    public State tail, prev_state;
    public float score;
    public String transition;
    public ArrayList<Integer> feature;
    public int used_stag_id;
    
    public State(){}
    
    public State(int n_tokens){
        this.n_tokens = n_tokens;
        this.arcs = new int[n_tokens];
        for (int i=0; i<n_tokens; ++i) this.arcs[i] = -2;
    }    
    
    @Override
    public State clone() {
        State b = null;
        try {
            b = (State) super.clone();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    public State(State state){
        this.s0 = state.s0;
        this.b0 = state.b0;
        this.s0L = state.s0L;
        this.s0R = state.s0R;
        this.arcs = state.arcs;
        this.last_action = state.last_action;
        this.tail = state.tail;
        this.prev_state = state.prev_state;
        this.score = state.score;
        this.transition = state.transition;
        this.feature = state.feature;
        this.used_stag_id = state.used_stag_id;
    }

    public void shift(ArrayList<Integer> feature, float score) {
        State cur_state = (State) this.clone();
        this.s0 = cur_state.b0;
        this.b0 = cur_state.b0+1;
        this.tail = cur_state;
        this.last_action = 0;
        this.prev_state = cur_state;
        this.feature = feature;
        this.score = score;
        if (cur_state.transition == null) this.transition = "0";
        else this.transition += "0";
    }

    public void left(ArrayList<Integer> feature, float score) {
        State cur_state = (State) this.clone();
        this.arcs[cur_state.tail.s0] = cur_state.s0;
        this.s0L = cur_state.tail.s0;
        this.s0R = cur_state.s0R;
        this.tail = cur_state.tail.tail;
        this.last_action = 1;
        this.prev_state = cur_state;
        this.feature = feature;
        this.score = score;
        if (cur_state.transition == null) this.transition = "1";
        else this.transition += "1";
    }
    
    final public void right(ArrayList<Integer> feature, float score) {
        State cur_state = (State) this.clone();
        this.arcs[cur_state.s0] = cur_state.tail.s0;
        this.s0 = cur_state.tail.s0;
        this.s0L = cur_state.tail.s0L;
        this.s0R = cur_state.s0;
        this.tail = cur_state.tail.tail;
        this.last_action = 2;
        this.prev_state = cur_state;
        this.feature = feature;
        this.score = score;
        if (cur_state.transition == null) this.transition = "2";
        else this.transition += "2";
    }        

    final public ArrayList<State> reverseState(){
        ArrayList<State> r_state = new ArrayList<>();
        State state = this;
        while(state.prev_state != null){
            r_state.add(0, state);
            state = state.prev_state;
        }
        return r_state;
    }
}
