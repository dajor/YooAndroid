package com.fellow.yoo.utils;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class BadgeUtils {


    public static void setBadge(Context context, int count) {
    	// work only on real device. on simulator will not working.
        setBadgeSamsung(context, count); // Samsung,LG and many other devices now, has the same setting.
        setBadgeSony(context, count);
        setBadgeAsus(context, count);
        setBadgeHTC(context, count);
        
    }

    public static void clearBadge(Context context) {
        setBadgeSamsung(context, 0);
        setBadgeAsus(context, 0);
        setBadgeSony(context, 0);
        setBadgeHTC(context, 0);
    }
    
    
    private static void setBadgeSamsung(Context context, int count) {
        String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        intent.putExtra("badge_count", count);
        intent.putExtra("badge_count_package_name", context.getPackageName());
        intent.putExtra("badge_count_class_name", launcherClassName);
        context.sendBroadcast(intent);
    }

    private static void setBadgeSony(Context context, int count) {
        String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", launcherClassName);
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count> 0 ? true : false);
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", String.valueOf(count));
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", context.getPackageName());

        context.sendBroadcast(intent);
    }
    
    
    public static void setBadgeHTC(Context context, int count) {
    	String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }
        
        Intent intent = new Intent("com.htc.launcher.action.UPDATE_SHORTCUT");
        intent.putExtra("packagename", context.getPackageName());
        intent.putExtra("count", count);
        context.sendBroadcast(intent);

        Intent setNotificationIntent = new Intent("com.htc.launcher.action.SET_NOTIFICATION");
        ComponentName componentName = new ComponentName(context.getPackageName(), launcherClassName);
        setNotificationIntent.putExtra("com.htc.launcher.extra.COMPONENT", componentName.flattenToShortString());
        setNotificationIntent.putExtra("com.htc.launcher.extra.COUNT", count);
        context.sendBroadcast(setNotificationIntent);
    }
   
    
    private static void setBadgeAsus(Context context, int count) {
        String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }
        // Arrays.asList("com.asus.launcher")
        Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        intent.putExtra("badge_count", count);
        intent.putExtra("badge_count_package_name", context.getPackageName());
        intent.putExtra("badge_count_class_name", launcherClassName);
        intent.putExtra("badge_vip_count", 0);
        context.sendBroadcast(intent);
    }

    

    private static String getLauncherClassName(Context context) {

        PackageManager pm = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
            if (pkgName.equalsIgnoreCase(context.getPackageName())) {
                String className = resolveInfo.activityInfo.name;
                
                //Toast.makeText(context, "Launcher device Name : " + className, Toast.LENGTH_LONG).show();
                return className;
            }
        }
        
        //Toast.makeText(context, "not found Launcher device Name : ", Toast.LENGTH_LONG).show();
        return null;
    }
    
    
    
}