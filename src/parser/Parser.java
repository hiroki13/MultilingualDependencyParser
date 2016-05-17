/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import ling.Sentence;
import java.util.ArrayList;

/**
 *
 * @author hiroki
 */

public class Parser {
    public int BEAM_WIDTH;
    public boolean STAG;
    public Perceptron perceptron;
    public Featurizer featurizer;
    public Sentence[] trainData;
    final public ArrayList<Integer> trainIndex = new ArrayList<>();
    public ArrayList<State>[] oracles;
    
    public Parser(){}

    public void train(Sentence[] sents){}    

    public void train(){}    

    public void decode(Sentence sentence, ArrayList<State> r_oracle){}

    public State[] decode(Sentence sentence){
        return new State[1];
    }
    
}
    
    

