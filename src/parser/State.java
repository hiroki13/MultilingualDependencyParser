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
    public int N_TOKENS;
    public int s0, b0, s0L, s0R;
    public int[] arcs;
    public int LAST_ACTION;
    public State tail, prevState;
    public float score;
    public String transition;
    public ArrayList<Integer> feature;
    
    public State(){}
    
    public State(int n_tokens){
        this.N_TOKENS = n_tokens;
        this.arcs = new int[n_tokens];
        for (int i=0; i<n_tokens; ++i) this.arcs[i] = -2;
    }    
    
    public State(State state){
        this.s0 = state.s0;
        this.b0 = state.b0;
        this.s0L = state.s0L;
        this.s0R = state.s0R;
        this.arcs = state.arcs;
        this.LAST_ACTION = state.LAST_ACTION;
        this.tail = state.tail;
        this.prevState = state.prevState;
        this.score = state.score;
        this.transition = state.transition;
        this.feature = state.feature;
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

    final public ArrayList<State> reverseState(){
        ArrayList<State> r_state = new ArrayList<>();
        State state = this;
        while(state.prevState != null){
            r_state.add(0, state);
            state = state.prevState;
        }
        return r_state;
    }
}
