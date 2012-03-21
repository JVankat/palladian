package ws.palladian.extraction.date.technique;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;

/**
 * <p>
 * Template for rater classes. Each technique that evaluates dates should implements this.
 * </p>
 * 
 * @author Martin Gregor
 * 
 * @param <T> subtype of {@link ExtractedDate} which concrete rater implementations process.
 */
public abstract class TechniqueDateRater<T extends ExtractedDate> {

    protected double minRate = 0.20;
    protected PageDateType dateType;

    public TechniqueDateRater(PageDateType dateType) {
        this.dateType = dateType;
    }

    /**
     * Rate-method fills this map for further use.
     */
    protected Map<T, Double> ratedDates = new HashMap<T, Double>();

    /**
     * Enter a list of dates. <br>
     * These will be rated in dependency of date-technique.
     * 
     * @param list
     * @return
     */
    public abstract Map<T, Double> rate(List<T> list);

    /**
     * Returns best rated date of property "ratedDates". <br>
     * In case of more than one best date the first one will be returned. <br>
     * For other function override this method in subclasses.
     * 
     * @return
     */
    public T getBestDate() {
        T date = null;
        if (this.ratedDates.size() > 0) {
            double rate = DateArrayHelper.getHighestRate(this.ratedDates);
            date = DateArrayHelper.getRatedDates(this.ratedDates, rate).get(0);
        }

        return date;
    }

    public void setMinRate(double minRate) {
        this.minRate = minRate;
    }

    public double getMinRate() {
        return this.minRate;
    }

}