package com.example.twitterandroidlogin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * @author angelcereijo
 *
 */
public class TwitterAndroidLoginMainActivity extends Activity {

	

	private final String TAG = TwitterAndroidLoginMainActivity.class.toString();
	
	private static final String CONSUMER_KEY = "TWITTER_KEY";
	private static final String CONSUMER_SECRET = "TWITTER_SECRET";

	private static String ACCESS_KEY = null;
	private static String ACCESS_SECRET = null;
	
	private static final String ACCESS_KEY_KEY = "access_key";
	private static final String ACCESS_SECRET_KEY = "access_secret";

	private static final String REQUEST_URL = "https://api.twitter.com/oauth/request_token";
	private static final String ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
	private static final String AUTH_URL = "https://api.twitter.com/oauth/authorize";
	private static final String URL_GET_USER_SETTINGS = "https://api.twitter.com/1.1/account/settings.json";
	private static final String URL_GET_USER_INFO = "https://api.twitter.com/1.1/users/show.json?screen_name=%s";
	private static final String URL_GET_USER_IMAGE_PROFILE = "http://api.twitter.com/1/users/profile_image/?screen_name=%s&size=original";
	
	private static final String CALLBACK_URL = "x-goodout-oauth-twitter://callback";
	
	private String twitterName;
	private String userName;
	private String imageUrl;
	private Drawable userPictureProfile;
	
	private static CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(
			CONSUMER_KEY, CONSUMER_SECRET);
	private static CommonsHttpOAuthProvider provider = new CommonsHttpOAuthProvider(
			REQUEST_URL, ACCESS_TOKEN_URL, AUTH_URL);
	
	private SharedPreferences privatePreferences;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_android_login_main);
        privatePreferences = getPreferences(MODE_PRIVATE);
        loadTokenAccessAndSecret();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_twitter_android_login_main, menu);
        return true;
    }
    
    
    /**
     * 
     * @param v
     */
    public void twitterLogin(View v){
    	try {
    		if(!isLoadTokenAccessAndSecret()){
	        	String authURL = provider.retrieveRequestToken(consumer, CALLBACK_URL);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authURL)));
    		}else{
    			getAndShowUserInfo();
    		}
	    } catch (OAuthMessageSignerException e) {
	    	Log.e(TAG, "Error twitter login",e);
	    } catch (OAuthNotAuthorizedException e) {
	    	Log.e(TAG, "Error twitter login",e);
	    } catch (OAuthExpectationFailedException e) {
	    	Log.e(TAG, "Error twitter login",e);
	    } catch (OAuthCommunicationException e) {
	    	Log.e(TAG, "Error twitter login",e);
	    }
    }
    
    
    @Override
    protected void onResume() {
    	super.onResume();
    	getAccesToken();
    }
    
    private void showLoginResult(){
    	((ImageButton)findViewById(R.id.twitter_icon)).setVisibility(View.GONE);
    	((TextView)findViewById(R.id.twitter_login_text)).setVisibility(View.GONE);
    	TextView loginResult = (TextView)findViewById(R.id.login_result);
    	loginResult.setText( getText(R.string.loged_text) + " " + twitterName);
    	loginResult.setVisibility(View.VISIBLE);
    	ImageView pictureProfile = (ImageView)findViewById(R.id.login_picture_profile);
    	pictureProfile.setImageDrawable(userPictureProfile);
    	pictureProfile.setVisibility(View.VISIBLE);
    	TextView username = (TextView)findViewById(R.id.login_result_username);
    	username.setText( getText(R.string.login_result_username_text) + " " + userName);
    	username.setVisibility(View.VISIBLE);
    }
    
    private void loadTokenAccessAndSecret(){
    	ACCESS_KEY = privatePreferences.getString(ACCESS_KEY_KEY, null);
    	ACCESS_SECRET = privatePreferences.getString(ACCESS_SECRET_KEY, null);
    }
    
    private boolean isLoadTokenAccessAndSecret(){
    	return (ACCESS_KEY != null && ACCESS_SECRET != null);
    }
    
    private void saveTokenAccessAndSecret(){
    	SharedPreferences.Editor editor = privatePreferences.edit();
    	editor.putString(ACCESS_KEY_KEY, ACCESS_KEY);
    	editor.putString(ACCESS_SECRET_KEY, ACCESS_SECRET);
    	editor.commit();
    }
    
    private void getAccesToken(){
    	Uri uri = this.getIntent().getData();
    	if (uri != null && uri.toString().startsWith(CALLBACK_URL) && !isLoadTokenAccessAndSecret()) {
    		String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
    		try {
    			provider.retrieveAccessToken(consumer, verifier);
    			ACCESS_KEY = consumer.getToken();
    			ACCESS_SECRET = consumer.getTokenSecret();
    			saveTokenAccessAndSecret();
    			getAndShowUserInfo();
    		} catch (OAuthMessageSignerException e) {
    			e.printStackTrace();
    		} catch (OAuthNotAuthorizedException e) {
    			e.printStackTrace();
    		} catch (OAuthExpectationFailedException e) {
    			e.printStackTrace();
    		} catch (OAuthCommunicationException e) {
    			e.printStackTrace();
    		}
    	}
    }

	private void getAndShowUserInfo() {
		getUserSettings();
		getUserInfo();
		getUserProfileImage();
		showLoginResult();
	}
    
    
    private void getUserSettings(){
    	try {
    		consumer.setTokenWithSecret(ACCESS_KEY, ACCESS_SECRET);
    		String a = consumer.sign(URL_GET_USER_SETTINGS);
    		HttpGet get = new HttpGet(a);
    		HttpClient c = new DefaultHttpClient();
    		HttpResponse resp = c.execute(get);
    		StatusLine statusLine = resp.getStatusLine();
    	      int statusCode = statusLine.getStatusCode();
    	      if (statusCode == 200) {
    	        JSONObject json = readJSONResponse(resp);
    	        twitterName = (String)json.get("screen_name");
    	      } else {
    	        Log.e(TAG, "Failed Get Twitter info");
    	      }
		} catch (OAuthMessageSignerException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (OAuthCommunicationException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (IOException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (JSONException e) {
			Log.e(TAG, "Get Twitter info",e);
		}
    }
    
    
    private void getUserInfo(){
    	try{
    		consumer.setTokenWithSecret(ACCESS_KEY, ACCESS_SECRET);
	    	String signedUrl = consumer.sign(String.format(URL_GET_USER_INFO,twitterName));
			HttpGet get = new HttpGet(signedUrl);
			HttpClient c = new DefaultHttpClient();
			HttpResponse resp = c.execute(get);
			int statusCode = resp.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				JSONObject json = readJSONResponse(resp);
				imageUrl = json.getString("profile_image_url");
				userName = json.getString("name");
			} else {
				Log.e(TAG, "Failed Get Twitter info");
			}
    	} catch (OAuthMessageSignerException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (OAuthCommunicationException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (IOException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (JSONException e) {
			Log.e(TAG, "Get Twitter info",e);
		}
    }
    
    private void getUserProfileImage(){
    	try{
    		consumer.setTokenWithSecret(ACCESS_KEY, ACCESS_SECRET);
	    	String signedUrl = consumer.sign(String.format(URL_GET_USER_IMAGE_PROFILE, twitterName));
			HttpGet get = new HttpGet(signedUrl);
			HttpClient c = new DefaultHttpClient();
			HttpResponse resp = c.execute(get);
			int statusCode = resp.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				InputStream in = resp.getEntity().getContent();
				userPictureProfile = Drawable.createFromStream(in, "profile_image_url");
			} else {
				Log.e(TAG, "Failed Get Twitter info");
			}
    	} catch (OAuthMessageSignerException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (OAuthCommunicationException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "Get Twitter info",e);
		} catch (IOException e) {
			Log.e(TAG, "Get Twitter info",e);
		}
    }
    
    private JSONObject readJSONResponse(HttpResponse resp) throws IllegalStateException, IOException, JSONException{
    	StringBuilder builder = new StringBuilder();
    	HttpEntity entity = resp.getEntity();
        InputStream content = entity.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        String line;
        while ((line = reader.readLine()) != null) {
          builder.append(line);
        }
        Log.i(TAG,builder.toString());
        JSONObject json  = new JSONObject(builder.toString());
        return json;
    }
}
