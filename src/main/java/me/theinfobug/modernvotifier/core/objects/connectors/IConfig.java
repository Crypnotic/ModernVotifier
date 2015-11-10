package me.theinfobug.modernvotifier.core.objects.connectors;

import java.io.File;

public interface IConfig {

	void init();
	
	File getFolder();
	
	File getFile();
	
	String get(String path);

	void set(String path, Object value);
	
	void save();
}
