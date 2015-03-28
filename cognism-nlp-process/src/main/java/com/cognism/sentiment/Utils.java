package com.cognism.sentiment;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Vector;

public class Utils {

    private static DecimalFormat df;

    static {
        String pattern = "0.###";
        df = new DecimalFormat(pattern);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);
    }

    /**
     * Pretty print a double
     *
     * @param d
     * @return
     */
    public static String f(double d) {
        return df.format(d);
    }

    // Formats a string so that we can put it into a CSV field
    public static String toCsvString(String s) {
        s = s.replace("\"", "'");
        s = s.replace("\r\n", "\n");
        s = s.replace("\t", " "); // not likely
        s = "\"" + s + "\"";
        return s;
    }

    public static List<String> readTextLines(String path) {
        List<String> ret = new Vector<>();
        try {
            InputStream is = new FileInputStream(path);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            LineNumberReader lnr = new LineNumberReader(isr);
            while (true) {
                String line = lnr.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                line = line.replaceAll(" +", " ");
                ret.add(line);
            }
            lnr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String readTextFile(String path) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = new FileInputStream(path);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            LineNumberReader lnr = new LineNumberReader(isr);
            while (true) {
                String line = lnr.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                line = line.replaceAll(" +", " ");
                sb.append(line).append("\n");
            }
            lnr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}
