package ws.palladian.helper.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract iterator implementation which provides some convenience for implementations; only the {@link #getNext()}
 * method needs to be implemented. In case, the iterator supports modifications, one must additionally implement the
 * {@link #remove()} metod (which triggers an {@link UnsupportedOperationException} by default).
 *
 * @param <E>
 * @author Philipp Katz
 * @deprecated Use {@link AbstractIterator2} from now on.
 */
@Deprecated
public abstract class AbstractIterator<E> implements Iterator<E> {

    /** Constant, for the exception thrown, when iteration has no more elements. */
    public static final Finished FINISHED = new Finished();

    /**
     * Thrown, when the iteration has no more elements (this exception is only used internally and is not re-thrown).
     *
     * @author Philipp Katz
     */
    @SuppressWarnings("serial")
    protected static final class Finished extends Exception {
        /**
         * @deprecated Refrain from instantiating this class (creating exceptions is expensive), use the predefined
         * {@link AbstractIterator#FINISHED} instead.
         */
        @Deprecated
        public Finished() {
            // no op.
        }
    }

    /**
     * Wrapper; necessary, because we need to handle <code>null</code> items; with this wrapper we can them
     * conveniently; as a <code>null</code> in this implementation indicates no more elements.
     */
    private static final class Wrap<I> {
        final I item;

        Wrap(I item) {
            this.item = item;
        }
    }

    private Wrap<E> next;

    @Override
    public final boolean hasNext() {
        if (next == null) {
            try {
                next = new Wrap<>(getNext());
            } catch (Finished e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final E next() {
        if (next == null) {
            try {
                next = new Wrap<>(getNext());
            } catch (Finished e) {
                throw new NoSuchElementException("No (more) elements");
            }
        }
        E result = next.item;
        next = null;
        return result;
    }

    /**
     * <p>
     * Get the next element for iteration.
     * </p>
     *
     * @return The next element (may also be <code>null</code> in case one iterates over <code>null</code> items).
     * @throws Finished Thrown when iteration ended and there are no more elements (use
     *                  {@link AbstractIterator#FINISHED}).
     */
    protected abstract E getNext() throws Finished;

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Modifications are not allowed");
    }

}
