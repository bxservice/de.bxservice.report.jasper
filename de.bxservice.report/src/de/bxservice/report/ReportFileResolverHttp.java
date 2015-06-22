package de.bxservice.report;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.logging.Level;

import net.sf.jasperreports.engine.util.FileResolver;

public class ReportFileResolverHttp extends ReportFileResolver {
	
	private MessageDigest md5Agent;
    private byte[] buffer = new byte[4096];

	public ReportFileResolverHttp(FileResolver parent) {
		super(parent);
	}
	
	public File resolveFile(String fileName){
		return httpDownloadedReport(fileName);
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
     * @author rlemeill
     * @param reportLocation http string url ex: http://adempiereserver.domain.com/webApp/standalone.jrxml
     * @return downloaded File (or already existing one)
     */
    private File httpDownloadedReport(String reportLocation)
    {
    	File reportFile = null;
    	File downloadedFile = null;
    	if (log.isLoggable(Level.INFO)) log.info(" report deployed to " + reportLocation);
    	try {

    		String[] tmps = reportLocation.split("/");
    		String cleanFile = tmps[tmps.length-1];
    		String localFile = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + cleanFile;
    		String downloadedLocalFile = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator")+"TMP" + cleanFile;

    		reportFile = new File(localFile);

    		if (reportFile.exists())
    		{
    			
    			md5Agent = MessageDigest.getInstance("MD5");
    			byte[] hash = md5Agent.digest(digestAsByteArray(reportFile));
    			String localMD5hash = new String(hash);
    			String remoteMD5Hash = getRemoteMD5(reportLocation);
    			if (log.isLoggable(Level.INFO)) 
    				log.info("MD5 for local file is "+localMD5hash );

    			if ( remoteMD5Hash != null)
    			{
    				if (localMD5hash.equals(remoteMD5Hash))
    				{
    					if (log.isLoggable(Level.INFO)) log.info(" no need to download: local report is up-to-date");
    				}
    				else
    				{
    					if (log.isLoggable(Level.INFO)) log.info(" report on server is different that local one, download and replace");
    					downloadedFile = getRemoteFile(reportLocation, downloadedLocalFile);
    					reportFile.delete();
    					downloadedFile.renameTo(reportFile);
    				}
    			}
    			else
    			{
    				log.warning("Remote hashing is not available did you deployed webApp.ear?");
    				downloadedFile = getRemoteFile(reportLocation, downloadedLocalFile);
    				//    				compare hash of existing and downloaded
    				if ( md5localHashCompare(reportFile,downloadedFile) )
    				{
    					//nothing file are identical
    					if (log.isLoggable(Level.INFO)) log.info(" no need to replace your existing report");
    				}
    				else
    				{
    					if (log.isLoggable(Level.INFO)) log.info(" report on server is different that local one, replacing");
    					reportFile.delete();
    					downloadedFile.renameTo(reportFile);
    				}
    			}
    		}
    		else
    		{
    			reportFile = getRemoteFile(reportLocation,localFile);
    		}

    	}
    	catch (Exception e) {
    		log.severe("Unknown exception: "+ e.getMessage());
    		return null;
    	}
    	return reportFile;
    }

    public String digestAsBase64(byte[] hash) throws Exception
    {
        String stringHash = new String(hash);
        return stringHash;
    }
    
    synchronized public byte[] digestAsByteArray(File file) throws Exception
    {
    	md5Agent.reset();
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        for (int bytesRead = 0; (bytesRead = is.read(buffer)) >= 0;)
        {
        	md5Agent.update(buffer, 0, bytesRead);
        }
        is.close();
        byte[] digest = md5Agent.digest();
        return digest;
    }
    
    private String getRemoteMD5(String reportLocation) {
    	try{
    		String md5url = reportLocation;
    		if (md5url.indexOf("?") > 0)
    			md5url = md5url + "&md5=true";
    		else
    			md5url = md5url + "?md5=true";
    		URL reportURL = new URL(md5url);
			InputStream in = reportURL.openStream();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte buf[] = new byte[1024];
			int s = 0;
 			while((s = in.read(buf, 0, 1024)) > 0)
				baos.write(buf, 0, s);

    		in.close();
    		String hash = new String(baos.toByteArray());
    		return hash;
    	} catch (IOException e) {
			log.severe("I/O error when trying to download (sub)report from server "+ e.getMessage());
    		return null;
    	}
	}
    
    /**
     * @author rlemeill
     * @param reportLocation http://applicationserver/webApp/standalone.jrxml for example
     * @param localPath Where to put the http downloaded file
     * @return abstract File which represent the downloaded file
     */
    private File getRemoteFile(String reportLocation, String localPath)
    {
    	try{
    		URL reportURL = new URL(reportLocation);
			InputStream in = reportURL.openStream();

    		File downloadedFile = new File(localPath);

    		if (downloadedFile.exists())
    		{
    			downloadedFile.delete();
    		}

    		FileOutputStream fout = new FileOutputStream(downloadedFile);

			byte buf[] = new byte[1024];
			int s = 0;
 			while((s = in.read(buf, 0, 1024)) > 0)
				fout.write(buf, 0, s);

    		in.close();
    		fout.flush();
    		fout.close();
    		return downloadedFile;
    	} catch (FileNotFoundException e) {
			if(reportLocation.indexOf("Subreport") == -1 && !reportLocation.endsWith(".properties")) // Only show the warning if it is not a subreport or properties
				log.warning("404 not found: Report cannot be found on server "+ e.getMessage());
    		return null;
    	} catch (IOException e) {
			log.severe("I/O error when trying to download (sub)report from server "+ e.getMessage());
    		return null;
    	}
    }
    
    /**
     * @param file1 first file to compare
     * @param file2 second file to compare
     * @return true if files are identic false otherwise
     */
    public boolean md5localHashCompare(File file1,File file2)
    {
    	//compute Hash of exisiting and downloaded
    	String hashFile1;
    	String hashFile2;
    	try{
    		md5Agent = MessageDigest.getInstance("MD5");
    		hashFile1 = new String(md5Agent.digest(digestAsByteArray(file1)));
    		hashFile2 = new String(md5Agent.digest(digestAsByteArray(file2)));
    		return hashFile1.equals(hashFile2) ; }
    	catch (Exception e)
		{
    		return false;			//if there is an error during comparison return files are difs
		}
    }
    
}
