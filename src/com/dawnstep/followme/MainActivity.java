package com.dawnstep.followme;

import java.io.IOException;

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
		 String httpUrl = "http://127.0.0.1:8000/location/?name="+"spmno"+"&longitude="+longitude.toString()+"&latitude="+latitude.toString();   
         //创建httpRequest对象
         HttpGet httpRequest = new HttpGet(httpUrl);
         
         try {
        	 HttpClient httpClient = new DefaultHttpClient();
        	 HttpResponse httpResponse = httpClient.execute(httpRequest);
        	 if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        		 String strResult = EntityUtils.toString(httpResponse.getEntity());
        		 resultText.setText(strResult);
        	 } else {
        		 resultText.setText("request error");
        	 }
         } catch (ClientProtocolException e) {
        	 resultText.setText(e.getMessage().toString());
         } catch (IOException e) {
        	 resultText.setText(e.getMessage().toString());
         }

	}

}
