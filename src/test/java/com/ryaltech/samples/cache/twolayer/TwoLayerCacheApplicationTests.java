package com.ryaltech.samples.cache.twolayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TwoLayerCacheApplicationTests {
	@Mock
	ServiceListener listener;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@Autowired
	Service service;


	File cacheDir;
	FileCache fileCache;
	
	@Autowired
	public void setFileCache( FileCache fileCache, CacheManager cacheManager) throws IOException {
		//cache was already preloaded. Since we change directory here we better flush
		cacheManager.getCacheNames().stream().   // gets the name of all caches as a stream
        map(cacheManager::getCache).     // map the cache names to a stream of Cache:s
        forEach(Cache::clear); 
		
		cacheDir = Files.createTempDirectory("filecache").toFile();
		cacheDir.mkdirs();
		cacheDir.deleteOnExit();		
		fileCache.withDirectory(cacheDir);
		this.fileCache = fileCache;
	}
	
	

	@Test
	public void testCaching() {
		assertEquals("value of key1", service.getValue("key1", listener));
		assertEquals("value of key1", service.getValue("key1", listener));
		verify(listener, times(1)).onServiceCall("key1");
	}

	@Test
	public void testCachingExpiration() throws Exception {
		assertEquals("value of key1", service.getValue("key1", listener));
		Thread.sleep(2000);
		assertEquals("value of key1", service.getValue("key1", listener));
		verify(listener, times(1)).onServiceCall("key1");
	}

	@Test
	public void testCachingLoadFileAfterLocalCacheCreation() throws Exception {
		assertEquals("value of key1", service.getValue("key1", listener));
		fileCache.write("key2", "value2");
		assertEquals("value2", service.getValue("key2", listener));
		verify(listener, times(1)).onServiceCall("key1");
		verify(listener, times(0)).onServiceCall("key2");
	}

	@Test
	public void testGarbageInFile() throws Exception {
		File cacheFile = new File(cacheDir, "keyForGarbage");
		try(FileWriter fw = new FileWriter(cacheFile)){
			fw.write("garbage");			
		}
		assertEquals(7,cacheFile.length());
		assertEquals("value of keyForGarbage", service.getValue("keyForGarbage", listener));
		verify(listener, times(1)).onServiceCall("keyForGarbage");
		
		//file should have changed
		assertNotEquals(7, cacheFile.length());
	}

}
