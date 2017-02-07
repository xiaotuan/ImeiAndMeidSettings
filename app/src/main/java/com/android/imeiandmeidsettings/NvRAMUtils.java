package com.android.imeiandmeidsettings;

import android.os.RemoteException;
import android.os.ServiceManager;

/**
 * Created by zheng_dp on 2016-6-21.
 */
public class NvRAMUtils {
    private static final NvRAMAgent mAgent = NvRAMAgent.Stub.asInterface(ServiceManager.getService("NvRAMAgent"));
    private static final String TAG = "NvRAMUtils";
    //private static byte[] mBuff = null;

    public static final int UNIFIED_LID = 59;
    
    /***** NvRAMUtils - Place your offset below *****/
    //@Description: Ensure there is no duplicated offset declared!
    // NV offset - Please Try to use the index between 1024 - 2047
    public static final int NV_OFFSET = 1024;

    public static final int INDEX_SIM1_IMEI = 64;
    public static final int INDEX_SIM2_IMEI = 74;
    public static final int INDEX_MEID = 84;
    public static final int SIM1_IMEI_LENGTH = 10;
    public static final int SIM2_IMEI_LENGTH = 10;
    public static final int MEID_LENGTH = 10;

    private static final int[] INDEX_LIST = {
            INDEX_SIM1_IMEI,
            INDEX_SIM2_IMEI,
            INDEX_MEID,
    };

    /***** NvRAMUtils - Place your offset above *****/

    /**
     * Read the specific index in NvRAM
     * @param index the position that need to be accessed
     * @return the byte written in the index, -1 when read failed
     */
    public synchronized static byte readNV(int index) {
        /*
        ZDP - We need to ensure the index is clearly declared in the array INDEX_LIST, otherwise this NvRAMUtils is not accessible -
        We cannot throw an Exception directly because this may cause the reduction of User Experience
        */
        byte result = -1;
        if(!verifyIndex(index)) {
            android.util.Log.d(TAG,"Argument: " + index + "is not declared in INDEX_LIST! Read failed");
            return result;
        }
        byte[] buff = readNV();
        if (buff != null && index < buff.length) {
            result = buff[index];
        }
        return result;
    }
    /**
     * Read the specific range in NvRAM
     * @param index the start index that need to be accessed
     * @param length from the start index, the length of the range that need to be accessed
     * @return the byte array written in the specified range, null when read failed
     */
    public synchronized static byte[] readNV(int index, int length){
        if(!verifyIndex(index)) {
            android.util.Log.d(TAG,"Argument: " + index + "is not declared in INDEX_LIST! Read failed");
            return null;
        }
        byte[] buff = readNV();
        byte[] target = new byte[length];
        if(buff != null){
            for(int i = 0 ; i < length; i++){
                target[i] = buff[i+index];
            }
        }
        return target;
    }

    /**
     * Read all the data in NvRAM
     * @return the byte array(usually with length of 2048) containing all the bytes in NvRAM, null when read failed.
     */
    public synchronized static byte[] readNV(){
        /*
         * TODO - Once we ensure all the uses of NvRAMAgent are invoked by this NvRAMUtils, we can use below codes to optimize efficiency
        if(mBuff == null){
            try {
                mBuff = mAgent.readFile(UNIFIED_LID);
            } catch (RemoteException e) {}
        }
        return mBuff;
        */
        byte[] buff = null;
        try {
            buff = mAgent.readFile(UNIFIED_LID);
        } catch (RemoteException e) {
        } catch (ArrayIndexOutOfBoundsException e1){}
        return buff;
    }

    /**
     * write a specified byte in a specified position
     * @param index the position that need to be written
     * @param value the value that need to be written
     * @return true when write succeeded, false when write failed
     */
    public synchronized static boolean writeNV(int index, byte value) {
        /*
        ZDP - We need to ensure the index is clearly declared in the array INDEX_LIST, otherwise this NvRAMUtils is not accessible -
        We cannot throw an Exception directly because this may cause the reduction of User Experience
        */
        if(!verifyIndex(index)) {
            android.util.Log.d(TAG,"Argument: " + index + "is not declared in INDEX_LIST! Write failed");
            return false;
        }
        boolean result = false;
        byte[] buff = readNV();
        if (buff != null && index < buff.length) {
            buff[index] = value;
            result = writeNV(buff);
        }
        return result;
    }

    /**
     * write a specified byte array into NvRAM, start with the specified index
     * @param index the start position that need to be written
     * @param buff the values that need to be written from the start position
     * @return true when write succeeded, false when write failed
     */
    public synchronized static boolean writeNV(int index, byte[] buff){
        if(!verifyIndex(index)) {
            android.util.Log.d(TAG,"Argument: " + index + "is not declared in INDEX_LIST! Write failed");
            return false;
        }
        boolean result = false;
        byte[] origin = readNV();
        if (buff != null && origin != null && (index + buff.length) < origin.length) {
            for(int i = 0 ; i < buff.length; i++){
                origin[index+i] = buff[i];
            }
            result = writeNV(origin);
        }
        return result;
    }

    /**
     * overwrite all the data in NvRAM
     * @param buff the byte array(usually with length of 2048) containing all the bytes in NvRAM
     * @return true when write succeeded, false when write failed
     */
    public synchronized static boolean writeNV(byte[] buff){
        boolean result = false;
        if (buff != null) {
            try{
                if (mAgent.writeFile(UNIFIED_LID, buff) > 0) {
                    result = true;
                }
            } catch (RemoteException e) {}
        }
        return result;
    }

    private synchronized static boolean verifyIndex(int index){
        //ZDP - To check whether the index is declared in the Array INDEX_LIST
        for(int i = 0 ; i < INDEX_LIST.length; i++){
            if(INDEX_LIST[i] == index){
                return true;
            }
        }
        return false;
    }
}
