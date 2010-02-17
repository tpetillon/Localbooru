package eu.nanaky.localbooru;

import java.io.File;
import java.util.Date;
import java.util.List;


public interface ImageCollection {

	public boolean registerProvider(DataProvider provider);
	public boolean updateProvider(DataProvider provider);
	public List<DataProvider> getProviders();
	
	public Image addImage(String md5, File path);
	public List<Image> getImages(int limit);
	public Image getImage(String md5);
	public List<Image> getImagesFromTags(List<Tag> tags);
	
	public Tag addTag(String name, String provider);
	public Tag getTag(String name, String provider);
	
	public void tagImage(Image image, Tag tag);
	public List<Tag> getImageTags(Image image);
	
	public void setTagDate(Image image, String provider, Date date);
	public Date getTagDate(Image image, String provider);
	
	public File getPath();
	
	public void close();
}
