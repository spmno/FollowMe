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
		// 获取Location manager
		mLocationManager = (LocationManager) mContext
				.getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 * 开始定位
	 * 
	 * @param timeout
	 *            超时设置
	 * @return LocationData位置数据，如果超时则为null
	 */
	public LocationData getLocation(long timeout) {
		mLocation = null;
		mLBSThread = new LBSThread();
		mLBSThread.start();// 启动LBSThread
		timeout = timeout > 0 ? timeout : 0;

		synchronized (mLBSThread) {
			try {
				Log.i(Thread.currentThread().getName(),
						"Waiting for LocationThread to complete...");
				mLBSThread.wait(timeout);// 主线程进入等待，等待时长timeout ms
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
			Looper.prepare();// 给LBSThread加上Looper
			mLooper = Looper.myLooper();
			registerLocationListener();
			Looper.loop();
			Log.e(Thread.currentThread().getName(), "--end--");

		}
	}

	private class MyLocationListner implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			// 当LocationManager检测到最小位置变化时，就会回调到这里
			Log.i(Thread.currentThread().getName(),
					"Got New Location of provider:" + location.getProvider());
			unRegisterLocationListener();// 停止LocationManager的工作
			try {
				synchronized (mLBSThread) {
					parseLatLon(location.getLatitude() + "",
							location.getLongitude() + "");// 解析地理位置
					mLooper.quit();// 解除LBSThread的Looper，LBSThread结束
					mLBSThread.notify();// 通知主线程继续
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 后3个方法此处不做处理
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

			// 五个参数分别为位置服务的提供者，最短通知时间间隔，最小位置变化，listener，listener所在消息队列的looper
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
	 * 判断GPS是否开启
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
	 * 判断Network是否开启(包括移动网络和wifi)
	 * 
	 * @return
	 */
	public boolean isNetworkEnabled() {
		return (isWIFIEnabled() || isTelephonyEnabled());
	}

	/**
	 * 判断移动网络是否开启
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
	 * 判断wifi是否开启
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
	 * 使用经纬度从goole服务器获取对应地址
	 * 
	 * @param 经纬度
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
	 * 注销监听器
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
