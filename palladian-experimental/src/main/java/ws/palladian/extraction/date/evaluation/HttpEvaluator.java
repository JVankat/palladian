package ws.palladian.extraction.date.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.getter.HttpDateGetter;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.extraction.date.rater.HttpDateRater;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;


public class HttpEvaluator {

	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HttpDateGetter dg = new HttpDateGetter();
		HttpDateRater dr = new HttpDateRater(PageDateType.PUBLISH);
		
		String file = "data/evaluation/daterecognition/datasets/httpdataset.txt";
		evaluate(DBExport.PUB_DATE, dg, dr,file);
		evaluate(DBExport.MOD_DATE, dg, dr,file);
	}

	private static void evaluate(int pub_mod, HttpDateGetter dg, HttpDateRater dr, String file){
		int rnf = 0;
		int ff= 0;
		int wnf= 0;
		int rf= 0;
		int wf = 0;
		int counter=0;
		int compare;
		Map<String, DBExport> set = EvaluationHelper.readFile(file);
		
		for(Entry<String, DBExport> e : set.entrySet()){
			String tempDateString = "";
			
			System.out.println(e.getValue().getUrl());
			System.out.print("get dates... ");
			
			List<MetaDate> dates = new ArrayList<MetaDate>();
			ExtractedDate dateDate = DateParser.findDate(e.getValue().get(DBExport.HEADER_DATE));
			MetaDate tempDate = new MetaDate(dateDate);
			if(tempDate != null){
				dates.add(tempDate);
			}
			ExtractedDate lastDate = DateParser.findDate(e.getValue().get(DBExport.HEADER_LAST));
			tempDate = new MetaDate(lastDate);
			if(tempDate != null){
				dates.add(tempDate);
			}
			
			System.out.print("rate...");
			
			ExtractedDate downloadedDate = DateParser.findDate(e.getValue().get(DBExport.ACTUAL_DATE));
			List<RatedDate<MetaDate>> dateArray = dr.evaluateHTTPDate(dates, downloadedDate);
			
			double rate = DateExtractionHelper.getHighestRate(dateArray);
			dates = DateExtractionHelper.getRatedDates(dateArray, rate);
			if(dates.size()>0 && dates.get(0) != null){
				tempDate = dates.get(0);
				tempDateString = tempDate.getNormalizedDateString(true);
				
			}else{
				tempDate = null;
			}
			
			System.out.println("compare...");

			if(rate == 0){
				tempDate = null;
			}
			compare = EvaluationHelper.compareDate(tempDate, e.getValue(), pub_mod);
			
			System.out.print(compare + " httpDate:" + tempDateString + " - " + pub_mod + ":" + e.getValue().get(pub_mod));
			switch(compare){
				case EvaluationHelper.AFW:
					wf++;
					break;
				case EvaluationHelper.ANF:
					wnf++;
					break;
				case EvaluationHelper.AWD:
					ff++;
					break;
				case EvaluationHelper.ARD:
					rnf++;
					break;
				case EvaluationHelper.AFR:
					rf++;
					break;
					
			}
			counter++;
			System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
		
	}
	

	
	
}
