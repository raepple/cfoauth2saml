package com.microsoft.samples;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.identity.client.UaaContext;
import org.cloudfoundry.identity.client.UaaContextFactory;
import org.cloudfoundry.identity.client.token.GrantType;
import org.cloudfoundry.identity.client.token.TokenRequest;
import org.cloudfoundry.identity.uaa.oauth.token.CompositeAccessToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.IOUtils;

@ServletSecurity(@HttpConstraint(rolesAllowed = {"Display"}))
@WebServlet("/hello")
public class HelloWorldServlet extends HttpServlet
{
    private static final String DESTINATION_SERVICE_NAME = "destination";
    private static final String DESTINATION_SERVICE_PATH = "/destination-configuration/v1/destinations/%s";
    private static final String DESTINATION_SERVICE_PROP_URI = "uri";
    private static final String[] ALLOWED_HEADERS_IN  = new String[]{"accept", "accept-language", "dataserviceversion", "maxdataserviceversion", "content-length", "content-type", "x-csrf-token", "cookie"};
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(HelloWorldServlet.class);

    @Override
    protected void service( final HttpServletRequest request, final HttpServletResponse response )
        throws IOException
    {
        logger.info("I am running!");
        response.getWriter().println("Hello World!");

        HttpURLConnection client = null;
        
        try {
            // get token for destination service
            CompositeAccessToken destServToken = getAccessToken();
            logger.info("Successfully retrieved access token for destination service!");

            // get destination 
            ConnectionAttributes attributes = getDestination(destServToken.getValue(), request.getHeader(Constants.HEADER_AUTORIZATION), "AAD");
            logger.info("Successfully retrieved destination!");
            
            URL url = new URL(attributes.getUrl());
			client = HttpUtils.openUrlConnection(url, attributes.getProxyType());
			client.setRequestMethod(request.getMethod());

            String authHeader = Constants.BEARER_WITH_TRAILING_SPACE
						+ attributes.getAuthenticationToken();
			logger.info(Constants.HEADER_AUTORIZATION + ": " + authHeader);
            client.setRequestProperty(Constants.HEADER_AUTORIZATION, authHeader);
            
			for(String header : ALLOWED_HEADERS_IN) {
				Enumeration<String> headerValues = request.getHeaders(header);
				while (headerValues.hasMoreElements()) {
					client.addRequestProperty(header, headerValues.nextElement());
				}
			}

            InputStream postData = null;
			if(request.getIntHeader("content-length") > 0) {
				client.setDoOutput(true);
				postData = request.getInputStream();
			}

            HttpResponse destResponse = HttpUtils.getResponse(client, postData);
            logger.info("Response from Destination: " + destResponse.getResponseCode());

            destResponse.getHeader().entrySet().forEach( e -> {
                e.getValue().forEach( v -> {
                    response.addHeader(e.getKey(), v);
                });
            });
                       
            OutputStream clientOutStream = response.getOutputStream();
            IOUtils.copy(destResponse.getResponseStream(), clientOutStream);
        } catch (Exception ex)
        {
            logger.error("Cannot retrieve access token for destination service: " + ex.getMessage());
            response.getOutputStream().println(ex.getMessage());
        }      

        response.flushBuffer();
    }

    // Get JWT token for the destination service from XSUAA
    private CompositeAccessToken getAccessToken() throws Exception {
        JSONObject destinationCredentials = EnvironmentVariableAccessor.getServiceCredentials("destination");
        String clientId = destinationCredentials.getString("clientid");
        String clientSecret = destinationCredentials.getString("clientsecret");

        // Make request to UAA to retrieve JWT token
        JSONObject xsuaaCredentials = EnvironmentVariableAccessor.getServiceCredentials("xsuaa");
        URI xsUaaUri = new URI(xsuaaCredentials.getString("url"));

        UaaContextFactory factory = UaaContextFactory.factory(xsUaaUri).authorizePath("/oauth/authorize")
                .tokenPath("/oauth/token");

        TokenRequest tokenRequest = factory.tokenRequest();
        tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setClientId(clientId);
        tokenRequest.setClientSecret(clientSecret);

        UaaContext xsUaaContext = factory.authenticate(tokenRequest);
        return xsUaaContext.getToken();
    }

	private ConnectionAttributes getDestination(String accessToken, String userToken, String destinationName) throws JSONException, MalformedURLException {
		String destinationServiceUri = EnvironmentVariableAccessor.getServiceCredentialsAttribute(DESTINATION_SERVICE_NAME, DESTINATION_SERVICE_PROP_URI);
		logger.info("Will get destination [" + destinationName + "], uri [" + destinationServiceUri + "] with access token [" + accessToken + "]");

		// to call a custom Destination Service, append its URL to the destination name separated by space
		String[] split = destinationName.split("\\s+");
		if (split.length > 1) {
			destinationName = split[0];
			destinationServiceUri = split[1];
		}
		
		String destinationPath = String.format(DESTINATION_SERVICE_PATH, destinationName);
		URL destinationUrl = new URL(destinationServiceUri + destinationPath);
		
		HttpURLConnection httpClient = null;
		try {
			httpClient = HttpUtils.openUrlConnection(destinationUrl);
			httpClient.setRequestProperty(Constants.HEADER_AUTORIZATION, Constants.BEARER_WITH_TRAILING_SPACE + accessToken);
            httpClient.setRequestProperty(Constants.HEADER_X_USER_TOKEN, userToken.split(" ")[1]);

			HttpResponse response = HttpUtils.getResponse(httpClient);

			if (response.getResponseCode() == HTTP_NOT_FOUND) {
				throw new Exception("Destination '" + destinationName + "' could not be found");
			}
			
			return ConnectionAttributes.fromDestination(response.getResponseStream());
		} catch (Exception e) { 
			String msg = "EXCEPTION: " + e.getMessage() + ", destinationName [" + destinationName + "], destination URL [" + destinationUrl + "]"; 
			logger.error(msg, e);
			throw new IllegalStateException(e);
		} finally {
			if(httpClient != null) {
				httpClient.disconnect();
			}
		}
	}

}
