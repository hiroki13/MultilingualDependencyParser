/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ling;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author hiroki
 */

final public class Dict implements Serializable{
    final public HashMap<String, Integer> keys = new HashMap(); 
    final public HashMap<Integer, String> values = new HashMap(); 

    public Dict() {}
    
    final public String getKey(int value) {
        return values.get(value);
    }

    final public Integer getValue(String key) {
        if (!keys.containsKey(key)) {
            int value = keys.size();
            keys.put(key, value);
            values.put(value, key);
        }
        return keys.get(key);
    }

    final public Integer getHashValue(String key) {
        int value = key.hashCode();
        if (!values.containsKey(value)) values.put(value, key);
        return value;
    }
    
    final public int size() {
        return keys.size();                
    }

}
