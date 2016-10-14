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
    public static boolean IS_GOLD_STAG;

    final public static Dict vocabForm = new Dict();
    final public static Dict vocabCPos = new Dict();
    final public static Dict vocabPos = new Dict();
    final public static Dict vocabStag = new Dict();
    final public static Dict vocabLabel = new Dict();    

    final public int INDEX;
    final public String O_FORM;
    final public String O_CPOS;
    final public String O_POS;
    public String O_STAG;
    final public int O_HEAD;
    final public String O_LABEL;

    final public int FORM;
    final public int CPOS;
    final public int POS;
    public int STAG;
    final public int LABEL;
    
    public Token(String[] line, ArrayList<String[]> tmpSent) {
        this.INDEX = Integer.parseInt(line[0]);
        this.O_FORM = line[1];
        this.O_CPOS = line[3];
        this.O_POS = line[4];
        if (IS_GOLD_STAG == false)
            this.O_STAG = line[5];
        this.O_HEAD = Integer.parseInt(line[6]);
        this.O_LABEL = line[7];

        this.FORM = vocabForm.getHashValue(this.O_FORM);
        this.CPOS = vocabCPos.getHashValue(this.O_CPOS);
        if ("_".equals(line[4])) this.POS = vocabPos.getHashValue(this.O_CPOS);
        else this.POS = vocabPos.getHashValue(this.O_POS);
        if (IS_GOLD_STAG == false)
            this.STAG = vocabStag.getHashValue(this.O_STAG);
        this.LABEL = vocabLabel.getValue(this.O_LABEL);        
    }
}
