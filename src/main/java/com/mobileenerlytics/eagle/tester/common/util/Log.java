package com.mobileenerlytics.eagle.tester.common.util;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Log {
    @SuppressFBWarnings({"MS_SHOULD_BE_FINAL"})
    public static PrintStream out = System.out;
    @SuppressFBWarnings({"MS_CANNOT_BE_FINAL"})
    public static boolean debug = false;

    public static void i(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " info: " + msg);
    }

    public static void e(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " error: " + msg);
    }

    public static void w(String msg) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String now = df.format(Calendar.getInstance().getTime());
        out.println("[eagle-tester] " + now + " warn: " + msg);
    }

    public static void d(String msg) {
        if (debug) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String now = df.format(Calendar.getInstance().getTime());
            out.println("[eagle-tester] " + now + " debug: " + msg);
        }
    }
}
