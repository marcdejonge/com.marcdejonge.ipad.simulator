package com.marcdejonge.ipad.simulator;

import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TrustAllX509TrustManager implements X509TrustManager {
	public static final void trustAll() {
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[] { new TrustAllX509TrustManager() }, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier((string, ssls) -> true);
		} catch (Exception ex) {
			System.err.println("Could not set the trust all stuff");
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

	@Override
	public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
	                               String authType) {
	}

	@Override
	public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
	                               String authType) {
	}

}