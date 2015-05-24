package ws.palladian.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.Validate;

/**
 * @author Philipp Katz
 */
public final class ImmutableCategoryEntries extends AbstractCategoryEntries {

    /** The map with all {@link Category} entries, for quick access by category name. */
    private final Map<String, Category> entryMap;

    /** The most likely {@link Category}; determined and cached upon creation for quick access. */
    private final Category mostLikely;

    /** Empty ImmutableCategoryEntries; use the constant {@link CategoryEntries#EMPTY} */
    ImmutableCategoryEntries() {
        this.entryMap = Collections.<String, Category> emptyMap();
        this.mostLikely = null;
    }

    public ImmutableCategoryEntries(Map<String, Category> entryMap, Category mostLikely) {
        Validate.notNull(entryMap, "entryMap must not be null");
        this.entryMap = entryMap;
        this.mostLikely = mostLikely;
    }

    @Override
    public Iterator<Category> iterator() {
        return entryMap.values().iterator();
    }

    @Override
    public Category getMostLikely() {
        return mostLikely;
    }

    @Override
    public Category getCategory(String categoryName) {
        return entryMap.get(categoryName);
    }

    @Override
    public int hashCode() {
        return entryMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableCategoryEntries other = (ImmutableCategoryEntries)obj;
        return entryMap.equals(other.entryMap);
    }

}
