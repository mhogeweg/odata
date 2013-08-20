package com.esri.arcgis.odata;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.esri.gpt.framework.http.ContentProvider;
import com.esri.gpt.framework.http.CredentialProvider;
import com.esri.gpt.framework.http.HttpClientRequest;
import com.esri.gpt.framework.http.HttpClientRequest.MethodName;
import com.esri.gpt.framework.http.StringProvider;
import com.esri.gpt.framework.util.Val;
import org.json.*;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Servlet implementation class OData
 */
@WebServlet("/OData")
public class OData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOGGER = Logger.getLogger(OData.class.getName());

       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OData() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String sServiceInfo = request.getRequestURI().replace("/odata/", "");
		int    nPathIndex = sServiceInfo.indexOf("/");
		String sServer = sServiceInfo.substring(0, nPathIndex);
		String sServicePath = sServiceInfo.substring(nPathIndex+1);
		
		LOGGER.log(Level.INFO, "Hello " + sServicePath + " on " + sServer);
		
		//http://hogeweg.esri.com:8081/odata/hogeweg.esri.com/arcgis/rest/services/SampleWorldCities/MapServer
		//http://hogeweg.esri.com:8081/odata/hogeweg.esri.com/arcgis/rest/services/SampleWorldCities/MapServer/
		//http://hogeweg.esri.com:8081/odata/hogeweg.esri.com/arcgis/rest/services/SampleWorldCities/MapServer/$metadata
		
		if (sServicePath.endsWith("$metadata") 
				|| sServicePath.endsWith("/MapServer") 
				|| sServicePath.endsWith("/MapServer/")) {
			String sServiceMetadata;
			
			try {
				sServiceMetadata = GetServiceMetadata(sServer, sServicePath);
			    response.setContentType("text/xml");
			    PrintWriter out = response.getWriter();
			    out.println(sServiceMetadata);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private String GetServiceMetadata(String sServer, String sServicePath) throws IOException, JSONException {
		String sServiceMetadata = "";
		String sArcGISService = "http://" + sServer + "/" + sServicePath.replace("/$metadata", "");

		LOGGER.log(Level.INFO, "sArcGISService = " + sArcGISService);

		// get service name
		String[] pathElements;
		pathElements = sServicePath.split("/");
		String sServiceName = StringEscapeUtils.escapeXml(pathElements[pathElements.length - 2]);
		
		// get service info
		// http://hogeweg.esri.com:8081/odata/hogeweg.esri.com/arcgis/rest/services/SampleWorldCities/MapServer?f=json
		// Submit search query and get response as an InputStream object
		String requestUrl = sArcGISService + "/layers?f=json";
		InputStream responseStream = submitHttpRequest("GET", requestUrl, "", null, null);		 
		BufferedInputStream bIStream = new BufferedInputStream(responseStream);		 
	    String responseStr = readCharacters(bIStream, "UTF-8");
	    
	    // read response into JSON Object
	    JSONObject jsoParent = new JSONObject(responseStr);
	    

		// Init response
		sServiceMetadata = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>";
		sServiceMetadata += "<edmx:Edmx Version='1.0' xmlns:edmx='http://schemas.microsoft.com/ado/2007/06/edmx'>\n";
		sServiceMetadata += "<edmx:DataServices xmlns:m='http://schemas.microsoft.com/ado/2007/08/dataservices/metadata' m:DataServiceVersion='2.0'>\n";
		sServiceMetadata += "<Schema Namespace='ArcGISOData' xmlns:d='http://schemas.microsoft.com/ado/2007/08/dataservices' xmlns:m='http://schemas.microsoft.com/ado/2007/08/dataservices/metadata' xmlns='http://schemas.microsoft.com/ado/2007/05/edm'>\n";
		
	    try {
	        JSONArray jsoLayers = jsoParent.getJSONArray("layers");

	        // loop over the layers in the service 
	        for (int i=0;i<jsoLayers.length();i++) {
	        	JSONObject layer = jsoLayers.getJSONObject(i);

        		// set EntityType to layer name
            	String name = StringEscapeUtils.escapeXml(Val.chkStr(layer.getString("name")));
        		sServiceMetadata += "<EntityType Name='" + name + "'>\n";
            	
    	        JSONArray jsoLayerFields = layer.getJSONArray("fields");
    	        
    	        // get key field
    	        for (int j=0;j<jsoLayerFields.length();j++) {
    	        	JSONObject field = jsoLayerFields.getJSONObject(j);
    	        	if (field.getString("type").equalsIgnoreCase("esriFieldTypeOID")) {
    	        		sServiceMetadata += "<Key>\n";
    	        		sServiceMetadata += "<PropertyRef Name='" + StringEscapeUtils.escapeXml(field.getString("name")) + "' />\n";
    	        		sServiceMetadata += "</Key>\n";	    	        		
    	        	}
    	        }	            	

    			//loop over fields and output names
    	        for (int j=0;j<jsoLayerFields.length();j++) {
    	        	JSONObject field = jsoLayerFields.getJSONObject(j);
    	        	String fieldName = StringEscapeUtils.escapeXml(field.getString("name"));
    	        	String fieldType = getODataFieldType(field.getString("type"));
    	        	String fieldNullable = "true";
	        		sServiceMetadata += "<Property Name='" + fieldName + "' Type='" + fieldType + "' Nullable='" + fieldNullable + "' />\n";
    	        }
    	        
    	        // close the entity type
    			sServiceMetadata += "</EntityType>\n";
	        }
		
			sServiceMetadata += "<EntityContainer Name='" + sServiceName + "' m:IsDefaultEntityContainer='true'>\n";

	        // list the layers as entity sets 
	        for (int i=0;i<jsoLayers.length();i++) {
	        	JSONObject layer = jsoLayers.getJSONObject(i);

        		// set EntityType to layer name
            	String name = StringEscapeUtils.escapeXml(Val.chkStr(layer.getString("name")));
        		sServiceMetadata += "<EntityType Name='" + name + "' EntityType='self." + name + "' />\n";
	        }
	        
			sServiceMetadata += "</EntityContainer>\n";
			sServiceMetadata += "</Schema>\n";
			sServiceMetadata += "</edmx:DataServices>\n";
			sServiceMetadata += "</edmx:Edmx>";
	
			return sServiceMetadata;
			
	    } catch (JSONException e) {
			LOGGER.log(Level.SEVERE, "ERROR: " + e.getMessage());
			return e.getMessage();
	    }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Submit HTTP Request (Both GET and POST). Return InputStream object from the response
	 * 
	 * Submit an HTTP request.
	 * @return Response in plain text.
	 * 
	 * @param method    HTTP Method. for example "POST", "GET"
	 * @param urlString    URL to send HTTP Request to
	 * @param postdata    Data to be posted
	 * @param usr    Username
	 * @param pwd    Password
	 * @throws IOException in IOException
	 * @throws  java.net.SocketTimeoutException if connect or read timeout
	 */
	public InputStream submitHttpRequest(String method, String urlString,
	  String postdata, String usr, String pwd) throws IOException {

		
	  urlString = Val.chkStr(urlString);
	  urlString = urlString.replaceAll("\\n", "");
	  urlString = urlString.replaceAll("\\t", "");
	  urlString = urlString.replaceAll("\\r", "");
	  urlString = urlString.replaceAll(" ", "");

	  HttpClientRequest client = HttpClientRequest.newRequest();
	  client.setBatchHttpClient(client.getBatchHttpClient());
	  client.setUrl(urlString);
	  client.setRetries(1);
	  
	  usr = Val.chkStr(usr);
	  pwd = Val.chkStr(pwd);
	  if (usr.length() > 0 || pwd.length() > 0) {
	    CredentialProvider provider = new CredentialProvider(usr, pwd);
	    client.getCredentialProvider();
	    client.setCredentialProvider(provider);
	  }

	  // Send a request
	  if (Val.chkStr(method).equalsIgnoreCase("post")) {
	    ContentProvider contentProvider = new StringProvider(postdata, "text/xml");
	    client.setContentProvider(contentProvider);
	    
	  } else {
	    client.setMethodName(MethodName.GET);
	  }

	  String response = client.readResponseAsCharacters();

	  return new ByteArrayInputStream(response.getBytes("UTF-8"));
	  
	}
	

	/**
	 * Fully reads the characters from an input stream.
	 * @param stream the input stream
	 * @param charset the encoding of the input stream
	 * @return the characters read
	 * @throws IOException if an exception occurs
	 */
	private String readCharacters(InputStream stream, String charset)
	  throws IOException {
	  StringBuilder sb = new StringBuilder();
	  BufferedReader br = null;
	  InputStreamReader ir = null;
	  try {
	    if ((charset == null) || (charset.trim().length() == 0)) charset = "UTF-8";
	    char cbuf[] = new char[2048];
	    int n = 0;
	    int nLen = cbuf.length;
	    ir = new InputStreamReader(stream,charset);
	    br = new BufferedReader(ir);
	    while ((n = br.read(cbuf,0,nLen)) > 0) sb.append(cbuf,0,n);
	  } finally {
	    try {if (br != null) br.close();} catch (Exception ef) {}
	    try {if (ir != null) ir.close();} catch (Exception ef) {}
	  }
	  return sb.toString();
	}

	/*
	 * Translate Esri field type to OData field type
	 */
	private String getODataFieldType(String esriFieldType) {
		String sODataFieldType = "";
		
		/*
Edm.Boolean
Edm.Byte
Edm.Date
Edm.DateTimeOffset
Edm.Duration
Edm.Int16
Edm.Int64
		 */
		
		switch (esriFieldType) {
		case "esriFieldTypeOID":
			sODataFieldType = "Edm.Guid";
			break;
		case "esriFieldTypeGeometry":
			sODataFieldType = "Edm.SByte";
			break;
		case "esriFieldTypeString":
			sODataFieldType = "Edm.String";
			break;
		case "esriFieldTypeInteger":
			sODataFieldType = "Edm.Int32";
			break;
		case "esriFieldTypeDouble":
			sODataFieldType = "Edm.Decimal";
			break;
		default:
			sODataFieldType = "Edm.String";
			break;
		}
		
		return sODataFieldType;
	}

}
