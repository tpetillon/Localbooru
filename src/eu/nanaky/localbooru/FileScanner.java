package eu.nanaky.localbooru;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import eu.medsea.mimeutil.MimeException;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public class FileScanner {

	private File root;
	private List<MimeType> allowedTypes;
	
	private static Logger logger;
	
	public FileScanner(File directory) {
		root = directory;
		
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector");
		allowedTypes = new ArrayList<MimeType>();
		allowedTypes.add(MimeUtil.getFirstMimeType("image/jpeg"));
		allowedTypes.add(MimeUtil.getFirstMimeType("image/png"));
		allowedTypes.add(MimeUtil.getFirstMimeType("image/gif"));
		allowedTypes.add(MimeUtil.getFirstMimeType("image/bmp"));
		allowedTypes.add(MimeUtil.getFirstMimeType("image/tiff"));
		allowedTypes.add(MimeUtil.getFirstMimeType("image/svg+xml"));
		allowedTypes.add(MimeUtil.getFirstMimeType("application/x-shockwave-flash"));
		
		logger = Logger.getLogger(FileScanner.class);
		logger.setLevel((Level)Level.INFO);
	}
	
	public void scan(ImageCollection collection) {
		scanDirectory(root, collection);
	}
	
	private void scanDirectory(File directory, ImageCollection collection) {
		for(File file : directory.listFiles()) {
			if(file.isDirectory()) {
				scanDirectory(file, collection);
			} else {
				try {
					String md5 = Checksum.getFileMD5(file.getPath());
					MimeType type = (MimeType) MimeUtil.getMimeTypes(file).toArray()[0];
					if(allowedTypes.contains(type)) {
						Image image = collection.getImage(md5);
						if(image == null) {
							collection.addImage(md5, file);
						}
						logger.info(file.getPath() + ": " + type + ", " + md5);
					}
				} catch (MimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
