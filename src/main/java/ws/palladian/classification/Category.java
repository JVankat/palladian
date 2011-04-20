package ws.palladian.classification;

import java.io.Serializable;

import org.apache.log4j.Logger;

import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;

/**
 * A category has a name and a relevance for certain resource.
 * 
 * @author David Urbansky
 * 
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 8831509827509452692L;

    /** The name of the category. */
    private String name = "";

    /** The frequency of documents belonging to this category, it will be used to calculate the prior. */
    private int frequency = 0;

    /** the total number of weights for all terms in this category */
    private double totalTermWeight = 0.0;

    /** the prior probability of this category */
    private double prior = 0.0;

    /** weight of the category in test set (used for evaluation purposes), -1 means no weight calculated yet */
    private double testSetWeight = -1.0;

    /** in hierarchical mode a category can be a root category */
    private boolean mainCategory = false;

    /** what classification type does the category belong to? (simple, hiearchy or tag) */
    private int classType = ClassificationTypeSetting.SINGLE;

    public Category(String name) {
        if (name == null) {
            this.name = "UNASSIGNED";
            Logger.getRootLogger().warn("category with NULL as name was created");
        } else {
            this.name = name;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // public Double getRelevance() {
    // return relevance;
    // }
    //
    // public void setRelevance(Double relevance) {
    // this.relevance = relevance;
    // }
    //
    // public void addRelevance(double relevance) {
    // this.relevance += relevance;
    // }

    public int getFrequency() {
        return this.frequency;
    }

    public void increaseFrequency() {
        this.frequency++;
    }

    public void decreaseFrequency() {
        this.frequency--;
    }

    /**
     * The prior probability of this category. Set after learning.
     * 
     * @return The prior probability of this category.
     */
    public double getPrior() {
        if (prior == 0.0) {
            Logger.getRootLogger().debug("prior was set to 0.0 for category " + getName());
        }
        return prior;
    }

    private void setPrior(final double prior) {
        this.prior = prior;
    }

    /**
     * The prior can be indexed and read from the index. Instead of calculating it via Categories.calculatePriors(), it can be set using this method.
     * 
     * @param prior
     */
    public void setIndexedPrior(double prior) {
        this.prior = prior;
    }

    /**
     * <p>
     * Calculates the prior for this category, which is the ratio between this categories frequency to all documents in the corpus.
     * </p>
     * 
     * @param totalDocuments The count of total documents on this corpus.
     */
    public void calculatePrior(int totalDocuments) {
        setPrior((double) frequency / (double) totalDocuments);
    }

    public boolean isMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(boolean mainCategory) {
        this.mainCategory = mainCategory;
    }

    public int getClassType() {
        return classType;
    }

    public void setClassType(int classType) {
        this.classType = classType;
    }

    /**
     * Equality is checked by category name.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return ((String) obj).equals(getName());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName() + "(prior:" + getPrior() + ")";
    }

    public void setTestSetWeight(double testSetWeight) {
        this.testSetWeight = testSetWeight;
    }

    public double getTestSetWeight() {
        return testSetWeight;
    }

    public void increaseTotalTermWeight(double totalTermWeight) {
        this.totalTermWeight += totalTermWeight;
    }

    public double getTotalTermWeight() {
        return totalTermWeight;
    }

    public void resetFrequency() {
        this.frequency = 0;
    }
}