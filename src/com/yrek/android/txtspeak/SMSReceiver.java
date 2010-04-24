package com.yrek.android.txtspeak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class SMSReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())
            || !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enabled", false))
            return;
        Intent smsService = new Intent(context, SMSService.class);
        smsService.putExtras(intent.getExtras());
        context.startService(smsService);
    }
}
