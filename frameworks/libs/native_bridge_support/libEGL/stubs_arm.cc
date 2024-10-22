//
// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

// clang-format off
#include "native_bridge_support/vdso/interceptable_functions.h"

DEFINE_INTERCEPTABLE_STUB_FUNCTION(_Z13eglBeginFramePvS_);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t10initializeEPNS_13egl_display_tE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t10updateModeEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t12getCacheSizeEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t12setCacheModeENS0_12EGLCacheModeE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t13setCacheLimitEx);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t16setCacheFilenameEPKc);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t18getBlobCacheLockedEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t27getMultifileBlobCacheLockedEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t3getEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t7getBlobEPKvlPvl);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t7setBlobEPKvlS2_l);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_t9terminateEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_tC2Ev);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android11egl_cache_tD2Ev);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t10initializeEPiS1_);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t11loseCurrentEPNS_13egl_context_tE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t11makeCurrentEPNS_13egl_context_tES2_PvS3_S3_S3_S3_S3_);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t12removeObjectEPNS_12egl_object_tE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t15loseCurrentImplEPNS_13egl_context_tE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t18getPlatformDisplayEPvPKi);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t20getFromNativeDisplayEPvPKi);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t3getEPv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t9addObjectEPNS_12egl_object_tE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_t9terminateEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_tC2Ev);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android13egl_display_tD2Ev);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android17eglBeginFrameImplEPvS0_);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android18egl_get_connectionEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android18egl_get_init_countEPv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android19setGlThreadSpecificEPKNS_10gl_hooks_tE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android22egl_set_cache_filenameEPKc);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android34egl_get_string_for_current_contextEj);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android34egl_get_string_for_current_contextEjj);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android39eglQueryStringImplementationANDROIDImplEPvi);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZN7android42egl_get_num_extensions_for_current_contextEv);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZNK7android13egl_display_t13haveExtensionEPKcj);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(_ZNK7android13egl_display_t9getObjectEPNS_12egl_object_tE);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglBindAPI);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglBindTexImage);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglChooseConfig);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglClientWaitSync);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglClientWaitSyncKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCopyBuffers);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateContext);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateImage);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateImageKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreatePbufferFromClientBuffer);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreatePbufferSurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreatePixmapSurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreatePlatformPixmapSurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreatePlatformWindowSurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateStreamFromFileDescriptorKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateStreamKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateStreamProducerSurfaceKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateSync);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateSyncKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglCreateWindowSurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDestroyContext);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDestroyImage);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDestroyImageKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDestroyStreamKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDestroySurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDestroySync);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDestroySyncKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglDupNativeFenceFDANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetCompositorTimingANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetCompositorTimingSupportedANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetConfigAttrib);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetConfigs);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetCurrentContext);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetCurrentDisplay);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetCurrentSurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetDisplay);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetError);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetFrameTimestampSupportedANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetFrameTimestampsANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetNativeClientBufferANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetNextFrameIdANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetPlatformDisplay);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetProcAddress);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetStreamFileDescriptorKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetSyncAttrib);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetSyncAttribKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetSystemTimeFrequencyNV);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglGetSystemTimeNV);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglInitialize);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglLockSurfaceKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglMakeCurrent);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglPresentationTimeANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQueryAPI);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQueryContext);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQueryStreamKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQueryStreamTimeKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQueryStreamu64KHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQueryString);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQueryStringImplementationANDROID);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglQuerySurface);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglReleaseTexImage);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglReleaseThread);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglSetDamageRegionKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglSignalSyncKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglStreamAttribKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglStreamConsumerAcquireKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglStreamConsumerGLTextureExternalKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglStreamConsumerReleaseKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglSurfaceAttrib);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglSwapBuffers);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglSwapBuffersWithDamageKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglSwapInterval);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglTerminate);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglUnlockSurfaceKHR);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglWaitClient);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglWaitGL);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglWaitNative);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglWaitSync);
DEFINE_INTERCEPTABLE_STUB_FUNCTION(eglWaitSyncKHR);

static void __attribute__((constructor(0))) init_stub_library() {
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _Z13eglBeginFramePvS_);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t10initializeEPNS_13egl_display_tE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t10updateModeEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t12getCacheSizeEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t12setCacheModeENS0_12EGLCacheModeE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t13setCacheLimitEx);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t16setCacheFilenameEPKc);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t18getBlobCacheLockedEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t27getMultifileBlobCacheLockedEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t3getEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t7getBlobEPKvlPvl);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t7setBlobEPKvlS2_l);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_t9terminateEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_tC2Ev);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android11egl_cache_tD2Ev);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t10initializeEPiS1_);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t11loseCurrentEPNS_13egl_context_tE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t11makeCurrentEPNS_13egl_context_tES2_PvS3_S3_S3_S3_S3_);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t12removeObjectEPNS_12egl_object_tE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t15loseCurrentImplEPNS_13egl_context_tE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t18getPlatformDisplayEPvPKi);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t20getFromNativeDisplayEPvPKi);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t3getEPv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t9addObjectEPNS_12egl_object_tE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_t9terminateEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_tC2Ev);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android13egl_display_tD2Ev);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android17eglBeginFrameImplEPvS0_);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android18egl_get_connectionEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android18egl_get_init_countEPv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android19setGlThreadSpecificEPKNS_10gl_hooks_tE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android22egl_set_cache_filenameEPKc);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android34egl_get_string_for_current_contextEj);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android34egl_get_string_for_current_contextEjj);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android39eglQueryStringImplementationANDROIDImplEPvi);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZN7android42egl_get_num_extensions_for_current_contextEv);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZNK7android13egl_display_t13haveExtensionEPKcj);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", _ZNK7android13egl_display_t9getObjectEPNS_12egl_object_tE);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglBindAPI);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglBindTexImage);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglChooseConfig);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglClientWaitSync);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglClientWaitSyncKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCopyBuffers);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateContext);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateImage);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateImageKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreatePbufferFromClientBuffer);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreatePbufferSurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreatePixmapSurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreatePlatformPixmapSurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreatePlatformWindowSurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateStreamFromFileDescriptorKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateStreamKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateStreamProducerSurfaceKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateSync);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateSyncKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglCreateWindowSurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDestroyContext);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDestroyImage);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDestroyImageKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDestroyStreamKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDestroySurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDestroySync);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDestroySyncKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglDupNativeFenceFDANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetCompositorTimingANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetCompositorTimingSupportedANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetConfigAttrib);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetConfigs);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetCurrentContext);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetCurrentDisplay);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetCurrentSurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetDisplay);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetError);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetFrameTimestampSupportedANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetFrameTimestampsANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetNativeClientBufferANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetNextFrameIdANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetPlatformDisplay);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetProcAddress);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetStreamFileDescriptorKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetSyncAttrib);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetSyncAttribKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetSystemTimeFrequencyNV);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglGetSystemTimeNV);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglInitialize);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglLockSurfaceKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglMakeCurrent);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglPresentationTimeANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQueryAPI);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQueryContext);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQueryStreamKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQueryStreamTimeKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQueryStreamu64KHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQueryString);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQueryStringImplementationANDROID);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglQuerySurface);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglReleaseTexImage);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglReleaseThread);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglSetDamageRegionKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglSignalSyncKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglStreamAttribKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglStreamConsumerAcquireKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglStreamConsumerGLTextureExternalKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglStreamConsumerReleaseKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglSurfaceAttrib);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglSwapBuffers);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglSwapBuffersWithDamageKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglSwapInterval);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglTerminate);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglUnlockSurfaceKHR);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglWaitClient);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglWaitGL);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglWaitNative);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglWaitSync);
  INIT_INTERCEPTABLE_STUB_FUNCTION("libEGL.so", eglWaitSyncKHR);
}
// clang-format on
