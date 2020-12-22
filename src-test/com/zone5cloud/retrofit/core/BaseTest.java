package com.zone5cloud.retrofit.core;

import java.io.File;

import com.zone5cloud.core.ClientConfig;
import org.apache.commons.io.FileUtils;

public abstract class BaseTest {
	
	/* SET YOUR TEST EMAIL HERE */
	protected String TEST_EMAIL = "<enter-your-email-here@todaysplan.com.au>";
	protected String TEST_PASSWORD = "<enter-your-password-here>";
	protected String TEST_BIKE_UUID = null; // andrew SBC Staging: "d584c5cb-e81f-4fbe-bc0d-667e9bcd2c4c"
	
	/* SET YOUR SERVER ENDPOINT HERE */
	protected String server = "";
	// This is your allocated clientId and secret - these can be set to null for S-Digital environments
	protected  String zone5BaseUrl = ""; //"<add zone5 base url>";
	protected ClientConfig clientConfig = new ClientConfig();

    public BaseTest() {
    		// read config ~/tp.env or ~/z5.env
    		File f = new File(System.getProperty("user.home")+File.separatorChar+"tp.env");
    		if (!f.exists())
    			 f = new File(System.getProperty("user.home")+File.separatorChar+"z5.env");
    		
    		if (f.exists()) {
    			try {
    				for(String line : FileUtils.readLines(f, "UTF-8")) {
    					String[] arr = line.split("=");
    					if (arr.length == 2) {
    						String key = arr[0].trim();
    						String value = arr[1].trim();
    						switch(key) {
    						case "username":
    							TEST_EMAIL = value;
    							clientConfig.setUserName(value);
    							break;
    						case "password":
								TEST_PASSWORD = value;
    							break;
    						case "server":
    							server = value;
    							break;
    						case "clientID":
    							clientConfig.setClientID(value);
    							break;
    						case "clientSecret":
    							clientConfig.setClientSecret(value);
    							break;
							case "zone5BaseUrl":
								zone5BaseUrl = value;
								break;
    						}
    					}
    				}
    			} catch (Exception e) { }
    			
    			if (f.exists() && clientConfig.getUserName() != null || server != null)
    				System.out.println(String.format("[ Using credentials in file %s - server=%s, username=%s ]", f.getAbsolutePath(), server, clientConfig.getUserName()));
    		}
    }
    
	public String getBaseEndpoint() {
		if (server.startsWith("127.0.0.1") || server.endsWith("8080"))
			return String.format("http://%s", server);
		return String.format("https://%s", server);
	}

}
