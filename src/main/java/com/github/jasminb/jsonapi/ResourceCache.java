package com.github.jasminb.jsonapi;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource caching provider.
 * <p>
 *     Cache implementation is based on thread-local variables. In order to use cache in a thread, <code>init()</code>
 *     method must be called. When cache is no longer needed, <code>clear()</code> method should be used to perform
 *     thread local cleanup.
 * </p>
 * <p>
 *     Cache can be locked by invoking <code>lock()</code>, after cache is locked, no items will be cached, and calling
 *     any cache method will be a no-op. To unlock cache and make it accept cache calls again, <code>unlock()</code>
 *     method must be invoked.
 * </p>
 * <p>
 *     If cache is used in recursive calls, it is safe to call <code>init()</code> and <code>clear()</code>
 *     multiple times. Init call will not purge current cache state in case it is called recursievly. Clear call will
 *     clear cache state only if recursion exited (initDepth == 0).
 * </p>
 *
 *
 * @author jbegic
 */
public class ResourceCache {

	private ThreadLocal<Map<String, Object>> resourceCache;
	private ThreadLocal<Integer> initDepth;
	private ThreadLocal<Boolean> cacheLocked;

	public ResourceCache() {
		resourceCache = new ThreadLocal<>();
		initDepth = new ThreadLocal<>();
		cacheLocked = new ThreadLocal<>();
	}

	/**
	 * Initialises cache for current thread-scope.
	 */
	public void init() {
		if (initDepth.get() == null) {
			initDepth.set(1);
		} else {
			initDepth.set(initDepth.get() + 1);
		}

		if (resourceCache.get() == null) {
			resourceCache.set(new HashMap<String, Object>());
		}

		if (cacheLocked.get() == null) {
			cacheLocked.set(Boolean.FALSE);
		}
	}

	/**
	 * Clears current thread scope state.
	 * @throws IllegalStateException in case <code>init()</code> was not called
	 */
	public void clear() {
		verifyState();

		initDepth.set(initDepth.get() - 1);
		if (initDepth.get() == 0) {
			resourceCache.set(null);
			cacheLocked.set(null);
			initDepth.set(null);
		}
	}


	/**
	 * Adds multiple resources to cache.
	 * @param resources items to add
	 * @throws IllegalStateException in case <code>init()</code> was not called
	 */
	public void cache(Map<String, Object> resources) {
		verifyState();

		if (!cacheLocked.get()) {
			resourceCache.get().putAll(resources);
		}
	}

	/**
	 * Adds resource to cache.
	 * @param identifier resource identifier
	 * @param resource resource
	 * @throws IllegalStateException in case <code>init()</code> was not called
	 */
	public void cache(String identifier, Object resource) {
		verifyState();

		if (!cacheLocked.get()) {
			resourceCache.get().put(identifier, resource);
		}
	}

	/**
	 * Returns cached resource or <code>null</code> if resource was not found in cache.
	 * @param identifier resource identifier
	 * @return chached resource or <code>null</code>
	 */
	public Object get(String identifier) {
		verifyState();
		return resourceCache.get().get(identifier);
	}

	/**
	 * Checks if resource with given identifier is cached.
	 * @param identifier resource identifier
	 * @return <code>true</code> if resource is cached, else <code>false</code>
	 * @throws IllegalStateException in case <code>init()</code> was not called
	 */
	public boolean contains(String identifier) {
		verifyState();
		return resourceCache.get().containsKey(identifier);
	}

	/**
	 * Locks this cache instance for thread scope that method was invoked in.
	 * <p>
	 *     After invoking this method, all cache attempts will be no-op.
	 * </p>
	 * @throws IllegalStateException in case <code>init()</code> was not called
	 */
	public void lock() {
		verifyState();
		cacheLocked.set(true);
	}

	/**
	 * Unlocks this cache instance for thread scope that method was invoked in.
	 * <p>
	 *     After invoking this method. Cache calls will cache items accordingly.
	 * </p>
	 */
	public void unlock() {
		verifyState();
		cacheLocked.set(false);
	}

	private void verifyState() {
		if (resourceCache.get() == null) {
			throw new IllegalStateException("Cache not initialised, call init() first");
		}
	}
}
