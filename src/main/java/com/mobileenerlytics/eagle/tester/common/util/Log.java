package com.mobileenerlytics.eagle.tester.common.util;


import com.mobileenerlytics.eagle.tester.common.Configure;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Log {
    PrintStream out = System.out;

    private void _i(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " info: " + msg);
    }

    private void _w(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " warn: " + msg);
    }

    private void _e(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " error: " + msg);
    }

    private void _d(String msg) {
        if (Configure.debug) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String now = df.format(Calendar.getInstance().getTime());
            out.println("[eagle-tester] " + now + " debug: " + msg);
        }
    }

    private static Log log;
    Log(PrintStream ps) {
        out = ps;
    }

    public static void init(PrintStream ps) {
        log = new Log(ps);
    }

    public static void i(String msg) {
        log._i(msg);
    };
    public static void e(String msg) {
        log._e(msg);
    };
    public static void w(String msg) {
        log._w(msg);
    };
    public static void d(String msg) {
        log._d(msg);
    };
}
