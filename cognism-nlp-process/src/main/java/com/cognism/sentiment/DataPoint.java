package com.cognism.sentiment;

import java.util.List;
import java.util.Vector;

public class DataPoint {

    public String phrase;
    public List<Double> scores;
    public List<String> evidences;
    public double verdict;

    public DataPoint(String phrase) {
        this.phrase = phrase;
        scores = new Vector<Double>();
        evidences = new Vector<String>();
    }

    @Override
    public String toString() {
        String ret = phrase + "\n" + "\t" + Utils.f(verdict) + "\n";
        int n = scores.size();
        for (int i = 0; i < n; i++) {
            ret += "\t\t" + Utils.f(scores.get(i)) + " \"" + evidences.get(i) + "\"\n";
        }
        return ret;
    }
}
