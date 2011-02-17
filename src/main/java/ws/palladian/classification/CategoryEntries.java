package ws.palladian.classification;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

/**
 * Hold a number of category entries. For example, a word could have a list of relevant categories attached. Each category has a certain relevance for the word
 * which is expressed in the CategoryEntry.
 * 
 * @author David Urbansky
 * 
 */
public class CategoryEntries extends java.util.ArrayList<CategoryEntry> implements Serializable {

    private static final long serialVersionUID = 4321001999458490582L;

    private boolean relevancesInPercent = false;

    // in order to avoid recalculating all relative relevance scores for each category entry
    // we update them only if new entries were added
    private boolean relevancesUpToDate = false;

    /** Comparator to sort categories by relevance. */
    private transient Comparator<CategoryEntry> comparator = new Comparator<CategoryEntry>() {
        public int compare(CategoryEntry o1, CategoryEntry o2) {
            return ((Comparable<Double>) o2.getRelevance()).compareTo(o1.getRelevance());
        }
    };

    public boolean isRelevancesUpToDate() {
        return relevancesUpToDate;
    }

    public void setRelevancesUpToDate(boolean relevancesUpToDate) {
        this.relevancesUpToDate = relevancesUpToDate;
    }

    public CategoryEntry getCategoryEntry(Category category) {
        return getCategoryEntry(category.getName());
    }

    public CategoryEntry getCategoryEntry(String categoryName) {
        for (CategoryEntry ce : this) {
            if (ce.getCategory().getName().equalsIgnoreCase(categoryName)) {
                return ce;
            }
        }
        return null;
    }

    public void setRelevancesInPercent(boolean relevancesInPercent) {
        this.relevancesInPercent = relevancesInPercent;
    }

    /**
     * This method calculates the percentage for every category in the ArrayList. The sum of percentages of all categories must be 100% (+-1% round).
     * 
     * @parameter spread If true, percentages get spread.
     */
    public void transformRelevancesInPercent(boolean spread) {

    }

    @Override
    /**
     * If a CategoryEntry is entered, the relative relevances are not up to date anymore.
     */
    public boolean add(CategoryEntry e) {
        if (e == null) {
            return false;
        }
        setRelevancesUpToDate(false);
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends CategoryEntry> c) {
        boolean listChanged = false;

        setRelevancesUpToDate(false);

        for (CategoryEntry newCategoryEntry : c) {
            if (hasEntryWithCategory(newCategoryEntry.getCategory())) {
                CategoryEntry ce = getCategoryEntry(newCategoryEntry.getCategory());
                ce.addAbsoluteRelevance(newCategoryEntry.getAbsoluteRelevance());
            } else {
                super.add(newCategoryEntry);
            }
            listChanged = true;
        }

        return listChanged;
    }

    /*
     * public boolean addMerge(CategoryEntry e) { if (e == null) { return false; } CategoryEntry c = this.getCategoryEntry(e.getCategory().getName()); if (c ==
     * null) { add(e); } else { c.addAbsoluteRelevance(e.getAbsoluteRelevance()); } setRelevancesUpToDate(false); if (c == null) { return true; } return false;
     * //return (c == null) true? false }
     */

    /**
     * The relevance for a category entry is a sum of absolute relevance scores so far. To normalize the relevance to a value between 0 and 1 we need to divide
     * it by the total absolute relevances of all category entries that are in the same category entries group.
     */
    public void calculateRelativeRelevances() {

        Logger.getRootLogger().debug("recalculate category entries relevances");

        // normalize
        Double totalRelevance = 0.0;
        for (CategoryEntry entry : this) {
            totalRelevance += entry.getAbsoluteRelevance();
        }

        for (CategoryEntry entry : this) {
            if (totalRelevance > 0) {
                entry.setRelativeRelevance(entry.getAbsoluteRelevance() / totalRelevance);
            } else {
                entry.setRelativeRelevance(-1.0);
            }
        }

        setRelevancesUpToDate(true);

        // spread, often percentages are very close such as 23,24,26,27 (4 categories) but they can be polarized so that the differences are more visible
        /*
         * if (spread) { double lowestRelevance = 100; for (Category category : this) { if (category.getRelevance() < lowestRelevance) lowestRelevance =
         * category.getRelevance(); } double totalPlusRelevance = 0; for (Category category : this) { totalPlusRelevance += category.getRelevance() -
         * lowestRelevance; } if (totalPlusRelevance > 0) { for (Category category : this) { category.setRelevance((category.getRelevance()-lowestRelevance) /
         * totalPlusRelevance); } } }
         */
    }

    public void sortByRelevance() {
        Collections.sort(this, comparator);
    }

    public CategoryEntry getMostLikelyCategoryEntry() {
        sortByRelevance();
        return get(0);
    }

    /**
     * Get the percentage of all absolute term weights for all category entries in the given category. The percentage tells what ratio of term weights were
     * relevant for the given category in this entry set.
     * 
     * @param category The category entry.
     * @return The percentage.
     */
    public double getTermWeight(Category category) {

        double entriesWeights = 0.0;
        for (CategoryEntry e : this) {
            if (e.getCategory().getName().equalsIgnoreCase(category.getName())) {
                entriesWeights += e.getAbsoluteRelevance();
            }
        }

        return entriesWeights / category.getTotalTermWeight();
    }

    public boolean hasEntryWithCategory(Category category) {
        boolean hasEntry = false;

        for (CategoryEntry ce : this) {
            if (ce.getCategory().getName().equalsIgnoreCase(category.getName())) {
                hasEntry = true;
                break;
            }
        }

        return hasEntry;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CategoryEntry ce : this) {
            sb.append(ce).append("\n");
        }
        return sb.toString();
    }

}
