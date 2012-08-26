LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS   = -static
#LOCAL_CFLAGS   = -Wall -g

LOCAL_MODULE    := pkgdetails
LOCAL_SRC_FILES := pkgdetails.c

include $(BUILD_EXECUTABLE)
