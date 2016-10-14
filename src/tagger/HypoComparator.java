/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tagger;

import java.util.ArrayList;

/**
 *
 * @author hiroki
 */

public class HypoComparator extends ArrayList<Hypothesis>{
    final public int BEAM_WIDTH;

    public HypoComparator(int beamWidth) {
        this.BEAM_WIDTH = beamWidth;
    }

    final public void addSort(int label, double score, int[] featureID_t, int[] markovFeatureID, Hypothesis h){
        if (this.size() < BEAM_WIDTH) { 
            this.add(new Hypothesis(label, score, featureID_t, markovFeatureID, h));
            arrangeOrder();
        }
        else{
            int last = this.size() - 1;
            if (this.get(last).score < h.score + score) {
                this.remove(last);
                this.add(new Hypothesis(label, score, featureID_t, markovFeatureID, h));
                arrangeOrder();
            }
        }
    }
    
    private void arrangeOrder() {
        if (this.size() == 1) return;
        for (int i=this.size()-2; i>-1; i--) {
            if (this.get(i).score < this.get(i+1).score) {
                Hypothesis tmp = this.get(i);
                this.set(i, this.get(i+1));
                this.set(i+1, tmp);
            }
            else return;
        }
    }
    
}
