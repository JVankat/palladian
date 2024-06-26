package ws.palladian.classification.zeror;

import ws.palladian.core.AbstractLearner;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.Bag;

/**
 * <p>
 * Baseline classifier which does not consider any features but just learns the class distribution during training.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://www.saedsayad.com/zeror.htm">ZeroR</a>
 */
public final class ZeroRLearner extends AbstractLearner<ZeroRModel> {

    @Override
    public ZeroRModel train(Dataset dataset) {
        Bag<String> categoryCounts = new Bag<>();
        for (Instance trainingInstance : dataset) {
            categoryCounts.add(trainingInstance.getCategory());
        }
        return new ZeroRModel(categoryCounts);
    }

}
