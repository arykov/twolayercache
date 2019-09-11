package com.ryaltech.samples.cache.twolayer;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class Service {
	@Cacheable(value="serviceCache", key = "#a0")
	public Object getValue(String key, ServiceListener sl) {
		System.out.println("Called with key = " + key);
		sl.onServiceCall(key);
		return "value of " + key;
	}


}
