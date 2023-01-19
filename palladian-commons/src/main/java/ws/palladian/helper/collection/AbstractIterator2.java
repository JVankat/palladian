package ws.palladian.helper.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * Abstract iterator implementation for readonly iterators which provides some
 * convenience for implementations; only the {@link #getNext()} method needs to
 * be implemented. The {@link #remove()} method triggers an
 * {@link UnsupportedOperationException}.
 *
 * <p>
 * Typical implementation looks as follows:
 *
 * <pre>
 * new AbstractIterator&lt;Item&gt;() {
 *     &#64;Override
 *     protected Item getNext() {
 *         if (!source.moreItems()) {
 *             return finished();
 *         }
 *         return source.computeNextItem()
 *     }
 * }
 * </pre>
 *
 * @param <E> The type of the items.
 * @author Philipp Katz
 */
public abstract class AbstractIterator2<E> implements Iterator<E> {

    private E next;
    private boolean ready;
    private boolean finished;

    @Override
    public boolean hasNext() {
        if (finished) {
            return false;
        }
        if (!ready) {
            next = getNext();
            if (finished) {
                return false;
            }
            ready = true;
        }
        return true;

    }

    @Override
    public E next() {
        if (finished) {
            throw new NoSuchElementException("No (more) elements");
        }
        if (!ready) {
            next = getNext();
            if (finished) {
                throw new NoSuchElementException("No (more) elements");
            }
        }
        ready = false;
        return next;
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException("Modifications are not allowed");
    }

    /**
     * Get the next element for iteration. In case, there are no more elements,
     * invoke {@link #finished()} and return an arbitrary value, e.g.
     * <code>null</code>, which will not be within the iteration.
     *
     * @return The next element.
     */
    protected abstract E getNext();

    /**
     * Invoked from {@link #getNext()}, when there are no more elements.
     *
     * @return <code>null</code>
     */
    protected final E finished() {
        this.finished = true;
        return null;
    }

}
