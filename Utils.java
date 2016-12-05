package at.clicktovote;

import android.app.ActivityManager;
import android.content.Context;
import static android.content.Context.ACTIVITY_SERVICE;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import android.app.usage.*;
import android.app.*;

public class Utils {

    private Context ctx;

    public Utils(Context context) {
        ctx = context;
    }

    private String logName = this.getClass().getName();

    public String getLastAppTask() {
        String app = null;
        int i = 0;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (!hasUsageStatsPermission()) {
                return null;
            }

            List<UsageEvents.Event> mySortedMap = new ArrayList<UsageEvents.Event>();

            UsageStatsManager mUsageStatsManager = (UsageStatsManager) ctx.getSystemService("usagestats");
            long time = System.currentTimeMillis();
            UsageEvents uEvents = null; //100 seconds
            uEvents = mUsageStatsManager.queryEvents(time - 100000, time); //100 seconds

            while (uEvents.hasNextEvent()) {
                UsageEvents.Event e = new UsageEvents.Event();
                uEvents.getNextEvent(e);
                mySortedMap.add(e);
            }

            if (0 < mySortedMap.size()) {
                int maxHistory = 7;
                for (i = 1; i < maxHistory; i++) { //tweeter task = 6
                    String packg = mySortedMap.get(mySortedMap.size() - i).getPackageName();
                    if ((!packg.contains("chrome") || i == maxHistory - 1) //twitter opens chrome browser (on webview?) and would return to blank page
                            && !packg.contains("clicktovote")
                            && !packg.contains("com.android.vending") //not in google play
                            && !packg.contains("com.android.settings") //not in google play
                            && !packg.contains("system") //prevent error redirecting any system process (menu button like)
                            && !packg.contains("googlequicksearchbox") //home button press? don't let redirect here, is very random
                            ) {
                        app = packg;
                        break;
                    }
                    Log.i(logName, i + ": " + packg);
                }
            }

        } else {
            // TODO: CHECK THIS WORKS ! (older versions)
            ActivityManager aManager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RecentTaskInfo> mySortedMap = aManager.getRecentTasks(7, 0);

            for (ActivityManager.RecentTaskInfo recentInfo : mySortedMap) {
                String packg = getRecentTaskInfoPackage(recentInfo);
                if (!packg.contains("chrome") && !packg.contains("clicktovote")) {
                    app = packg;
                    break;
                }
                Log.i(logName, i + ": " + packg);
                i++;
            }
        }

        if (null != app) {
            Log.i(logName, "redirection package on " + i + ": " + app);
        }
        return app;
    }

    public boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private String getRecentTaskInfoPackage(ActivityManager.RecentTaskInfo recentInfo) {
        Intent intent = new Intent(recentInfo.baseIntent);
        if (recentInfo.origActivity != null) {
            intent.setComponent(recentInfo.origActivity);
        }

        final PackageManager pm = ctx.getPackageManager();
        final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        final ActivityInfo info = resolveInfo.activityInfo;

        return info.packageName;
    }
}
