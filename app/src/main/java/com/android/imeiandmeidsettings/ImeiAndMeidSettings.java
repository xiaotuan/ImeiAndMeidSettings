package com.android.imeiandmeidsettings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ImeiAndMeidSettings extends Activity implements View.OnClickListener {

    private TextView mSim1ImeiTv;
    private TextView mSim2ImeiTv;
    private TextView mMeidTv;
    private EditText mSim1ImeiEt;
    private EditText mSim2ImeiEt;
    private EditText mMeidEt;
    private Button mSettingBt;
    private SettingHelper mHelper;
    private Dialog mSettingDialog;

    private String mSim1Imei;
    private String mSim2Imei;
    private String mMeid;
    private int mSettingCount;
    private int mSuccessCount;
    private int mFailCount;
    private long mSetMeidDelayedTime;
    private boolean mEnabledSettingSim1Imei;
    private boolean mEnabledSettingSim2Imei;
    private boolean mEnabledSettingMeid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imei_and_meid_settings);

        Resources res = getResources();
        mEnabledSettingSim1Imei = res.getBoolean(R.bool.enabled_setting_sim1_imei);
        mEnabledSettingSim2Imei = res.getBoolean(R.bool.enabled_setting_sim2_imei);
        mEnabledSettingMeid = res.getBoolean(R.bool.enabled_setting_meid) && SystemProperties.get("ro.mtk_c2k_support").equals("1");
        mSetMeidDelayedTime = res.getInteger(R.integer.set_meid_delayed_time);
        mHelper = new SettingHelper(this, mHandler);
        mSettingDialog = createSettingProgressDialog();

        mSim1ImeiTv = (TextView) findViewById(R.id.sim1_imei_title);
        mSim2ImeiTv = (TextView) findViewById(R.id.sim2_imei_title);
        mMeidTv = (TextView) findViewById(R.id.meid_title);
        mSim1ImeiEt = (EditText) findViewById(R.id.sim1_imei);
        mSim2ImeiEt = (EditText) findViewById(R.id.sim2_imei);
        mMeidEt = (EditText) findViewById(R.id.meid);
        mSettingBt = (Button) findViewById(R.id.setting);
        mSettingBt.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateViews();
    }

    @Override
    public void onClick(View v) {
        mSim1Imei = mSim1ImeiEt.getText().toString().trim();
        mSim2Imei = mSim2ImeiEt.getText().toString().trim();
        mMeid = mMeidEt.getText().toString().trim();
        boolean enabledSetting = mHelper.enabledSettingImeiAndMeid(mSim1Imei, mSim2Imei, mMeid, true);
        if (enabledSetting) {
            mSuccessCount = 0;
            mFailCount = 0;
            mSettingCount = mHelper.getSettingCount(mSim1Imei, mSim2Imei, mMeid);
            if (mSettingCount > 0) {
                if (!mSettingDialog.isShowing()) {
                    mSettingDialog.show();
                }
                mSim1ImeiEt.setEnabled(false);
                mSim2ImeiEt.setEnabled(false);
                mMeidEt.setEnabled(false);
                mSettingBt.setEnabled(false);
                mHandler.sendEmptyMessage(SettingHelper.MSG_SETTING_SIM1_IMEI);
            }
        }
    }

    private void updateViews() {
        mSim1ImeiTv.setVisibility(mEnabledSettingSim1Imei ? View.VISIBLE : View.GONE);
        mSim1ImeiEt.setVisibility(mEnabledSettingSim1Imei ? View.VISIBLE : View.GONE);
        mSim2ImeiTv.setVisibility(mEnabledSettingSim2Imei ? View.VISIBLE : View.GONE);
        mSim2ImeiEt.setVisibility(mEnabledSettingSim2Imei ? View.VISIBLE : View.GONE);
        mMeidTv.setVisibility(mEnabledSettingMeid ? View.VISIBLE : View.GONE);
        mMeidEt.setVisibility(mEnabledSettingMeid ? View.VISIBLE : View.GONE);

        if (mEnabledSettingSim1Imei) {
            String sim1Imei = mHelper.getDeviceId(0);
            if (!TextUtils.isEmpty(sim1Imei)) {
                mSim1ImeiEt.setHint(sim1Imei);
            }
        }
        if (mEnabledSettingSim2Imei) {
            String sim2Imei = mHelper.getDeviceId(1);
            if (!TextUtils.isEmpty(sim2Imei)) {
                mSim2ImeiEt.setHint(sim2Imei);
            }
        }
        if (mEnabledSettingMeid) {
            String meid = mHelper.getMeid();
            if (!TextUtils.isEmpty(meid)) {
                mMeidEt.setHint(meid);
            }
        }
    }

    private Dialog createSettingProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this, R.style.ProgressDialogTheme);
        dialog.setMessage(getString(R.string.setting_dialog_msg));
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void needShowResultDialog() {
        Log.d(ImeiAndMeidSettings.this, "needShowResultDialog=>count: " + mSettingCount + " success: " + mSuccessCount + " fail: " + mFailCount);
        if (mSettingDialog.isShowing()) {
            mSettingDialog.dismiss();
        }
        if (mSuccessCount + mFailCount == mSettingCount) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ImeiAndMeidSettings.this);
            if (mEnabledSettingSim1Imei || mEnabledSettingSim2Imei) {
                if (mEnabledSettingMeid) {
                    builder.setTitle(R.string.app_name);
                } else {
                    builder.setTitle(R.string.setting_imei);
                }
            } else {
                builder.setTitle(R.string.setting_meid);
            }
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mSuccessCount == mSettingCount) {
                        ImeiAndMeidSettings.this.finish();
                    }
                }
            });
            builder.setCancelable(false);
            if (mSuccessCount == mSettingCount) {
                if (mEnabledSettingSim1Imei || mEnabledSettingSim2Imei) {
                    if (mEnabledSettingMeid) {
                        builder.setMessage(R.string.success);
                    } else {
                        builder.setMessage(R.string.success_no_meid);
                    }
                } else {
                    builder.setMessage(R.string.success_no_imei);
                }
            } else {
                if (mEnabledSettingSim1Imei || mEnabledSettingSim2Imei) {
                    if (mEnabledSettingMeid) {
                        builder.setMessage(R.string.fail);
                    } else {
                        builder.setMessage(R.string.fail_no_meid);
                    }
                } else {
                    builder.setMessage(R.string.fail_no_imei);
                }
            }
            builder.create().show();
            mSim1ImeiEt.setEnabled(true);
            mSim2ImeiEt.setEnabled(true);
            mMeidEt.setEnabled(true);
            mSettingBt.setEnabled(true);
            mSettingBt.setEnabled(true);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            Log.d(ImeiAndMeidSettings.this, "handleMessage=>what: " + msg.what);
            String sim1Imei = mSim1Imei;
            String sim2Imei = mSim2Imei;
            if (mHelper.needSwapImei()) {
                sim1Imei = mSim2Imei;
                sim2Imei = mSim1Imei;
            }
            switch (msg.what) {
                case SettingHelper.MSG_SETTING_SIM1_IMEI:
                    if (mHelper.needSettingSim1Imei(sim1Imei)) {
                        mHelper.executeWriteSim1ImeiATCommand(sim1Imei);
                    } else if (mHelper.needSettingSim2Imei(sim2Imei)) {
                        mHelper.executeWriteSim2ImeiATCommand(sim2Imei);
                    } else if (mHelper.needSettingMeid(mMeid)) {
                        mHelper.executeWriteMeidATCommand(mMeid);
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
                        }
                    }
                    break;

                case SettingHelper.MSG_SETTING_MEID:
                    if (mHelper.needSettingMeid(mMeid)) {
                        mHelper.executeWriteMeidATCommand(mMeid);
                    }
                    break;

                case SettingHelper.MSG_RESTART_GSM_MODE:
                    mHelper.restartGsmMode();
                    break;

                case SettingHelper.MSG_RESTART_CDMA_MODE:
                    mHelper.restartCdmaMode();
                    break;

                case SettingHelper.MSG_SETTING_SIM1_IMEI_RESULT:
                    Log.d(ImeiAndMeidSettings.this, "handleMessage=>MSG_SETTING_SIM1_IMEI_RESULT: " + ar.exception);
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
                    } else {
                        mFailCount++;
                    }
                    mHandler.sendEmptyMessage(SettingHelper.MSG_SETTING_SIM2_IMEI);
                    break;

                case SettingHelper.MSG_SETTING_SIM2_IMEI_RESULT:
                    Log.d(ImeiAndMeidSettings.this, "handleMessage=>MSG_SETTING_SIM2_IMEI_RESULT: " + ar.exception);
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
                    } else {
                        mFailCount++;
                    }
                    mHandler.sendEmptyMessage(SettingHelper.MSG_RESTART_GSM_MODE);
                    break;

                case SettingHelper.MSG_SETTING_MEID_RESULT:
                    Log.d(ImeiAndMeidSettings.this, "handleMessage=>MSG_SETTING_MEID_RESULT: " + ar.exception);
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
                    Log.d(ImeiAndMeidSettings.this, "handleMessage=>MSG_RESTART_GSM_MODE_RESULT: " + ar.exception);
                    if (ar.exception != null) {
                        mFailCount = mSettingCount;
                        mSuccessCount = 0;
                    }
                    if (mHelper.needSettingMeid(mMeid)) {
                        mHandler.sendEmptyMessageDelayed(SettingHelper.MSG_SETTING_MEID, mSetMeidDelayedTime);
                    } else {
                        needShowResultDialog();
                    }
                    break;

                case SettingHelper.MSG_RESTART_CDMA_MODE_RESULT:
                    Log.d(ImeiAndMeidSettings.this, "handleMessage=>MSG_RESTART_CDMA_MODE_RESULT: " + ar.exception);
                    if (ar.exception != null) {
                        mFailCount = mSettingCount;
                        mSuccessCount = 0;
                    }
                    needShowResultDialog();
                    break;
            }
        }
    };
}
