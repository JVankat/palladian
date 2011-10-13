package ws.palladian.extraction.date.technique;

import java.util.ArrayList;

import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.retrieval.DocumentRetriever;

public class MetaDateGetter extends TechniqueDateGetter<MetaDate>{

	private boolean lookHttpDates = true;
	
	private HttpDateGetter httpDateGetter = new HttpDateGetter();
	private HeadDateGetter headDateGetter = new HeadDateGetter();
	
	@Override
	public ArrayList<MetaDate> getDates() {
		ArrayList<MetaDate> dates = new ArrayList<MetaDate>();
		if(checkDocAndUrl()){
			if(lookHttpDates){
				httpDateGetter.setUrl(this.url);
				dates.addAll(httpDateGetter.getDates());
			}
			headDateGetter.setDocument(this.document);
			dates.addAll(headDateGetter.getDates());
		}
		return dates;
	}

	public void setHttpDateGetter(HttpDateGetter httpDateGetter) {
		this.httpDateGetter = httpDateGetter;
	}

	public HttpDateGetter getHttpDateGetter() {
		return httpDateGetter;
	}

	public void setHeadDateGetter(HeadDateGetter headDateGetter) {
		this.headDateGetter = headDateGetter;
	}

	public HeadDateGetter getHeadDateGetter() {
		return headDateGetter;
	}

	private boolean checkDocAndUrl(){
		boolean result;
		if(this.url == null && this.document == null){
			result = false;
		}else {
			result = true;
			if(this.url == null){
				this.url = document.getBaseURI();
			}else if(this.document == null){
				DocumentRetriever crawler = new DocumentRetriever();
				this.document = crawler.getWebDocument(this.url);
			}
		}
		return result;
	}
	
	public void setLookHttpDates(boolean lookHttpDates){
		this.lookHttpDates = lookHttpDates;
	}
	
}
