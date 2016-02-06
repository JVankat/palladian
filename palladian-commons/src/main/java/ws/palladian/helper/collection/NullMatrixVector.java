package ws.palladian.helper.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import ws.palladian.helper.collection.Matrix.MatrixVector;

/**
 * <p>
 * A vector which can be used instead of <code>null</code> (null object pattern).
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K>
 * @param <V>
 */
final class NullMatrixVector<K, V> implements MatrixVector<K, V> {

    private final K key;

    NullMatrixVector(K key) {
        this.key = key;
    }

    @Override
    public Iterator<VectorEntry<K, V>> iterator() {
        return Collections.<VectorEntry<K, V>> emptySet().iterator();
    }

    @Override
    public V get(K k) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public K key() {
        return key;
    }

    @Override
    public Set<K> keys() {
        return Collections.emptySet();
    }

    @Override
    public Collection<V> values() {
        return Collections.emptySet();
    }

}
