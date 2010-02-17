package eu.nanaky.localbooru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DanbooruDataProvider implements DataProvider {
	
	private String name;
	private String baseURL;
	private String salt;
	private String login;
	private String password;
	
	private Logger logger;
	
	public DanbooruDataProvider() {
		name = "";
		baseURL = "";
		salt = "";
		login = "";
		password = "";
		
		logger = Logger.getLogger(FileScanner.class);
		logger.setLevel((Level)Level.INFO);
	}
	
	public DanbooruDataProvider(String name, String baseURL) {
		this.name = name;
		this.baseURL = baseURL;
		salt = "";
		login = "";
		password = "";
		
		logger = Logger.getLogger(FileScanner.class);
		logger.setLevel((Level)Level.INFO);
	}
	
	@Override
	public boolean setProperty(String property, String value) {
		if(property.compareTo("name") == 0) {
			name = value;
		} else if(property.compareTo("baseURL") == 0) {
			baseURL = value;
		} else if(property.compareTo("salt") == 0) {
			salt = value;
		} else if(property.compareTo("login") == 0) {
			login = value;
		} else if(property.compareTo("password") == 0) {
			password = value;
		} else {
			return false;
		}
		return true;
	}
	
	@Override
	public String getProperty(String property) {
		if(property.compareTo("name") == 0) {
			return name;
		} else if(property.compareTo("baseURL") == 0) {
			return baseURL;
		} else if(property.compareTo("salt") == 0) {
			return salt;
		} else if(property.compareTo("login") == 0) {
			return login;
		} else if(property.compareTo("password") == 0) {
			return password;
		} else {
			return null;
		}
	}

	@Override
	public Map<String, String> getProperties() {
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("name", name);
		properties.put("baseURL", baseURL);
		properties.put("salt", salt);
		properties.put("login", login);
		properties.put("password", password);
		return properties;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean isComplete() {
		return name.compareTo("") != 0 && baseURL.compareTo("") != 0;
	}

	@Override
	public List<String> getTagsFromMd5(String md5) {
		Map<String, String> arguments = new HashMap<String, String>();
		arguments.put("tags", "md5%3A" + md5);
		boolean useLogin = false;
		if(login.compareTo("") != 0 && password.compareTo("") != 0) {
			useLogin = true;
		}
		URL url = makeURL("/post/index.xml", arguments, useLogin);
		List<String> tags = null;
		try {
			String xml = sendRequest(url);
			tags = new ArrayList<String>();
			DOMParser parser = new DOMParser();
			parser.parse(new InputSource(new java.io.StringReader(xml)));
			Document doc = parser.getDocument();
			String allTags = doc.getElementsByTagName("post").item(0).getAttributes().getNamedItem("tags").getNodeValue();
			if(allTags == null) {
				return tags;
			}
			StringTokenizer tokenizer = new StringTokenizer(allTags, " ");
			while(tokenizer.hasMoreTokens()) {
				tags.add(tokenizer.nextToken());
			}
		} catch (SAXException e) {
			return null;
		} catch (IOException e) {
			logger.error("Network problem when retieving tags for md5 " + md5 + ": " + e);
			return null;
		} catch (NullPointerException e) {
			return null;
		}
		return tags;
	}
	
	private URL makeURL(String requestURL, Map<String, String> arguments, boolean useLogin) {
		String url = baseURL + requestURL + "?";
		for(String argument : arguments.keySet()) {
			url += argument + "=" + arguments.get(argument) + "&";
		}
		if(useLogin) {
			url += "login=" + login + "&";
			url += "password_hash=" + hashPassword(password, salt) + "&";
		}
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL !");
			e.printStackTrace();
			return null;
		}
	}
	
	private static String hashPassword(String password, String salt) {
		return DigestUtils.shaHex(salt.replaceFirst("<\\?>", password));
	}
	
	private String sendRequest(URL url) throws IOException {
		String result = "";
		
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.connect();
		BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuffer sb = new StringBuffer();
		String line;
		while((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		result = sb.toString();
		
		return result;
	}
	
	public String toString() {
		return name;
	}

}
