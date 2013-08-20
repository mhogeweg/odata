package com.esri.arcgis.odata.resources;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;


public class ArcGISMapServerResource extends Resource {
	String mapServiceName;
	String FID;
	String attributeName;

	String mapServer = "http://hogeweg.esri.com/arcgis/rest/services/";
	
	public ArcGISMapServerResource(Context context, Request request, Response response) {
		super(context, request, response);
		this.mapServiceName = (String) request.getAttributes().get("MapServiceName");  
		if (this.mapServiceName.endsWith(")")) {
			this.FID = this.mapServiceName.substring(this.mapServiceName.indexOf("(")+1, this.mapServiceName.indexOf(")"));
			this.mapServiceName = this.mapServiceName.substring(0, this.mapServiceName.indexOf("("));
		} else {
			this.FID = null;
		}
		
		this.attributeName = (String) request.getAttributes().get("AttributeName");  
		
		//This representation has only one type of representation.
		//getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	
	/**
	 * Returns a full representation for a given variant.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		
		String sResponse = "";
		URL url = null;
		String sUrl = this.mapServer + this.mapServiceName + "/MapServer";
		
		// determine if a feature ID (FID) was given. if so, get that item from the map server
		// otherwise do something with the map service info
		if (this.FID==null){
			sUrl += "?f=pjson";
		} else {
			sUrl += "/find?searchText=" + this.FID + "&contains=true&searchFields=FID&sr=&layers=0&layerdefs=&returnGeometry=true&maxAllowableOffset=&f=pjson";
		}
		
		// send HTTP GET request and read response		
    	try {       		
			url = new URL(sUrl);
			URLConnection conn = url.openConnection();

	        // Get the response
	        String line = "";
	        
	        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        while ((line = rd.readLine()) != null) {
	            // Process line...
	    		sResponse += line;
	    	}
	        //wr.close();
	        rd.close();
			
			conn = null;
			url = null;
			
		} catch(Exception e) {
		  System.out.println("Error when processing ArcGIS service request: " + sUrl + " :" + e.getMessage() );
		  sResponse = "Oopsie!";
		}    
		
		// if the request was ok, then see if an attribute name was given
		// if so return only that attribute value
		// otherwise return the entire feature info

		if (sResponse != "Oopsie!") {
			if (this.attributeName != "") {
				// quick and dirty: grab string after "attributes" then find "{attributeName} : {attributeValue},"
				String sStart = sResponse.substring(sResponse.indexOf("\"attributes\" : {"));
				sStart = sStart.substring(0, sStart.indexOf("}"));
				
				String sAttribute = sStart.substring(sStart.indexOf("\"" + this.attributeName + "\" : "));
				sAttribute = sAttribute.substring(sAttribute.indexOf(" : \"")+4, sAttribute.length()-2);
				sAttribute = sAttribute.substring(0, sAttribute.indexOf("\","));
				
				sResponse = sAttribute;
			}
		}
		Representation representation = new StringRepresentation(sResponse, MediaType.TEXT_PLAIN);
		return representation;
	}
}
