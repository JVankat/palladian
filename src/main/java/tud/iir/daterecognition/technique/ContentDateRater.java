package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.DateRaterHelper;
import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.knowledge.KeyWords;

/**
 *This class evaluates content-dates. <br>
 *Doing this by dividing dates in three parts: Keyword in attribute, in text and no keyword.<br>
 * Each part will be rate different.<br>
 * Part one by keyword classes, see {@link KeyWords#getKeywordPriority(String)} and age.
 * Part two by distance of keyword an date, keyword classes and age.
 * Part three by age.
 * 
 * @author Martin Gregor
 * 
 */
public class ContentDateRater extends TechniqueDateRater<ContentDate> {

    @Override
    public HashMap<ContentDate, Double> rate(ArrayList<ContentDate> list) {
        return evaluateContentDate(list);
    }

    /**
     * Evaluates content dates.<br>
     * Divide all dates in one of three parts: keyword in attribute, in text an no keyword.<br>
     * Evaluate each part. <br>
     * Put all parts together.
     * 
     * @param dates
     * @return
     */
    private HashMap<ContentDate, Double> evaluateContentDate(ArrayList<ContentDate> dates) {
        HashMap<ContentDate, Double> result = new HashMap<ContentDate, Double>();

        ArrayList<ContentDate> attrDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_ATTR);
        ArrayList<ContentDate> contDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_CONT);
        ArrayList<ContentDate> nokeywordDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_NO);

        HashMap<ContentDate, Double> attrResult = evaluateKeyLocAttr(attrDates);
        HashMap<ContentDate, Double> contResult = evaluateKeyLocCont(contDates);
        HashMap<ContentDate, Double> nokeywordResult = new HashMap<ContentDate, Double>();

        // Run through dates without keyword.
        double newRate;
        for (int i = 0; i < nokeywordDates.size(); i++) {
            ContentDate date = nokeywordDates.get(i);
            String tag = date.getTag();
            String[] keys = KeyWords.allKeywords;

            newRate = 0;
            for (int j = 0; j < keys.length; j++) {
                if (tag.equalsIgnoreCase(keys[j])) {
                    newRate = 1.0 / dates.size();
                    break;
                }
            }
            nokeywordResult.put(date, Math.round(newRate * 10000) / 10000.0);
        }

        // increase rate, if tag is a headline tag. (h1..h6)
        attrResult = DateRaterHelper.evaluateTag(attrResult);
        contResult = DateRaterHelper.evaluateTag(contResult);
        nokeywordResult = DateRaterHelper.evaluateTag(nokeywordResult);

        result.putAll(attrResult);
        result.putAll(contResult);
        result.putAll(nokeywordResult);

        // evaluatePosInDoc(result);

        return result;
    }

    /**
     * Calculates the rate of dates with keywords within text (content).<br>
     * Factors are keyword-class, distance of keyword and date as well as age.
     * 
     * @param contDates
     * @return
     */
    private HashMap<ContentDate, Double> evaluateKeyLocCont(ArrayList<ContentDate> contDates) {
        HashMap<ContentDate, Double> contResult = new HashMap<ContentDate, Double>();
        double factor_keyword;
        double factor_content;
        for (int i = 0; i < contDates.size(); i++) {
            ContentDate date = contDates.get(i);
            factor_content = calcContDateContent(date);
            contResult.put(date, factor_content);
        }
        ArrayList<ContentDate> rate1dates = DateArrayHelper.getRatedDates(contResult, 1.0);

        ArrayList<ContentDate> rateRestDates = DateArrayHelper.getRatedDates(contResult, 1.0, false);

        ContentDate key;

        for (int i = 0; i < rate1dates.size(); i++) {
            // anz der dates mit gleichen werten / anz aller dates
            key = rate1dates.get(i);

            factor_keyword = calcContDateAttr(key);
            int countSame = DateArrayHelper.countDates(key, rate1dates, -1) + 1;
            double newRate = (1.0 * countSame / rate1dates.size());
            contResult.put(key, Math.round(newRate * factor_keyword * 10000) / 10000.0);
        }

        for (int i = 0; i < rateRestDates.size(); i++) {
            key = rateRestDates.get(i);
            factor_keyword = calcContDateAttr(key);
            int countSame = DateArrayHelper.countDates(key, rateRestDates, -1) + 1;
            double newRate = (1.0 * contResult.get(key) * countSame / contDates.size());
            contResult.put(key, Math.round(newRate * factor_keyword * 10000) / 10000.0);
        }

        return contResult;
    }

    /**
     * Calculates rate of dates with keyword within attribute.<br>
     * Factors are keyword-class and age.
     * 
     * @param attrDates
     * @return
     */
    private HashMap<ContentDate, Double> evaluateKeyLocAttr(ArrayList<ContentDate> attrDates) {
        HashMap<ContentDate, Double> attrResult = new HashMap<ContentDate, Double>();
        ContentDate date;
        double rate;
        for (int i = 0; i < attrDates.size(); i++) {
            date = attrDates.get(i);
            rate = calcContDateAttr(date);
            attrResult.put(date, rate);
        }

        ArrayList<ContentDate> rate1Dates = DateArrayHelper.getRatedDates(attrResult, 1);
        ArrayList<ContentDate> middleRatedDates = DateArrayHelper.getRatedDates(attrResult, 0.7);
        ArrayList<ContentDate> lowRatedDates = DateArrayHelper.getRatedDates(attrResult, 0.5);

        if (rate1Dates.size() > 0) {
            attrResult.putAll(DateRaterHelper.setRateWhightedByGroups(rate1Dates, attrDates));

            DateRaterHelper.setRateToZero(middleRatedDates, attrResult);
            DateRaterHelper.setRateToZero(lowRatedDates, attrResult);
        } else if (middleRatedDates.size() > 0) {
            attrResult.putAll(DateRaterHelper.setRateWhightedByGroups(middleRatedDates, attrDates));

            DateRaterHelper.setRateToZero(lowRatedDates, attrResult);
        } else {
            attrResult.putAll(DateRaterHelper.setRateWhightedByGroups(lowRatedDates, attrDates));
        }
        return attrResult;

    }

    /**
     * Sets the factor for keyword-classes.
     * 
     * @param date
     * @return
     */
    private double calcContDateAttr(ContentDate date) {
        String key = date.getKeyword();
        double factor = 0;
        byte keywordPriority = DateRaterHelper.getKeywordPriority(date);
        if (key != null) {
            if (keywordPriority == KeyWords.FIRST_PRIORITY) {
                factor = 1;
            } else if (keywordPriority == KeyWords.SECOND_PRIORITY) {
                factor = 0.7;
            } else if (keywordPriority == KeyWords.THIRD_PRIORITY) {
                factor = 0.5;
            } else {
                factor = 0.0;
            }
        }
        return factor;
    }

    /**
     * Sets the factor for distance of keyword and date.
     * 
     * @param date
     * @return
     */
    private double calcContDateContent(ContentDate date) {
        int distance = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
        // f(x) = -1/17*x+20/17
        double factor = ((-1.0 / 17.0) * distance) + (20.0 / 17.0);
        factor = Math.max(0, Math.min(1.0, factor));
        return Math.round(factor * 10000) / 10000.0;
    }
}
