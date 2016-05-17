/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.ArrayList;

/**
 *
 * @author hiroki
 */

public class StateComparator extends ArrayList<PredictedAction> {
    
    public StateComparator(){}
    
    final public void addSort(PredictedAction action){
        this.add(action);

        if (this.size() < 2) return;

        for (int i=this.size()-2; i>-1; i--){
            if (this.get(i).score < this.get(i+1).score){
                PredictedAction tmp = this.get(i);
                this.set(i, this.get(i+1));
                this.set(i+1, tmp);
            }
            else return;
        }
    }
}
