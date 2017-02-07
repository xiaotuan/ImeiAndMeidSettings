LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_PACKAGE_NAME := ImeiAndMeidSettings
LOCAL_PRIVILEGED_MODULE := true
LOCAL_JAVA_LIBRARIES := telephony-common

include $(BUILD_PACKAGE)
