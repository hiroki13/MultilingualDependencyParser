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
import ling.Token;

/**
 *
 * @author hiroki
 */

final public class Reader {
    final String DELIMITER = "\t";
    final String[] ROOT = {"0", "_ROOT_", "-", "ROOT", "ROOT", "ROOT", "-1", "PAD"};

    final public Sentence[] read(String fn, int dataSizeLimit, boolean isParser, boolean test) throws Exception{
        if (fn == null) return null;

        String line;
        int sentIndex = 0;
        int count = 0;
        ArrayList<Sentence> sentList = new ArrayList<>();
        ArrayList<String[]> tmpSent = new ArrayList<>();
        if (isParser) tmpSent.add(ROOT);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fn)))){
            while ((line=br.readLine()) != null){
                if (line.length() == 0){
                    count++;

                    Sentence sent = new Sentence(sentIndex, tmpSent, test);
                    if (sent.isProjective() || test || !isParser) {
                        if (Token.IS_GOLD_STAG)
                            sent.setGoldStags();
                        sentList.add(sent);
                        sentIndex++;
                    }

                    tmpSent = new ArrayList<>();
                    if (isParser) tmpSent.add(ROOT);

                    if (sentList.size() == dataSizeLimit) break;
                }
                else if (!line.startsWith("#"))
                    tmpSent.add(line.split(DELIMITER));
            }

            if(tmpSent.size() > 1) {
                Sentence sent = new Sentence(sentIndex, tmpSent, test);
                if (sent.isProjective() || test || !isParser) {
                    sentList.add(sent);
                    sentIndex++;                    
                }
            }
        }
        
        Sentence[] sents = new Sentence[sentIndex];
        for (int i=0; i<sentIndex; ++i) sents[i] = sentList.get(i);

        System.out.println(String.format("\tproj: %d  non-proj: %d", sentIndex, count-sentIndex));
        return sents;
    }

}
