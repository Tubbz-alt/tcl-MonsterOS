LOCAL_PATH:= $(call my-dir)

# Build the Phone app which includes the emergency dialer. See Contacts
# for the 'other' dialer.
include $(CLEAR_VARS)

phone_common_dir := ../PhoneCommon

src_dirs := src $(phone_common_dir)/src sip/src tct_src
res_dirs := res $(phone_common_dir)/res sip/res

LOCAL_JAVA_LIBRARIES := telephony-common voip-common ims-common telephony-ext
LOCAL_STATIC_JAVA_LIBRARIES := \
        guava \
        ims-ext-common

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_SRC_FILES += \
        src/com/android/phone/EventLogTags.logtags \
        src/com/android/phone/INetworkQueryService.aidl \
        src/com/android/phone/INetworkQueryServiceCallback.aidl
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.phone.common \
    --extra-packages com.android.services.telephony.sip

# 引用mst-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/mst-framework-res_intermediates/package-export.apk
 
# 引用mst-framework的类
LOCAL_JAVA_LIBRARIES += mst-framework

LOCAL_PACKAGE_NAME := Monster_Telephony

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags sip/proguard.flags

include frameworks/base/packages/SettingsLib/common.mk

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PLF)
include $(BUILD_PACKAGE)

# Build the test package
include $(call all-makefiles-under,$(LOCAL_PATH))