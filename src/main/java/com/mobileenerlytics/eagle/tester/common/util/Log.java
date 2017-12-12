package com.mobileenerlytics.eagle.tester.common.util;


import com.mobileenerlytics.eagle.tester.common.Configure;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Log {
    public static PrintStream out = System.out;
    public static ArrayList<String> warningList = new ArrayList<String>();
    public static ArrayList<String> errorList = new ArrayList<String>();

    public static void i(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " info: " + msg);
    }

    public static void w(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " warn: " + msg);
        warningList.add(now + " " + msg);
    }

    public static void e(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " error: " + msg);
        errorList.add(now + " " + msg);
    }

    public static void d(String msg) {
        if (Configure.debug) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String now = df.format(Calendar.getInstance().getTime());
            out.println("[eagle-tester] " + now + " debug: " + msg);
        }
    }
}
