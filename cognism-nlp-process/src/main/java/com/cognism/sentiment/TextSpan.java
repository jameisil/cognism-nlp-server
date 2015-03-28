package com.cognism.sentiment;

public class TextSpan { // also a token
	public String text;
	public int start;
	public int end;
	public String partOfSpeech;
	
	public TextSpan(String t, int start, int end) {
		text = t;
		this.start = start;
		this.end = end;
		partOfSpeech = "-";
	}

	@Override
	public String toString() {
		return text + " (" + start + "-" + end + ") " + partOfSpeech;
	}
}
