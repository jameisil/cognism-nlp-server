package com.cognism.sentiment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentiWordNet {

    private static String DATA_URL = "C:/cognismprocess/nlpdata";
    private Map<String, double[]> wordScores;
    private Map<String, double[]> stemScores;
    private static final String SEP = ":";

    /*
     * TODO (only if improvment needed, probably good enough)
     * - use all senses (only 1st is used now)
     * - do sense disambiguation
     * - use lemmatizer instead of stemmer
     */
    public SentiWordNet() {
        wordScores = new HashMap<>();
        stemScores = new HashMap<>();
        List<String> lines = Utils.readTextLines(DATA_URL + "/SentiWordNet_3.0.0_20130122.txt");

        /*
         * a
         * 00580805
         * 0.25
         * 0.125
         * striking#2 spectacular#3 salient#1 prominent#1 outstanding#2
         * having a quality that thrusts itself into attention; "an outstanding fact of our time is that nations poisoned by anti semitism proved less fortunate in regard to their own freedom"
         */
        for (String line : lines) {
            String[] parts = line.split("\t");
            String partOfSpeech = parts[0];
            double pos = Double.parseDouble(parts[2]);
            double neg = Double.parseDouble(parts[3]);
            if (pos == 0 && neg == 0) {
                continue; // skip irrelevant words
            }
            String[] words = parts[4].split(" ");
            for (String word : words) {
                String[] pair = word.split("#");
                if (!pair[1].equals("1")) {
                    continue;
                }
                word = pair[0];
                double[] scores = new double[]{pos, neg};
                wordScores.put(partOfSpeech + SEP + word, scores);
                String stem = LanguageUtils.stem(word);
                stemScores.put(partOfSpeech + SEP + stem, scores);
            }
        }
    }

    public double[] getScores(TextSpan token) {
        return getScores(token.text, token.partOfSpeech);
    }

    public double[] getScores(String word, String partOfSpeech) {
        // map from Penn POS to SWN POS
        String POS = null;
        if (partOfSpeech.startsWith("J")) {
            POS = "a";
        } else if (partOfSpeech.startsWith("N")) {
            POS = "n";
        } else if (partOfSpeech.startsWith("RB")) {
            POS = "r";
        } else if (partOfSpeech.startsWith("V")) {
            POS = "v";
        } else {
            return new double[]{0, 0};
        }

        String word_ = word.toLowerCase();
        double[] ret = wordScores.get(POS + SEP + word_);
        if (ret != null) {
            return ret;
        }
        String stem = LanguageUtils.stem(POS + SEP + word_);
        ret = stemScores.get(stem);
        if (ret != null) {
            return ret;
        }
        return new double[]{0, 0};
    }

    // test only
    public static void main(String[] args) {
        SentiWordNet swn = new SentiWordNet();
        double[] scores = swn.getScores("delighted", "JJ");
        System.out.println(scores[0] + " " + scores[1]);
        scores = swn.getScores("delighted", "VBD");
        System.out.println(scores[0] + " " + scores[1]);
    }
}
