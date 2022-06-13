/*
 * Copyright (c) 2012-2019, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * *    * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#define LOG_NIDEBUG 0

#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <unistd.h>

#define LOG_TAG "QTI PowerHAL"
#include <utils/Log.h>
#include <hardware/hardware.h>
#include <hardware/power.h>

#include "utils.h"
#include <inttypes.h>
#include "hint-data.h"
#include "performance.h"
#include "power-common.h"
#include <cutils/properties.h>
#define USINSEC 1000000L
#define NSINUS 1000L

static struct hint_handles handles[NUM_HINTS];

void power_init()
{
    ALOGI("Initing");

    for (int i=0; i<NUM_HINTS; i++) {
        handles[i].handle       = 0;
        handles[i].ref_count    = 0;
    }
}

//interaction boost global variables
static pthread_mutex_t s_interaction_lock = PTHREAD_MUTEX_INITIALIZER;
static struct timespec s_previous_boost_timespec;
static int s_previous_duration;

// Create Vox Populi variables
int enable_interaction_boost;
int fling_min_boost_duration;
int fling_max_boost_duration;
int fling_boost_topapp;
int fling_min_freq_big;
int fling_min_freq_little;
int boost_duration;
int touch_boost_topapp;
int touch_min_freq_big;
int touch_min_freq_little;

int __attribute__ ((weak)) power_hint_override(power_hint_t hint,
        void *data)
{
    return HINT_NONE;
}

/* Declare function before use */
void interaction(int duration, int num_args, int opt_list[]);
void release_request(int lock_handle);

static long long calc_timespan_us(struct timespec start, struct timespec end) {
    long long diff_in_us = 0;
    diff_in_us += (end.tv_sec - start.tv_sec) * USINSEC;
    diff_in_us += (end.tv_nsec - start.tv_nsec) / NSINUS;
    return diff_in_us;
}

void power_hint(power_hint_t hint, void *data)
{
    /* Check if this hint has been overridden. */
    if (power_hint_override(hint, data) == HINT_HANDLED) {
        /* The power_hint has been handled. We can skip the rest. */
        return;
    }
    switch(hint) {
        case POWER_HINT_VSYNC:
        break;
        case POWER_HINT_VR_MODE:
            ALOGI("VR mode power hint not handled in power_hint_override");
            break;
        case POWER_HINT_INTERACTION:
        {
            // Check if interaction_boost is enabled
            if (enable_interaction_boost) {
                if (data) { // Boost duration for scrolls/flings
                    int input_duration = *((int*)data) + fling_min_boost_duration;
                    boost_duration = (input_duration > fling_max_boost_duration) ? fling_max_boost_duration : input_duration;
                } 

                struct timespec cur_boost_timespec;
                clock_gettime(CLOCK_MONOTONIC, &cur_boost_timespec);
                long long elapsed_time = calc_timespan_us(s_previous_boost_timespec, cur_boost_timespec);
                // don't hint if previous hint's duration covers this hint's duration
                if ((s_previous_duration * 1000) > (elapsed_time + boost_duration * 1000)) {
                    pthread_mutex_unlock(&s_interaction_lock);
                    return;
                }
                s_previous_boost_timespec = cur_boost_timespec;
                s_previous_duration = boost_duration;

                // Scrolls/flings
                if (data) {
                    int eas_interaction_resources[] = { MIN_FREQ_BIG_CORE_0, fling_min_freq_big, 
                                                        MIN_FREQ_LITTLE_CORE_0, fling_min_freq_little, 
                                                        0x42C0C000, fling_boost_topapp,
                                                        CPUBW_HWMON_MIN_FREQ, 0x33};
                    interaction(boost_duration, sizeof(eas_interaction_resources)/sizeof(eas_interaction_resources[0]), eas_interaction_resources);
                }
                // Touches/taps
                else {
                    int eas_interaction_resources[] = { MIN_FREQ_BIG_CORE_0, touch_min_freq_big, 
                                                        MIN_FREQ_LITTLE_CORE_0, touch_min_freq_little, 
                                                        0x42C0C000, touch_boost_topapp, 
                                                        CPUBW_HWMON_MIN_FREQ, 0x33};
                    interaction(boost_duration, sizeof(eas_interaction_resources)/sizeof(eas_interaction_resources[0]), eas_interaction_resources);
                }
            }
            pthread_mutex_unlock(&s_interaction_lock);

            // Update tunable values again
            get_int(ENABLE_INTERACTION_BOOST_PATH, &enable_interaction_boost, 1);
            get_int(FLING_MIN_BOOST_DURATION_PATH, &fling_min_boost_duration, 300);
            get_int(FLING_MAX_BOOST_DURATION_PATH, &fling_max_boost_duration, 800);
            get_int(FLING_BOOST_TOPAPP_PATH, &fling_boost_topapp, 10);
            get_int(FLING_MIN_FREQ_BIG_PATH, &fling_min_freq_big, 1113);
            get_int(FLING_MIN_FREQ_LITTLE_PATH, &fling_min_freq_little, 1113);
            get_int(TOUCH_BOOST_DURATION_PATH, &boost_duration, 300);
            get_int(TOUCH_BOOST_TOPAPP_PATH, &touch_boost_topapp, 10);
            get_int(TOUCH_MIN_FREQ_BIG_PATH, &touch_min_freq_big, 1113);
            get_int(TOUCH_MIN_FREQ_LITTLE_PATH, &touch_min_freq_little, 1113);
        }
        break;
        //fall through below, hints will fail if not defined in powerhint.xml
        case POWER_HINT_SUSTAINED_PERFORMANCE:
        case POWER_HINT_VIDEO_ENCODE:
            if (data) {
                if (handles[hint].ref_count == 0)
                    handles[hint].handle = perf_hint_enable((AOSP_DELTA + hint), 0);

                if (handles[hint].handle > 0)
                    handles[hint].ref_count++;
            }
            else
                if (handles[hint].handle > 0)
                    if (--handles[hint].ref_count == 0) {
                        release_request(handles[hint].handle);
                        handles[hint].handle = 0;
                    }
                else
                    ALOGE("Lock for hint: %X was not acquired, cannot be released", hint);
        break;
    }
}

int __attribute__ ((weak)) set_interactive_override(int on)
{
    return HINT_NONE;
}

void set_interactive(int on)
{
    if (!on) {
        /* Send Display OFF hint to perf HAL */
        perf_hint_enable(VENDOR_HINT_DISPLAY_OFF, 0);
    } else {
        /* Send Display ON hint to perf HAL */
        perf_hint_enable(VENDOR_HINT_DISPLAY_ON, 0);
    }

    if (set_interactive_override(on) == HINT_HANDLED) {
        return;
    }

    ALOGI("Got set_interactive hint");
}

void get_int(const char* file_path, int* value, int fallback_value) {
    FILE *file;
    file = fopen(file_path, "r");
    if (file == NULL) {
        *value = fallback_value;
        return;
    }
    fscanf(file, "%d", value);
    fclose(file);
}

void get_hex(const char* file_path, int* value, int fallback_value) {
    FILE *file;
    file = fopen(file_path, "r");
    if (file == NULL) {
        *value = fallback_value;
        return;
    }
    fscanf(file, "0x%x", value);
    fclose(file);
}
