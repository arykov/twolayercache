package com.ryaltech.samples.cache.twolayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.CacheWriter;
import com.github.benmanes.caffeine.cache.RemovalCause;

/**
 * 
 * Serializer picked as combination of
 * https://github.com/eishay/jvm-serializers/wiki (using no schemas here) and
 * simplicity
 * 
 * @author rykov
 *
 * @param <T>
 */
public class FileCache<T> implements CacheLoader<Object, T>, CacheWriter<Object, T> {

	private KryoPool kryoPool;
	private Logger logger = LoggerFactory.getLogger(FileCache.class);
	private File directory = new File(".");
	
	

	public FileCache() {
		kryoPool = new KryoPool.Builder(new KryoFactory() {
			public Kryo create() {
				Kryo kryo = new Kryo();
				return kryo;
			}
		}).build();
	}

	
	public FileCache withDirectory(File directory) {
		this.directory = directory;

		if(!directory.exists()) {
			directory.mkdirs();
		}
		//should take care of race condition with another process or will it?
		if(directory.exists() && directory.isDirectory()) {
			return this;
		}else {
			throw new RuntimeException(String.format("Failed to create %s or it is an existing file ", directory));
		}
			
		
	}

	@Override
	public @Nullable T load(@NonNull Object key) {
		File f = getDestinationFile(key);
		if (f.exists()) {
			Kryo kryo = null;
			try (FileInputStream fis = new FileInputStream(f); Input in = new Input(fis)) {
				kryo = kryoPool.borrow();
				return (T) kryo.readClassAndObject(in);
			} catch (Exception ex) {
				logger.debug("Failed loading file: {}", f.getName(), ex);
				f.delete();
			} finally {
				if (kryo != null)
					kryoPool.release(kryo);

			}
		}
		return null;

	}

	@Override
	public void write(Object key, T value) {
		Kryo kryo = null;
		try {
			//using temp file to minimize exposure when file is modified but not complete.
			File f = File.createTempFile("filecache", "tmp", directory);
			File destinationFile = getDestinationFile(key);
			try (FileOutputStream fos = new FileOutputStream(f); Output out = new Output(fos)) {
				kryo = kryoPool.borrow();
				kryo.writeClassAndObject(out, value);
			} catch (Exception e) {
				logger.debug("Failed to write {}", destinationFile, e);
			} finally {
				if (kryo != null)
					kryoPool.release(kryo);
			}
			if(!f.renameTo(destinationFile)) {
				logger.debug("Failed to rename file {} to {}", f, destinationFile);
			}

		} catch (Exception ex) {
			logger.debug("Failed to create", ex);
		}
	}

	private File getDestinationFile(Object key) {
		return new File(directory, key.toString());
	}

	@Override
	public void delete(Object key, T value, RemovalCause cause) {
		//deletion should be separate
	}
}
