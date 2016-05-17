/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tagger;

import static org.apache.commons.math3.linear.MatrixUtils.createRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author hiroki
 */

public class MatrixPerceptron {
    final private int N_LABELS;
    final public double[][] weight;
    final public double[][] aweight;
    public double t = 1.0d;

    public Hypothesis maxOracleHypo;
    public Hypothesis maxSystemHypo;
    public double maxScoreDiff;

    public MatrixPerceptron(int n_labels, int weight_size){
        this.N_LABELS = n_labels;
        this.weight = new double[weight_size][n_labels];
        this.aweight = new double[weight_size][n_labels];
        this.maxScoreDiff = 1000000.0d;
    }

    final public void setMaxViolationPoint(int t, Hypothesis oracleHypo, Hypothesis systemHypo){
        double scoreDiff =  oracleHypo.score - systemHypo.score;
        if (scoreDiff <= maxScoreDiff) {
            maxOracleHypo = oracleHypo;
            maxSystemHypo = systemHypo;
            maxScoreDiff = scoreDiff;
        }
    }
    
    final public void updateWeights() {
        Hypothesis oh = maxOracleHypo;
        Hypothesis sh = maxSystemHypo;

        while (oh.label > -1) {
            int o_label = oh.label;
            int s_label = sh.label;

            for (int i=0; i<oh.phi.length; ++i) {
                weight[oh.phi[i]][o_label] += 1.0;
                aweight[oh.phi[i]][o_label] += t;
            }
            for (int i=0; i<sh.phi.length; ++i) {
                weight[sh.phi[i]][s_label] -= 1.0;
                aweight[sh.phi[i]][s_label] -= t;
            }
            for (int i=0; i<oh.phi_l.length; ++i) {
                weight[oh.phi_l[i]][o_label] += 1.0;
                aweight[oh.phi_l[i]][o_label] += t;
            }
            for (int i=0; i<sh.phi_l.length; ++i) {
                weight[sh.phi_l[i]][s_label] -= 1.0;
                aweight[sh.phi_l[i]][s_label] -= t;
            }
            
            oh = oh.prevHypo;
            sh = sh.prevHypo;
        }
        
        t += 1.0d;
        maxOracleHypo = new Hypothesis(-1, -1000000.0d);
        maxSystemHypo = new Hypothesis(-1, -1000000.0d);
        maxScoreDiff = 0.0d;
    }
    
    final public RealVector getScore(int[] featureID) {
        RealVector score = createRealVector(new double[N_LABELS]);
        for (int i=0; i<featureID.length; ++i)
            score = score.add(createRealVector(weight[featureID[i]]));
        return score;
    }
    
    final public double getScore(int[] featureID, int label) {
        double score = 0.0;
        for (int i=0; i<featureID.length; ++i)
            score += weight[featureID[i]][label];
        return score;
    }
    
}
