/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author hiroki
 */

final public class Sentence {
    final static private String HYPH = "-";
    final static private List<String> nomDep = Arrays.asList("nsubj", "nsubjpass", "dobj", "iobj", "SUB", "OBJ", "PRD");
    final static private List<String> prdDep = Arrays.asList("csubj", "csubjpass", "ccomp", "VC");
    final static private String xDep = "xcomp";

    public static boolean PARSER = false;
    public static int STAG_ID;
    final public int INDEX, N_TOKENS;
    final public Token[] tokens;
    public int[] arcs;

    final public static Dict stagDict = new Dict();
    public int[] stags;

    public Sentence(int index, ArrayList<String[]> lines, boolean test) {
        INDEX = index;
        N_TOKENS = lines.size() - countMultiWords(lines);
        tokens = new Token[N_TOKENS];
        setTokens(lines);
                
        if (!PARSER) {
            if (STAG_ID == 2) setStagB(test);
            else if (STAG_ID == 3) setStagC(test);
            else if (STAG_ID == 4) setStagD(test);
            else if (STAG_ID == 5) setStagE(test);
            else if (STAG_ID == 6) setStagF(test);
            else if (STAG_ID == 7) setStagG(test);
            else if (STAG_ID == 8) setStagH(test);
            else if (STAG_ID == 9) setStagI(test);
            else if (STAG_ID == 10) setStagJ(test);
            else if (STAG_ID == 11) setStagK(test);
            else setStagA(test);
        }
        else setArcs();
    }
    
    public void setGoldStags() {
        boolean test = false;
        if (STAG_ID == 2) setStagB(test);            
        else if (STAG_ID == 3) setStagC(test);        
        else if (STAG_ID == 4) setStagD(test);        
        else if (STAG_ID == 5) setStagE(test);        
        else if (STAG_ID == 6) setStagF(test);        
        else if (STAG_ID == 7) setStagG(test);        
        else if (STAG_ID == 8) setStagH(test);        
        else if (STAG_ID == 9) setStagI(test);        
        else if (STAG_ID == 10) setStagJ(test);        
        else if (STAG_ID == 11) setStagK(test);        
        else setStagA(test);
        
        for (int i=0; i<N_TOKENS; ++i)
            tokens[i].STAG = stags[i];
    }

    private int countMultiWords(ArrayList<String[]> lines) {
        int k = 0;
        for (int i=0; i<lines.size(); ++i)
            if (lines.get(i)[0].contains(HYPH)) k++;
        return k;
    }

    final public void setTokens(ArrayList<String[]> lines) {
        int k = 0;
        for (int i=0; i<lines.size(); ++i) {
            String[] line = lines.get(i);
            if (!line[0].contains(HYPH)) tokens[k++] = new Token(line, lines);
        }
    }

    final public void setArcs(){
        arcs = new int[N_TOKENS];
        for (int i=0; i<N_TOKENS; ++i) {
            Token t = tokens[i];
            arcs[t.INDEX] = t.O_HEAD;
        }
    }
    
    final public boolean isProjective(){
        for (int i=1; i<N_TOKENS; ++i) {
            Token t1 = tokens[i];

            for (int j=1; j<N_TOKENS; ++j) {
                if (i == j) continue;
                Token t2 = tokens[j];
                if (t1.INDEX < t2.INDEX && t2.INDEX < t1.O_HEAD)
                    if (t1.O_HEAD < t2.O_HEAD || t2.O_HEAD < t1.INDEX)
                        return false;
                if (t1.INDEX > t2.INDEX && t2.INDEX > t1.O_HEAD)
                    if (t1.O_HEAD > t2.O_HEAD || t2.O_HEAD > t1.INDEX)
                        return false;
            }
        }
        return true;
    }
    
    final public void setStagA(boolean test) {
        // Label/Dir+_L_R

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag, parent;
            boolean lchild = false, rchild = false;

            Token token1 = tokens[i];

            if (token1.O_HEAD == 0) parent = token1.O_LABEL;
            else if (token1.INDEX > token1.O_HEAD) parent = token1.O_LABEL + "/L";
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

    final public void setStagB(boolean test) {
        // Label/Dir+_Label/L_Label/R

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag;
            boolean lchild = false, rchild = false;
            ArrayList<String> lchLabel = new ArrayList<>();
            ArrayList<String> rchLabel = new ArrayList<>();

            Token token1 = tokens[i];

            if (token1.O_HEAD == 0) stag = token1.O_LABEL;
            else if (token1.INDEX > token1.O_HEAD) stag = token1.O_LABEL + "/L";
            else stag = token1.O_LABEL + "/R";
            
            for (int j=0; j<N_TOKENS; ++j) {
                if (i==j) continue;

                Token token2 = tokens[j];

                if (token1.INDEX == token2.O_HEAD){
                    String label = token2.O_LABEL;
                    if (token1.INDEX > token2.INDEX) {
                        if (nomDep.contains(label) || prdDep.contains(label) || xDep.equals(label))
                            lchLabel.add(token2.O_LABEL);
                        lchild = true;
                    }
                    else {
                        if (nomDep.contains(label) || prdDep.contains(label) || xDep.equals(label))
                            rchLabel.add(token2.O_LABEL);
                        rchild = true;
                    }
                }
            }
            
            Collections.sort(lchLabel);
            Collections.sort(rchLabel);

            if (lchild) {
                stag += "_L";
                for (int j=0; j<lchLabel.size(); ++j)
                    stag += ":" + lchLabel.get(j);
            }
            if (rchild) {
                stag += "_R";
                for (int j=0; j<rchLabel.size(); ++j)
                    stag += ":" + rchLabel.get(j);
            }

            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagC(boolean test) {
        // Label/Dir
        
        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag;

            Token token1 = tokens[i];

            if (token1.O_HEAD == 0) stag = token1.O_LABEL;
            else if (token1.INDEX > token1.O_HEAD) stag = token1.O_LABEL + "/L";
            else stag = token1.O_LABEL + "/R";

            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagD(boolean test) {
        // Dir+_L_R

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag, parent;
            boolean lchild = false, rchild = false;

            Token token1 = tokens[i];

            if (token1.O_HEAD == 0) parent = "N";
            else if (token1.INDEX > token1.O_HEAD) parent = "L";
            else parent = "R";            
            
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

    final public void setStagE(boolean test) {
        // Dir

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag;

            Token token1 = tokens[i];

            if (token1.INDEX > token1.O_HEAD) stag = "L";
            else stag = "R";

            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagF(boolean test) {
        // L_R

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag;
            boolean lchild = false, rchild = false;

            Token token1 = tokens[i];

            for (int j=0; j<N_TOKENS; ++j) {
                if (i==j) continue;

                Token token2 = tokens[j];

                if (token1.INDEX == token2.O_HEAD){
                    if (token1.INDEX > token2.INDEX) lchild = true;
                    else rchild = true;
                }
            }
            
            if (lchild && rchild) stag = "L_R";
            else if (lchild) stag = "L";
            else if (rchild) stag = "R";
            else stag = "N";

            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagG(boolean test) {
        // HLabel+DLabel:DLabel:DLabel...

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag;
            ArrayList<String> chLabel = new ArrayList<>();

            Token token1 = tokens[i];

            stag = token1.O_LABEL + "_";
            
            for (int j=0; j<N_TOKENS; ++j) {
                if (i==j) continue;

                Token token2 = tokens[j];

                if (token1.INDEX == token2.O_HEAD){
                    String label = token2.O_LABEL;
                    if (nomDep.contains(label) || prdDep.contains(label) || xDep.equals(label))
                        chLabel.add(token2.O_LABEL);
                }
            }
            
            Collections.sort(chLabel);

            for (int j=0; j<chLabel.size(); ++j)
                stag += chLabel.get(j) + ":";
                
            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagH(boolean test) {
        // DLabel:DLabel:DLabel

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag = "NULL_";
            ArrayList<String> chLabel = new ArrayList<>();

            Token token1 = tokens[i];

            for (int j=0; j<N_TOKENS; ++j) {
                if (i==j) continue;

                Token token2 = tokens[j];

                if (token1.INDEX == token2.O_HEAD){
                    String label = token2.O_LABEL;
                    if (nomDep.contains(label) || prdDep.contains(label) || xDep.equals(label))
                        chLabel.add(token2.O_LABEL);
                }
            }
            
            Collections.sort(chLabel);
            for (int j=0; j<chLabel.size(); ++j)
                stag += chLabel.get(j) + ":";
                
            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagI(boolean test) {
        // HLabel

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            Token token1 = tokens[i];
            String stag = token1.O_LABEL;

            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagJ(boolean test) {
        // Label/Dir+_DLabel:DLabel:

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag;
            ArrayList<String> chLabel = new ArrayList<>();

            Token token1 = tokens[i];

            if (token1.O_HEAD == 0) stag = token1.O_LABEL;
            else if (token1.INDEX > token1.O_HEAD) stag = token1.O_LABEL + "/L_";
            else stag = token1.O_LABEL + "/R_";
            
            for (int j=0; j<N_TOKENS; ++j) {
                if (i==j) continue;

                Token token2 = tokens[j];

                if (token1.INDEX == token2.O_HEAD){
                    String label = token2.O_LABEL;
                    if (nomDep.contains(label) || prdDep.contains(label) || xDep.equals(label))
                        chLabel.add(token2.O_LABEL);
                }
            }
            
            Collections.sort(chLabel);

            for (int j=0; j<chLabel.size(); ++j)                
                stag += chLabel.get(j) + ":";

            if (!test) stags[i] = stagDict.getValue(stag);
            else {
                if (stagDict.keys.containsKey(stag))
                    stags[i] = stagDict.getValue(stag);
                else
                    stags[i] = -1;
            }
        }
    }

    final public void setStagK(boolean test) {
        // Dir+DLabel:DLabel:

        stags = new int[N_TOKENS];

        for (int i=0; i<N_TOKENS; ++i) {
            String stag;
            ArrayList<String> chLabel = new ArrayList<>();

            Token token1 = tokens[i];

            if (token1.INDEX > token1.O_HEAD) stag = "L_";
            else stag = "R_";
            
            for (int j=0; j<N_TOKENS; ++j) {
                if (i==j) continue;

                Token token2 = tokens[j];

                if (token1.INDEX == token2.O_HEAD){
                    String label = token2.O_LABEL;
                    if (nomDep.contains(label) || prdDep.contains(label) || xDep.equals(label))
                        chLabel.add(token2.O_LABEL);
                }
            }
            
            Collections.sort(chLabel);

            for (int j=0; j<chLabel.size(); ++j)                
                stag += chLabel.get(j) + ":";

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
