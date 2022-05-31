LOCAL_PATH := $(call my-dir)

ifeq ($(call is-vendor-board-platform,QCOM),true)

include $(CLEAR_VARS)

LOCAL_MODULE_RELATIVE_PATH := hw

LOCAL_SHARED_LIBRARIES := \
    liblog \
    libcutils \
    libdl \
    libbase \
    libutils \
    android.hardware.power-V1-ndk_platform \
    libbinder_ndk

LOCAL_HEADER_LIBRARIES := \
    libhardware_headers

LOCAL_SRC_FILES := \
    power-common.c \
    metadata-parser.c \
    utils.c \
    list.c \
    hint-data.c \
    Power.cpp \
    main.cpp

LOCAL_CFLAGS += -Wall -Wextra -Werror

LOCAL_SRC_FILES += power-8996.c

ifneq ($(TARGET_TAP_TO_WAKE_NODE),)
    LOCAL_CFLAGS += -DTAP_TO_WAKE_NODE=\"$(TARGET_TAP_TO_WAKE_NODE)\"
endif

ifeq ($(TARGET_USES_INTERACTION_BOOST),true)
    LOCAL_CFLAGS += -DINTERACTION_BOOST
endif

LOCAL_MODULE := android.hardware.power-service-x2
LOCAL_INIT_RC := android.hardware.power-service-x2.rc
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS += -Wno-unused-parameter -Wno-unused-variable
LOCAL_VENDOR_MODULE := true
LOCAL_VINTF_FRAGMENTS := android.hardware.power-service-x2.xml

include $(BUILD_EXECUTABLE)
endif
