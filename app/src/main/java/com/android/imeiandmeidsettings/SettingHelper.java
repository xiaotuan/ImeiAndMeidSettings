package com.android.imeiandmeidsettings;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.internal.telephony.ITelephonyEx;

import java.util.Arrays;
import java.util.regex.Pattern;

public class SettingHelper {

    public static final int MSG_SETTING_SIM1_IMEI = 0;
    public static final int MSG_SETTING_SIM2_IMEI = 1;
    public static final int MSG_SETTING_MEID = 2;
    public static final int MSG_RESTART_GSM_MODE = 4;
    public static final int MSG_RESTART_CDMA_MODE = 5;
    public static final int MSG_SETTING_SIM1_IMEI_RESULT = 6;
    public static final int MSG_SETTING_SIM2_IMEI_RESULT = 7;
    public static final int MSG_SETTING_MEID_RESULT = 8;
    public static final int MSG_RESTART_GSM_MODE_RESULT = 9;
    public static final int MSG_RESTART_CDMA_MODE_RESULT = 10;

    private Context mContext;
    private Handler mHandler;

    private int mSim1ImeiLength;
    private int mSim2ImeiLength;
    private int mMeidLength;
    private boolean mEnabledSettingSim1Imei;
    private boolean mEnabledSettingSim2Imei;
    private boolean mEnabledSettingMeid;
    private boolean mIsC2KProject;
    private boolean mEnabledRespectivelySet;

    public SettingHelper(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;

        Resources res = mContext.getResources();
        mSim1ImeiLength = res.getInteger(R.integer.sim1_imei_length);
        mSim2ImeiLength = res.getInteger(R.integer.sim2_imei_length);
        mMeidLength = res.getInteger(R.integer.meid_length);
        mEnabledSettingSim1Imei = res.getBoolean(R.bool.enabled_setting_sim1_imei);
        mEnabledSettingSim2Imei = res.getBoolean(R.bool.enabled_setting_sim2_imei);
        mEnabledSettingMeid = res.getBoolean(R.bool.enabled_setting_meid);
        mIsC2KProject = SystemProperties.get("ro.mtk_c2k_support").equals("1");
        mEnabledRespectivelySet = res.getBoolean(R.bool.enabled_respectively_set);

        Log.d(this, "SettingHelper=>sim1: " + mEnabledSettingSim1Imei + " sim2: " + mEnabledSettingSim2Imei
                + " meid: " + mEnabledSettingMeid + " c2k: " + mIsC2KProject + " resPectivelySet: " + mEnabledRespectivelySet);
    }

    public boolean enabledSettingImeiAndMeid(String sim1Imei, String sim2Imei, String meid, boolean showTip) {
        Log.d(this, "enabledSettingImeiAndMeid=>imei1: " + sim1Imei.trim() + " imei2: " + sim2Imei.trim() + " meid: " + meid.trim());
        boolean result = true;

        if (!checkSim1Imei(sim1Imei.trim(), showTip)) {
            result = false;
        } else if (!checkSim2Imei(sim2Imei.trim(), showTip)) {
            result = false;
        } else if (!checkMeid(meid.trim(), showTip)) {
            result = false;
        }

        Log.d(this, "enabledSettingImeiAndMeid=>result: " + result);
        return result;
    }

    public void restartGsmMode() {
        final String[] command = new String[2];
        command[0] = "AT+EPON";
        command[1] = "+EPON";
        Phone gsmPhone = getGSMPhone();
        Log.d(this, "restartGsmMode=>radioOn: " + gsmPhone.isRadioOn() + " available: " + gsmPhone.isRadioAvailable());
        gsmPhone.invokeOemRilRequestStrings(command, mHandler != null ? mHandler.obtainMessage(MSG_RESTART_GSM_MODE_RESULT) : null);
    }

    public void restartCdmaMode() {
        final String[] command = new String[3];
        command[0] = "AT+EPON";
        command[1] = "+EPON";
        Phone cdmaPhone = getCDMAPhone();
        Log.d(this, "restartCdmaMode=>radioOn: " + cdmaPhone.isRadioOn() + " available: " + cdmaPhone.isRadioAvailable());
        cdmaPhone.invokeOemRilRequestStrings(command, mHandler != null ? mHandler.obtainMessage(MSG_RESTART_CDMA_MODE_RESULT) : null);
    }

    public void executeWriteSim1ImeiATCommand(String imei) {
        Log.d(this, "settingSim1Imei=>imei: " + imei);
        Phone phone = getGSMPhone();
        Log.d(this, "executeWriteSim1ImeiATCommand=>radioOn: " + phone.isRadioOn() + " available: " + phone.isRadioAvailable());
        String[] commands = new String[2];
        commands[0] = "AT+EGMR=1,7,\"" + imei + "\"";
        commands[1] = "+EGMR";
        phone.invokeOemRilRequestStrings(commands, mHandler != null ? mHandler.obtainMessage(MSG_SETTING_SIM1_IMEI_RESULT) : null);
    }

    public void executeWriteSim2ImeiATCommand(String imei) {
        Log.d(this, "settingSim2Imei=>imei: " + imei);
        Phone phone = getGSMPhone();
        Log.d(this, "executeWriteSim2ImeiATCommand=>radioOn: " + phone.isRadioOn() + " available: " + phone.isRadioAvailable());
        String[] commands = new String[2];
        commands[0] = "AT+EGMR=1,10,\"" + imei + "\"";
        commands[1] = "+EGMR";
        phone.invokeOemRilRequestStrings(commands, mHandler != null ? mHandler.obtainMessage(MSG_SETTING_SIM2_IMEI_RESULT) : null);
    }

    public void executeWriteMeidATCommand(String meid) {
        Log.d(this, "settingMeid=>meid: " + meid);
        Phone phone = getCDMAPhone();
        Log.d(this, "executeWriteMeidATCommand=>radioOn: " + phone.isRadioOn() + " available: " + phone.isRadioAvailable());
        String[] commands = new String[2];
        commands[0] = "AT+VMOBID=0, \"7268324842763108\", 2, \"" + meid.toUpperCase() + "\"";
        commands[1] = "+VMOBID";
        phone.invokeOemRilRequestStrings(commands, mHandler != null ? mHandler.obtainMessage(MSG_SETTING_MEID_RESULT) : null);
    }

    public boolean writeSim1ImeiToNv(String imei) {
        Log.d(this, "writeSim1ImeiToNv=>imei: " + imei);
        byte[] bytes = stringToBytes(imei, mSim1ImeiLength);
        bytes = Arrays.copyOf(bytes, 10);
        return NvRAMUtils.writeNV(NvRAMUtils.INDEX_SIM1_IMEI, bytes);
    }

    public boolean writeSim2ImeiToNv(String imei) {
        Log.d(this, "writeSim2ImeiToNv=>imei: " + imei);
        byte[] bytes = stringToBytes(imei, mSim2ImeiLength);
        bytes = Arrays.copyOf(bytes, 10);
        return NvRAMUtils.writeNV(NvRAMUtils.INDEX_SIM2_IMEI, bytes);
    }

    public boolean writeMeidToNv(String meid) {
        Log.d(this, "writeMeidToNv=>meid: " + meid);
        byte[] bytes = stringToBytes(meid, mMeidLength);
        bytes = Arrays.copyOf(bytes, 10);
        return NvRAMUtils.writeNV(NvRAMUtils.INDEX_MEID, bytes);
    }

    public String getSim1ImeiFromNv() {
        String imei = null;
        byte[] bytes = NvRAMUtils.readNV(NvRAMUtils.INDEX_SIM1_IMEI, NvRAMUtils.SIM1_IMEI_LENGTH);
        imei = bytesToString(bytes, mSim1ImeiLength);
        Log.d(this, "getSim1ImeiFromNv=>imei: " + imei);
        if (!isLegitimateImei(imei, mSim1ImeiLength)) {
            imei = "";
        }
        return imei;
    }

    public String getSim2ImeiFromNv() {
        String imei = null;
        byte[] bytes = NvRAMUtils.readNV(NvRAMUtils.INDEX_SIM2_IMEI, NvRAMUtils.SIM2_IMEI_LENGTH);
        imei = bytesToString(bytes, mSim2ImeiLength);
        Log.d(this, "getSim2ImeiFromNv=>imei: " + imei);
        if (!isLegitimateImei(imei, mSim2ImeiLength)) {
            imei = "";
        }
        return imei.trim();
    }

    public String getMeidFromNv() {
        String meid = null;
        byte[] bytes = NvRAMUtils.readNV(NvRAMUtils.INDEX_MEID, NvRAMUtils.MEID_LENGTH);
        meid = bytesToString(bytes, mMeidLength);
        Log.d(this, "getMeidFromNv=>meid: " + meid);
        if (!isLegitimateMeid(meid, mMeidLength)) {
            meid = "";
        }
        return meid.trim();
    }

    public int getSettingCount(String sim1Imei, String sim2Imei, String meid) {
        Log.d(this, "getSettingCount=>imei1: " + sim1Imei + " imei2: " + sim2Imei + " meid: " + meid);
        int count = 0;

        if (needSettingSim1Imei(sim1Imei)) {
            count++;
        }
        if (needSettingSim2Imei(sim2Imei)) {
            count++;
        }
        if (needSettingMeid(meid)) {
            count++;
        }
        Log.d(this, "getSettingCount=>count: " + count);
        return count;
    }

    public boolean checkSim1Imei(String imei, boolean showTip) {
        boolean result = true;
        if (mEnabledSettingSim1Imei) {
            if (mEnabledRespectivelySet) {
                if (!TextUtils.isEmpty(imei) && imei.length() != mSim1ImeiLength) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim1_imei_length_limit_tip, mSim1ImeiLength), Toast.LENGTH_SHORT).show();
                    }
                } else if ("000000000000000".equals(imei)) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim1_imei_value_limit_tip, "000000000000000"), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (imei.length() != mSim1ImeiLength) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim1_imei_length_limit_tip, mSim1ImeiLength), Toast.LENGTH_SHORT).show();
                    }
                } else if ("000000000000000".equals(imei)) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim1_imei_value_limit_tip, "000000000000000"), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        return result;
    }

    public boolean checkSim2Imei(String imei, boolean showTip) {
        boolean result = true;
        if (mEnabledSettingSim2Imei) {
            if (mEnabledRespectivelySet) {
                if (!TextUtils.isEmpty(imei) && imei.length() != mSim2ImeiLength) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim2_imei_length_limit_tip, mSim2ImeiLength), Toast.LENGTH_SHORT).show();
                    }
                } else if ("000000000000000".equals(imei)) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim2_imei_value_limit_tip, "000000000000000"), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (imei.length() != mSim2ImeiLength) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim2_imei_length_limit_tip, mSim2ImeiLength), Toast.LENGTH_SHORT).show();
                    }
                } else if ("000000000000000".equals(imei)) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim2_imei_value_limit_tip, "000000000000000"), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        return result;
    }

    public boolean checkMeid(String meid, boolean showTip) {
        boolean result = true;
        if (mEnabledSettingMeid && mIsC2KProject) {
            if (mEnabledRespectivelySet) {
                if (!TextUtils.isEmpty(meid) && meid.length() != mMeidLength) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.meid_length_limit_tip, mMeidLength), Toast.LENGTH_SHORT).show();
                    }
                } else if ("00000000000000".equals(meid)) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.meid_value_limit_tip, "00000000000000"), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (meid.length() != mMeidLength) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.meid_length_limit_tip, mMeidLength), Toast.LENGTH_SHORT).show();
                    }
                } else if ("00000000000000".equals(meid)) {
                    result = false;
                    if (showTip) {
                        Toast.makeText(mContext, mContext.getString(R.string.meid_value_limit_tip, "00000000000000"), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        return result;
    }

    public boolean needSettingSim1Imei(String imei) {
        boolean need = false;
        if (mEnabledSettingSim1Imei) {
            if (imei.length() == mSim1ImeiLength) {
                need = true;
            }
        }
        return need;
    }

    public boolean needSettingSim2Imei(String imei) {
        boolean need = false;
        if (mEnabledSettingSim2Imei) {
            if (imei.length() == mSim2ImeiLength) {
                need = true;
            }
        }
        return need;
    }

    public boolean needSettingMeid(String meid) {
        boolean need = false;
        if (mEnabledSettingMeid && mIsC2KProject) {
            if (meid.length() == mMeidLength) {
                need = true;
            }
        }
        return need;
    }

    public byte[] stringToBytes(String str, int length) {
        if (str == null || "".equals(str)) {
            return null;
        }
        int len = length / 2;
        if (length % 2 != 0) {
            len++;
        }
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; i < str.length() && j < len; i += 2, j++) {
            if (i + 1 >= str.length()) {
                bytes[j] = (byte) str.charAt(i);
            } else {
                bytes[j] = mergeByte(str.charAt(i), str.charAt(i + 1));
            }
        }
        return bytes;
    }

    public byte mergeByte(char first, char second) {
        int f = Integer.parseInt(String.valueOf(first), 16);
        int s = Integer.parseInt(String.valueOf(second), 16);
        int result = f << 4;
        result |= s;
        return (byte) (result & 0xFF);
    }

    public String bytesToString(byte[] bytes, int length) {
        String result = "";
        if (bytes != null && bytes.length != 0) {
            byte[] realBytes = new byte[length];
            for (int i = 0, j = 0; i < bytes.length && j < length; i++, j += 2) {
                if (j + 1 >= length) {
                    realBytes[j] = bytes[i];
                } else {
                    realBytes[j] = getHightByte(bytes[i]);
                    realBytes[j + 1] = getLowByte(bytes[i]);
                }
            }
            if (!isUninitializedValue(realBytes)) {
                result = new String(result).trim().toUpperCase();
            }
        }
        return result;
    }

    public boolean isUninitializedValue(byte[] bytes) {
        boolean result = true;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != 0) {
                result = false;
            }
        }
        return result;
    }

    public byte getHightByte(byte b) {
        return Integer.toString(((b & 0xFF) >>> 4), 16).getBytes()[0];
    }

    public byte getLowByte(byte b) {
        return Integer.toString((b & 0xF), 16).getBytes()[0];
    }

    public boolean isLegitimateImei(String imei, int length) {
        String regex = "[0-9]*";
        return (imei != null && imei.length() == length && Pattern.compile(regex).matcher(imei).matches());
    }

    public boolean isLegitimateMeid(String meid, int length) {
        String regex = "[0-9a-fA-F]*";
        return (meid != null && meid.length() == length && Pattern.compile(regex).matcher(meid).matches());
    }

    public Phone getCDMAPhone() {
        Phone phone = null;
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            Phone defaultPhone = PhoneFactory.getDefaultPhone();
            if (defaultPhone instanceof SvltePhoneProxy) {
                SvltePhoneProxy spp = (SvltePhoneProxy) defaultPhone;
                if (spp.getNLtePhone() != null) {
                    Log.d(this, "getCDMAPhone=>NLtePhone...");
                    phone = spp.getNLtePhone();
                    if (!phone.isRadioAvailable()) {
                        spp.toggleActivePhone(3);
                    }
                } else {
                    phone = defaultPhone;
                }
            }
        } else {
            phone = PhoneFactory.getCdmaPhone();
        }
        return phone;
    }

    public Phone getGSMPhone() {
        Phone phone = null;
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            Phone defaultPhone = PhoneFactory.getDefaultPhone();
            if (defaultPhone instanceof SvltePhoneProxy) {
                SvltePhoneProxy spp = (SvltePhoneProxy) defaultPhone;
                if (spp.getNLtePhone() != null) {
                    Log.d(this, "getGSMPhone=>LtePhone...");
                    phone = spp.getLtePhone();
                    if (!phone.isRadioAvailable()) {
                        spp.toggleActivePhone(2);
                    }
                } else {
                    phone = defaultPhone;
                }
            }
        } else {
            phone = PhoneFactory.getGsmPhone();
        }
        return phone;
    }

    public boolean needSwapImei() {
        String swap = SystemProperties.get("persist.radio.simswitch", "0");
        return swap.equals("2");
    }

    public String getDeviceId(int slotId) {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (mIsC2KProject) {
            try {
                ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
                if (iTel != null) {
                    if (slotId >= 0 && slotId < tm.getPhoneCount()) {
                        return iTel.getSvlteImei(slotId);
                    }
                }
            } catch (RemoteException e) {
                Log.e(this, "getDeviceId=>error: ", e);
            }
        } else {
            if (slotId >= 0 && slotId < tm.getPhoneCount()) {
                return PhoneFactory.getPhone(slotId).getImei();
            }
        }
        return null;
    }

    public String getMeid() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (mIsC2KProject) {
            try {
                ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
                if (iTel != null) {
                    return iTel.getMeid();
                }
            } catch (RemoteException e) {
                Log.e(this, "getDeviceId=>error: ", e);
            }
        }
        return null;
    }

    public boolean clearNV() {
        boolean result = false;
        result = NvRAMUtils.writeNV(NvRAMUtils.INDEX_SIM1_IMEI, new byte[NvRAMUtils.INDEX_MEID + NvRAMUtils.MEID_LENGTH - NvRAMUtils.INDEX_SIM1_IMEI]);
        return result;
    }
}
