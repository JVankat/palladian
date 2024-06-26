package ws.palladian.extraction.date.evaluation;

import org.w3c.dom.Document;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.WebPageDateEvaluator;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.extraction.date.getter.ContentDateGetter;
import ws.palladian.extraction.date.getter.MetaDateGetter;
import ws.palladian.extraction.date.getter.TechniqueDateGetter;
import ws.palladian.extraction.date.getter.UrlDateGetter;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.extraction.date.rater.TechniqueDateRater;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.DocumentRetriever;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class KairosEvaluator {

    /**
     * @param args
     */
    public static void main(String[] args) {

        TechniqueDateGetter<ContentDate> dg = new ContentDateGetter();
        TechniqueDateRater<ContentDate> pub_dr = new ContentDateRater(PageDateType.PUBLISH);
        TechniqueDateRater<ContentDate> mod_dr = new ContentDateRater(PageDateType.LAST_MODIFIED);

        String file = "data/evaluation/daterecognition/datasets/finalEvaluation.txt";
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        evaluate(PageDateType.PUBLISH, dg, pub_dr, file, false);
        long pubTime = stopwatch.getElapsedTime();
        System.out.println("pubTime: " + pubTime);
        stopwatch.stop();
        stopwatch.start();
        evaluate(PageDateType.LAST_MODIFIED, dg, mod_dr, file, false);
        long modTime = stopwatch.getElapsedTime();
        System.out.println("Time incl. DB zugriff - pub: " + pubTime + " - mod: " + modTime);
    }

    private static void countUrlHeadDates(String file) {
        Map<String, DBExport> set = EvaluationHelper.readFile(file);

        UrlDateGetter urlDateGetter = new UrlDateGetter();
        MetaDateGetter metaDateGetter = new MetaDateGetter();

        int cntAllUrlDates = 0;
        int cntPubUrlDates = 0;
        int cntModUrlDates = 0;

        int cntAllMetaDates = 0;
        int cntPubMetaDates = 0;
        int cntModMetaDates = 0;

        int cntAll = 0;

        DateComparator dc = new DateComparator(DateExactness.DAY);

        for (Entry<String, DBExport> e : set.entrySet()) {
            System.out.println(cntAll++);
            List<UrlDate> urlDates = urlDateGetter.getDates(e.getValue().getUrl());

            DocumentRetriever dr = new DocumentRetriever();
            Document doc = dr.getWebDocument(e.getValue().getFilePath());
            List<MetaDate> metaDates = metaDateGetter.getDates(doc);

            ExtractedDate pubDate = DateParser.findDate(e.getValue().getPubDate());
            ExtractedDate modDate = DateParser.findDate(e.getValue().getModDate());

            if (metaDates != null && metaDates.size() > 0) {
                for (ExtractedDate metaDate : metaDates) {
                    cntAllMetaDates++;
                    if (pubDate != null && dc.compare(metaDate, pubDate) == 0) {
                        cntPubMetaDates++;
                    }
                    if (modDate != null && dc.compare(metaDate, modDate) == 0) {
                        cntModMetaDates++;
                    }
                }
            }

            if (urlDates != null && urlDates.size() > 0 && urlDates.get(0) != null && (pubDate != null || modDate != null) && urlDates.get(0).getExactness().ordinal() >= 3) {
                cntAllUrlDates++;
                System.out.println(cntAllUrlDates);
                if (pubDate != null && dc.compare(urlDates.get(0), pubDate) == 0) {
                    cntPubUrlDates++;
                }
                if (pubDate != null && dc.compare(urlDates.get(0), pubDate) != 0) {
                    System.out.println(pubDate.getNormalizedDateString() + " - " + urlDates.get(0).getNormalizedDateString());
                }
                if (modDate != null && dc.compare(urlDates.get(0), modDate) == 0) {
                    cntModUrlDates++;
                }
            }

        }

        System.out.println("Url: " + cntAllUrlDates + " pub: " + cntPubUrlDates + " mod: " + cntModUrlDates);
        System.out.println("Meta: " + cntAllMetaDates + " pub: " + cntPubMetaDates + " mod: " + cntModMetaDates);

    }

    private static List<MetaDate> getHttpDates(DBExport dbExport) {
        ArrayList<MetaDate> dates = new ArrayList<MetaDate>();
        String headerDate = dbExport.get(DBExport.HEADER_DATE);
        String headerLastMod = dbExport.get(DBExport.HEADER_LAST);

        ExtractedDate headerExtrDate = DateParser.findDate(headerDate);
        MetaDate headerHttpDate = new MetaDate(headerExtrDate);
        if (headerHttpDate != null) {
            headerHttpDate.setKeyword("date");
        }
        ExtractedDate headerExtrLastMod = DateParser.findDate(headerLastMod);
        MetaDate headerHttpLastMod = new MetaDate(headerExtrLastMod);
        if (headerHttpLastMod != null) {
            headerHttpLastMod.setKeyword("last-modified");
        }
        dates.add(headerHttpDate);
        dates.add(headerHttpLastMod);

        return dates;

    }

    private static ExtractedDate getDownloadedDate(DBExport dbExport) {
        return DateParser.findDate(dbExport.get(DBExport.ACTUAL_DATE));
    }

    public static void evaluate(PageDateType pub_mod, TechniqueDateGetter<ContentDate> dg, TechniqueDateRater<ContentDate> dr, String file, boolean writeRate) {
        int ard = 0;
        int awd = 0;
        int anf = 0;
        int afr = 0;
        int afw = 0;
        int counter = 0;
        int compare;

        Map<String, DBExport> set = EvaluationHelper.readFile(file);
        DocumentRetriever crawler = new DocumentRetriever();

        StopWatch timer = new StopWatch();
        long time = 0;

        for (Entry<String, DBExport> e : set.entrySet()) {

            ExtractedDate date;
            String dbExportDateString;

            String url = e.getValue().get(DBExport.URL);
            String path = e.getValue().get(DBExport.PATH);
            Document document = crawler.getWebDocument(path);
            document.setDocumentURI(path);

            String bestDateString = "";
            String rate = "-1";

            System.out.println(url);

            timer.start();
            RatedDate<? extends ExtractedDate> bestDate = WebPageDateEvaluator.getBestDate(document, pub_mod);
            time += timer.getElapsedTime();
            System.out.print("get dates... ");
            if (bestDate != null) {
                bestDateString = bestDate.getDateString();
                rate = String.valueOf(bestDate.getRate());
            }

            System.out.println("compare...");

            if (pub_mod.equals(PageDateType.PUBLISH)) {
                compare = EvaluationHelper.compareDate(bestDate, e.getValue(), DBExport.PUB_DATE);
                date = DateParser.findDate(e.getValue().getPubDate());
                // dbDateString = e.getValue().getPubDate();

                dbExportDateString = " - pubDate:";
            } else {
                compare = EvaluationHelper.compareDate(bestDate, e.getValue(), DBExport.MOD_DATE);
                date = DateParser.findDate(e.getValue().getModDate());
                // dbDateString = e.getValue().getModDate();
                dbExportDateString = " - modDate:";
            }

            if (date != null) {
                dbExportDateString += date.getNormalizedDateString();
            }

            System.out.print(compare + " bestDate:" + bestDateString + " (" + rate + ")" + dbExportDateString);

            switch (compare) {
                case EvaluationHelper.AFW:
                    afw++;
                    break;
                case EvaluationHelper.ANF:
                    anf++;
                    break;
                case EvaluationHelper.AWD:
                    awd++;
                    break;
                case EvaluationHelper.ARD:
                    ard++;
                    break;
                case EvaluationHelper.AFR:
                    afr++;
                    break;

            }

            counter++;

            System.out.println();
            System.out.println("all: " + counter + " afr: " + afr + " ard: " + ard + " afw: " + afw + " awd: " + awd + " anf: " + anf);
            System.out.println("---------------------------------------------------------------------");
            System.out.println("time: " + time);
        }
        System.out.println("all: " + counter + " afr: " + afr + " ard: " + ard + " afw: " + afw + " awd: " + awd + " anf: " + anf);
        System.out.println("final time: " + time);

    }

}
