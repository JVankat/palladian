package ws.palladian.classification.evaluation;

import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;

public abstract class AbstractClassificationEvaluator<R> implements ClassificationEvaluator<R> {
    @Override
    public <M extends Model> R evaluate(Learner<M> learner, Classifier<M> classifier, Iterable<? extends Instance> trainData, Iterable<? extends Instance> testData) {
        @SuppressWarnings("deprecation")
        M model = learner.train(trainData);
        return evaluate(classifier, model, testData);
    }

    @Override
    public <M extends Model> R evaluate(Learner<M> learner, Classifier<M> classifier, Dataset trainData, Dataset testData) {
        M model = learner.train(trainData);
        return evaluate(classifier, model, testData);
    }

    @Override
    public <M extends Model> R evaluate(Classifier<M> classifier, M model, Iterable<? extends Instance> data) {
        return evaluate(classifier, model, new DefaultDataset(data));
    }

    @Override
    public String getCsvHeader(R result) {
        throw new UnsupportedOperationException("Not supported by the evaluator.");
    }

    @Override
    public String getCsvLine(R result) {
        throw new UnsupportedOperationException("Not supported by the evaluator.");
    }
}
