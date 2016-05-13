package com.github.jasminb.jsonapi;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.github.jasminb.jsonapi.JSONAPISpecConstants.FIRST;
import static com.github.jasminb.jsonapi.JSONAPISpecConstants.LAST;
import static com.github.jasminb.jsonapi.JSONAPISpecConstants.NEXT;
import static com.github.jasminb.jsonapi.JSONAPISpecConstants.PREV;
import static com.github.jasminb.jsonapi.JSONAPISpecConstants.RELATED;
import static com.github.jasminb.jsonapi.JSONAPISpecConstants.SELF;

/**
 * Encapsulates a JSON API response that includes a collection of resource objects, with optional links and meta
 * objects.
 * <p>
 * JSON API calls that return a collection of resource objects may be paginated or may contain meta information
 * describing the response.  This implementation exposes this information by providing access to a {@code Map} of
 * {@link #getLinks() links}, and a {@code Map} of {@link #getMeta() meta information}.  Convenience methods exist for
 * common use cases like <a href="http://jsonapi.org/format/#fetching-pagination">pagination</a> links, and obtaining
 * the {@link #getRelated() related} and {@link #getSelf() self} relation types.
 * </p>
 * <p>
 * <em>Implementation note</em>: any {@code List} operations are forwarded to the enclosed
 * {@link #ResourceList(List) resource list} supplied on construction.
 * </p>
 */
public class ResourceList<E> implements List<E> {

    /**
     * A map of link objects keyed by link name.
     */
    private Map<String, Link> links = Collections.emptyMap();

    /**
     * A map of meta fields, keyed by the meta field name
     */
    private Map<String, ?> meta = Collections.emptyMap();

    /**
     * A list of resource objects, the primary data of JSON API response
     */
    private List<E> resources = Collections.emptyList();

    /**
     * Constructs a new resource list composed of the underlying list of resources.
     *
     * @param resources a list of resource objects representing the primary data in the JSON API response;
     *                  must not be {@code null}
     */
    public ResourceList(List<E> resources) {
        if (resources == null) {
            throw new IllegalArgumentException("Resources list must not be null.");
        }
        this.resources = resources;
    }

    /**
     * Convenience method for returning the value of the {@code prev} link.
     *
     * @return the link value, or {@code null} if the named link does not exist or has no value
     */
    public String getPrevious() {
        return getLink(PREV);
    }

    /**
     * Convenience method for returning the value of the {@code first} link.
     *
     * @return the link value, or {@code null} if the named link does not exist or has no value
     */
    public String getFirst() {
        return getLink(FIRST);
    }

    /**
     * Convenience method for returning the value of the {@code next} link.
     *
     * @return the link value, or {@code null} if the named link does not exist or has no value
     */
    public String getNext() {
        return getLink(NEXT);
    }

    /**
     * Convenience method for returning the value of the {@code last} link.
     *
     * @return the link value, or {@code null} if the named link does not exist or has no value
     */
    public String getLast() {
        return getLink(LAST);
    }

    /**
     * Convenience method for returning the value of the {@code self} link.
     *
     * @return the link value, or {@code null} if the named link does not exist or has no value
     */
    public String getSelf() {
        return getLink(SELF);
    }

    /**
     * Convenience method for returning the value of the {@code related} link.
     *
     * @return the link value, or {@code null} if the named link does not exist or has no value
     */
    public String getRelated() {
        return getLink(RELATED);
    }

    /**
     * Returns the <a href="http://jsonapi.org/format/#document-links">JSON API links</a> present in the
     * response.
     *
     * @return the links in the response keyed by link name; may be empty but never {@code null}
     */
    public Map<String, Link> getLinks() {
        return links;
    }

    /**
     * Convenience method for returning the value of the named link.
     *
     * @return the link value, or {@code null} if the named link does not exist or has no value
     */
    public String getLink(String linkName) {
        if (links.containsKey(linkName)) {
            return links.get(linkName).getHref();
        }

        return null;
    }

    /**
     * Returns the <a href="http://jsonapi.org/format/#document-meta">JSON API meta information</a> present in the
     * response.  Because meta information can contain arbitrary data, the values in the returned {@code Map} are of
     * unknown type.
     *
     * @return the meta information in the response keyed by field name; may be empty but never {@code null}
     */
    public Map<String, ?> getMeta() {
        return meta;
    }

    /**
     * Package-private method for setting the meta information.
     *
     * @param meta the meta information, must not be {@code null}
     */
    void setMeta(Map<String, ?> meta) {
        if (meta == null) {
            throw new IllegalArgumentException("Meta information must not be null.");
        }
        this.meta = meta;
    }

    /**
     * Package-private method for setting link information.
     *
     * @param links the links information, must not be {@code null}
     */
    void setLinks(Map<String, Link> links) {
        if (links == null) {
            throw new IllegalArgumentException("Links map must not be null.");
        }
        this.links = links;
    }

    /*
     * List implementation: all methods for the List interface forward to the member {@link #resources resources list}.
     */

    @Override
    public int size() {
        return resources.size();
    }

    @Override
    public boolean isEmpty() {
        return resources.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return resources.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return resources.iterator();
    }

    @Override
    public Object[] toArray() {
        return resources.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return resources.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return resources.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return resources.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return resources.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return resources.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return resources.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return resources.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return resources.retainAll(c);
    }

    @Override
    public void clear() {
        resources.clear();
    }

    @Override
    public E get(int index) {
        return resources.get(index);
    }

    @Override
    public E set(int index, E element) {
        return resources.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        resources.add(index, element);
    }

    @Override
    public E remove(int index) {
        return resources.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return resources.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return resources.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return resources.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return resources.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return resources.subList(fromIndex, toIndex);
    }
}
