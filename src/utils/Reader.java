/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import ling.Sentence;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author hiroki
 */

final public class Reader {
    final String DELIMITER = "\t";
    final String[] ROOT = {"0", "_ROOT_", "-", "ROOT", "ROOT", "ROOT", "-1", "PAD"};

    final public Sentence[] read(String fn, int n_sents, boolean isStag, boolean test) throws Exception{
        if (fn == null) return null;

        String line;
        int sentIndex = 0;
        ArrayList<Sentence> sentList = new ArrayList<>();
        ArrayList<String[]> tmpSent = new ArrayList<>();
        if (!isStag) tmpSent.add(ROOT);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fn)))){
            while ((line=br.readLine()) != null){
                if (line.length() == 0){
                    sentList.add(new Sentence(sentIndex++, tmpSent, isStag, test));

                    if (sentList.size() == n_sents) break;

                    tmpSent = new ArrayList<>();
                    if (!isStag) tmpSent.add(ROOT);

                    continue;
                }

                tmpSent.add(line.split(DELIMITER));
            }

            if(tmpSent.size() > 1 || (isStag && tmpSent.size() > 0))
                sentList.add(new Sentence(sentIndex++, tmpSent, isStag, test));
        }
        
        Sentence[] sents = new Sentence[sentIndex];
        for (int i=0; i<sentIndex; ++i) sents[i] = sentList.get(i);

        return sents;
    }

}
