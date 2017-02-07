package com.android.imeiandmeidsettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SettingsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Uri uri = intent.getData();
        Log.d(this, "onReceive=>action: " + action + " uri: " + uri);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            if (context.getResources().getBoolean(R.bool.restore_imei_and_meid)) {
                Intent service = new Intent(context, UpdateService.class);
                service.putExtra(UpdateService.EXTRA_ACTION, Intent.ACTION_BOOT_COMPLETED);
                context.startService(service);
            }
        } else if("android.provider.Telephony.SECRET_CODE".equals(action)) {
            Uri secretUri = Uri.parse("android_secret_code://" + context.getString(R.string.secret_code));
            if (secretUri.equals(uri)) {
                Log.d(SettingsReceiver.this, "start ImeiAndMeidSettings activity.");
                Intent settings = new Intent(context, ImeiAndMeidSettings.class);
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settings);
            }
        }
    }

}
