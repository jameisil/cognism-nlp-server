package com.cognism.webservices;

import java.util.Objects;

public class Cognitive {

    private String phrase;
  
    private String sentiment;
   
    private String sentimentScore;
    
    public Cognitive() {
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(String sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.phrase);
        hash = 37 * hash + Objects.hashCode(this.sentiment);
      
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cognitive other = (Cognitive) obj;
        if (!Objects.equals(this.phrase, other.phrase)) {
            return false;
        }
        if (!Objects.equals(this.sentiment, other.sentiment)) {
            return false;
        }
      
        return true;
    }

    @Override
    public String toString() {
        return "Cognitive{" + "phrase=" + phrase + ", sentiment=" + sentiment + ", sentimentScore=" + sentimentScore + '}';
    }

   
}
