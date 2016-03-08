package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.Validate;

import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.math.ClassificationEvaluator;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.ConfusionMatrixEvaluator;

public final class BackwardFeatureEliminationConfig {
	static final class EvaluationMeasure<M extends Model, R> {
		private final Factory<? extends Learner<M>> learnerFactory;
		private final Factory<? extends Classifier<M>> classifierFactory;
		private final ClassificationEvaluator<R> evaluator;
		private final Function<R, Double> mapper;
		private EvaluationMeasure(Factory<? extends Learner<M>> learnerFactory, Factory<? extends Classifier<M>> classifierFactory, ClassificationEvaluator<R> evaluator, Function<R, Double> mapper) {
			this.learnerFactory = learnerFactory;
			this.classifierFactory = classifierFactory;
			this.evaluator = evaluator;
			this.mapper = mapper;
		}
		public double score(Iterable<? extends Instance> trainData, Iterable<? extends Instance> testData) {
			return mapper.compute(evaluator.evaluate(learnerFactory.create(), classifierFactory.create(), trainData, testData));
		}
	}
	public static final class Builder<M extends Model> implements Factory<BackwardFeatureElimination>{
		private final Factory<? extends Learner<M>> learnerFactory;
		private final Factory<? extends Classifier<M>> classifierFactory;
		private EvaluationMeasure<M, ?> evaluator;
		private int numThreads = 1;
		private Collection<Filter<? super String>> featureGroups = new HashSet<>();
		private Builder(Learner<M> learner, Classifier<M> classifier) {
			this(Factories.constant(learner), Factories.constant(classifier));
		}
		@SuppressWarnings("deprecation")
		private Builder(Factory<? extends Learner<M>> learnerFactory, Factory<? extends Classifier<M>> classifierFactory) {
			this.learnerFactory = learnerFactory;
			this.classifierFactory = classifierFactory;
			this.evaluator = new EvaluationMeasure<>(learnerFactory, classifierFactory, new ConfusionMatrixEvaluator(), BackwardFeatureElimination.ACCURACY_SCORER);
		}
//		public Builder<M> learner(Learner<M> learner) {
//			Validate.notNull(learner, "learner must not be null");
//			learner(Factories.constant(learner));
//			return this;
//		}
//		public Builder<M> learner(Factory<? extends Learner<M>> learnerFactory) {
//			Validate.notNull(learnerFactory, "learnerFactory must not be null");
//			this.learnerFactory = learnerFactory;
//			return this;
//		}
//		public Builder<M> classifier(Classifier<M> classifier) {
//			Validate.notNull(classifier, "classifier must not be null");
//			classifier(Factories.constant(classifier));
//			return this;
//		}
//		public Builder<M> classifier(Factory<? extends Classifier<M>> classifierFactory) {
//			Validate.notNull(classifierFactory, "classifierFactory must not be null");
//			this.classifierFactory = classifierFactory;
//			return this;
//		}
		@Deprecated
		public Builder<M> scorer(Function<ConfusionMatrix, Double> scorer) {
			Validate.notNull(scorer, "scorer must not be null");
			evaluator(new ConfusionMatrixEvaluator(), scorer);
			return this;
		}
		@SuppressWarnings("deprecation")
		public Builder<M> scoreAccuracy() {
			scorer(BackwardFeatureElimination.ACCURACY_SCORER);
			return this;
		}
		@SuppressWarnings("deprecation")
		public Builder<M> scoreF1(String className) {
			scorer(new BackwardFeatureElimination.FMeasureScorer(className));
			return this;
		}
		public <R> Builder<M> evaluator(ClassificationEvaluator<R> evaluator, Function<R, Double> mapper) {
			this.evaluator = new EvaluationMeasure<M, R>(learnerFactory, classifierFactory, evaluator, mapper);
			return this;
		}
		public Builder<M> numThreads(int numThreads) {
			Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
			this.numThreads = numThreads;
			return this;
		}
		public Builder<M> featureGroups(Collection<? extends Filter<? super String>> featureGroups) {
			Validate.notNull(featureGroups, "featureGroups must not be null");
			this.featureGroups = new HashSet<>(featureGroups);
			return this;
		}
		public Builder<M> addFeatureGroup(Filter<? super String> featureGroup) {
			Validate.notNull(featureGroup, "featureGroup must not be null");
			this.featureGroups.add(featureGroup);
			return this;
		}
		@Override
		public BackwardFeatureElimination create() {
			return new BackwardFeatureElimination(createConfig());
		}
		BackwardFeatureEliminationConfig createConfig() {
			if (learnerFactory == null) {
				throw new IllegalArgumentException("no learner specified");
			}
			if (classifierFactory == null) {
				throw new IllegalArgumentException("no classifier specified");
			}
			return new BackwardFeatureEliminationConfig(this);
		}
	}
	public static <M extends Model> Builder<M> with(Learner<M> learner, Classifier<M> classifier) {
		return new Builder<M>(learner, classifier);
	}
	public static <M extends Model> Builder<M> with(Factory<? extends Learner<M>> learnerFactory, Factory<? extends Classifier<M>> classifierFactory) {
		return new Builder<M>(learnerFactory, classifierFactory);
	}
//	private final Factory<? extends Learner<M>> learnerFactory;
//	private final Factory<? extends Classifier<M>> classifierFactory;
	private final EvaluationMeasure<?, ?> evaluator;
	private final int numThreads;
	private final Collection<? extends Filter<? super String>> featureGroups;
	private BackwardFeatureEliminationConfig(Builder<?> builder) {
//		learnerFactory = builder.learnerFactory;
//		classifierFactory = builder.classifierFactory;
		evaluator = builder.evaluator;
		numThreads = builder.numThreads;
		featureGroups = builder.featureGroups;
	}
//	public Learner<M> createLearner() {
//		return learnerFactory.create();
//	}
//	public Classifier<M> createClassifier() {
//		return classifierFactory.create();
//	}
	public EvaluationMeasure<?, ?> evaluator() {
		return evaluator;
	}
	public int numThreads() {
		return numThreads;
	}
	public Collection<? extends Filter<? super String>> featureGroups() {
		return featureGroups;
	}
}
