package ws.palladian.classification;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import ws.palladian.core.Model;

import java.util.*;

/**
 * <p>
 * A Palladian model wrapping a Weka classifier and all information necessary to apply that classifier.
 * </p>
 *
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class WekaModel implements Model {

    private static final long serialVersionUID = 1L;

    private final Classifier classifier;

    private final Map<String, Attribute> schema;

    private final Instances dataset;

    public WekaModel(Classifier classifier, Instances data) {
        this.classifier = classifier;
        Enumeration<?> schema = data.enumerateAttributes();
        this.schema = new HashMap<String, Attribute>();
        while (schema.hasMoreElements()) {
            Attribute attribute = (Attribute) schema.nextElement();
            this.schema.put(attribute.name(), attribute);
        }
        this.dataset = data;
    }

    public Classifier getClassifier() {
        return classifier;
    }

    public Map<String, Attribute> getSchema() {
        return schema;
    }

    /**
     * @return the dataset
     */
    public Instances getDataset() {
        return dataset;
    }

    @Override
    public Set<String> getCategories() {
        Enumeration<?> values = dataset.classAttribute().enumerateValues();
        Set<String> categories = new HashSet<>();
        while (values.hasMoreElements()) {
            String category = (String) values.nextElement();
            if (category.equals(WekaLearner.DUMMY_CLASS)) {
                // ignore this dummy class, see comment at constant.
                continue;
            }
            categories.add(category);
        }
        return categories;
    }

}
