package eu.nanaky.localbooru;

import java.util.Date;
import java.util.List;

public class Image {

	private String md5;
	private String path;
	
	private ImageCollection collection;
	
	public Image(String md5, String path, ImageCollection collection) {
		this.md5 = md5;
		this.path = path;
		this.collection = collection;
	}
	
	public String getMd5() {
		return md5;
	}
	
	public String getPath() {
		return path;
	}
	
	public boolean tag(DataProvider provider) {
		List<String> rawTags = provider.getTagsFromMd5(md5);
		if(rawTags == null) {
			return false;
		}
		
		for(String rawTag : rawTags) {
			Tag tag = collection.getTag(rawTag, provider.getName());
			if(tag == null) {
				tag = collection.addTag(rawTag, provider.getName());
			}
			if(!getTags().contains(tag)) {
				collection.tagImage(this, tag);
			}
		}
		return true;
	}
	
	public List<Tag> getTags() {
		return collection.getImageTags(this);
	}
	
	public void setTagDate(String provider, Date date) {
		collection.setTagDate(this, provider, date);
	}
	
	public Date getTagDate(String provider) {
		return collection.getTagDate(this, provider);
	}
	
	@Override
	public String toString() {
		return "Image " + md5;
	}
	
	@Override
	public boolean equals(Object aThat) {
		if(this == aThat) {
			return true;
		}
		if(!(aThat instanceof Image)) {
			return false;
		}
		Image that = (Image) aThat;
		return md5.compareTo(that.md5) == 0 && path.compareTo(that.path) == 0;
	}
	
	@Override
	public int hashCode() {
		return md5.hashCode() + path.hashCode();
	}
}
