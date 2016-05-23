/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parser;

import ling.Sentence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author hiroki
 */

final public class Featurizer implements Serializable{
    final private int WEIGHT_SIZE;
    final private boolean IS_STAG;
    final private int[][] PAIR = {{2,3},{2,4},{1,2},{1,4},{3,4}};
    
    public Featurizer(int w_size, boolean stag) {
        this.WEIGHT_SIZE = w_size;
        this.IS_STAG = stag;
    }

    final public ArrayList<Integer> featurize(Sentence sentence, State state){
        ArrayList<Integer> usedFeatures = new ArrayList<>();
        int[][] feature = new int[150][];
        int k = 0;
        
        Feature[] feats = makeFeatures(state, sentence);

        for(int i=0; i<feats.length; ++i) {
            Feature f = feats[i];
            feature[k++] = new int[]{1, i, f.pos};

            if (f.index != -1){
                feature[k++] = new int[]{2, i, f.form};
                if (IS_STAG) feature[k++] = new int[]{21, i, f.stag};
 
                if (i < 3){
                    if (f.lmc > -1)
                        feature[k++] = new int[]{3, i, f.pos, f.lmc};
                    if (f.rmc  > -1)
                        feature[k++] = new int[]{4, i, f.pos, f.rmc};
                    if (f.lmc > -1 || f.rmc > -1)
                        feature[k++] = new int[]{5, i, f.pos, f.lmc, f.rmc};
                }
            }
            
            int nochild = -1;
            if (f.lmc < 0 && f.rmc < 0) nochild = 1;
            feature[k++] = new int[]{6, i, f.pos, nochild};

            if (f.lmc_index != -1){
                int span = f.index - f.lmc_index;
                feature[k++] = new int[]{7, i, f.pos, span};
            }
            if (f.rmc_index != -1){
                int span = f.rmc_index - f.index;
                feature[k++] = new int[]{8, i, f.pos, span};
            }
        }
        
        for(int i=0; i<PAIR.length; ++i){
            int[] b = PAIR[i];
            int w1 = b[0];
            int w2 = b[1];
            Feature f1 = feats[b[0]];
            Feature f2 = feats[b[1]];
            
            feature[k++] = new int[]{11, i, w1, w2, f1.pos, f2.pos};
            feature[k++] = new int[]{12, i, w1, w2, f1.form, f2.form};
            feature[k++] = new int[]{13, i, w1, w2, f1.form, f2.pos};
            feature[k++] = new int[]{14, i, w1, w2, f1.pos, f2.form};

            if (IS_STAG) {
                feature[k++] = new int[]{31, i, f1.stag, f2.stag};
                feature[k++] = new int[]{32, i, f1.stag, f2.pos};
                feature[k++] = new int[]{33, i, f1.pos, f2.stag};
                feature[k++] = new int[]{34, i, f1.stag, f2.form};
                feature[k++] = new int[]{35, i, f1.form, f2.stag};
            }        

            if (w1 < 3){
                if (f1.lmc > -1 || f2.lmc > -1)
                    feature[k++] = new int[]{15, i, w1, w2, f1.pos, f2.pos, f1.lmc, f2.lmc};
                if (f1.lmc > -1 || f2.rmc > -1)
                    feature[k++] = new int[]{16, i, w1, w2, f1.pos, f2.pos, f1.lmc, f2.rmc};
                if (f1.rmc > -1 || f2.lmc > -1)
                    feature[k++] = new int[]{17, i, w1, w2, f1.pos, f2.pos, f1.rmc, f2.lmc};
                if (f1.rmc > -1 || f2.rmc > -1)
                    feature[k++] = new int[]{18, i, w1, w2, f1.pos, f2.pos, f1.rmc, f2.rmc};
            }
            
        }
        
        for(int i=0; i<feats.length-1; ++i){
            Feature f1 = feats[i];
            Feature f2 = feats[i+1];
            
            if (f1.index > -1 && f2.index > -1){
                int length = f2.index - f1.index;
                feature[k++] = new int[]{101, i, length};
                feature[k++] = new int[]{102, i, f1.pos, f2.pos, length};
            }
        }
        
        for(int i=0; i<k; ++i)
            usedFeatures.add((Arrays.hashCode(feature[i]) >>> 1) % WEIGHT_SIZE);

        return usedFeatures;
    }

    private Feature[] makeFeatures(State state, Sentence sent) {
        Feature[] feats = new Feature[6];
        feats[2] = new Feature(state, sent);
        
        if(state.tail != null){
            feats[1] = new Feature(state.tail, sent);
            if(state.tail.tail != null)
                feats[0] = new Feature(state.tail.tail, sent);
            else
                feats[0] = new Feature();
        }        
        else {
            feats[0] = new Feature();            
            feats[1] = new Feature();
        }

        if(state.b0 < sent.size())
            feats[3] = new Feature(sent.tokens[state.b0]);
        else
            feats[3] = new Feature();

        if(state.b0+1 < sent.size())
            feats[4] = new Feature(sent.tokens[state.b0 + 1]);
        else
            feats[4] = new Feature();

        if(state.b0+2 < sent.size())
            feats[5] = new Feature(sent.tokens[state.b0 + 2]);
        else
            feats[5] = new Feature();
        
        return feats;
    }

}
