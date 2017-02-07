package com.android.imeiandmeidsettings;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;

public class UpdateService extends Service {

    public static final String EXTRA_ACTION = "action";

    private static final int MSG_UPDATE_IMEI_AND_MEID = 100;

    private SettingHelper mHelper;

    private String mSim1Imei;
    private String mSim2Imei;
    private String mMeid;
    private long mSetMeidDelayedTime;
    private long mUpdateImeiAndMeidDelayedTime;
    private int mSettingCount;
    private int mSuccessCount;
    private int mFailCount;
    private boolean mEnabledSettingSim1Imei;
    private boolean mEnabledSettingSim2Imei;
    private boolean mEnabledSettingMeid;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(this, "onCreate()...");
        Resources res = getResources();
        mUpdateImeiAndMeidDelayedTime = res.getInteger(R.integer.update_imei_and_meid_delayed_time);
        mSetMeidDelayedTime = res.getInteger(R.integer.set_meid_delayed_time);
        mEnabledSettingSim1Imei = res.getBoolean(R.bool.enabled_setting_sim1_imei);
        mEnabledSettingSim2Imei = res.getBoolean(R.bool.enabled_setting_sim2_imei);
        mEnabledSettingMeid = (res.getBoolean(R.bool.enabled_setting_meid) && SystemProperties.get("ro.mtk_c2k_support").equals("1"));

        mHelper = new SettingHelper(this, mHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this, "onStartCommand=>intent: " + intent + " flags: " + flags + " startId: " + startId);
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getStringExtra(EXTRA_ACTION))) {
            if (mHandler.hasMessages(MSG_UPDATE_IMEI_AND_MEID)) {
                mHandler.removeMessages(MSG_UPDATE_IMEI_AND_MEID);
            }
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_IMEI_AND_MEID, mUpdateImeiAndMeidDelayedTime);
        } else {
            if (!mHandler.hasMessages(MSG_UPDATE_IMEI_AND_MEID)) {
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(this, "onDestroy()...");
    }

    private void updateImeiAndMeid() {
        mSim1Imei = mHelper.getSim1ImeiFromNv();
        mSim2Imei = mHelper.getSim2ImeiFromNv();
        mMeid = mHelper.getMeidFromNv();
        Log.d(this, "updateImeiAndMeid(nv)=>imei1: " + mSim1Imei + " imei2: " + mSim2Imei + " meid: " + mMeid);

        String sim1Imei = mHelper.getDeviceId(0);
        String sim2Imei = mHelper.getDeviceId(1);
        String meid = mHelper.getMeid();
        Log.d(this, "updateImeiAndMeid(device)=>imei1: " + sim1Imei + " imei2: " + sim2Imei + " meid: " + meid);

        if (TextUtils.isEmpty(mSim1Imei) || mSim1Imei.equals(sim1Imei)) {
            mSim1Imei = "";
        }

        if (TextUtils.isEmpty(mSim2Imei) || mSim2Imei.equals(sim2Imei)) {
            mSim2Imei = "";
        }

        if (TextUtils.isEmpty(mMeid) || mMeid.equals(meid)) {
            mMeid = "";
        }
        Log.d(this, "updateImeiAndMeid(final)=>imei1: " + mSim1Imei + " imei2: " + mSim2Imei + " meid: " + mMeid);
        boolean enabledSetting = mHelper.enabledSettingImeiAndMeid(mSim1Imei, mSim2Imei, mMeid, true);
        if (enabledSetting) {
            mSuccessCount = 0;
            mFailCount = 0;
            mSettingCount = mHelper.getSettingCount(mSim1Imei, mSim2Imei, mMeid);
            if (mSettingCount > 0) {
                mHandler.sendEmptyMessage(SettingHelper.MSG_SETTING_SIM1_IMEI);
            } else {
                stopSelf();
            }
        } else {
            stopSelf();
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(UpdateService.this, "handleMessage=>what: " + msg.what);
            AsyncResult ar = (AsyncResult) msg.obj;
            String sim1Imei = mSim1Imei;
            String sim2Imei = mSim2Imei;
            if (mHelper.needSwapImei()) {
                sim1Imei = mSim2Imei;
                sim2Imei = mSim1Imei;
            }
            switch (msg.what) {
                case MSG_UPDATE_IMEI_AND_MEID:
                    updateImeiAndMeid();
                    break;

                case SettingHelper.MSG_SETTING_SIM1_IMEI:
                    if (mHelper.needSettingSim1Imei(sim1Imei)) {
                        mHelper.executeWriteSim1ImeiATCommand(sim1Imei);
                    } else if (mHelper.needSettingSim2Imei(sim2Imei)) {
                        mHelper.executeWriteSim2ImeiATCommand(sim2Imei);
                    } else if (mHelper.needSettingMeid(mMeid)) {
                        mHelper.executeWriteMeidATCommand(mMeid);
                    } else {
                        stopSelf();
                    }
                    break;

                case SettingHelper.MSG_SETTING_SIM2_IMEI:
                    if (mHelper.needSettingSim2Imei(sim2Imei)) {
                        mHelper.executeWriteSim2ImeiATCommand(sim2Imei);
                    } else if (mHelper.needSettingMeid(mMeid)) {
                        mHelper.executeWriteMeidATCommand(mMeid);
                    } else {
                        if (mHelper.needSettingSim1Imei(sim1Imei) || mHelper.needSettingSim2Imei(sim2Imei)) {
                            mHandler.sendEmptyMessage(SettingHelper.MSG_RESTART_GSM_MODE);
                        } else {
                            stopSelf();
                        }
                    }
                    break;

                case SettingHelper.MSG_SETTING_MEID:
                    if (mHelper.needSettingMeid(mMeid)) {
                        mHelper.executeWriteMeidATCommand(mMeid);
                    } else {
                        stopSelf();
                    }
                    break;

                case SettingHelper.MSG_RESTART_GSM_MODE:
                    mHelper.restartGsmMode();
                    break;

                case SettingHelper.MSG_RESTART_CDMA_MODE:
                    mHelper.restartCdmaMode();
                    break;

                case SettingHelper.MSG_SETTING_SIM1_IMEI_RESULT:
                    Log.d(UpdateService.this, "handleMessage=>MSG_SETTING_SIM1_IMEI_RESULT: " + ar.exception);
                    if (ar.exception == null) {
                        boolean result = false;
                        if (mHelper.needSwapImei()) {
                            result = mHelper.writeSim2ImeiToNv(mSim2Imei.trim());
                        } else {
                            result = mHelper.writeSim1ImeiToNv(mSim1Imei.trim());
                        }
                        if (result) {
                            mSuccessCount++;
                        } else {
                            mFailCount++;
                        }
                        mHandler.sendEmptyMessage(SettingHelper.MSG_SETTING_SIM2_IMEI);
                    } else {
                        mFailCount++;
                        mHandler.sendEmptyMessage(SettingHelper.MSG_SETTING_SIM2_IMEI);
                    }
                    break;

                case SettingHelper.MSG_SETTING_SIM2_IMEI_RESULT:
                    Log.d(UpdateService.this, "handleMessage=>MSG_SETTING_SIM2_IMEI_RESULT: " + ar.exception);
                    if (ar.exception == null) {
                        boolean result = false;
                        if (mHelper.needSwapImei()) {
                            result = mHelper.writeSim1ImeiToNv(mSim1Imei.trim());
                        } else {
                            result = mHelper.writeSim2ImeiToNv(mSim2Imei.trim());
                        }
                        if (result) {
                            mSuccessCount++;
                        } else {
                            mFailCount++;
                        }
                        mHandler.sendEmptyMessage(SettingHelper.MSG_RESTART_GSM_MODE);
                    } else {
                        mFailCount++;
                        mHandler.sendEmptyMessage(SettingHelper.MSG_RESTART_GSM_MODE);
                    }
                    break;

                case SettingHelper.MSG_SETTING_MEID_RESULT:
                    Log.d(UpdateService.this, "handleMessage=>MSG_SETTING_MEID_RESULT: " + ar.exception);
                    if (ar.exception == null) {
                        if (mHelper.writeMeidToNv(mMeid.trim())) {
                            mSuccessCount++;
                        } else {
                            mFailCount++;
                        }
                    } else {
                        mFailCount++;
                    }
                    mHandler.sendEmptyMessage(SettingHelper.MSG_RESTART_CDMA_MODE);
                    break;

                case SettingHelper.MSG_RESTART_GSM_MODE_RESULT:
                    Log.d(UpdateService.this, "handleMessage=>MSG_RESTART_GSM_MODE_RESULT: " + ar.exception);
                    if (ar.exception != null) {
                        mFailCount = mSettingCount;
                        mSuccessCount = 0;
                    }
                    if (mHelper.needSettingMeid(mMeid)) {
                        mHandler.sendEmptyMessageDelayed(SettingHelper.MSG_SETTING_MEID, mSetMeidDelayedTime);
                    } else {
                        stopSelf();
                    }
                    break;

                case SettingHelper.MSG_RESTART_CDMA_MODE_RESULT:
                    Log.d(UpdateService.this, "handleMessage=>MSG_RESTART_CDMA_MODE_RESULT: " + ar.exception);
                    if (ar.exception != null) {
                        mFailCount = mSettingCount;
                        mSuccessCount = 0;
                    }
                    stopSelf();
                    break;

            }
        }
    };
}
