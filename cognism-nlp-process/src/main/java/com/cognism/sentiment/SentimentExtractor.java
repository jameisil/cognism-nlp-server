package com.cognism.sentiment;

import com.cognism.webservices.Cognitive;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import opennlp.tools.parser.Parse;
import org.apache.log4j.Logger;

@Service("nlpService")
@Component
//@EnableScheduling
public class SentimentExtractor {

    private static final Logger log = Logger.getLogger(SentimentExtractor.class);
    private boolean doCsvOutput = true;
    private List<String> outputLines;

    private SentiWordNet swn;
    private String[] phrases;
    private Map<String, List<String>> garbagePatternsPerSource;
    private static String FILE_REPOSITORY_URL = "C:/cognismprocess/bodytext/unprocessed";
    private static String FILE_REPOSITORY_OUTPUT = "C:/cognismprocess/cognitives/unprocessed";
    private static String FILE_REPOSITORY_DONE = "C:/cognismprocess/bodytext/processed";
    // private static String FILE_REPOSITORY_DONE = "C:/cognismprocess/cognitives/unprocessed";
    private static String DATA_URL = "C:/cognismprocess/nlpdata";

    public SentimentExtractor() throws Exception {
        log.info("Reading SentiWordNet...");
        swn = new SentiWordNet();
        log.info("Initializing POS tagger...");
        LanguageUtils.initPosTagger();
        log.info("Initializing parser...");
        LanguageUtils.initParser();
        readPhrases();
        readGarbagePatterns();
    }

    private void readGarbagePatterns() throws Exception {
        garbagePatternsPerSource = new HashMap<>();
        File[] patternFiles = new File(DATA_URL + "/cannedTexts").listFiles();
        for (File f : patternFiles) {
            String name = f.getName().replace(".txt", "");
            List<String> lines = Utils.readTextLines(f.getCanonicalPath());
            garbagePatternsPerSource.put(name, lines);
        }
    }

    private void readPhrases() {
        log.info("Reading target phrases...");
        List<String> lines = Utils.readTextLines(DATA_URL + "/target_phrases.txt");
        phrases = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            phrases[i] = lines.get(i).toLowerCase();
        }
        Arrays.sort(phrases, new StringLengthSorter());
    }

    private static File[] getFileList(String dirPath) {
        File dir = new File(dirPath);
        File[] fileList = null;
        try {
            fileList = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".txt");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileList;
    }

    @Scheduled(cron = "*/60 * * * * ?")//every 60 seconds
    public void startScheduler() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        log.info("Keep alive - checking for new files " + dateFormat.format(new Date()));

        File[] fileList = SentimentExtractor.getFileList(FILE_REPOSITORY_URL);

        //Pick up list of new BodyText articles from target location
        for (File file : fileList) {
            log.info("Found bodyText file named " + file.getName());

            //Expect article name in format - SourceID-ArticleID-Type-Source-Company-ArticleName.txt.
            String fileName = file.getName();

            if (fileName.endsWith(".txt")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }

            String delims = "-";
            String[] splitName = fileName.split(delims);

            try {
                String sourceID = splitName[0];
                log.info("SourceID : " + sourceID);
                String articleID = splitName[1];
                log.info("articleID : " + articleID);
                String type = splitName[2];
                log.info("type : " + type);
                String source = splitName[3];
                log.info("source : " + source);
                String company = splitName[4];
                log.info("company: " + company);
                String articleName = splitName[5];

                run(sourceID, file.getPath(), FILE_REPOSITORY_OUTPUT, fileName);

            } catch (Exception e) {
                log.info("ERROR - article name in the wrong format - please check");
            }
            //todo - check article name in right format

            log.info("Content added, extract success ");
            file.renameTo(new File(FILE_REPOSITORY_DONE + "/" + "processed_" + file.getName()));//move to done
        }

        //Call the run process for the target BodyText
    }

    public List<Cognitive> getOutputList(String content) {
        List<Cognitive> list = new ArrayList<Cognitive>();
        try {
            System.out.println("CONTENT RECEIVED------------------");
            System.out.println(content);
            System.out.println("_____________________________________");
            list = this.startNLP(content);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    private List<Cognitive> startNLP(String text) throws Exception {
        List<Cognitive> list = new ArrayList<Cognitive>();
        String[] lines = text.split("\n");
        text = getFlowingText(lines);
        log.info(text);

        String[] paragraphs = text.split("\n");
        //  log.info("paragraphs " + paragraphs);

        Map<String, DataPoint> phraseMap = new LinkedHashMap<>();
        for (String p : paragraphs) {
            List<String> sentences = LanguageUtils.sentenceSplitter(p);
            for (String sentence : sentences) {
                String sentence_ = sentence.toLowerCase();
                // log.info("sentence_ " + sentence_);
                // collect spans where target phrases are found
                List<TextSpan> phraseSpans = new Vector<>();
                for (String phrase : phrases) {
                    int start = -1;
                    while (true) {
                        start = sentence_.indexOf(phrase, start + 1);
                        if (start == -1) {
                            break;
                        }
                        int end = start + phrase.length();
                        boolean ok = true; // don't allow overlaps
                        for (TextSpan s : phraseSpans) {
                            if (!(start > s.end || end < s.start)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            TextSpan span = new TextSpan(phrase, start, end);
                            phraseSpans.add(span);
                        }
                    }
                } // for phrases
                if (phraseSpans.isEmpty()) {
                    continue;
                }

                List<TextSpan> tokens = LanguageUtils.tokenize(sentence, false);
                Map<Integer, Integer> startToTokenIndexMap = new HashMap<>();
                for (int i = 0; i < tokens.size(); i++) {
                    TextSpan t = tokens.get(i);
                    startToTokenIndexMap.put(t.start, i);
                }

                // POS tagging needed for SentiWordNet
                LanguageUtils.posTag(tokens);

                // parse to find clauses
                Parse parse = LanguageUtils.parse(tokens);
                TextSpan[] clauses = LanguageUtils.getClauses(sentence, tokens, parse);
                // can't find clauses: probably an invalid sentence
                if (clauses.length == 0) {
                    continue;
                }

                // score clauses
                Map<TextSpan, Double> clauseToScoreMap = new HashMap<>();
                for (TextSpan clause : clauses) {
                    boolean isNegated = false;
                    double score = 0;
                    double previousBoost = 1;
                    int n = 0;
                    for (TextSpan token : tokens) {
                        if (token.start < clause.start || token.start >= clause.end) {
                            continue;
                        }
                        String word = token.text;
                        if (word.length() == 1) {
                            continue; // ignore punctuation
                        }
                        if (LanguageUtils.isNegator(word)) {
                            /*
                             * Trivial negation detection: span of negator
                             * word is from the word up to the end of clause.
                             * TODO improve if needed
                             */
                            isNegated = true;
                        }
                        boolean ok = true; // don't allow overlaps with phrases
                        for (TextSpan s : phraseSpans) {
                            if (!(token.start > s.end || token.end < s.start)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            double[] scores = swn.getScores(token);
                            double diff = scores[0] - scores[1];
                            if (diff != 0) {
                                if (isNegated) {
                                    diff = -diff;
                                }
                                // apply previous token's intensifier boost
                                // (only in non-negated contexts)
                                if (!isNegated) {
                                    diff *= previousBoost;
                                    // cap score to [-1, 1]
                                    diff = Math.min(diff, 1);
                                    diff = Math.max(diff, -1);
                                }
                                score += diff;
                                n++;
                            }
                            previousBoost = LanguageUtils.getIntensifierRatio(token);
                        } else {
                            previousBoost = 1;
                        }
                    } // for tokens
                    if (n > 0) {
                        score /= n;
                    }
                    clauseToScoreMap.put(clause, score);
                }

                // For each phrase find the smallest enclosing clause
                // Keep track of phrase-clause pairs to avoid duplication
                Set<String> phraseClausePairs = new HashSet<>();
                for (TextSpan phrase : phraseSpans) {
                    // ignore if phrase doesn't start on token, like
                    // ROIC in heroic
                    if (!startToTokenIndexMap.containsKey(phrase.start)) {
                        continue;
                    }
                    // ignore if out of clause
                    TextSpan bestClause = null;
                    int minLength = Integer.MAX_VALUE;
                    for (TextSpan clause : clauses) {
                        if (phrase.start < clause.start || phrase.start >= clause.end) {
                            continue;
                        }
                        int len = clause.end - clause.start;
                        if (len < minLength) {
                            minLength = len;
                            bestClause = clause;
                        }
                    }
                    if (bestClause == null) {
                        continue; // shouldn't happen
                    }
                    String phrase_ = phrase.text.toLowerCase();
                    String key = phrase_ + " - " + bestClause.text;
                    if (phraseClausePairs.contains(key)) {
                        continue;
                    }
                    phraseClausePairs.add(key);
                    DataPoint dp = phraseMap.get(phrase_);
                    if (dp == null) {
                        dp = new DataPoint(phrase_);
                        phraseMap.put(phrase_, dp);
                    }
                    dp.scores.add(clauseToScoreMap.get(bestClause));
                    dp.evidences.add(bestClause.text);
                }

            } // for sentences
        } // for paragraphs

        // go through data, calculate averages
        double documentScore = 0;
        for (String phrase : phraseMap.keySet()) {
            DataPoint dp = phraseMap.get(phrase);
            double sum = 0;
            for (double score : dp.scores) {
                sum += score;
            }
            dp.verdict = sum / dp.scores.size();
            documentScore += dp.verdict;
        }
        if (phraseMap.size() > 0) {
            documentScore /= phraseMap.size();
        }

        //addCsvLineFront("phrase", "snippet", "score");
       // addCsvLine("Document sentiment", "---", Utils.f(documentScore));
        log.info("--NLP OUTPUT");
        Cognitive cognitive = null;
        for (String phrase : phraseMap.keySet()) {
            cognitive = new Cognitive();
            DataPoint dp = phraseMap.get(phrase);
            //log.info(dp);
            //addCsvLine(dp.phrase, "avg", Utils.f(dp.verdict));
            cognitive.setPhrase(dp.phrase);
            cognitive.setSentiment("avg");
            cognitive.setSentimentScore(Utils.f(dp.verdict));
            int n = dp.scores.size();
            for (int i = 0; i < n; i++) {
                cognitive.setPhrase(dp.phrase);
                cognitive.setSentiment(dp.evidences.get(i));
                cognitive.setSentimentScore(Utils.f(dp.scores.get(i)));
               // addCsvLine(dp.phrase, dp.evidences.get(i), Utils.f(dp.scores.get(i)));
                list.add(cognitive);
            }
        }
        //writeCsvOutput(outputPath, outputName);
        return list;
    }

    private void run(String sourceId, String inputPath, String outputPath, String outputName) throws Exception {
        log.info("Extracting data from " + inputPath + "...");
        prepareCsvOutput();
        String text = Utils.readTextFile(inputPath);
        log.info("sourceId " + sourceId);
        text = removeCannedText(sourceId, text);
        String[] lines = text.split("\n");
        text = getFlowingText(lines);
        log.info(text);

        String[] paragraphs = text.split("\n");
        //  log.info("paragraphs " + paragraphs);

        Map<String, DataPoint> phraseMap = new LinkedHashMap<>();
        for (String p : paragraphs) {
            List<String> sentences = LanguageUtils.sentenceSplitter(p);
            for (String sentence : sentences) {
                String sentence_ = sentence.toLowerCase();
                // log.info("sentence_ " + sentence_);
                // collect spans where target phrases are found
                List<TextSpan> phraseSpans = new Vector<>();
                for (String phrase : phrases) {
                    int start = -1;
                    while (true) {
                        start = sentence_.indexOf(phrase, start + 1);
                        if (start == -1) {
                            break;
                        }
                        int end = start + phrase.length();
                        boolean ok = true; // don't allow overlaps
                        for (TextSpan s : phraseSpans) {
                            if (!(start > s.end || end < s.start)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            TextSpan span = new TextSpan(phrase, start, end);
                            phraseSpans.add(span);
                        }
                    }
                } // for phrases
                if (phraseSpans.isEmpty()) {
                    continue;
                }

                List<TextSpan> tokens = LanguageUtils.tokenize(sentence, false);
                Map<Integer, Integer> startToTokenIndexMap = new HashMap<>();
                for (int i = 0; i < tokens.size(); i++) {
                    TextSpan t = tokens.get(i);
                    startToTokenIndexMap.put(t.start, i);
                }

                // POS tagging needed for SentiWordNet
                LanguageUtils.posTag(tokens);

                // parse to find clauses
                Parse parse = LanguageUtils.parse(tokens);
                TextSpan[] clauses = LanguageUtils.getClauses(sentence, tokens, parse);
                // can't find clauses: probably an invalid sentence
                if (clauses.length == 0) {
                    continue;
                }

                // score clauses
                Map<TextSpan, Double> clauseToScoreMap = new HashMap<>();
                for (TextSpan clause : clauses) {
                    boolean isNegated = false;
                    double score = 0;
                    double previousBoost = 1;
                    int n = 0;
                    for (TextSpan token : tokens) {
                        if (token.start < clause.start || token.start >= clause.end) {
                            continue;
                        }
                        String word = token.text;
                        if (word.length() == 1) {
                            continue; // ignore punctuation
                        }
                        if (LanguageUtils.isNegator(word)) {
                            /*
                             * Trivial negation detection: span of negator
                             * word is from the word up to the end of clause.
                             * TODO improve if needed
                             */
                            isNegated = true;
                        }
                        boolean ok = true; // don't allow overlaps with phrases
                        for (TextSpan s : phraseSpans) {
                            if (!(token.start > s.end || token.end < s.start)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            double[] scores = swn.getScores(token);
                            double diff = scores[0] - scores[1];
                            if (diff != 0) {
                                if (isNegated) {
                                    diff = -diff;
                                }
                                // apply previous token's intensifier boost
                                // (only in non-negated contexts)
                                if (!isNegated) {
                                    diff *= previousBoost;
                                    // cap score to [-1, 1]
                                    diff = Math.min(diff, 1);
                                    diff = Math.max(diff, -1);
                                }
                                score += diff;
                                n++;
                            }
                            previousBoost = LanguageUtils.getIntensifierRatio(token);
                        } else {
                            previousBoost = 1;
                        }
                    } // for tokens
                    if (n > 0) {
                        score /= n;
                    }
                    clauseToScoreMap.put(clause, score);
                }

                // For each phrase find the smallest enclosing clause
                // Keep track of phrase-clause pairs to avoid duplication
                Set<String> phraseClausePairs = new HashSet<>();
                for (TextSpan phrase : phraseSpans) {
                    // ignore if phrase doesn't start on token, like
                    // ROIC in heroic
                    if (!startToTokenIndexMap.containsKey(phrase.start)) {
                        continue;
                    }
                    // ignore if out of clause
                    TextSpan bestClause = null;
                    int minLength = Integer.MAX_VALUE;
                    for (TextSpan clause : clauses) {
                        if (phrase.start < clause.start || phrase.start >= clause.end) {
                            continue;
                        }
                        int len = clause.end - clause.start;
                        if (len < minLength) {
                            minLength = len;
                            bestClause = clause;
                        }
                    }
                    if (bestClause == null) {
                        continue; // shouldn't happen
                    }
                    String phrase_ = phrase.text.toLowerCase();
                    String key = phrase_ + " - " + bestClause.text;
                    if (phraseClausePairs.contains(key)) {
                        continue;
                    }
                    phraseClausePairs.add(key);
                    DataPoint dp = phraseMap.get(phrase_);
                    if (dp == null) {
                        dp = new DataPoint(phrase_);
                        phraseMap.put(phrase_, dp);
                    }
                    dp.scores.add(clauseToScoreMap.get(bestClause));
                    dp.evidences.add(bestClause.text);
                }

            } // for sentences
        } // for paragraphs

        // go through data, calculate averages
        double documentScore = 0;
        for (String phrase : phraseMap.keySet()) {
            DataPoint dp = phraseMap.get(phrase);
            double sum = 0;
            for (double score : dp.scores) {
                sum += score;
            }
            dp.verdict = sum / dp.scores.size();
            documentScore += dp.verdict;
        }
        if (phraseMap.size() > 0) {
            documentScore /= phraseMap.size();
        }

        addCsvLineFront("phrase", "snippet", "score");
        addCsvLine("Document sentiment", "---", Utils.f(documentScore));
        log.info("--------------------------------------START LINES");
        for (String phrase : phraseMap.keySet()) {
            DataPoint dp = phraseMap.get(phrase);
            log.info(dp);
            addCsvLine(dp.phrase, "avg", Utils.f(dp.verdict));
            int n = dp.scores.size();
            for (int i = 0; i < n; i++) {
                addCsvLine(dp.phrase, dp.evidences.get(i), Utils.f(dp.scores.get(i)));
            }
        }
        writeCsvOutput(outputPath, outputName);
    }

    private void prepareCsvOutput() {
        if (doCsvOutput) {
            outputLines = new Vector<>();
        }
    }

    private void addCsvLine(String... fields) {
        if (!doCsvOutput) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : fields) {
            sb.append(Utils.toCsvString(s)).append(",");
        }
        String line = sb.substring(0, sb.length() - 1);
        outputLines.add(line);
    }

    private void addCsvLineFront(String... fields) {
        if (!doCsvOutput) {
            return;
        }
        addCsvLine(fields);
        // move last line to first
        int n = outputLines.size();
        String line = outputLines.remove(n - 1);
        outputLines.add(0, line);
    }

    private void writeCsvOutput(String path, String outputName) throws Exception {
        if (!doCsvOutput) {
            return;
        }
        if (path.contains(".")) {
            path = path.substring(0, path.lastIndexOf("."));
        }
        String name = path + "/" + outputName + ".csv";
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(name), "UTF-8"));
        for (String line : outputLines) {
            out.write(line + "\n");
        }
        out.close();
    }

    private String removeCannedText(String sourceId, String text) {
        List<String> patterns = garbagePatternsPerSource.get(sourceId);
        if (patterns == null) {
            return text; // shouldn't happen
        }
        for (String pattern : patterns) {
            pattern = pattern.replace(" ", "."); // allow new lines or spaces
            int split = pattern.indexOf("\t");
            if (split == -1) { // literal value, remove as it is
                text = text.replaceAll(pattern, "");
            } else {
                String[] parts = pattern.split("\t");
                Pattern startPattern = Pattern.compile(parts[0], Pattern.DOTALL | Pattern.MULTILINE);
                while (true) {
                    Matcher startMatcher = startPattern.matcher(text);
                    if (startMatcher.find()) {
                        int startIndex = startMatcher.start();
                        Pattern endPattern = Pattern.compile(parts[1], Pattern.DOTALL | Pattern.MULTILINE);
                        Matcher endMatcher = endPattern.matcher(text);
                        if (endMatcher.find(startIndex)) {
                            int endIndex = endMatcher.end();
                            if (text.length() > endIndex) {
                                text = text.substring(0, startIndex) + text.substring(endIndex + 1);
                            } else {
                                text = text.substring(0, startIndex);
                            }
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return text;
    }

    private static final int NOT_SURE = 0;
    private static final int GARBAGE = 1;
    private static final int FLOW = 2;

    // tries to classify lines as belonging to the text flow or anything else like
    // charts, section headers, etc
    private String getFlowingText(String[] lines) {
        int n = lines.length;
        int[] lineTypes = new int[n];
        for (int i = 0; i < n; i++) {
            String line = lines[i];
            if (line.toUpperCase().equals(line)) { // all CAPS
                lineTypes[i] = GARBAGE;
                continue;
            }
            List<TextSpan> tokens = LanguageUtils.tokenize(line, false);

            if (line.endsWith(".")) { // suspicious
                lineTypes[i] = FLOW;
                continue;
            }

            // count small caps words relative to all words
            int words = 0;
            int smallCapsWords = 0;
            for (TextSpan t : tokens) {
                String w = t.text;
                boolean ok = true;
                words++;
                for (int j = 0; j < w.length(); j++) {
                    char c = w.charAt(j);
                    if (!Character.isLowerCase(c)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    smallCapsWords++;
                }
            }
            if (words > 3 && smallCapsWords >= words / 2) {
                lineTypes[i] = FLOW;
                continue;
            }

        }
        // go through once more, check neighbours for NOT_SURE
        for (int i = 1; i < n - 1; i++) {
            if (lineTypes[i] == NOT_SURE) {
                if (lineTypes[i - 1] == FLOW || lineTypes[i + 1] == FLOW) {
                    lineTypes[i] = FLOW;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (lineTypes[i] != FLOW) {
                continue;
            }
            String line = lines[i];
            sb.append(line);
            // append space or new line depending on context
            if (line.endsWith(".")
                    || (i < n - 1) && lineTypes[i + 1] != FLOW) {
                sb.append("\n");
            } else {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private class StringLengthSorter implements Comparator<String> {

        @Override
        public int compare(String s1, String s2) {
            return s2.length() - s1.length();
        }
    }
}
