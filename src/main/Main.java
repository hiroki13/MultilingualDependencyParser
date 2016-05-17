/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

/**
 *
 * @author hiroki
 */

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("\nSYSTEM START\n");
        Mode mode = new Mode(args);
        mode.setup();
    }
}