package com.cognism.sentiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import opennlp.tools.cmdline.parser.ParserModelLoader;
import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Span;

import com.cognism.tartarus.snowball.SnowballStemmer;
import com.cognism.tartarus.snowball.ext.englishStemmer;

import com.ibm.icu.text.BreakIterator;

public class LanguageUtils {

    private static String DATA_URL = "C:/cognismprocess/nlpdata";
    private static final String STOPS = "i me my myself we our ours ourselves "
            + "you your yours yourself yourselves he him his himself she her "
            + "hers herself it its itself they them their theirs themselves "
            + "what which who whom this that these those am is are was were "
            + "be been being have has had having do does did doing would "
            + "should could ought i'm you're he's she's it's we're they're "
            + "i've you've we've they've i'd you'd he'd she'd we'd they'd i'll "
            + "you'll he'll she'll we'll they'll isn't aren't wasn't weren't "
            + "hasn't haven't hadn't doesn't don't didn't won't wouldn't shan't "
            + "shouldn't can't cannot couldn't mustn't let's that's who's what's "
            + "here's there's when's where's why's how's a an the and but if or "
            + "because as until while of at by for with about against between "
            + "into through during before after above below to from up down in "
            + "out on off over under again further then once here there when "
            + "where why how all any both each few more most other some such "
            + "no nor not only own same so than too very";

    private static Set<String> STOPWORDS = new HashSet<String>();

    static {
        String[] ws = STOPS.split(" ");
        for (String w : ws) {
            STOPWORDS.add(w);
        }
    }

    public static boolean isStopWord(String w) {
        return STOPWORDS.contains(w);
    }

    // from http://www.aclweb.org/anthology/W10-3110
    private static final String NEGATORS = "hardly lack lacking lacks neither "
            + "nor never no nobody none nothing nowhere not aint cant cannot "
            + "darent dont doesnt didnt hadnt hasnt havnt havent isnt mightnt "
            + "mustnt neednt oughtnt shant shouldnt wasnt wouldnt without";

    private static Set<String> NEGATOR_WORDS = new HashSet<String>();

    static {
        String[] ws = NEGATORS.split(" ");
        for (String w : ws) {
            NEGATOR_WORDS.add(w);
        }
    }

    public static boolean isNegator(String w) {
        return NEGATOR_WORDS.contains(w);
    }

    private static Map<String, Double> intensifierMap;

    static {
        intensifierMap = new HashMap<String, Double>();
        List<String> lines = Utils.readTextLines(DATA_URL + "/intensifiers.txt");
        for (String line : lines) {
            String[] parts = line.split("\t");
            intensifierMap.put(parts[0], Double.parseDouble(parts[1]));
        }
    }

    public static double getIntensifierRatio(TextSpan token) {
        String pos = token.partOfSpeech;
        // adverbs only
        if (!pos.startsWith("RB")) {
            return 1;
        }
        Double d = intensifierMap.get(token.text);
        if (d == null) {
            return 1;
        }
        return d;
    }

    private static BreakIterator wordIterator = BreakIterator.getWordInstance(Locale.US);

    public static List<TextSpan> tokenize(String text, boolean addSpaces) {
        List<TextSpan> ret = new Vector<TextSpan>();
        wordIterator.setText(text);
        int start = wordIterator.first();
        while (true) {
            int end = wordIterator.next();
            if (end == BreakIterator.DONE) {
                break;
            }
            String word = text.substring(start, end);
            if (addSpaces || !word.equals(" ")) {
                ret.add(new TextSpan(word, start, end));
            }
            start = end;
        }
        return ret;
    }

    private static BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(Locale.US);

    public static List<String> sentenceSplitter(String text) {
        List<String> ret = new Vector<String>();
        sentenceIterator.setText(text);
        int start = sentenceIterator.first();
        while (true) {
            int end = sentenceIterator.next();
            if (end == BreakIterator.DONE) {
                break;
            }
            ret.add(text.substring(start, end));
            start = end;
        }
        return ret;
    }

    private static SnowballStemmer stemmer = new englishStemmer();

    public static String stem(String word) {
        stemmer.setCurrent(word);
        stemmer.stem();
        String stem = stemmer.getCurrent();
        return stem;
    }

    private static POSTaggerME posTagger;

    public static void initPosTagger() throws Exception {
        InputStream modelStream = new FileInputStream(DATA_URL + "/en-pos-maxent.bin");
        POSModel model = new POSModel(modelStream);
        posTagger = new POSTaggerME(model);
        modelStream.close();
    }

    /*
     * Assumes initPosTagger() was called already.
     * Takes a list of tokens having NO spaces, adds POS info to tokens.
     */
    public static void posTag(List<TextSpan> tokens) {
        int n = tokens.size();
        String[] words = new String[n];
        for (int i = 0; i < n; i++) {
            words[i] = tokens.get(i).text;
        }
        String[] tags = posTagger.tag(words);
        for (int i = 0; i < n; i++) {
            tokens.get(i).partOfSpeech = tags[i];
        }
    }

    private static Parser parser;

    public static void initParser() throws Exception {
        ParserModel model = new ParserModelLoader().load(new File(DATA_URL + "/en-parser-chunking.bin"));
        parser = ParserFactory.create(model,
                AbstractBottomUpParser.defaultBeamSize,
                AbstractBottomUpParser.defaultAdvancePercentage);
    }

    /*
     * Assumes initParser() was called already.
     * Takes a list of tokens having NO spaces
     */
    public static Parse parse(List<TextSpan> tokens) {
        StringBuilder sb = new StringBuilder();
        for (TextSpan s : tokens) {
            sb.append(s.text).append(" ");
        }
        String text = sb.substring(0, sb.length() - 1);
        Parse p = new Parse(text, new Span(0, text.length()), AbstractBottomUpParser.INC_NODE, 0, 0);
        int start = 0;
        int i = 0;
        for (TextSpan token : tokens) {
            String word = token.text;
            p.insert(new Parse(text, new Span(start, start + word.length()), AbstractBottomUpParser.TOK_NODE, 0, i));
            start += word.length() + 1;
        }
        Parse ret = parser.parse(p);
        return ret;
    }

    /*
     * Returns spans representing sentence clauses. Input is a parsed and 
     * tokenized sentence. 
     */
    public static TextSpan[] getClauses(String sentence, List<TextSpan> tokens, Parse parse) {
        List<int[]> clauseSpans = new Vector<int[]>();
        getClauseStartPositions(parse, clauseSpans);
        /*	
         * Map positions in the parse to the position in the original sentence.
         * These differ because the parser needs a different tokenization
         * around commas, braces, etc.
         * Eg: original: John (my brother) is nice.
         * Parser version: John ( my brother ) is nice .
         */
        int n = tokens.size();
        Map<Integer, Integer> startsMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> endsMap = new HashMap<Integer, Integer>();
        int len = 0;
        for (int i = 0; i < n; i++) {
            TextSpan token = tokens.get(i);
            startsMap.put(len, token.start);
            endsMap.put(len + token.text.length(), token.end);
            len += token.text.length() + 1;
        }
        n = clauseSpans.size();
        TextSpan[] ret = new TextSpan[n];
        for (int i = 0; i < n; i++) {
            int startP = clauseSpans.get(i)[0];
            int endP = clauseSpans.get(i)[1];
            int start = startsMap.get(startP);
            int end = endsMap.get(endP);
            TextSpan span = new TextSpan(sentence.substring(start, end), start, end);
            ret[i] = span;
        }
        return ret;
    }

    private static void getClauseStartPositions(Parse parse, List<int[]> clauseSpans) {
        int start = parse.getSpan().getStart();
        String type = parse.getType();
        if (type.equals("S")) {
            int end = parse.getSpan().getEnd();
            clauseSpans.add(new int[]{start, end});
        }
        for (Parse p : parse.getChildren()) {
            //Span s = p.getSpan();
            getClauseStartPositions(p, clauseSpans);
            //start = s.getEnd();
        }
    }

    // test only
    public static void main(String[] args) throws Exception {
        //System.out.println(stem("sitting"));

        String text;
        text = "MICROSOFT CORP is showing strong Earnings "
                + "Quality, Cash Flow Quality and Operating Efficiency, and "
                + "Valuation suggests a lower amount of price risk, but Balance "
                + "Sheet Quality is weak.";
        text = "The operating efficiency rating for MSFT doesn't remain STRONG as the SGA costs and EBIT margin strengthened over the last quarter, while at the same time the ROIC, gross margin and net margin weakened.";

        List<TextSpan> words = tokenize(text, false);
        initPosTagger();
        posTag(words);
        for (int i = 0; i < words.size(); i++) {
            TextSpan s = words.get(i);
            System.out.println(i + "\t" + s);
        }
        initParser();
        Parse p = parse(words);
        p.show();
        TextSpan[] clauses = getClauses(text, words, p);
        for (TextSpan s : clauses) {
            System.out.println(s);
        }
    }
}
