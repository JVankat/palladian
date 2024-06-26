package ws.palladian.extraction.entity.dataset;

import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.io.FileHelper;

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternAnalyzer {

    private StringBuilder analyzePatterns(String type, String xml, Pattern pattern) {

        Bag<String> patternPrefixCounts = new Bag<>();
        Bag<String> patternSuffixCounts = new Bag<>();

        Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(2);
            patternPrefixCounts.add(prefix.toLowerCase());
            patternSuffixCounts.add(suffix.toLowerCase());
        }

        StringBuilder tsv = new StringBuilder();
        tsv.append(type).append("\n\n");
        tsv.append("PREFIX PATTERNS\n");
        for (Entry<String, Integer> entry : patternPrefixCounts.createSorted(Order.DESCENDING).unique()) {
            tsv.append(entry.getKey());
            tsv.append("\t");
            tsv.append(entry.getValue());
            tsv.append("\n");
        }
        tsv.append("\nSUFFIX PATTERNS\n");
        for (Entry<String, Integer> entry : patternSuffixCounts.createSorted(Order.DESCENDING).unique()) {
            tsv.append(entry.getKey());
            tsv.append("\t");
            tsv.append(entry.getValue());
            tsv.append("\n");
        }

        return tsv;
    }

    public void analyzePatterns(String filePath, String resultFilePath) {

        String[] types = {"LOCATION", "ORGANIZATION", "PERSON"};

        String w = "[\\w,:;-]+";
        String p = "(" + w + "\\s" + w + "\\s" + w + ")\\s\\<TYPE\\>.*?\\</TYPE\\>\\s(" + w + "\\s" + w + "\\s" + w + ")";

        String xml = FileHelper.tryReadFileToString(filePath);

        StringBuilder tsv = new StringBuilder();
        for (String type : types) {
            StringBuilder sb = analyzePatterns(type, xml, Pattern.compile(p.replace("TYPE", type)));
            tsv.append("\n");
            tsv.append(sb);
        }

        FileHelper.writeToFile(resultFilePath, tsv);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new PatternAnalyzer().analyzePatterns("allCleansed.xml", "analyzedPatterns.tsv");

    }

}
