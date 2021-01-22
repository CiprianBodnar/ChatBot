package com.example.vmac.WatBot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class LunchJobService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent intentToBroadcast =  new Intent(MainActivity.LUNCH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentToBroadcast);
        Log.d("MyAlarmBelal", "Alarm just fired");
    }
}