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

public class Hypothesis {
    final public int label;
    public double score;
    public int[] phi;
    public int[] phi_l;
    public Hypothesis prevHypo;

    public Hypothesis() {
        this.label = -1;
        this.score = 0.0d;
        this.prevHypo = new Hypothesis(-2, 0.0d);
    }
    
    public Hypothesis(int label, double score) {
        this.label = label;
        this.score = score;
    }
    
    public Hypothesis(int label, double score, int[] phi, int[] phi_l) {
        this.label = label;
        this.score = score;
        this.phi = phi;
        this.phi_l = phi_l;
    }
    
    public Hypothesis(int label, double score, int[] phi, int[] phi_l, Hypothesis prevHypo) {
        this.label = label;
        this.score = prevHypo.score + score;
        this.phi = phi;
        this.phi_l = phi_l;
        this.prevHypo = prevHypo;
    }
    
    final public Hypothesis[] reverse(int n_tokens) {
        Hypothesis[] r = new Hypothesis[n_tokens];
        Hypothesis h = this;
        int k = 0;

        while(h.label > -1) {
            r[n_tokens-1-k] = h;
            h = h.prevHypo;
            k ++;
        }
        return r;
    }

    final public ArrayList<Integer> getLabels() {
        ArrayList<Integer> labels = new ArrayList<>();
        Hypothesis h = this;
        while(h.prevHypo != null) {
            labels.add(h.label);
            h = h.prevHypo;
        }
        return labels;
    }
    
}
