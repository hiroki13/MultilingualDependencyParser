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

final public class OracleParser {
    public OracleParser(){}
    
    final public State shift(State cur_state, ArrayList<Integer> feature){
        State new_state = new State();
        new_state.s0 = cur_state.b0;
        new_state.b0 = cur_state.b0+1;
        new_state.arcs = cur_state.arcs;
        new_state.tail = cur_state;
        new_state.last_action = 0;
        new_state.prev_state = cur_state;
        new_state.feature = feature;
        if (cur_state.transition == null) new_state.transition = "0";
        else new_state.transition = cur_state.transition + "0";
        return new_state;
    }

    final public State left(State cur_state, ArrayList<Integer> feature){
        State new_state = new State();
        new_state.arcs = cur_state.arcs;
        new_state.arcs[cur_state.tail.s0] = cur_state.s0;
        new_state.s0 = cur_state.s0;
        new_state.s0L = cur_state.tail.s0;
        new_state.s0R = cur_state.s0R;
        new_state.b0 = cur_state.b0;
        new_state.tail = cur_state.tail.tail;
        new_state.last_action = 1;
        new_state.prev_state = cur_state;
        new_state.feature = feature;
        if (cur_state.transition == null) new_state.transition = "1";
        else new_state.transition = cur_state.transition + "1";
        return new_state;        
    }
    
    final public State right(State cur_state, ArrayList<Integer> feature){
        State new_state = new State();
        new_state.arcs = cur_state.arcs;
        new_state.arcs[cur_state.s0] = cur_state.tail.s0;
        new_state.s0 = cur_state.tail.s0;
        new_state.s0L = cur_state.tail.s0L;
        new_state.s0R = cur_state.s0;
        new_state.b0 = cur_state.b0;
        new_state.tail = cur_state.tail.tail;
        new_state.last_action = 2;
        new_state.prev_state = cur_state;
        new_state.feature = feature;
        if (cur_state.transition == null) new_state.transition = "2";
        else new_state.transition = cur_state.transition + "2";
        return new_state;
    }        
}
