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

final public class Token {
    final public static Dict vocabForm = new Dict();
    final public static Dict vocabCPos = new Dict();
    final public static Dict vocabPos = new Dict();
    final public static Dict vocabStag = new Dict();
    final public static Dict vocabLabel = new Dict();

    final public int INDEX;
    final public int FORM;
    final public int CPOS;
    final public int POS;
    final public int STAG;
    final public int O_HEAD;
    final public int LABEL;
    final public String O_FORM;
    final public String O_CPOS;
    final public String O_POS;
    final public String O_STAG;
    final public String O_LABEL;
    
    public Token(String[] line) {
        this.INDEX = Integer.parseInt(line[0]);
        this.O_FORM = line[1];
        this.O_CPOS = line[3];
        this.O_POS = line[4];
        this.O_STAG = line[5];
        this.O_HEAD = Integer.parseInt(line[6]);
        this.O_LABEL = line[7];

        this.FORM = vocabForm.getHashValue(this.O_FORM);
        this.CPOS = vocabCPos.getHashValue(this.O_CPOS);
        this.POS = vocabPos.getHashValue(this.O_POS);
        this.STAG = vocabStag.getHashValue(this.O_STAG);
        this.LABEL = vocabLabel.getHashValue(this.O_LABEL);
    }
    
    public Token(String[] line, ArrayList<String[]> tmpSent) {
        this.INDEX = Integer.parseInt(line[0]);
        this.O_FORM = line[1];
        this.O_CPOS = line[3];
        this.O_POS = line[4];
        this.O_STAG = setStag(line, tmpSent);
        this.O_HEAD = Integer.parseInt(line[6]);
        this.O_LABEL = line[7];

        this.FORM = vocabForm.getHashValue(this.O_FORM);
        this.CPOS = vocabCPos.getHashValue(this.O_CPOS);
        this.POS = vocabPos.getHashValue(this.O_POS);
        this.STAG = vocabStag.getHashValue(this.O_STAG);
        this.LABEL = vocabLabel.getHashValue(this.O_LABEL);
    }
    
    final public String setStag(String[] line, ArrayList<String[]> tmpSent) {
        String stag, parent;        
        boolean lchild = false, rchild = false;

        if (this.INDEX > Integer.parseInt(line[6])) parent = line[7] + "/L";
        else parent = line[7] + "/R";            
            
        for (int j=0; j<tmpSent.size(); ++j) {                
            if (this.INDEX == j) continue;
            
            String[] word = tmpSent.get(j);
            int head = Integer.parseInt(word[6]);

            if (this.INDEX == head){
                if (this.INDEX > j) lchild = true;        
                else rchild = true;
            }
        }

        if (lchild && rchild) stag = parent + "_L_R";         
        else if (lchild) stag = parent + "_L";
        else if (rchild) stag = parent + "_R";
        else stag = parent;
        
        return stag;
    }


}
