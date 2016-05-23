/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.ArrayList;
import ling.Sentence;

/**
 *
 * @author hiroki
 */

final public class OracleParser {
    final public Featurizer featurizer;
    public int LABEL_SIZE;

    public OracleParser(Featurizer featurizer) {
        this.featurizer = featurizer;
    }
    
    public OracleParser(Featurizer featurizer, int labelSize) {
        this.featurizer = featurizer;
        this.LABEL_SIZE = labelSize;
    }
    
    final public State getOracle(Sentence sent) {
        State state = new State(sent.N_TOKENS);
        state.s0 = 0;  // 0 = root
        state.b0 = 1;

        while (state.s0 != 0 || state.b0 < sent.size()-1) {
            int action = getOracleAction(state, sent);
            ArrayList feature = featurizer.featurize(sent, state);

            if (action == 0) state = shift(state, feature, action);
            else if (action == 1) state = left(state, feature, action);
            else state = right(state, feature, action);            
        }
        
        return state;
    }
    
    final public State getLabeledOracle(Sentence sent) {
        State state = new State(sent.N_TOKENS);
        state.s0 = 0;
        state.b0 = 1;

        while (state.s0 != 0 || state.b0 < sent.size()-1) {
            int action = getLabeledOracleAction(state, sent);
            ArrayList feature = featurizer.featurize(sent, state);

            if (action == 0) state = shift(state, feature, action);
            else if (action < LABEL_SIZE + 1) state = left(state, feature, action);
            else state = right(state, feature, action);
        }
        
        return state;
    }
    
    private int getOracleAction(State state, Sentence sent) {
        if(state.s0 == 0) return 0;

        final int[] gold_heads = sent.arcs;
        final int stack_top1 = state.s0;
        final int stack_top2 = state.tail.s0;
        Boolean L = true;
        Boolean R = true;
        
        // Check if a reduced word has any dependents or not
        for (int i=0; i<sent.N_TOKENS; ++i) {
            final int gold_head = gold_heads[i]; // annotated head index
            final int tmp_head = state.arcs[i];  // predicted head index
            if (stack_top2 == gold_head && stack_top2 != tmp_head) L = false;
            if (stack_top1 == gold_head && stack_top1 != tmp_head) R = false;
        }

        if (gold_heads[stack_top2] == stack_top1 && L) return 1;      // arc-left
        else if (gold_heads[stack_top1] == stack_top2 && R) return 2; // arc-right
        return 0;                                                     // shift
    }

    private int getLabeledOracleAction(State state, Sentence sent) {
        if(state.s0 == 0) return 0;

        final int[] goldHeads = sent.arcs;
        final int stackTop_1 = state.s0;
        final int stackTop_2 = state.tail.s0;
        Boolean L = true;
        Boolean R = true;
        
        // Check if a reduced word has any dependents or not
        for (int i=0; i<sent.N_TOKENS; ++i) {
            final int gold_head = goldHeads[i]; // annotated head index
            final int tmp_head = state.arcs[i]; // predicted head index
            if (stackTop_2 == gold_head && stackTop_2 != tmp_head) L = false;
            if (stackTop_1 == gold_head && stackTop_1 != tmp_head) R = false;
        }

        if (goldHeads[stackTop_2] == stackTop_1 && L)
            return sent.tokens[stackTop_2].LABEL + 1;
        else if (goldHeads[stackTop_1] == stackTop_2 && R)
            return sent.tokens[stackTop_1].LABEL + LABEL_SIZE + 1;
        return 0;
    }

    final public State shift(State cur_state, ArrayList<Integer> feature, int action){
        State new_state = new State();
        new_state.s0 = cur_state.b0;
        new_state.b0 = cur_state.b0+1;
        new_state.arcs = cur_state.arcs;
        new_state.tail = cur_state;
        new_state.LAST_ACTION = action;
        new_state.prevState = cur_state;
        new_state.feature = feature;
        return new_state;
    }

    final public State left(State cur_state, ArrayList<Integer> feature, int action){
        State new_state = new State();
        new_state.arcs = cur_state.arcs;
        new_state.arcs[cur_state.tail.s0] = cur_state.s0;
        new_state.s0 = cur_state.s0;
        new_state.s0L = cur_state.tail.s0;
        new_state.s0R = cur_state.s0R;
        new_state.b0 = cur_state.b0;
        new_state.tail = cur_state.tail.tail;
        new_state.LAST_ACTION = action;
        new_state.prevState = cur_state;
        new_state.feature = feature;
        return new_state;        
    }
    
    final public State right(State cur_state, ArrayList<Integer> feature, int action){
        State new_state = new State();
        new_state.arcs = cur_state.arcs;
        new_state.arcs[cur_state.s0] = cur_state.tail.s0;
        new_state.s0 = cur_state.tail.s0;
        new_state.s0L = cur_state.tail.s0L;
        new_state.s0R = cur_state.s0;
        new_state.b0 = cur_state.b0;
        new_state.tail = cur_state.tail.tail;
        new_state.LAST_ACTION = action;
        new_state.prevState = cur_state;
        new_state.feature = feature;
        return new_state;
    }
    
}
