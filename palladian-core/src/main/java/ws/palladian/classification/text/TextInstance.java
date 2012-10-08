package ws.palladian.classification.text;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;

/**
 * The document representation.
 * 
 * @author David Urbansky
 */
public class TextInstance extends Instance {

    /**
     * The real categories are given for training documents (and test documents that are used to determine the quality
     * of the classifier).
     */
    protected Categories realCategories;

    private String content = "";

    /** The weighted terms with term,weight representation. */
    private Map<String, Double> weightedTerms;

    /** Type of classification (tags or hierarchy). */
    private int classifiedAs = ClassificationTypeSetting.TAG;

    /** The category of the instance, null if not classified. */
    protected CategoryEntries assignedCategoryEntries = new CategoryEntries();

    /** If the class is nominal we have an instance category. */
    private Category instanceCategory;

    public void assignCategoryEntries(CategoryEntries categoryEntries) {
        this.assignedCategoryEntries = categoryEntries;
    }

    /**
     * The constructor.
     */
    public TextInstance() {
        weightedTerms = new HashMap<String, Double>();
        assignedCategoryEntries = new CategoryEntries();
    }

    /**
     * Get the real categories of the document.
     * 
     * @return The real categories.
     */
    public Categories getRealCategories() {
        return realCategories;
    }

    public Category getFirstRealCategory() {
        if (realCategories != null && !realCategories.isEmpty()) {
            return realCategories.get(0);
        }
        return null;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Double> getWeightedTerms() {
        return weightedTerms;
    }

    /**
     * Get all categories for the document.
     * 
     * @param relevancesInPercent If true then the relevance will be output in percent.
     * @return All categories.
     */
    public CategoryEntries getAssignedCategoryEntries() {
        return assignedCategoryEntries;
    }

    public CategoryEntries getAssignedCategoryEntriesByRelevance(int classType) {
        if (classType == ClassificationTypeSetting.HIERARCHICAL) {
            return assignedCategoryEntries;
        }
        sortCategoriesByRelevance();
        return assignedCategoryEntries;
    }

    public int getClassifiedAs() {
        return classifiedAs;
    }

    public Category getInstanceCategory() {
        return instanceCategory;
    }

    public String getInstanceCategoryName() {
        return instanceCategory.getName();
    }

    /**
     * Get the category that is most relevant to this document.
     * 
     * @param relevanceInPercent If true then the relevance will be output in percent.
     * @return The most relevant category.
     */
    public CategoryEntry getMainCategoryEntry() {
        CategoryEntry highestMatch = null;

        for (CategoryEntry ce : this.assignedCategoryEntries) {

            if (ce == null) {
                continue;
            }

            if (highestMatch == null) {
                highestMatch = ce;
                continue;
            }

            if (ce.getRelevance() > highestMatch.getRelevance()) {
                highestMatch = ce;
            }
        }

        if (highestMatch == null) {
            Logger.getRootLogger().warn("no assigned category found");
            return new CategoryEntry(this.assignedCategoryEntries, new Category(null), 0.0);
        }

        return highestMatch;
    }

    public void setInstanceCategory(Category instanceCategory) {
        this.instanceCategory = instanceCategory;
    }

    public void setInstanceCategory(String categoryName) {
//        Category category = instances.getCategories().getCategoryByName(categoryName);
//        if (category == null) {
//            category = new Category(categoryName);
//            instances.getCategories().add(category);
//        }
        this.instanceCategory = new Category(categoryName);
    }

//    protected void setInstances(List<? extends UniversalInstance> instances) {
//        this.instances = instances;
//    }

    public void sortCategoriesByRelevance() {
        assignedCategoryEntries.sortByRelevance();
    }

}
