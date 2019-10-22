package com.zone5ventures.retrofit;

import java.io.File;

import org.apache.commons.io.FileUtils;

public abstract class BaseTest {
	
	/* SET YOUR OAUTH BEARER TOKEN HERE */
	protected String token = null;
	
	/* SET YOUR SERVER ENDPOINT HERE */
	protected String server = "staging.todaysplan.com.au";
	
	{
		// read token and server from ~/tp.env
		// token = ...
		// server = ...
		File f = new File(System.getProperty("user.home")+File.separatorChar+"tp.env");
		if (!f.exists())
			 f = new File(System.getProperty("user.home")+File.separatorChar+"z5.env");
		
		if (f.exists()) {
			try {
				for(String line : FileUtils.readLines(f, "UTF-8")) {
					String[] arr = line.split(" = ");
					if (arr.length == 2) {
						if (arr[0].trim().equals("token"))
							token = arr[1].trim();
						else if (arr[0].trim().equals("server"))
							server = arr[1].trim();
					}
				}
			} catch (Exception e) { }
			
			if (token != null || server != null)
				System.out.println(String.format("[ Using credentials in file %s - server=%s, token=%s ]", f.getAbsolutePath(), server, token));
		}
	}
	
	public String getBaseEndpoint() {
		if (server.startsWith("127.0.0.1"))
			return String.format("http://%s", server);
		return String.format("https://%s", server);
	}

}
