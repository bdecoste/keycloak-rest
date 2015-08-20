package org.keycloak.openshift;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.installed.KeycloakInstalled;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class KeyCloakCli {

    public static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    private static BufferedReader br;

    private static KeycloakInstalled keycloak;

    public static void main(String[] args) throws Exception {
    	AccessTokenResponse tokenResponse = getToken();
    	
    	realms(tokenResponse);
    	
    }
    
    public static AccessTokenResponse getToken() throws IOException {

        HttpClient client = new DefaultHttpClient();

        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri("http://localhost:8080/auth")
                    .path(ServiceUrlConstants.TOKEN_PATH).build("demo"));
            List <NameValuePair> formparams = new ArrayList <NameValuePair>();
            formparams.add(new BasicNameValuePair("username", "admin"));
            formparams.add(new BasicNameValuePair("password", "cyclone1"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "admin-client"));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                String json = getContent(entity);
                throw new IOException("Bad status: " + status + " response: " + json);
            }
            if (entity == null) {
                throw new IOException("No Entity");
            }
            String json = getContent(entity);
            return JsonSerialization.readValue(json, AccessTokenResponse.class);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
    
    public static String getContent(HttpEntity entity) throws IOException {
        if (entity == null) return null;
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            String data = new String(bytes);
            return data;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }

    }

    public static void realms(AccessTokenResponse tokenResponse) throws Exception {
    	
    	HttpClient client = new DefaultHttpClient();
    	
    	try {
    	
	    	String baseUrl = "http://localhost:8080";
	
	        String realmsUrl = baseUrl + "/auth/admin/realms";
	        
	        System.out.println(realmsUrl);
	        HttpGet get = new HttpGet(realmsUrl);
	        get.setHeader("Accept", "application/json");
	        get.setHeader("Authorization", "Bearer " + tokenResponse.getToken());
	        
	        HttpResponse response = client.execute(get);
	
	        if (response.getStatusLine().getStatusCode() == 200) {
	            print(response.getEntity().getContent());
	        } else {
	            System.out.println(response.getStatusLine().toString());
	        }
    	} finally {
            client.getConnectionManager().shutdown();
        }
    }

    private static void print(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String l = br.readLine(); l != null; l = br.readLine()) {
            System.out.println(l);
        }
    }

}
