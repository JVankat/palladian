package 
ws.palladian.daterecognition.evaluation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.DateConverter;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.ContentDateGetter;
import ws.palladian.daterecognition.technique.ContentDateRater;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;
import ws.palladian.daterecognition.technique.testtechniques.WebPageDateEvaluatorTest;
import ws.palladian.helper.date.DateComparator;
import ws.palladian.web.Crawler;

public class KairosEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		// TODO Auto-generated method stub
		TechniqueDateGetter<ContentDate> dg = new ContentDateGetter();
		TechniqueDateRater<ContentDate> pub_dr = new ContentDateRater(PageDateType.publish);
		TechniqueDateRater<ContentDate> mod_dr = new ContentDateRater(PageDateType.last_modified);
		
		String file = "data/evaluation/daterecognition/datasets/dataset.txt";
		evaluate(EvaluationHelper.KAIROSEVAL, "pub0",PageDateType.publish, dg, pub_dr, file);
		evaluate(EvaluationHelper.KAIROSEVAL, "mod0",PageDateType.last_modified, dg, mod_dr, file);
		
		
		
		//EvaluationHelper.calculateOutput(0, EvaluationHelper.CONTENTEVAL);
		System.out.println("pub");
		System.out.println("RF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.AFR));
		System.out.println("RNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.ARD));
		System.out.println("WF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.AFW));
		System.out.println("WNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.ANF));
		System.out.println("FF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.AWD));
				
		System.out.println("mod");
		System.out.println("RF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.AFR));
		System.out.println("RNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.ARD));
		System.out.println("WF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.AFW));
		System.out.println("WNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.ANF));
		System.out.println("FF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.AWD));
		*/
		
		KairosEvaluator ke = new KairosEvaluator();
		String file = "data/evaluation/daterecognition/datasets/dataset.txt";
		ke.evaluationToDB(file, "kairosweka", PageDateType.publish);
		
		ke.evaluationOut(PageDateType.publish);
		
	}
	
	private void evaluationOut(PageDateType pageDateType){
		HashMap<String, ArrayList<String>> dbMap = importKairosDate(pageDateType);
		DateComparator dc = new DateComparator();
		System.out.println(" - ARD - AWD - ANF - AFR - AFW - Accuracy  - Detection");
		for(int i=0; i<=100; i++){
			double limit = ((double)i) / 100.0;
			
			
			int ard = 0;
			int awd = 0;
			int anf = 0;
			int afr = 0;
			int afw = 0;
			
			for(Entry<String, ArrayList<String>> entry : dbMap.entrySet()){
				ExtractedDate webDate = DateGetterHelper.findDate(entry.getValue().get(0));
				ExtractedDate kairosDate = null;
				double rate = Double.valueOf(entry.getValue().get(2));
				if(rate >= limit){
					kairosDate = DateGetterHelper.findDate(entry.getValue().get(1));;
				}
				if(webDate == null){
					if(kairosDate == null){
						ard++;
					}else{
						awd++;
					}
				}else{
					if(kairosDate == null){
						anf++;
					}else{
						int compare = dc.compare(webDate, kairosDate, dc.getCompareDepth(webDate, kairosDate));
						if(compare == 0){
							afr++;
						}else{
							afw++;
						}
					}
				}
			}
			
			int accuracy = (int) Math.round(((double)(ard + afr) / (double)(ard + awd + anf + afr +afw)) * 1000.0);
			int dedection = (int) Math.round(((double)ard / (double)(ard + awd)) * 1000.0);
			
			System.out.print(limit + " - ");
			System.out.print(ard + " - ");
			System.out.print(awd + " - ");
			System.out.print(anf + " - ");
			System.out.print(afr + " - ");
			System.out.print(afw + " - ");
			System.out.print(accuracy + " - ");
			System.out.println(dedection);
			
		}
	}
	
	private HashMap<String, ArrayList<String>> importKairosDate(PageDateType pageDateType){
		HashMap<String,  ArrayList<String>> dbMap = new HashMap<String, ArrayList<String>>();
		DataSetHandler.openConnection();
		String sqlQuery = "Select * From kairosweka";
		try{
			ResultSet rs = DataSetHandler.st.executeQuery(sqlQuery);
			while(rs.next()){
				String url = rs.getString("url");
				String webDate;
				String kairosDate;
				String kairosRate;
				
				if(pageDateType.equals(PageDateType.publish)){
					webDate = rs.getString("webDatePub");
					kairosDate = rs.getString("kairosDatePub");
					kairosRate = String.valueOf(rs.getDouble("kairosRatePub"));
				}else{
					webDate = rs.getString("webDateMod");
					kairosDate = rs.getString("kairosDateMod");
					kairosRate = String.valueOf(rs.getDouble("kairosRateMod"));
				}
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(webDate);
				temp.add(kairosDate);
				temp.add(kairosRate);
				
				dbMap.put(url, temp);
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		DataSetHandler.closeConnection();
		return dbMap;
	}
	
	private void evaluationToDB (String file, String table, PageDateType pageDateType){
		HashMap<String, DBExport> fileMap = importUrl(file);
		Crawler c = new Crawler();
		int index = 0;
		for(Entry<String, DBExport> entry : fileMap.entrySet()){
			WebPageDateEvaluatorTest wpde = new WebPageDateEvaluatorTest();
			String url = entry.getKey();
			System.out.println(index++ + ": " + url);
			wpde.setUrl(url);
			wpde.setDocument(c.getWebDocument(entry.getValue().getFilePath()));
			wpde.setPubMod(pageDateType);
			wpde.setHttpDates(getHttpDates(entry.getValue()));
			wpde.setActualDate(getDownloadedDate(entry.getValue()));
			wpde.evaluate();
			ExtractedDate bestDate =  wpde.getBestRatedDate();
			
			DataSetHandler.openConnection();
			String bestPubDate = "";
			String bestModDate = "";
			double pubRate = 0;
			double modRate = 0;
			
			String updateString;
			
			if(pageDateType.equals(PageDateType.publish)){
				if(bestDate != null){
					bestPubDate = bestDate.getNormalizedDateString();
					pubRate = bestDate.getRate();
				}
				updateString = "kairosDatePub = '" + bestPubDate + "', kairosRatePub = " + pubRate;
			} else{
				if(bestDate != null){
					bestModDate = bestDate.getNormalizedDateString();
					modRate = bestDate.getRate();
				}
				updateString = "kairosDateMod = '" + bestModDate + "', kairosRateMod = " + modRate;
			}
			
			String sqlQuery ="INSERT INTO " + table + 
								" (url, webDatePub, webDateMod, kairosDatePub, kairosDateMod, kairosRatePub, kairosRateMod) " +
								"VALUES ('" + url + "', '"+ entry.getValue().getPubDate() + "','" + entry.getValue().getModDate() + "','" + 
								bestPubDate + "', '" + bestModDate + "', " + pubRate + ", " + modRate + ") " +
										"ON DUPLICATE KEY UPDATE " + updateString;
	    	try {
	    		//System.out.println(sqlQuery);
				DataSetHandler.st.executeUpdate(sqlQuery);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			DataSetHandler.closeConnection();
			
		}
		
		
		
	}

	private HashMap<String, DBExport> importUrl (String file){
		return EvaluationHelper.readFile(file);
	}
	
	private static ArrayList<HTTPDate> getHttpDates(DBExport dbExport){
		ArrayList<HTTPDate> dates = new ArrayList<HTTPDate>();
		String headerDate = dbExport.get(DBExport.HEADER_DATE);
		String headerLastMod = dbExport.get(DBExport.HEADER_LAST);
		
		ExtractedDate headerExtrDate = DateGetterHelper.findDate(headerDate);
		HTTPDate headerHttpDate = DateConverter.convert(headerExtrDate, DateConverter.TECH_HTTP_HEADER);
		if(headerHttpDate != null){
			headerHttpDate.setKeyword("date");
		}
		ExtractedDate headerExtrLastMod = DateGetterHelper.findDate(headerLastMod);
		HTTPDate headerHttpLastMod = DateConverter.convert(headerExtrLastMod, DateConverter.TECH_HTTP_HEADER);
		if(headerHttpLastMod != null){
			headerHttpLastMod.setKeyword("last-modified");
		}
		dates.add(headerHttpDate);
		dates.add(headerHttpLastMod);
		
		
		return dates;
		
	}
	
	private static ExtractedDate getDownloadedDate(DBExport dbExport){
		return DateGetterHelper.findDate(dbExport.get(DBExport.ACTUAL_DATE));
	}
	
	public static <T> void evaluate(String table, String round,PageDateType pub_mod, TechniqueDateGetter<ContentDate> dg, TechniqueDateRater<ContentDate> dr, String file){
		int rnf = 0;
		int ff= 0;
		int wnf= 0;
		int rf= 0;
		int wf = 0;
		int counter=0;
		int compare;
		
		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);
		Crawler crawler = new Crawler();
		
		for(Entry<String, DBExport> e : set.entrySet()){
			String url =e.getValue().get(DBExport.URL);
			String path = e.getValue().get(DBExport.PATH);
			Document document = crawler.getWebDocument(path);
			System.out.println(url);
			
			WebPageDateEvaluatorTest wp = new WebPageDateEvaluatorTest();
			wp.setUrl(url);
			wp.setDocument(document);
			wp.setPubMod(pub_mod);
			wp.setHttpDates(getHttpDates(e.getValue()));
			wp.setActualDate(getDownloadedDate(e.getValue()));
			wp.evaluate();
			T bestDate = (T) wp.getBestRatedDate();
			
			String bestDateString ="";
			
			System.out.print("get dates... ");
			bestDateString = ((ExtractedDate) bestDate).getNormalizedDate(true);
				
			System.out.println("compare...");
			int pub_mod_int;
			if(PageDateType.publish == pub_mod){
				pub_mod_int = DBExport.PUB_DATE;
			}else{
				pub_mod_int = DBExport.MOD_DATE;
			}
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(),pub_mod_int);
			ExtractedDate date;
			String dbExportDateString;
			if(pub_mod_int == DBExport.PUB_DATE){
				date = DateGetterHelper.findDate(e.getValue().getPubDate());
				dbExportDateString =" - pubDate:" ;
			}else{
				date = DateGetterHelper.findDate(e.getValue().getModDate());
				dbExportDateString =" - modDate:" ;
			}
			
			if(date!=null){
				dbExportDateString +=  date.getNormalizedDateString();
			}
			
			System.out.print(compare + " bestDate:" + bestDateString + dbExportDateString);
			
			switch(compare){
				case DataSetHandler.AFW:
					wf++;
					break;
				case DataSetHandler.ANF:
					wnf++;
					break;
				case DataSetHandler.AWD:
					ff++;
					break;
				case DataSetHandler.ARD:
					rnf++;
					break;
				case DataSetHandler.AFR:
					rf++;
					break;
					
			}
			
			DataSetHandler.writeInDB(table, e.getValue().getUrl(), compare, round);
			counter++;
			
			System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
		
	}
}
