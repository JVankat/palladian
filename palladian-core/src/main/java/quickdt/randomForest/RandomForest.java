package quickdt.randomForest;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AtomicDouble;
import quickdt.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ian
 * Date: 4/18/13
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class RandomForest implements PredictiveModel {
    public final List<Tree> trees;

    protected RandomForest(List<Tree> trees) {
        this.trees = trees;
    }

    @Override
    public double getProbability(Attributes attributes, Serializable classification) {
        double total = 0;
        for (Tree tree : trees) {
            total += tree.getProbability(attributes, classification);
        }
        return total / trees.size();
    }

    @Override
    public Serializable getClassificationByMaxProb(Attributes attributes) {
        Map<Serializable, AtomicDouble> probTotals = Maps.newHashMap();
        for (Tree tree : trees) {
            Leaf leaf =tree.node.getLeaf(attributes);
            for (Serializable classification : leaf.getClassifications()) {
                AtomicDouble ttlProb = probTotals.get(classification);
                if (ttlProb == null) {
                    ttlProb = new AtomicDouble(0);
                    probTotals.put(classification, ttlProb);
                }
                ttlProb.addAndGet(leaf.getProbability(classification));
            }
        }
        Serializable bestClassification = null;
        double bestClassificationTtlProb = 0;
        for (Map.Entry<Serializable, AtomicDouble> classificationProb : probTotals.entrySet()) {
            if (bestClassification == null || classificationProb.getValue().get() > bestClassificationTtlProb) {
                bestClassification = classificationProb.getKey();
                bestClassificationTtlProb = classificationProb.getValue().get();
            }
        }
        return bestClassification;
    }

}
