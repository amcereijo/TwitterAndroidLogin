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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class TwitterAndroidAsyncTask extends
		AsyncTask<Integer, Void, Void> {
	
	private final String TAG = TwitterAndroidAsyncTask.class.toString();
	
	public static final int GET_ACCESS = 1;
	public static final int GRANT_ACCESS = 2;

	private static final String ACCESS_KEY_KEY = "access_key";
	private static final String ACCESS_SECRET_KEY = "access_secret";

	private static String ACCESS_KEY = "";
	private static String ACCESS_SECRET = "";	
	
	private static final String URL_GET_USER_SETTINGS = "https://api.twitter.com/1.1/account/settings.json";
	private static final String URL_GET_USER_INFO = "https://api.twitter.com/1.1/users/show.json?screen_name=%s";
	private static final String URL_GET_USER_IMAGE_PROFILE = "http://api.twitter.com/1/users/profile_image/?screen_name=%s&size=original";
	
	private static final String CALLBACK_URL = "x-goodout-oauth-twitter://callback";
	
	private CommonsHttpOAuthConsumer consumer;
	private CommonsHttpOAuthProvider provider;
	
	private SharedPreferences privatePreferences;
	private ProgressDialog progress ;
	private Activity c;
	private String twitterName;
	private String userName;
	private Drawable userPictureProfile;
	private boolean dataLoaded = false;
	
	public  TwitterAndroidAsyncTask(Activity c, CommonsHttpOAuthConsumer consumer,
			CommonsHttpOAuthProvider provider){
		this.c = c; 
		this.consumer = consumer;
		this.provider = provider;
		privatePreferences = c.getPreferences(Context.MODE_PRIVATE);
		loadTokenAccessAndSecret();
	}
	
	@Override
	protected Void doInBackground(Integer... action) {
		try {
			switch(action[0]){
				case GRANT_ACCESS:
					grantAccess();
					break;
				case GET_ACCESS:
				getAccess();
					break;
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
		return null;
	}

	private void getAccess() throws OAuthMessageSignerException,
			OAuthNotAuthorizedException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		String authURL;
		if(!isLoadTokenAccessAndSecret()){
			authURL = provider.retrieveRequestToken(consumer, CALLBACK_URL);
			Intent webIntent = new Intent(c, TwitterWebView.class);
			webIntent.putExtra(TwitterWebView.KEY_URL_TO_LOAD, authURL);
			c.startActivityForResult(webIntent,TwitterWebView.WEBVIEW_RETURN_CODE);
			
		}else{
			getAndShowUserInfo();
		}
	}
	
	
	private void grantAccess(){
		Uri uri = c.getIntent().getData();
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

	private boolean isLoadTokenAccessAndSecret(){
    	return (ACCESS_KEY != null && ACCESS_SECRET != null);
    }
	
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		if(dataLoaded){
			showLoginResult();
		}
		progress.cancel();
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progress = ProgressDialog.show(c, "Title", "Conecting twitter..");
	}

	private void loadTokenAccessAndSecret(){
    	ACCESS_KEY = privatePreferences.getString(ACCESS_KEY_KEY, null);
    	ACCESS_SECRET = privatePreferences.getString(ACCESS_SECRET_KEY, null);
    }
	
	private void saveTokenAccessAndSecret(){
    	SharedPreferences.Editor editor = privatePreferences.edit();
    	editor.putString(ACCESS_KEY_KEY, ACCESS_KEY);
    	editor.putString(ACCESS_SECRET_KEY, ACCESS_SECRET);
    	editor.commit();
    }
	
	private void getAndShowUserInfo() {
		getUserSettings();
		getUserInfo();
		getUserProfileImage();
		dataLoaded = true;
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
    
    private void showLoginResult(){
    	((ImageButton)c.findViewById(R.id.twitter_icon)).setVisibility(View.GONE);
    	((TextView)c.findViewById(R.id.twitter_login_text)).setVisibility(View.GONE);
    	TextView loginResult = (TextView)c.findViewById(R.id.login_result);
    	loginResult.setText( c.getText(R.string.loged_text) + " " + twitterName);
    	loginResult.setVisibility(View.VISIBLE);
    	ImageView pictureProfile = (ImageView)c.findViewById(R.id.login_picture_profile);
    	pictureProfile.setImageDrawable(userPictureProfile);
    	pictureProfile.setVisibility(View.VISIBLE);
    	TextView username = (TextView)c.findViewById(R.id.login_result_username);
    	username.setText(c.getText(R.string.login_result_username_text) + " " + userName);
    	username.setVisibility(View.VISIBLE);
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
