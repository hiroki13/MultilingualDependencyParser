/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ling;

import java.util.ArrayList;

/**
 *
 * @author hiroki
 */

final public class Sentence {
    final public int INDEX, N_TOKENS;
    final public Token[] tokens;
    public int[] arcs;

    final public static Dict stagDict = new Dict();
    public int[] stags;

    public Sentence(int index, ArrayList<String[]> lines, boolean isStag, boolean test) {
        INDEX = index;
        N_TOKENS = lines.size();
        tokens = new Token[N_TOKENS];
        setTokens(lines);
                
        if (isStag) setStags(test);
        else setArcs();
    }
    
    final public void setTokens(ArrayList<String[]> lines) {
        for (int i=0; i<lines.size(); ++i) tokens[i] = new Token(lines.get(i), lines);
    }

    final public void setArcs(){
        arcs = new int[N_TOKENS];
        for (int i=0; i<N_TOKENS; ++i) {
            Token t = tokens[i];
            arcs[t.INDEX] = t.O_HEAD;
        }
    }
    
    final public void setStags(boolean test) {
        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag, parent;
            boolean lchild = false, rchild = false;

            Token token1 = tokens[i];

            if (token1.INDEX > token1.O_HEAD) parent = token1.O_LABEL + "/L";
            else parent = token1.O_LABEL + "/R";            
            
            for (int j=0; j<N_TOKENS; ++j) {
                if (i==j) continue;

                Token token2 = tokens[j];

                if (token1.INDEX == token2.O_HEAD){
                    if (token1.INDEX > token2.INDEX) lchild = true;
                    else rchild = true;
                }
            }
            
            if (lchild && rchild) stag = parent + "_L_R";
            else if (lchild) stag = parent + "_L";
            else if (rchild) stag = parent + "_R";
            else stag = parent;

            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public int size() {
        return N_TOKENS;
    }

}
