package com.ksy.media.player.log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;
import com.ksy.media.player.db.DBManager;
import com.ksy.media.player.db.RecordResult;
import com.ksy.media.player.exception.Ks3ClientException;
import com.ksy.media.player.util.Constants;
import com.ksy.media.player.util.GzipUtil;
import com.ksy.media.player.util.NetworkUtil;
import com.loopj.android.http.SyncHttpClient;

public class LogClient {
	private static final int LOG_ONCE_LIMIT = 120;
	private static final long TIMER_INTERVAL = 60 * 60 * 1000;
	private static LogClient mInstance;
	private static Object mLockObject = new Object();
//	private static SyncHttpClient syncClient;
	private static Context mContext;
	private volatile boolean mStarted = false;
	private Timer timer;
	private boolean isNeedloop;
	private int sendCount; 

	public static LogGetData logGetData;
	private LogRecord logRecord = LogRecord.getInstance();
	private static String pack = null;
	
	private LogClient() {
	};

	private LogClient(Context context) {
	}

	public static LogClient getInstance() {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mInstance = new LogClient();
//					syncClient = new SyncHttpClient();
					logGetData = LogGetData.getInstance();
				}
			}
		}
		return mInstance;
	}
	
	
	public static LogClient getInstance(Context context) {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mContext = context;
					mInstance = new LogClient(context);
					logGetData = LogGetData.getInstance(context);
					pack = getPackName(context);
				}
			}
		}
		return mInstance;
	}
	
	//get packname
	public static String getPackName(Context context) {
		PackageInfo info;
		String packageName = null;
		
	    try {    
	        info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);    
	 
	        packageName = info.packageName;
	        
	    } catch (NameNotFoundException e) {    
	        e.printStackTrace();    
	    }
	    
		return packageName;    
	}

	
	//add data  TODO
	protected void addData() { 
		logRecord.setUuid(logGetData.getUuid());
		
		logRecord.setCpu(logGetData.getCpuInfo());
		logRecord.setCore(logGetData.getCoreVersion());
		logRecord.setCpuUsage(logGetData.getCpuUsage(pack));
		logRecord.setMemory(logGetData.getMemory());
		logRecord.setMemoryUsage(logGetData.getMemoryUsage());
		logRecord.setDate(logGetData.currentTimeGmt());
		logRecord.setDevice(logGetData.getImei());
		logRecord.setGmt(logGetData.getGmt());
		logRecord.setNet(logGetData.getNetState());
		logRecord.setDeviceIp(logGetData.getDeviceIp());
		logRecord.setSystem("Android");
		logRecord.setUserAgent("Android");
//		logRecord.setServerIp(serverIp); //TODO
		
	}
	
	//TODO
	private void sendRecordJson(final RecordResult recordsResult,
			final int sendCount, final int allCount, final boolean isNeedloop) {
		ByteArrayEntity byteArrayEntity = null;
		String jsonString = makeJsonLog(recordsResult.contentBuffer.toString());
		try {
			byteArrayEntity = new ByteArrayEntity(GzipUtil.compress(jsonString)
					.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			Log.d(Constants.LOG_TAG, "gzip is failed, send log ingored");
			return;
		}

		//第一步
		HttpClient httpClient = new DefaultHttpClient();
		//第二步：生成使用POST方法的请求对象
		HttpPost httpPost = new HttpPost(Constants.LOG_SERVER_URL);
//		httpPost.setHeader("Content-Type", "text/plain");
		httpPost.addHeader("accept-encoding", "gzip, deflate");
		
		try {
			//将请求体放置在请求对象当中
			httpPost.setEntity(byteArrayEntity);
			// 执行请求对象
			try {
				//第三步：执行请求对象，获取服务器发还的相应对象
				HttpResponse response = httpClient.execute(httpPost);
				//第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
				if (response.getStatusLine().getStatusCode() == 200) {
					
					//TODO
					DBManager.getInstance(mContext).deleteLogs(
							recordsResult.idBuffer.toString());
					Log.d(Constants.LOG_TAG, "log send count:" + sendCount
							+ ",next count : " + (allCount - sendCount));
					
					recordsResult.release();
					if (isNeedloop) {
						if (allCount - sendCount > 0) {
							sendRecord(allCount - sendCount);
						} else {
							Log.d(Constants.LOG_TAG, "more than 120 mode, last send all over");
						}
					} else {
						Log.d(Constants.LOG_TAG, "less than 120 mode, send all over");
					}
					
				} else {
                   //failure
				   Log.e(Constants.LOG_TAG, "response.getStatusLine().getStatusCode()=" + response.getStatusLine().getStatusCode()); 
				   
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(Constants.LOG_TAG, "HttpResponse error =" + e);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Constants.LOG_TAG, "httpPost error ===" + e);
		}
	}

	//TODO
	private String makeJsonLog(String recordsJson) {
		JSONArray array = new JSONArray();
		String[] singlgLogJson = recordsJson.split("/n");
		for (int i = 0; i < singlgLogJson.length; i++) {
			JSONObject record;
			try {
				record = new JSONObject(singlgLogJson[i]);
				array.put(record);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return array.toString();
	}

	
	public void start() {
		if (mStarted) {
			return;
		}
		mStarted = true;
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// Judge the way of send for test
				int current_count = DBManager.getInstance(mContext)
						.queryCount();
				
				Log.d(Constants.LOG_TAG, "send schedule, current thread id = "
						+ Thread.currentThread().getId() + ",log count = "
						+ current_count);
				if (NetworkUtil.isNetworkAvailable(mContext)) {
					Log.d(Constants.LOG_TAG, "network valiable");
					if (NetworkUtil.getNetWorkType(mContext) == ConnectivityManager.TYPE_WIFI) {
						Log.d(Constants.LOG_TAG, "network valiable,type wifi");
						if (current_count > 0) {
							Log.d(Constants.LOG_TAG, "send record");
							sendRecord(current_count);
						} else {
							Log.d(Constants.LOG_TAG, "no record");
						}
					} else {
						Log.d(Constants.LOG_TAG,
								"network valiable,type not wifi");
					}
				} else {
					Log.d(Constants.LOG_TAG, "network unvaliable");
				}
			}
		}, 5000, TIMER_INTERVAL);
	}

	//TODO
	private void sendRecord(int all_count) {
		isNeedloop = all_count >= LOG_ONCE_LIMIT;
		sendCount = isNeedloop ? LOG_ONCE_LIMIT : all_count;
		RecordResult recordResults = new RecordResult();
		DBManager.getInstance(mContext).getRecords(sendCount, recordResults);
		if (!TextUtils.isEmpty(recordResults.contentBuffer.toString())
				&& !TextUtils.isEmpty(recordResults.idBuffer.toString())) {
			sendRecordJson(recordResults, sendCount, all_count, isNeedloop);
		} else {
			Log.d(Constants.LOG_TAG, "read record result is not correct");
		}
	}

	private void stop() {
		if (!mStarted) {
			return;
		}
		if (null != timer) {
			timer.cancel();
		}
		mStarted = false;
	}

	public void put(String message) throws Ks3ClientException {
		Log.d(Constants.LOG_TAG, "new log: " + message);
		if (jsonCheck(message)) {
			DBManager.getInstance(mContext).insertLog(message);
		} else {
			throw new Ks3ClientException(
					"new log format is not correct, sdk will ingore it");
		}
	}

	private boolean jsonCheck(String message) {
		boolean isJson = true;
		try {
			JSONObject object = new JSONObject(message);
		} catch (JSONException e) {
			isJson = false;
		}
		return isJson;
	}

	public void put(LogRecord record) throws Ks3ClientException {
		if (record != null) {
			Log.d(Constants.LOG_TAG, "new log: " + record.toString());
			DBManager.getInstance(mContext).insertLog(record.toString());
		} else {
			throw new Ks3ClientException("record can not be null");
		}
	}

	/*
	 * private void saveToSDCard(String filename, String content) throws
	 * Exception { File file = new
	 * File(Environment.getExternalStorageDirectory(), filename); OutputStream
	 * out = new FileOutputStream(file); out.write(content.getBytes());
	 * out.close(); }
	 * 
	 * private void saveToSDCard(String filename, byte[] content) throws
	 * Exception { File file = new
	 * File(Environment.getExternalStorageDirectory(), filename); OutputStream
	 * out = new FileOutputStream(file); out.write(content); out.close(); }
	 */

}
