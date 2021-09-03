package com.microsoft.samples;

public final class Constants {

	public static final String HEADER_AUTORIZATION = "Authorization";
	public static final String HEADER_X_USER_TOKEN = "X-user-token";

	public static final int CONNECT_TIMEOUT = 10000;
	public static final int READ_TIMEOUT = 60000;
	
	public static final String BEARER_WITH_TRAILING_SPACE = "Bearer ";
	public static final String BASIC_WITH_TRAILING_SPACE = "Basic ";

	public static final String HTTP = "http://";

	public static final String PARAM_HTTP_CLIENT = "HttpClient"; // A form element, used when reading from the HTML form

	private Constants() {
		// Can not be created from outside the class
	}
}
