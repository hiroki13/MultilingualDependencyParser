/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tagger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import ling.Sentence;
import ling.Token;

/**
 *
 * @author hiroki
 */

final public class SequentialFeaturizer implements Serializable {
    final private int WEIGHT_SIZE;
    final private int WINDOW_SIZE;
    final private int SLIDE;
    final private int[][] bi = {{0,1},{0,2},{0,3},{-1,0},{-2,0},{-3,0},{1,2},{-2,-1}};
    final private int BOS = Token.vocabForm.getValue("<BOS>");
    final private int EOS = Token.vocabForm.getValue("<EOS>");
    final private HashMap<String, Integer> phi_dict;
    int k = 0;

    public SequentialFeaturizer(int weightSize, int windowSize) {
        WEIGHT_SIZE = weightSize;
        WINDOW_SIZE = windowSize;
        SLIDE = WINDOW_SIZE / 2;
        phi_dict = new HashMap<>();
    }
/*
    final public int[] featurize(Sentence sentence, int w_i) {
        return getFeatureID(getFeature(sentence, w_i));
    }
*/
    final public int[] featurize(Sentence sentence, int w_i) {
        return getFeatureID(getStrFeature(sentence, w_i), false);
    }

    final public int[] featurize(Sentence sentence, int w_i, int label1, int label2) {
        return getFeatureID(getStrMarkovFeature(sentence, w_i), label1, label2, false);
    }

    final public int[][] getFeature(Sentence sent, int w_i) {
        k = 0;
        Token[] tokens = sent.tokens;
        int[][] feature = new int[30][];
        
        for(int i=-SLIDE; i<SLIDE+1; ++i) {
            int index = w_i + i;
            int[] phi = getPhi(index, tokens);
            int form = phi[0];
            int pos = phi[1];

            feature[k++] = new int[]{1, i, form};
            feature[k++] = new int[]{2, i, pos};
        }
        
        for(int i=0; i<bi.length; ++i) {
            int[] indices = bi[i];
            int index1 = w_i + indices[0];
            int index2 = w_i + indices[1];
            int[] phi1 = getPhi(index1, tokens);
            int[] phi2 = getPhi(index2, tokens);
            
            feature[k++] = new int[]{11, i, phi1[1], phi2[1]};
            feature[k++] = new int[]{12, i, phi1[0], phi2[0]};
        }
        
        return feature;
    }

    final public String[] getStrFeature(Sentence sent, int w_i) {
        k = 0;
        Token[] tokens = sent.tokens;
        String[] feature = new String[30];
        
        for(int i=-SLIDE; i<SLIDE+1; ++i) {
            int index = w_i + i;
            String[] phi = getStrPhi(index, tokens);
            String form = phi[0];
            String pos = phi[1];

            feature[k++] = "1_" + i + "_" + form;
            feature[k++] = "2_" + i + "_" + pos;
        }
        
        for(int i=0; i<bi.length; ++i) {
            int[] indices = bi[i];
            int index1 = w_i + indices[0];
            int index2 = w_i + indices[1];
            String[] phi1 = getStrPhi(index1, tokens);
            String[] phi2 = getStrPhi(index2, tokens);
            
            feature[k++] = "11_" + i + "_" + phi1[1] + "_" + phi2[1];
            feature[k++] = "12_" + i + "_" + phi1[0] + "_" + phi2[0];
        }
        
        return feature;
    }

    final public int[][] getMarkovFeature(Sentence sentence, int w_i) {
        k = 0;
        Token[] tokens = sentence.tokens;
        int[][] feature = new int[6][];
        
        Token t = tokens[w_i];
        feature[k++] = new int[]{100, -1};
        feature[k++] = new int[]{101, -1, t.POS};
        feature[k++] = new int[]{102, -1};
        feature[k++] = new int[]{103, -1, t.POS};
        feature[k++] = new int[]{104, -1, -1};
        feature[k++] = new int[]{105, -1, -1, t.POS};
        
        return feature;
    }
    
    final public String[] getStrMarkovFeature(Sentence sentence, int w_i) {
        k = 0;
        Token[] tokens = sentence.tokens;
        String[] feature = new String[6];
        
        Token t = tokens[w_i];
        feature[k++] = "100_";
        feature[k++] = "101_" + t.O_POS + "_";
        feature[k++] = "102_";
        feature[k++] = "103_" + t.O_POS + "_";
        feature[k++] = "104_";
        feature[k++] = "105_" + t.O_POS + "_";
        
        return feature;
    }
    
    private int[] getPhi(int index, Token[] tokens) {           
        int[] phi = new int[2];
            
        if (index < 0) {
            phi[0] = BOS;            
            phi[1] = BOS;            
        }
        else if (index >= tokens.length) {
            phi[0] = EOS;
            phi[1] = EOS;
        }
        else {
            Token t = tokens[index];
            phi[0] = t.FORM;
            phi[1] = t.POS;
        }
        
        return phi;
    }

    private String[] getStrPhi(int index, Token[] tokens) {           
        String[] phi = new String[2];
            
        if (index < 0) {
            phi[0] = "<BOS>";            
            phi[1] = "<BOS>";            
        }
        else if (index >= tokens.length) {
            phi[0] = "<EOS>";
            phi[1] = "<EOS>";
        }
        else {
            Token t = tokens[index];
            phi[0] = t.O_FORM;
            phi[1] = t.O_POS;
        }
        
        return phi;
    }

    final public int[] getFeatureID(int[][] feature) {
        int[] featureID = new int[k];
        for (int i=0; i<featureID.length; ++i)
            featureID[i] = (Arrays.hashCode(feature[i]) >>> 1) % WEIGHT_SIZE;            
        return featureID;
    }

    final public int[] getFeatureID(String[] feature, boolean test) {
        int c = 0;
        int[] featureID = new int[k];
        for (int i=0; i<featureID.length; ++i) {
            String p = feature[i];

            if (!phi_dict.containsKey(p)) {
                if (!test) {
                    phi_dict.put(p, phi_dict.size());
                    featureID[i] = phi_dict.get(p);
                }
            }
            else {
                c += 1;
                featureID[i] = phi_dict.get(p);
            }
        }
        if (test) featureID = Arrays.copyOfRange(featureID, 0, c);
        return featureID;
    }

    final public int[] getFeatureID(int[][] feature, int label1, int label2) {
        int[] featureID = new int[k];
        for (int i=0; i<featureID.length; ++i) {
            int[] p = feature[i];
            if (i < 2) p[1] = label1;
            else if (i < 4) p[1] = label2;
            else {
                p[1] = label1;
                p[2] = label2;
            }
            featureID[i] = (Arrays.hashCode(p) >>> 1) % WEIGHT_SIZE;            
        }
        return featureID;
    }

    final public int[] getFeatureID(String[] feature, int label1, int label2, boolean test) {
        int c = 0;
        int[] featureID = new int[k];
        for (int i=0; i<featureID.length; ++i) {
            String p = feature[i];

            if (i < 2) p += label1;
            else if (i < 4) p += label2;
            else p += label1 + "_" + label2;

            if (!phi_dict.containsKey(p)) {
                if (!test) {
                    phi_dict.put(p, phi_dict.size());
                    featureID[i] = phi_dict.get(p);
                }
            }
            else {
                c += 1;
                featureID[i] = phi_dict.get(p);
            }
        }
        if (test) featureID = Arrays.copyOfRange(featureID, 0, c);
        return featureID;
    }

}
