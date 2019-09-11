package com.ryaltech.samples.cache.twolayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Extending Caffeine cache manager to allow flexible per-cache configuration
 * found at: https://techblog.bozho.net/multiple-cache-configurations-with-caffeine-and-spring-boot/
 */
public class FlexibleCaffeineCacheManager<K,V> extends CaffeineCacheManager implements InitializingBean {
	private Map<String, String> cacheSpecs = new HashMap<>();

	private Map<String, Caffeine<Object, Object>> builders = new HashMap<>();

	private CacheLoader<Object,Object> cacheLoader;

	public FlexibleCaffeineCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));

	}
	@Override
	public void afterPropertiesSet() throws Exception {
		for (Map.Entry<String, String> cacheSpecEntry : cacheSpecs.entrySet()) {
			builders.put(cacheSpecEntry.getKey(), Caffeine.from(cacheSpecEntry.getValue()));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Cache<Object, Object> createNativeCaffeineCache(String name) {
		Caffeine<Object, Object> builder = builders.get(name);
		if (builder == null) {
			return super.createNativeCaffeineCache(name);
		}

		if (this.cacheLoader != null) {
			return builder.build(this.cacheLoader);
		} else {
			return builder.build();
		}
	}

	public Map<String, String> getCacheSpecs() {
		return cacheSpecs;
	}

	public void setCacheSpecs(Map<String, String> cacheSpecs) {
		this.cacheSpecs = cacheSpecs;
	}

	public void setCacheLoader(CacheLoader cacheLoader) {
		super.setCacheLoader(cacheLoader);
		this.cacheLoader = cacheLoader;
	}
}