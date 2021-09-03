package com.microsoft.samples;

import java.text.MessageFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Methods for extracting configurations from the environment variables
 */
public final class EnvironmentVariableAccessor {

	private static final String VCAP_SERVICES = System.getenv("VCAP_SERVICES");
	private static final String VCAP_SERVICES_CREDENTIALS = "credentials";
	private static final String VCAP_SERVICES_NAME = "name";

	private EnvironmentVariableAccessor() {
		// Can not be created from outside the class
	}

	/**
	 * Returns service credentials for a given service from VCAP_SERVICES
	 * 
	 * @see <a href=
	 *      "https://docs.run.pivotal.io/devguide/deploy-apps/environment-variable.html#VCAP-SERVICES">VCAP_SERVICES</a>
	 */
	public static JSONObject getServiceCredentials(String serviceName) throws JSONException {
		JSONObject jsonObj = new JSONObject(VCAP_SERVICES);
		JSONArray jsonArr = jsonObj.getJSONArray(serviceName);
		return jsonArr.getJSONObject(0).getJSONObject(VCAP_SERVICES_CREDENTIALS);
	}

	/**
	 * Returns service credentials for a given service instance from VCAP_SERVICES
	 * 
	 * @see <a href=
	 *      "https://docs.run.pivotal.io/devguide/deploy-apps/environment-variable.html#VCAP-SERVICES">VCAP_SERVICES</a>
	 */
	public static JSONObject getServiceCredentials(String serviceName, String serviceInstanceName)
			throws JSONException {
		JSONObject jsonObj = new JSONObject(VCAP_SERVICES);
		JSONArray jsonarr = jsonObj.getJSONArray(serviceName);
		for (int i = 0; i < jsonarr.length(); i++) {
			JSONObject serviceInstanceObject = jsonarr.getJSONObject(i);
			String instanceName = serviceInstanceObject.getString(VCAP_SERVICES_NAME);
			if (instanceName.equals(serviceInstanceName)) {
				return serviceInstanceObject.getJSONObject(VCAP_SERVICES_CREDENTIALS);
			}
		}
		throw new RuntimeException(MessageFormat.format("Service instance {0} of service {1} not bound to application",
				serviceInstanceName, serviceName));
	}

	/**
	 * Returns service credentials attribute for a given service from VCAP_SERVICES
	 * 
	 * @see <a href=
	 *      "https://docs.run.pivotal.io/devguide/deploy-apps/environment-variable.html#VCAP-SERVICES">VCAP_SERVICES</a>
	 */
	public static String getServiceCredentialsAttribute(String serviceName, String attributeName) throws JSONException {
		JSONObject jsonCredentials = getServiceCredentials(serviceName);
		return jsonCredentials.getString(attributeName);
	}

	/**
	 * Returns service credentials attribute for a given service from VCAP_SERVICES
	 * 
	 * @see <a href=
	 *      "https://docs.run.pivotal.io/devguide/deploy-apps/environment-variable.html#VCAP-SERVICES">VCAP_SERVICES</a>
	 */
	public static String getServiceCredentialsAttribute(String serviceName, String serviceInstanceName,
			String attributeName) throws JSONException {
		JSONObject jsonCredentials = getServiceCredentials(serviceName, serviceInstanceName);
		return jsonCredentials.getString(attributeName);
	}
}
