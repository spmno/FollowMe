package com.datacool.LocationManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

public class LBSTool {
	private Context mContext;
	private LocationManager mLocationManager;
	private LocationData mLocation;
	private LBSThread mLBSThread;
	private MyLocationListner mNetworkListner;
	private MyLocationListner mGPSListener;
	private Looper mLooper;

	public LBSTool(Context context) {
		mContext = context;
		// ��ȡLocation manager
		mLocationManager = (LocationManager) mContext
				.getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 * ��ʼ��λ
	 * 
	 * @param timeout
	 *            ��ʱ����
	 * @return LocationDataλ�����ݣ������ʱ��Ϊnull
	 */
	public LocationData getLocation(long timeout) {
		mLocation = null;
		mLBSThread = new LBSThread();
		mLBSThread.start();// ����LBSThread
		timeout = timeout > 0 ? timeout : 0;

		synchronized (mLBSThread) {
			try {
				Log.i(Thread.currentThread().getName(),
						"Waiting for LocationThread to complete...");
				mLBSThread.wait(timeout);// ���߳̽���ȴ����ȴ�ʱ��timeout ms
				Log.i(Thread.currentThread().getName(),
						"Completed.Now back to main thread");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mLBSThread = null;
		return mLocation;
	}

	private class LBSThread extends Thread {
		@Override
		public void run() {
			setName("location thread");
			Log.i(Thread.currentThread().getName(), "--start--");
			Looper.prepare();// ��LBSThread����Looper
			mLooper = Looper.myLooper();
			registerLocationListener();
			Looper.loop();
			Log.e(Thread.currentThread().getName(), "--end--");

		}
	}

	private class MyLocationListner implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			// ��LocationManager��⵽��Сλ�ñ仯ʱ���ͻ�ص�������
			Log.i(Thread.currentThread().getName(),
					"Got New Location of provider:" + location.getProvider());
			unRegisterLocationListener();// ֹͣLocationManager�Ĺ���
			try {
				synchronized (mLBSThread) {
					parseLatLon(location.getLatitude() + "",
							location.getLongitude() + "");// ��������λ��
					mLooper.quit();// ���LBSThread��Looper��LBSThread����
					mLBSThread.notify();// ֪ͨ���̼߳���
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// ��3�������˴���������
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	};

	private void registerLocationListener() {
		Log.i(Thread.currentThread().getName(), "registerLocationListener");
		if (isGPSEnabled()) {
			mGPSListener = new MyLocationListner();

			// ��������ֱ�Ϊλ�÷�����ṩ�ߣ����֪ͨʱ��������Сλ�ñ仯��listener��listener������Ϣ���е�looper
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 5000, 0, mGPSListener,
					mLooper);
		}
		if (isNetworkEnabled()) {
			mNetworkListner = new MyLocationListner();

			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 3000, 0, mNetworkListner,
					mLooper);
		}
	}

	/**
	 * �ж�GPS�Ƿ���
	 * 
	 * @return
	 */
	public boolean isGPSEnabled() {
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Log.i(Thread.currentThread().getName(), "isGPSEnabled");
			return true;
		} else {
			return false;
		}
	}

	/**
	 * �ж�Network�Ƿ���(�����ƶ������wifi)
	 * 
	 * @return
	 */
	public boolean isNetworkEnabled() {
		return (isWIFIEnabled() || isTelephonyEnabled());
	}

	/**
	 * �ж��ƶ������Ƿ���
	 * 
	 * @return
	 */
	public boolean isTelephonyEnabled() {
		boolean enable = false;
		TelephonyManager telephonyManager = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (telephonyManager != null) {
			if (telephonyManager.getNetworkType() != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
				enable = true;
				Log.i(Thread.currentThread().getName(), "isTelephonyEnabled");
			}
		}

		return enable;
	}

	/**
	 * �ж�wifi�Ƿ���
	 */
	public boolean isWIFIEnabled() {
		boolean enable = false;
		WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			enable = true;
			Log.i(Thread.currentThread().getName(), "isWIFIEnabled");
		}
		return enable;
	}

	/**
	 * ʹ�þ�γ�ȴ�goole��������ȡ��Ӧ��ַ
	 * 
	 * @param ��γ��
	 */
	private void parseLatLon(String lat, String lon) throws Exception {
		Log.e(Thread.currentThread().getName(), "---parseLatLon---");
		Log.e(Thread.currentThread().getName(), "---" + lat + "---");
		Log.e(Thread.currentThread().getName(), "---" + lon + "---");
		try {
			HttpClient httpClient = new DefaultHttpClient();
			String url = "http://maps.google.com/maps/api/geocode/json?latlng="
					+ lat + "," + lon + "&language=zh-CN&sensor=false";
			Log.e(Thread.currentThread().getName(), "---" + url + "---");
			HttpGet get = new HttpGet(url);
			HttpResponse response = httpClient.execute(get);
			String resultString = EntityUtils.toString(response.getEntity());
			JSONObject jsonresult = new JSONObject(resultString);
			if (jsonresult.optJSONArray("results") != null) {
				mLocation = new LocationData();
				mLocation.latitude = lat;
				mLocation.longitude = lon;
				mLocation.address = jsonresult.optJSONArray("results")
						.optJSONObject(0).optString("formatted_address");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ע��������
	 */
	private void unRegisterLocationListener() {
		if (mGPSListener != null) {
			mLocationManager.removeUpdates(mGPSListener);
			mGPSListener = null;
		}
		if (mNetworkListner != null) {
			mLocationManager.removeUpdates(mNetworkListner);
			mNetworkListner = null;
		}
	}

}
