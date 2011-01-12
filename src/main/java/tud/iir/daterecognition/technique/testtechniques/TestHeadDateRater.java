package tud.iir.daterecognition.technique.testtechniques;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.DateRaterHelper;
import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.daterecognition.technique.HeadDateRater;
import tud.iir.daterecognition.technique.PageDateType;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.knowledge.KeyWords;

public class TestHeadDateRater extends HeadDateRater {
	
	public TestHeadDateRater(PageDateType dateType) {
		super(dateType);
	}



	public static final byte PUB_DATE_PARAMETER = 0;
	public static final byte MOD_DATE_PARAMETER = 1;
	public static final byte TIME_DIFF_PARAMETER = 2;
	public static final byte MEASURE_PARAMETER = 3;
	public static final byte OLDEST_PARAMETER = 4;
	public static final byte YOUNGEST_PARAMERT = 5;
	
	private byte hightPriority = KeyWords.PUBLISH_KEYWORD;
	private byte middlePriority = KeyWords.MODIFIED_KEYWORD;
	private byte lowPriority;
	private int diffMeasure = DateComparator.MEASURE_HOUR;
	private boolean old = true;
	
	public void setParameter(HashMap<Byte, Integer[]> parameter){
		for(Entry<Byte, Integer[]> e : parameter.entrySet()){
			switch(e.getKey()){
			case PUB_DATE_PARAMETER:
				setPubParameter();
				break;
			case MOD_DATE_PARAMETER:
				setModParameter();
				break;
			case MEASURE_PARAMETER:
				setDiffMeasure(e.getValue()[0]);
				break;
			case OLDEST_PARAMETER:
				old=true;
				break;
			case YOUNGEST_PARAMERT:
				old=false;
				break;
			}
		}
	}
	
	private void setPubParameter(){
		System.out.println("set Pub Parameter");
		hightPriority = KeyWords.PUBLISH_KEYWORD;
		middlePriority = KeyWords.MODIFIED_KEYWORD;
	}
	private void setModParameter(){
		System.out.println("set Mod Parameter");
		hightPriority = KeyWords.MODIFIED_KEYWORD;
		middlePriority = KeyWords.PUBLISH_KEYWORD;
	}
	private void setDiffMeasure(int measure){
		System.out.println("set Time Parameter");
		diffMeasure = measure;
	}
	
	/**
     * Evaluates the head-dates.<br>
     * Rating by check keywords and age difference between dates.
     * 
     * @param List of head-dates.
     * @return Hashmap with dates and rateings.
     */
	protected HashMap<HeadDate, Double> evaluateHeadDate(ArrayList<HeadDate> headDates) {
		ExtractedDate actualDate = ExtractedDateHelper.createActualDate();
		return evaluateHeadDate(headDates, actualDate);
	}
	
	 /**
     * Evaluates the head-dates.<br>
     * Rating by check keywords and age difference between dates.
     * 
     * @param List of head-dates.
     * @return Hashmap with dates and rateings.
     */
	protected HashMap<HeadDate, Double> evaluateHeadDate(ArrayList<HeadDate> headDates, ExtractedDate actualDate) {
        HashMap<HeadDate, Double> result = new HashMap<HeadDate, Double>();
        double rate;
        HeadDate date;
        for (int i = 0; i < headDates.size(); i++) {
            date = headDates.get(i);
            byte keywordPriority = DateRaterHelper.getKeywordPriority(date);
            if (keywordPriority == hightPriority) {
                rate = 1;
            } else if (keywordPriority == middlePriority) {
                rate = -1;
            } else {
                rate = -2;
            }
            result.put(date, rate);
        }
        ArrayList<HeadDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1, true);
        ArrayList<HeadDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1, true);
        ArrayList<HeadDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2, true);
        if (highRatedDates.size() > 0) {
            DateRaterHelper.setRateToZero(middleRatedDates, result);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else if (middleRatedDates.size() > 0) {
            DateRaterHelper.setRat(middleRatedDates, result, 1.0);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else {

            
            DateComparator dc = new DateComparator();
            for (int i = 0; i < lowRatedDates.size(); i++) {
                rate = 0.75;
                if (dc.getDifference(actualDate, lowRatedDates.get(i), DateComparator.MEASURE_HOUR) < 12) {
                    rate = 0.0;
                }
                result.put(lowRatedDates.get(i), rate);
            }
        }

        DateComparator dc = new DateComparator();
        ArrayList<HeadDate> dates = dc.orderDates(result);
        HeadDate tempDate;
        if(old){
        	tempDate = dc.getOldestDate(DateArrayHelper.getExactestMap(result));
        }else{
        	tempDate = dc.getYoungestDate(DateArrayHelper.getExactestMap(result));
        }

        double diff;
        double oldRate;
        double newRate;

        for (int i = 0; i < dates.size(); i++) {
            diff = dc.getDifference(tempDate, dates.get(i), diffMeasure);
            if (diff > 24) {
                diff = 24;
            }
            date = dates.get(i);
            oldRate = result.get(date);
            newRate = oldRate - (oldRate * (diff / 24.0));
            result.put(date, Math.round(newRate * 10000) / 10000.0);
        }
        
        this.ratedDates = result;
        return result;
    }
	
	
	
	public HeadDate getBestDate(){
		double rate = DateArrayHelper.getHighestRate(this.ratedDates);
		ArrayList<HeadDate> list = DateArrayHelper.getRatedDates(this.ratedDates, rate);
		DateComparator dc = new DateComparator();
		HeadDate date;
		if(old){
			date = dc.getOldestDate(list);
		}else{
			date = dc.getYoungestDate(list);
		}
		
		return date;
		
	}
	
}
