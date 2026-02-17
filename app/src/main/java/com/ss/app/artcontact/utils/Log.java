package com.ss.app.artcontact.utils;

public class Log {

    public static void l(int flag, String msg) {
        if (flag==1) {
            System.out.println(msg);
        }
    }

    public static void l(boolean flag, String msg) {
        if (flag) {
            System.out.println(msg);
        }
    }

}
