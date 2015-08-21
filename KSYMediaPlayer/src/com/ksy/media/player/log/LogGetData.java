package com.ksy.media.player.log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import com.ksy.media.player.util.Cpu;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;

/**
 * get data
 * @author LIXIAOPENG
 *
 */
public class LogGetData {
	
	private static LogGetData mInstance;
	private static Object mLockObject = new Object();
	private static Context mContext;
	private static TelephonyManager tm;
	private static Cpu mCpuStats; 
	
	private LogGetData() {
		
	};

	private LogGetData(Context context) {
		tm = (TelephonyManager) context.getSystemService("phone");
	}

	public static LogGetData getInstance() {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mInstance = new LogGetData();
				}
			}
		}
		return mInstance;
	}

	public static LogGetData getInstance(Context context) {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mContext = context;
					mInstance = new LogGetData(context);
				}
			}
		}
		return mInstance;
	}

	/**
	 * get memory info
	 */
	public static String getMemory() {

		String str1 = "/proc/meminfo";// 系统内存信息文件
		String memoryInfo;
		String[] arrayOfString;
		long initial_memory = 0;

		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(
					localFileReader, 8192);
			memoryInfo = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小

			arrayOfString = memoryInfo.split("\\s+");
			for (String num : arrayOfString) {
				Log.i(memoryInfo, num + "\t");
			}

			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
			localBufferedReader.close();

		} catch (IOException e) {
			
		}
		return Formatter.formatFileSize(mContext, initial_memory);// Byte转换为KB或者MB，内存大小规格化

	}

	/**
	 * get cpu info
	 */
	public static String getCpuInfo() {
		String cup = getMaxCpu();
		String cpuCore = String.valueOf(Runtime.getRuntime().availableProcessors());
		cup = cpuCore + "*" + cup;
		
		return cup;
	}

	public static String getMaxCpu() {
		String result = "";
		ProcessBuilder cmd;
		try {
			String[] args = { "/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "N/A";
		}
		return result.trim();
	}
	
	/**
	 * get core version
	 */
	public static String getCoreVersion() {

		String coreVersion = android.os.Build.VERSION.RELEASE;

		return coreVersion;
	}

	/**
	 * get device id & imei
	 */
	public static String getImei() {
		String imeiInfo = tm.getDeviceId();

		return imeiInfo;
	}

	/**
	 * get uuid
	 */
	public static String getUuid() {

		UUID uuid = UUID.randomUUID();

		return uuid.toString();
	}

	/**
	 * get system
	 */
	public static String getSystem() {
		String system = "Android";

		return system;
	}

	/**
	 * get net state
	 */
	public static String getNetState() {
		String net = null;

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.isConnectedOrConnecting()) {
            switch (ni.getType()) {
            	//wifi 
                case ConnectivityManager.TYPE_WIFI:
                	net = "NET_WIFI";  
                    break;
                //mobile 网络
                case ConnectivityManager.TYPE_MOBILE:
                    switch (ni.getSubtype()) {
                        case TelephonyManager.NETWORK_TYPE_GPRS: //联通2g
                        case TelephonyManager.NETWORK_TYPE_CDMA: //电信2g
                        case TelephonyManager.NETWORK_TYPE_EDGE: //移动2g
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager. NETWORK_TYPE_IDEN:
                        	net = "NET_2G";
                            break;
                        case TelephonyManager.NETWORK_TYPE_EVDO_A: //电信3g
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        case TelephonyManager.NETWORK_TYPE_EHRPD:
                        case TelephonyManager.NETWORK_TYPE_HSPAP:
                        	net = "NET_3G";
                            break;
                        case TelephonyManager.NETWORK_TYPE_LTE://4G
                        	net = "NET_4G";
                            break;
                        //未知,一般不会出现
                        default:
                        	net = "NET_UNKNOWN";
                    }
                    break;
                default:
                	net = "NET_UNKNOWN";
            }
        }

		return net;
	}

	/**
	 * get gmt
	 */
	public static String getGmt() {

		Calendar calendar = Calendar.getInstance();
		TimeZone timeZone = calendar.getTimeZone();

		return timeZone.getDisplayName();
	}

	/**
	 * get current time GMT
	 */
	public static String currentTimeGmt() {
		
		Calendar cd = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE d MMM yyyy HH:mm:ss 'GMT'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // set GMT

		return sdf.format(cd.getTime());
	}

	/**
	 * get browser info
	 */
	public static String userAgent() {

		return "Android";
	}

	/**
	 * get deviceip
	 */
	public static String getDeviceIp() {

		try {
			WifiManager wifiManager = (WifiManager) mContext
					.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int i = wifiInfo.getIpAddress();
			return int2ip(i);
		} catch (Exception ex) {
			return "ex==" + ex.getMessage();
		}

	}

	/**
	 * get serverIp TODO
	 */
	public static String getServerIp() {
		String serverIp = null;

		return serverIp;
	}

	/**
	 * get cpu usage
	 */
	public static String getCpuUsage(String pack) {
		String cpuUsage = null;

		mCpuStats = new Cpu(pack);
		
		mCpuStats.parseTopResults();
		
		cpuUsage = mCpuStats.getProcessCpuUsage();
		
		return cpuUsage;
	}

	/**
	 * get memory usage TODO
	 */
	public static String getMemoryUsage() {
		String memoryUsage = null;

		return memoryUsage;
	}

	/**
	 * get firstframe TODO
	 */
	public static long firstFrameTime(long startTime, long endTime) {

		long firstFrame = endTime - startTime;

		return firstFrame;
	}

	/**
	 * get cacheBufferSize TODO
	 */
	public static int cacheBufferSize(int cacheSize) {

		return cacheSize;
	}

	/**
	 * get seek begin time
	 */
	public static long seekBeginTime() {
		long seekBegin = System.currentTimeMillis();

		return seekBegin;
	}

	/**
	 * get seek end time
	 */
	public static long seekEndTime() {
		long seekEnd = System.currentTimeMillis();

		return seekEnd;
	}

	/**
	 * get seek statue TODO
	 */
	public static String statue(String statue) {
        String seekStatus = null;
		
		return statue;
	}

	/**
	 * get seek message TODO
	 */
	public static String seekMessage() {
		String seekMessage = null;

		return seekMessage;
	}

	/**
	 * get play failure record TODO
	 */
	public static String playFailureRecord() {
		String playFailure = null;

		return playFailure;
	}

	/**
	 * TODO
	 */
	public static String playMetaData() {
		String data = null;

		return data;
	}

	/**
	 * 将ip的整数形式转换成ip形式
	 * 
	 * @param ipInt
	 * @return
	 */
	public static String int2ip(int ipInt) {
		StringBuilder sb = new StringBuilder();
		sb.append(ipInt & 0xFF).append(".");
		sb.append((ipInt >> 8) & 0xFF).append(".");
		sb.append((ipInt >> 16) & 0xFF).append(".");
		sb.append((ipInt >> 24) & 0xFF);
		return sb.toString();
	}

}



