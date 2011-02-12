package tud.iir.classification.page;

import java.util.HashMap;

import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Instance;
import tud.iir.classification.Term;
import tud.iir.helper.MathHelper;

/**
 * The document representation.
 * 
 * @author David Urbansky
 */
public class ClassificationDocument extends Instance {

    // a document can be a test or a training document
    public static final int TEST = 1;
    public static final int TRAINING = 2;
    public static final int UNCLASSIFIED = 3;

    /**
     * The real categories are given for training documents (and test documents that are used to determine the quality
     * of the classifier).
     */
    protected Categories realCategories;

    /** Each document has a unique URL. */
    private String url = "";

    /** The weighted terms with term,weight representation. */
    private HashMap<Term, Double> weightedTerms;

    /** The type of the document (TEST, TRAINING or unknown). */
    private int documentType = UNCLASSIFIED;


    /**
     * The constructor.
     */
    public ClassificationDocument() {
        weightedTerms = new HashMap<Term, Double>();
        assignedCategoryEntries = new CategoryEntries();
    }

    /**
     * Set the real categories (mainly for training documents).
     * 
     * @param categories The real categories.
     */
    public void setRealCategories(Categories categories) {
        this.realCategories = categories;
    }

    /**
     * Get the real categories of the document.
     * 
     * @return The real categories.
     */
    public Categories getRealCategories() {
        return realCategories;
    }

    public String getRealCategoriesString() {
        StringBuilder sb = new StringBuilder();
        for (Category c : realCategories) {
            sb.append(c.getName()).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public Category getFirstRealCategory() {
        if (realCategories != null && !realCategories.isEmpty()) {
            return realCategories.get(0);
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashMap<Term, Double> getWeightedTerms() {
        return weightedTerms;
    }

    public void setWeightedTerms(HashMap<Term, Double> weightedTerms) {
        this.weightedTerms = weightedTerms;
    }

    public int getDocumentType() {
        return documentType;
    }

    public void setDocumentType(int documentType) {
        this.documentType = documentType;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        //string.append(getUrl());
        CategoryEntries sortedCategories = getAssignedCategoryEntriesByRelevance(getClassifiedAs());
        for (CategoryEntry categoryEntry : sortedCategories) {
            string.append(categoryEntry.getCategory().getName()).append(" (").append(MathHelper.round(100 * categoryEntry.getRelevance(), 2)).append("%)\n");
        }
        return string.toString();
    }
}