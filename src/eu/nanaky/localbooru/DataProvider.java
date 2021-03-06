package eu.nanaky.localbooru;

import java.util.List;
import java.util.Map;

public interface DataProvider {
	
	public boolean setProperty(String property, String value);
	public String getProperty(String property);
	public Map<String, String> getProperties();

	public String getName();
	
	public boolean isComplete();
	
	public List<String> getTagsFromMd5(String md5);
}
