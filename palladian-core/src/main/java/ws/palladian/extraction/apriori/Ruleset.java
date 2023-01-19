package ws.palladian.extraction.apriori;

import ws.palladian.extraction.apriori.Apriori.Rule;

import java.util.*;

public class Ruleset<T> implements Iterable<Rule<T>> {
    private final List<Rule<T>> rules;

    protected Ruleset(List<Rule<T>> rules) {
        this.rules = rules;
    }

    @Override
    public Iterator<Rule<T>> iterator() {
        return rules.iterator();
    }

    public int size() {
        return rules.size();
    }

    public Rule<T> getRule(Set<T> X, Set<T> Y) {
        Objects.requireNonNull(X, "X must not be null");
        Objects.requireNonNull(Y, "Y must not be null");
        for (Rule<T> rule : rules) {
            if (rule.getIf().equals(X) && rule.getThen().equals(Y)) {
                return rule;
            }
        }
        return null;
    }

    public Collection<Rule<T>> getRules(Set<T> X) {
        Objects.requireNonNull(X, "X must not be null");
        Collection<Rule<T>> matchingRules = new ArrayList<>();
        for (Rule<T> rule : rules) {
            if (X.containsAll(rule.getIf())) {
                matchingRules.add(rule);
            }
        }
        return matchingRules;
    }

}
