package datastructures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HashDag<E> implements Serializable{

    private static final long serialVersionUID = 5033382726452421996L;
	/**
     * The backing {@link Map} that represents this DAG.
     * Each key of the map is a node, and each value is a collection of that node's outgoing nodes.
     */
    private final HashMap<E, Collection<E>> map;

    /**
     * Constructs an empty {@link HashDag}
     */
    public HashDag() {
        this.map = new HashMap<>();
    }

    /**
     * Creates a new DAG and initialize it with the contents and structure of a given {@link Map}.
     * Each key of the map is a node, and each value is a collection of that node's outgoing nodes.
     *
     * @param map the map to initialize this DAG with
     */
    public HashDag(Map<E, Collection<E>> map) {
        this.map = new HashMap<>();
        map.forEach(this::putAll);
    }


    /* Methods exclusive to Dag<> */

    public boolean put(E source, E target) {
        boolean changed;
        if (!map.containsKey(source)) {
            Set<E> targets = new HashSet<>();
            targets.add(target);
            changed = map.put(source, targets) != targets;
        } else {
            changed = map.get(source).add(target);
        }
        changed |= add(target);
        return changed;
    }

    public boolean putAll(E source, Collection<E> targets) {
        boolean changed = false;
        if (!targets.isEmpty()) {
            for (E target : targets) {
                changed |= put(source, target);
            }
        } else {
            changed = add(source);
        }
        return changed;
    }

    public boolean removeEdge(E source, E target) {
        return map.containsKey(source) && map.get(source).remove(target);
    }

    public List<E> sort() {

        // https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm
        // Great for running a task on these elements in a single thread

        List<E> sorted = new LinkedList<>();
        Deque<E> s = new LinkedList<>(getRoots());

        Map<E, Collection<E>> copy = this.toMap();

        while (!s.isEmpty()) {
            E n = s.pop();
            sorted.add(n);

            for (E m : copy.remove(n)) {
                boolean hasIncoming = false;
                for (Collection<E> entry : copy.values()) {
                    if (entry.contains(m)) {
                        hasIncoming = true;
                        break;
                    }
                }
                if (!hasIncoming) {
                    s.add(m);
                }
            }
        }

        if (!copy.isEmpty()) {
            return null;
        }

        return sorted;

    }

    public Set<E> getRoots() {
        Set<E> roots = new HashSet<>(map.keySet());
        for (Collection<E> targets : map.values()) {
            roots.removeAll(targets);
        }
        return roots;
    }

    public Set<E> getLeaves() {
        Set<E> leaves = new HashSet<>();
        for (Map.Entry<E, Collection<E>> entry : map.entrySet()) {
            if (entry.getValue().isEmpty()) {
                leaves.add(entry.getKey());
            }
        }
        return leaves;
    }

    public Set<E> getIncoming(E node) {
        Set<E> set = new HashSet<>();
        for (Map.Entry<E, Collection<E>> entry : map.entrySet()) {
            if (entry.getValue().contains(node)) {
                set.add(entry.getKey());
            }
        }
        return set;
    }

    public Set<E> getOutgoing(E node) {
        Collection<E> outgoing = map.get(node);
        if (outgoing == null) {
            return new HashSet<>();
        } else {
            return new HashSet<>(outgoing);
        }
    }

    public Set<E> getAncestors(E node) {
        checkForCircularDependency();
        return getAncestorsImpl(node);
    }

    private Set<E> getAncestorsImpl(E node) {
        Set<E> ancestors = getIncoming(node);
        for (E ancestor : new HashSet<>(ancestors)) {
            ancestors.addAll(getAncestorsImpl(ancestor));
        }
        return ancestors;
    }

    public Set<E> getDescendants(E node) {
        checkForCircularDependency();
        return getDescendantsImpl(node);
    }

    private Set<E> getDescendantsImpl(E node) {
        Set<E> descendants = getOutgoing(node);
        for (E descendant : new HashSet<>(descendants)) {
            descendants.addAll(getDescendantsImpl(descendant));
        }
        return descendants;
    }

    public Set<E> getFamily(E node) {
        checkForCircularDependency();
        Set<E> result = getAncestorsImpl(node);
        result.addAll(getDescendantsImpl(node));
        result.add(node);
        return result;
    }

    private void checkForCircularDependency() {
        if (sort() == null) {
            throw new IllegalArgumentException("DAG contains a circular dependency");
        }
    }

    public Set<E> getNodes() {
        return toMap().keySet();
    }

    public HashDag<E> inverted() {
        Map<E, Collection<E>> result = new HashMap<>();
        for (Map.Entry<E, Collection<E>> entry : this.map.entrySet()) {
            E source = entry.getKey();
            Collection<E> targets = entry.getValue();
            for (E target : targets) {
                if (!result.containsKey(target)) {
                    result.put(target, new HashSet<>());
                }
                result.get(target).add(source);
            }
        }
        return new HashDag<>(result);
    }

    
    public HashDag<E> union(HashDag<E> other) {
    	HashDag<E> union = new HashDag<>(map);
        for (Map.Entry<E, Collection<E>> entry : other.toMap().entrySet()) {
            union.putAll(entry.getKey(), entry.getValue());
        }
        return union;
    }

    
    public Map<E, Collection<E>> toMap() {
        Map<E, Collection<E>> copy = new HashMap<>();
        map.forEach((key, value) -> copy.put(key, new HashSet<>(value)));
        return copy;
    }


    /* Methods from Collection<E> */

    /**
     * Returns the number of nodes this DAG contains
     *
     * @return the size of the DAG
     */
    
    public int size() {
        return map.keySet().size();
    }

    /**
     * Returns {@code true} if this DAG contains no nodes
     *
     * @return {@code true} if this DAG contains no nodes
     */
    
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns {@code true} if this DAG contains the specified node
     *
     * @param node the node whose presence is to be tested
     * @return {@code true} if this DAG contains the specified node
     */
    
    public boolean contains(Object node) {
        return map.containsKey(node);
    }

    /**
     * Returns a {@link Iterator} over the nodes in this DAG.
     * The iterator will return nodes in topological order.
     *
     * @return a {@link Iterator} over the nodes in this DAG
     */
    
    public Iterator<E> iterator() {
        return sort().iterator();
    }

    /**
     * Returns an array containing all the nodes in this DAG
     *
     * @return an array containing all the nodes in this DAG
     */
    
    public Object[] toArray() {
        return sort().toArray();
    }


    /**
     * Returns an array containing all the nodes in this DAG
     *
     * @param array the array into which the nodes of this DAG will be stored if it is big enough.
     *              Otherwise, a new array of the same runtime type is allocated for this purpose.
     * @param <T>   the type of the array to create
     * @return an array containing all the nodes in this DAG
     */
    
    public <T> T[] toArray(T[] array) {
        @SuppressWarnings("unchecked")
        T[] sorted = (T[]) sort().toArray();
        if (array.length < sorted.length) {
            @SuppressWarnings("unchecked")
            T[] result = (T[]) Arrays.copyOf(sorted, sorted.length, array.getClass());
            return result;
        }
        System.arraycopy(sorted, 0, array, 0, sorted.length);
        if (array.length > sorted.length) {
            array[sorted.length] = null;
        }
        return array;
    }

    /**
     * Adds a single node to this DAG
     *
     * @param node the node to add
     * @return {@code true} if this DAG changed as a result of the call
     */
    
    public boolean add(E node) {
        return map.putIfAbsent(node, new HashSet<>()) == null;
    }

    /**
     * Removes a node and all its incoming and outgoing edges from this DAG
     *
     * @param node the node to be removed from this DAG, if present
     * @return {@code true} if the node was removed as a result of the call
     */
    
    public boolean remove(Object node) {
        boolean removed = map.remove(node) != null;
        for (E source : map.keySet()) {
            Collection<E> outgoing = map.get(source);
            if (outgoing != null) {
                removed |= outgoing.remove(node);
            }
        }
        return removed;
    }

    /**
     * Returns {@code true} if this DAG contains all the elements in the specified collection
     *
     * @param collection the collection of nodes whose entire presence is to be tested
     * @return {@code true} if this DAG contains all the elements in the specified collection
     */
    
    public boolean containsAll(Collection<?> collection) {
        return map.keySet().containsAll(collection);
    }

    /**
     * Removes each node in the specified collection and all their incoming and outgoing edges from this DAG
     *
     * @param nodes the collection of nodes to be removed from this DAG, if present
     * @return {@code true} if this DAG changed as a result of the call
     */
    
    public boolean removeAll(Collection<?> nodes) {
        boolean changed = false;
        for (Object node : nodes) {
            changed |= remove(node);
        }
        return changed;
    }

    /**
     * Retains only the nodes in this DAG that are also present in the specified collection
     *
     * @param collection the collection containing the nodes to be retained in this DAG
     * @return {@code true} if this collection changed as a result of the call
     */
    
    public boolean retainAll(Collection<?> collection) {
        // Collect the nodes to be removed
        List<E> remove = new ArrayList<>();
        for (E node : map.keySet()) {
            if (!collection.contains(node)) {
                remove.add(node);
            }
        }

        // Remove them
        boolean changed = false;
        for (E node : remove) {
            remove(node);
            changed = true;
        }

        return changed;
    }

    /**
     * Removes all the nodes from this DAG
     */
    
    public void clear() {
        map.clear();
    }

    /**
     * Compares the specified object with this DAG for equality
     *
     * @param o object to be compared for equality with this collection
     * @return {@code true} if the specified object is equal to this DAG
     */
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashDag<?> other = (HashDag<?>) o;
        return map.equals(other.map);
    }

    /**
     * Returns the hash code value for this DAG
     *
     * @return the hash code value for this DAG
     */
    
    public int hashCode() {
        return Objects.hash(map);
    }    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Iterate over each node and its outgoing edges
        for (Map.Entry<E, Collection<E>> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(" --> ");
            if (entry.getValue().isEmpty()) {
                sb.append("No outgoing edges");
            } else {
                sb.append("[");
                sb.append(String.join(", ", formatEdges(entry.getValue())));
                sb.append("]");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // Helper method to format each outgoing edge as a string
    private Collection<String> formatEdges(Collection<E> edges) {
        Collection<String> formattedEdges = new ArrayList<>();
        for (E edge : edges) {
            formattedEdges.add(edge.toString());
        }
        return formattedEdges;
    }

}