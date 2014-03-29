package com.dawnstep.followme;

import java.io.IOException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class MainActivity extends Activity implements OnClickListener {

	private Button sendLocationButton;
	private TextView resultText;
	private Double longitude = 123.4;
	private Double latitude = 23.44; 
	private HttpTask httpTask;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		sendLocationButton = (Button)findViewById(R.id.sendButton);
		sendLocationButton.setOnClickListener(this);
		resultText = (TextView)findViewById(R.id.textView1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onClick(View view) {
		httpTask = new HttpTask();
		httpTask.execute("hello");
	}
	
	private class HttpTask extends AsyncTask<String, Integer, String> {
		
		@Override
		protected void onPostExecute(String result) {
			resultText.setText(result);
		}

		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			 String httpUrl = "http://192.168.2.151:8000/location/?name="+"spmno"+"&longitude="+longitude.toString()+"&latitude="+latitude.toString();   
	         //创建httpRequest对象
	         HttpGet httpRequest = new HttpGet(httpUrl);
	         String result;
	         try {
	        	 HttpClient httpClient = new DefaultHttpClient();
	        	 HttpResponse httpResponse = httpClient.execute(httpRequest);
	        	 if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
	        		 result = EntityUtils.toString(httpResponse.getEntity());
	        		 
	        	 } else {
	        		 result = "request error";
	        	 }
	         } catch (ClientProtocolException e) {
	        	 result = e.getMessage().toString();
	         } catch (IOException e) {
	        	 result = e.getMessage().toString();
	         }
			return result;
		}
		
	}

}
