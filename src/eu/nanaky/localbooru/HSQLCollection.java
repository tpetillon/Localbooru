package eu.nanaky.localbooru;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class HSQLCollection implements ImageCollection {
	
	private File root;
	
	private static final int pathLength = 259;
	private static final int tagLength = 64;
	private static final int providerLength = 64;
	private static final int propertyLength = 64;
	private static final int valueLength = Integer.MAX_VALUE;
	
	private Connection connection;
	
	private static Logger logger;

	public HSQLCollection(File path, File root) {
		this.root = root;
		try {
			connection = DriverManager.getConnection("jdbc:hsqldb:file:" + path + ";shutdown=true", "SA", "");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger = Logger.getLogger(HSQLCollection.class);
		logger.setLevel((Level)Level.INFO);
	}
	
	public File getPath() {
		return root;
	}
	
	public void init() {
		try {
			String createTables =
				"drop table tagdates if exists;" +
				"drop table imagetags if exists;" +
				"drop table tags if exists;" +
				"drop table providers if exists;" +
				"drop table providerdata if exists;" +
				"drop table images if exists;" +
				"create table images (" +
					"md5 char(32) primary key," +
					"path varchar(" + pathLength + ")," +
					"width smallint," +
					"height smallint," +
					"note tinyint" +
				");" +
				"create table providers (" +
					"provider varchar(" + providerLength + ") primary key" +
				");" +
				"create table providerdata (" +
					"provider varchar(" + providerLength + ") references providers(provider)," +
					"property varchar(" + propertyLength + ")," +
					"value varchar(" + valueLength + ")," +
					"constraint providerdata_pk primary key(provider, property)" +
				");" +
				"create table tags (" +
					"id identity," +
					"name varchar(" + tagLength + ")," +
					"type tinyint," +
					"provider varchar(" + providerLength + ") references providers(provider)," +
					"ambiguous tinyint" +
				");" +
				"create table imagetags (" +
					"image char(32) references images(md5)," +
					"tag integer references tags(id)," +
					"constraint imagetags_pk primary key(image, tag)" +
				");" +
				"create table tagdates (" +
					"image char(32) references images(md5)," +
					"provider varchar(" + providerLength + ") references providers(provider)," +
					"date bigint," +
					"constraint tagdates_pk primary key(image, provider)" +
				");";
			Statement statement = connection.createStatement();
			statement.executeUpdate(createTables);
			statement.close();
			logger.info("Database created");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			connection.commit();
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean registerProvider(DataProvider provider) {
		if(!provider.isComplete()) {
			return false;
		}
		try {
			String register = "insert into providers values ('" + provider.getName() + "');";
			Statement statement = connection.createStatement();
			statement.executeUpdate(register);
			Map<String, String> properties = provider.getProperties();
			properties.put("class", provider.getClass().toString());
			for(String property : properties.keySet()) {
				register = "insert into providerdata values (" +
								"'" + provider.getName() + "', " +
								"'" + property + "', " +
								"'" + properties.get(property) + "'" +
								");";
				statement.executeUpdate(register);
			}
			statement.close();
			logger.info("Provider added: " + provider.getName());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean updateProvider(DataProvider provider) {
		if(!provider.isComplete()) {
			return false;
		}
		try {
			String delete = "delete from providerdata where provider='" + provider.getName() + "'";
			Statement statement = connection.createStatement();
			statement.executeUpdate(delete);
			statement.close();
			statement = connection.createStatement();
			Map<String, String> properties = provider.getProperties();
			properties.put("class", provider.getClass().getName());
			for(String property : properties.keySet()) {
				String register = "insert into providerdata values (" +
								"'" + provider.getName() + "', " +
								"'" + property + "', " +
								"'" + properties.get(property) + "'" +
								");";
				statement.executeUpdate(register);
			}
			statement.close();
			logger.info("Provider updated: " + provider.getName());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public List<DataProvider> getProviders() {
		List<DataProvider> providers = new ArrayList<DataProvider>();
		Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
		try {
			Statement select = connection.createStatement();
			String query = "select * from providerdata;";
			ResultSet result = select.executeQuery(query);
			while(result.next()) {
				if(!properties.containsKey(result.getString(1))) {
					properties.put(result.getString(1), new HashMap<String, String>());
				}
				properties.get(result.getString(1)).put(result.getString(2), result.getString(3));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(String providerName : properties.keySet()) {
			Map<String, String> providerProperties = properties.get(providerName);
			String className = providerProperties.get("class");
			DataProvider provider = null;
			try {
				provider = (DataProvider) Class.forName(className).newInstance();
				for(String propertyName : providerProperties.keySet()) {
					if(propertyName.compareTo("class") != 0) {
						provider.setProperty(propertyName, providerProperties.get(propertyName));
					}
				}
				providers.add(provider);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return providers;
	}
	
	public Image addImage(String md5, File path) {
		String imagePath = root.toURI().relativize(path.toURI()).getPath();
		try {
			String insert = "insert into images values ('" + md5 + "', '" + imagePath + "', null, null, null);";
			Statement statement = connection.createStatement();
			statement.executeUpdate(insert);
			statement.close();
			logger.info("Image added: " + md5);
			return new Image(md5, imagePath, this);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public List<Image> getImages(int limit) {
		List<Image> list = new ArrayList<Image>();
		try {
			Statement select = connection.createStatement();
			String query = "select md5, path from images";
			if(limit > 0) {
				query += " limit " + limit;
			}
			query += ";";
			ResultSet result = select.executeQuery(query);
			while(result.next()) {
				Image image = new Image(result.getString(1), result.getString(2), this);
				list.add(image);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	public Image getImage(String md5) {
		Image image = null;
		try {
			Statement select = connection.createStatement();
			ResultSet result = select.executeQuery("select md5, path from images where md5='" + md5 + "';");
			if(result.next()) {
				image = new Image(result.getString(1), result.getString(2), this);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return image;
	}
	
	public Tag addTag(String name, String provider) {
		try {
			String insert = "insert into tags values (null, '" + name + "', null, '" + provider + "', null);";
			Statement statement = connection.createStatement();
			statement.executeUpdate(insert);
			statement.close();
			logger.info("Tag added: " + name + " @ " + provider);
			return getTag(name, provider);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public Tag getTag(String name, String provider) {
		Tag tag = null;
		try {
			Statement select = connection.createStatement();
			ResultSet result = select.executeQuery("select id, name, provider from tags where name='" + name + "' and provider='" + provider + "';");
			if(result.next()) {
				tag = new Tag(result.getInt(1), result.getString(2), result.getString(3), this);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tag;
	}
	
	public void tagImage(Image image, Tag tag) {
		try {
			String insert = "insert into imagetags values ('" + image.getMd5() + "', " + tag.getId() + ");";
			Statement statement = connection.createStatement();
			statement.executeUpdate(insert);
			statement.close();
			logger.info("Image tagged: " + image.getMd5() + " -> " + tag);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Tag> getImageTags(Image image) {
		List<Tag> tags = new ArrayList<Tag>();
		try {
			Statement select = connection.createStatement();
			ResultSet result = select.executeQuery("select id, name, provider from imagetags, tags " +
						"where image='" + image.getMd5() + "' and tag=id;");
			while(result.next()) {
				Tag tag = new Tag(result.getInt(1), result.getString(2), result.getString(3), this);
				tags.add(tag);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tags;
	}
	
	public List<Image> getImagesFromTags(List<Tag> tags) {
		List<Image> images = new ArrayList<Image>();
		try {
			Statement select = connection.createStatement();
			String request = "";
			boolean firstTag = true;
			for(Tag tag : tags) {
				if(firstTag) {
					firstTag = false;
				} else {
					request += " intersect ";
				}
				request += "select md5, path from images, imagetags where image=md5 and tag=" + tag.getId();
			}
			request += ";";
			ResultSet result = select.executeQuery(request);
			while(result.next()) {
				Image image = new Image(result.getString(1), result.getString(2), this);
				images.add(image);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return images;
	}
	
	public void setTagDate(Image image, String provider, Date date) {
		if(getTagDate(image, provider) == null) {
			try {
				String insert = "insert into tagdates values ('" +
						image.getMd5() + "', '" +
						provider + "', " +
						date.getTime() +");";
				Statement statement = connection.createStatement();
				statement.executeUpdate(insert);
				statement.close();
				logger.info("Date added: " + image.getMd5() + ", " + provider + ": " + date);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				Statement statement = connection.createStatement();
				statement.executeUpdate("update tagdates set date=" + date.getTime() + " where " +
						"image='" + image.getMd5() + "' and provider='" + provider + "';");
				statement.close();
				logger.info("Date modified: " + image.getMd5() + ", " + provider + ": " + date);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public Date getTagDate(Image image, String provider) {
		Date date = null;
		try {
			Statement select = connection.createStatement();
			ResultSet result = select.executeQuery("select date from tagdates " +
					"where image='" + image.getMd5() + "' and provider='" + provider + "';");
			if(result.next()) {
				date = new Date(result.getLong(1));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return date;
	}
}
