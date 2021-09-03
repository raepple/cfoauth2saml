package com.microsoft.samples;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.microsoft.samples.ConnectionAttributes.ProxyType;

import org.apache.commons.io.IOUtils;

public class HttpUtils {

	private HttpUtils() {
		// Can not be created from outside the class
	}
	
	public static HttpURLConnection openUrlConnection(URL url) throws IOException {
		return openUrlConnection(url, ProxyType.INTERNET);
	}

	public static HttpURLConnection openUrlConnection(URL url, ProxyType proxyType) throws IOException {
		HttpURLConnection connection;
		
		switch (proxyType) {
		case INTERNET:
			connection = (HttpURLConnection) url.openConnection();
			break;
		default:
			throw new IllegalArgumentException("Unsupported Proxy Type: " + proxyType);
		}
		
		connection.setConnectTimeout(Constants.CONNECT_TIMEOUT);
		connection.setReadTimeout(Constants.READ_TIMEOUT);
		return connection;
	}

	public static HttpResponse getResponse(HttpURLConnection urlConnection) throws IOException {
		return getResponse(urlConnection, null);
	}

	public static HttpResponse getResponse(HttpURLConnection urlConnection, InputStream postData) throws IOException {
		urlConnection.connect();
		
		if(postData != null) {
			IOUtils.copy(postData, urlConnection.getOutputStream());
		}
		
		int backendResponseCode = urlConnection.getResponseCode();
		
		InputStream backendInStream = null;

		if (backendResponseCode < HTTP_BAD_REQUEST) {
			backendInStream = urlConnection.getInputStream();
		} else if (urlConnection.getErrorStream() != null) {
			backendInStream = urlConnection.getErrorStream();
		}
		
		if(backendInStream == null) {
			backendInStream = IOUtils.toInputStream("Empty input stream from backend!", "UTF-8");;
		}
		
		return new HttpResponse(backendResponseCode, backendInStream, urlConnection.getHeaderFields());
	}
}