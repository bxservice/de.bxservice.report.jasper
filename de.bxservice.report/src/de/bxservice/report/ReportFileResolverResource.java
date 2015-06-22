package de.bxservice.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import net.sf.jasperreports.engine.util.FileResolver;

public class ReportFileResolverResource extends ReportFileResolver {

	public ReportFileResolverResource(FileResolver parent) {
		super(parent);
	}

	public File resolveFile(String fileName){
		try {
			return getFileAsResource(fileName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	protected Boolean checkCacheFreshness(File cacheFile, String path, String name, String suffix) {
		return null;
	}

	@Override
	protected InputStream loadOriginalFileAsStream(String path, String name, String suffix) {
		return null;
	}
	
	/**
	 * @param reportPath
	 * @return
	 * @throws Exception
	 */
	private File getFileAsResource(String reportPath) throws Exception {
		File reportFile;
		String name = reportPath.substring("resource:".length()).trim();
		String localName = name.replace('/', '_');
		if (log.isLoggable(Level.INFO)) {
			log.info("reportPath = " + reportPath);
			log.info("getting resource from = " + getClass().getClassLoader().getResource(name));
		}
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
		String localFile = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + localName;
		if (log.isLoggable(Level.INFO)) 
			log.info("localFile = " + localFile);
		
		reportFile = new File(localFile);

		boolean empty = true;
		OutputStream out = null;
		try {
			out = new FileOutputStream(reportFile);
			if (out != null){
				byte buf[]=new byte[1024];
				int len;
				while((len=inputStream.read(buf))>0) {
					empty = false;
					out.write(buf,0,len);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (out != null)
				out.close();
			if (inputStream != null)
				inputStream.close();
		}

		if (empty)
			return null;
		else
			return reportFile;
	}

}
