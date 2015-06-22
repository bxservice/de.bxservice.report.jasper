package de.bxservice.report;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import net.sf.jasperreports.engine.util.FileResolver;

public class ReportFileResolverURIFile extends ReportFileResolver {

	public ReportFileResolverURIFile(FileResolver parent) {
		super(parent);
	}
	
	public File resolveFile(String fileName){
		try {
			return new File(new URI(fileName));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected Boolean checkCacheFreshness(File cacheFile, String path,
			String name, String suffix) {
		return null;
	}

	@Override
	protected InputStream loadOriginalFileAsStream(String path, String name,
			String suffix) {
		return null;
	}

}
