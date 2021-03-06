/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

/**
 *
 * @author hiroki
 */
final public class OptionParser {
    final private String[] args;

    public OptionParser(String[] args){
        this.args = args;
    }
    
    final public boolean isExsist(String key){
        for(String arg:args)
            if(("-" + key).equals(arg)) return true;
        return false;
    }
    
    final public String getString(String key){
        for(int i=0; i<args.length; i++)
            if(("-" + key).equals(args[i])) return args[i+1];
        return null;
    }

    final public String getString(String key, String defaultvalue){
        for(int i=0; i<args.length; i++)
            if(("-" + key).equals(args[i])) return args[i+1];
        return defaultvalue;
    }

    final public int getInt(String key, int defaultvalue){
        for(int i=0; i<args.length; i++)
            if(("-" + key).equals(args[i]))
                return Integer.valueOf(args[i+1]);
        return defaultvalue;
    }
    
    final public float getFloat(String key, float defaultvalue){
        for(int i=0; i<args.length; i++)
            if(("-" + key).equals(args[i]))
                return Float.valueOf(args[i+1]);
        return defaultvalue;
    }
    
}
