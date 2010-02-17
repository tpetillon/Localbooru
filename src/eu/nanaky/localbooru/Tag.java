package eu.nanaky.localbooru;

import java.util.ArrayList;
import java.util.List;

public class Tag {

	private int id;
	private String name;
	private String provider;
	
	private ImageCollection collection;
	
	public Tag(int id, String name, String provider, ImageCollection collection) {
		this.id = id;
		this.name = name;
		this.provider = provider;
		this.collection = collection;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getProvider() {
		return provider;
	}
	
	public List<Image> getImages() {
		List<Tag> list = new ArrayList<Tag>();
		list.add(this);
		return collection.getImagesFromTags(list);
	}
	
	@Override
	public String toString() {
		return id + ": " + name + " @ " + provider;
	}
	
	@Override
	public boolean equals(Object aThat) {
		if(this == aThat) {
			return true;
		}
		if(!(aThat instanceof Tag)) {
			return false;
		}
		Tag that = (Tag) aThat;
		return id == that.id
				&& name.compareTo(that.name) == 0
				&& provider.compareTo(that.provider) == 0;
	}
	
	@Override
	public int hashCode() {
		return id + name.hashCode() + provider.hashCode();
	}
}
