package com.ryaltech.samples.cache.twolayer;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import com.github.benmanes.caffeine.cache.Caffeine;

@SpringBootApplication
@EnableCaching
public class TwoLayerCacheApplication implements CommandLineRunner{
	@Bean
	public FileCache fileCache() {
		return new FileCache();
	}

	@Bean
	public CacheManager cacheManager(FileCache fileCache) {
		FlexibleCaffeineCacheManager cacheManager = new FlexibleCaffeineCacheManager();
		cacheManager.setCacheNames(Arrays.asList("serviceCache"));
		cacheManager.setCaffeine(caffeineCacheBuilder(fileCache));
		cacheManager.setCacheLoader(fileCache);
		return cacheManager;
	}
	

	Caffeine < Object, Object > caffeineCacheBuilder(FileCache fileCache) {
		  return Caffeine.newBuilder()
		   .initialCapacity(100)
		   .maximumSize(500)
		   .expireAfterAccess(1, TimeUnit.SECONDS)		   
		   .recordStats().writer(fileCache);
		 }
	@Autowired
	Service service;
	public static void main(String[] args) {
		SpringApplication.run(TwoLayerCacheApplication.class, args);
	}

}
