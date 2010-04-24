package com.yrek.android.txtspeak;

import java.util.HashMap;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSService extends IntentService implements TextToSpeech.OnInitListener {
    private TextToSpeech tts = null;
    private HashMap<String,String> params = null;
    private boolean initialized = false;

    public SMSService() {
        super("SMSService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SMSService","onCreate");
        params = new HashMap<String,String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onDestroy() {
        Log.d("SMSService","onDestroy");
        tts.shutdown();
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        Log.d("SMSService","onInit status="+status);
        if (status != TextToSpeech.SUCCESS)
            tts = null;
        initialized = true;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (initialized && tts == null)
            return;
        Object[] pdus = intent.getExtras() != null ? (Object[]) intent.getExtras().get("pdus") : null;
        if (!initialized || (pdus == null && tts.isSpeaking())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            startService(intent);
            return;
        }
        if (pdus == null)
            return;
        boolean contactsOnly = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("contacts",true);
        Log.d("SMSService","contactsOnly="+contactsOnly);
        for (Object pdu : pdus)
            handleMessage(SmsMessage.createFromPdu((byte[]) pdu), contactsOnly);
        startService(new Intent(this, SMSService.class));
    }

    private void handleMessage(SmsMessage sms, boolean contactsOnly) {
        String sender = sms.getOriginatingAddress();
        Log.d("SMSService","sender="+sender);
        if (contactsOnly && sender == null)
            return;
        if (sender == null) {
            sender = sms.getDisplayOriginatingAddress();
        } else {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender)), new String[] { PhoneLookup.DISPLAY_NAME, PhoneLookup.SEND_TO_VOICEMAIL }, null, null, null);
                if (!cursor.moveToNext()) {
                    if (contactsOnly)
                        return;
                } else {
                    if ("1".equals(cursor.getString(1)))
                        return;
                    sender = cursor.getString(0);
                    if (sender == null)
                        sender = sms.getDisplayOriginatingAddress();
                }
            } finally {
                cursor.close();
            }
        }
        Log.d("SMSService","sender="+sender);
        StringBuffer sb = new StringBuffer();
        sb.append("From ").append(sender).append(", ").append(sms.getDisplayMessageBody());
        tts.speak(sb.toString(), TextToSpeech.QUEUE_ADD, params);
    }
}
