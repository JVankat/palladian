package quickdt;

import java.io.PrintStream;
import java.io.Serializable;

public abstract class Node implements Serializable {
    public abstract void dump(int indent, PrintStream ps);

    public final Node parent;

    public Node(Node parent) {
        this.parent = parent;
    }

    /**
     * Writes a textual representation of this tree to a PrintStream
     *
     * @param ps
     */
    public void dump(final PrintStream ps) {
        dump(0, ps);
    }

    /**
     * Get a label for a given set of HashMapAttributes
     *
     * @param attributes
     * @return
     */
    public abstract Leaf getLeaf(Attributes attributes);

    /**
     * Does this tree achieve full recall on the training set?
     *
     * @return
     */
    public abstract boolean fullRecall();

    /**
     * Return the mean depth of leaves in the tree. A lower number generally
     * indicates that the decision tree learner has done a better job.
     *
     * @return
     */
    public double meanDepth() {
        final LeafDepthStats stats = new LeafDepthStats();
        calcMeanDepth(stats);
        return (double) stats.ttlDepth / stats.ttlSamples;
    }

    /**
     * Return the number of nodes in this decision tree.
     *
     * @return
     */
    public abstract int size();

    protected abstract void calcMeanDepth(LeafDepthStats stats);

    protected static class LeafDepthStats {
        int ttlDepth = 0;
        int ttlSamples = 0;
    }
}
