package com.esri.arcgis.odata.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
	String configFileName = "C:/src/workspace/odata/com/esri/arcgis/odata/config/odata.properties";
	String appName = "";
	String appVersion = "";
	String appVendor = "";

	String odataArcGISServer = "";
	int odataPort = 8181;
	
	public Config () throws IOException { 
		
		Properties prop = new Properties();
	    InputStream is = new FileInputStream(this.configFileName);

	    prop.load(is);

	    this.appName = prop.getProperty("app.name");
	    this.appVersion = prop.getProperty("app.version");
	    this.appVendor = prop.getProperty("app.vendor");
	    this.odataPort =  Integer.parseInt(prop.getProperty("app.port"));
	    this.odataArcGISServer = prop.getProperty("app.arcgisserver");
	    
	    System.out.println(this.appName);
	    System.out.println(this.appVersion);

	    System.out.println(this.odataArcGISServer);
	    //System.out.println(this.odataPort);
		
	    prop = null;
	    is = null;
	}
	
	public String getAppName() {
		  return this.appName;
	}

	public String getAppVersion() {
		  return this.appVersion;
	}
	public String getAppVendor() {
		  return this.appVendor;
	}
	public int getODataPort() {
		  return this.odataPort;
	}
	public String getODataArcGISServer() {
		  return this.odataArcGISServer;
	}



}
