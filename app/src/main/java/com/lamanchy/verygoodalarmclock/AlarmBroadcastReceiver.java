package com.lamanchy.verygoodalarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;


public class AlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED) ||
                Objects.equals(intent.getAction(), Intent.ACTION_TIME_CHANGED) ||
                Objects.equals(intent.getAction(), Intent.ACTION_TIMEZONE_CHANGED)) {
            Intent service = new Intent(context, AlarmService.class);
            service.setAction(Enums.RESET_ACTION);
            context.startService(service);
        }
    }
}