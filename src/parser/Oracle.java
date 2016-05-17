/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parser;

import ling.Sentence;
import java.util.ArrayList;

/**
 *
 * @author hiroki
 */

final public class Oracle {
    final public Perceptron perceptron;
    final public Featurizer featurizer;
    final public int n_tokens;
    public State state;
    
    public Oracle(Perceptron perceptron, Featurizer featurizer, Sentence sentence) {
        this.perceptron = perceptron;
        this.featurizer = featurizer;
        this.n_tokens = sentence.N_TOKENS;
        setOracle(sentence);
    }

    private void setOracle(Sentence sentence) {
        OracleParser parser = new OracleParser();
        state = new State(sentence.N_TOKENS);
        state.s0 = 0;  // 0 = root
        state.b0 = 1;

        while (state.s0 != 0 || state.b0 < sentence.size()-1) {
            final int action = getOracleAction(sentence);
            ArrayList feature = featurizer.featurize(sentence, state);

            if (action == 0) state = parser.shift(state, feature);
            else if (action == 1) state = parser.left(state, feature);
            else state = parser.right(state, feature);            
        }
    }
    
    private int getOracleAction(Sentence sentence) {
        if(state.s0 == 0) return 0;

        final int[] gold_heads = sentence.arcs;
        final int stack_top1 = state.s0;
        final int stack_top2 = state.tail.s0;
        Boolean L = true;
        Boolean R = true;
        
        // Check if a reduced word has any dependents or not
        for (int i=0; i<n_tokens; ++i) {
            final int gold_head = gold_heads[i]; // annotated head index
            final int tmp_head = state.arcs[i];  // predicted head index
            if (stack_top2 == gold_head && stack_top2 != tmp_head) L = false;
            if (stack_top1 == gold_head && stack_top1 != tmp_head) R = false;
        }

        if (gold_heads[stack_top2] == stack_top1 && L) return 1;      // arc-left
        else if (gold_heads[stack_top1] == stack_top2 && R) return 2; // arc-right
        return 0;                                                     // shift
    }
/*    
    private int getOracleAction(Sentence sentence) {
        Boolean L = true;
        Boolean R = true;
        
        if(state.s0 == 0) return 0;
        
        for(String arc:sentence.arcs){
            String lc = String.format("%1$03d", state.tail.s0) + arc.substring(3);
            String rc = String.format("%1$03d", state.s0) + arc.substring(3);
            if(sentence.arcs.contains(lc) && !state.arcs.contains(lc)) L=false;
            if(sentence.arcs.contains(rc) && !state.arcs.contains(rc)) R=false;
        }

        String arcL = String.format("%1$03d",state.s0) + String.format("%1$03d",state.tail.s0);
        String arcR = String.format("%1$03d",state.tail.s0) + String.format("%1$03d",state.s0);
        
        if(sentence.arcs.contains(arcL) && L) return 1;
        else if(sentence.arcs.contains(arcR) && R) return 2;
        else return 0;
    }
*/    
}
