package com.mj.imagedownloader;

public class Log {
	public static void i(String tag, String msg) {
		System.out.println(tag + " : " + msg + "\n");
	}
	
	public static void d(String tag, String msg) {
		System.out.println(tag + " : " + msg + "\n");
	}
	
	public static void e(String tag, String msg) {
		System.err.println(tag + " : " + msg + "\n");
	}
}
