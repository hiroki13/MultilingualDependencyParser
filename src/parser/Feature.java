/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parser;

import ling.Sentence;
import ling.Token;

/**
 *
 * @author hiroki
 */

final public class Feature {
    final public int index;
    final public int form;
    final public int pos;
    final public int stag;
    final public int lmc;
    final public int rmc;
    final public int lmc_index;
    final public int rmc_index;
    final public int PAD = Token.vocabForm.getHashValue("<PAD>");
    final public int BOS = Token.vocabForm.getHashValue("<BOS>");
    final public int EOS = Token.vocabForm.getHashValue("<EOS>");
    
    public Feature(State state, Sentence sent) {
        final Token[] tokens = sent.tokens;

        index = state.s0;
        form = tokens[state.s0].FORM;
        pos = tokens[state.s0].POS;
        stag = tokens[state.s0].STAG;

        if(state.s0L != -1) lmc = tokens[state.s0L].POS;
        else lmc = PAD;
        lmc_index = state.s0L;

        if(state.s0R != -1) rmc = tokens[state.s0R].POS;
        else rmc = PAD;
        rmc_index = state.s0R;
    }

    public Feature(Token t) {
        index = t.INDEX;
        form = t.FORM;
        pos = t.POS;
        stag = t.STAG;
        lmc = PAD;
        rmc = PAD;
        lmc_index = -1;
        rmc_index = -1;
    }

    public Feature() {
        index = PAD;
        form = PAD;
        pos = PAD;
        stag = PAD;
        lmc = PAD;
        rmc = PAD;
        lmc_index = -1;
        rmc_index = -1;
    }
    
}
