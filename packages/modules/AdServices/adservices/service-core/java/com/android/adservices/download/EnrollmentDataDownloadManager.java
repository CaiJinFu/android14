/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.adservices.download;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.adservices.LogUtil;
import com.android.adservices.data.enrollment.EnrollmentDao;
import com.android.adservices.service.Flags;
import com.android.adservices.service.FlagsFactory;
import com.android.adservices.service.enrollment.EnrollmentData;

import com.google.android.libraries.mobiledatadownload.GetFileGroupRequest;
import com.google.android.libraries.mobiledatadownload.MobileDataDownload;
import com.google.android.libraries.mobiledatadownload.file.SynchronousFileStorage;
import com.google.android.libraries.mobiledatadownload.file.openers.ReadStreamOpener;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mobiledatadownload.ClientConfigProto.ClientFile;
import com.google.mobiledatadownload.ClientConfigProto.ClientFileGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/** Handles EnrollmentData download from MDD server to device. */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public class EnrollmentDataDownloadManager {
    private final Context mContext;
    private static volatile EnrollmentDataDownloadManager sEnrollmentDataDownloadManager;
    private final MobileDataDownload mMobileDataDownload;
    private final SynchronousFileStorage mFileStorage;

    private static final String GROUP_NAME = "adtech_enrollment_data";
    private static final String DOWNLOADED_ENROLLMENT_DATA_FILE_ID = "adtech_enrollment_data.csv";
    private static final String ENROLLMENT_FILE_READ_STATUS_SHARED_PREFERENCES =
            "enrollment_data_read_status";

    @VisibleForTesting
    EnrollmentDataDownloadManager(Context context, Flags flags) {
        mContext = context.getApplicationContext();
        mMobileDataDownload = MobileDataDownloadFactory.getMdd(context, flags);
        mFileStorage = MobileDataDownloadFactory.getFileStorage(context);
    }

    /** Gets an instance of EnrollmentDataDownloadManager to be used. */
    public static EnrollmentDataDownloadManager getInstance(@NonNull Context context) {
        if (sEnrollmentDataDownloadManager == null) {
            synchronized (EnrollmentDataDownloadManager.class) {
                if (sEnrollmentDataDownloadManager == null) {
                    sEnrollmentDataDownloadManager =
                            new EnrollmentDataDownloadManager(context, FlagsFactory.getFlags());
                }
            }
        }
        return sEnrollmentDataDownloadManager;
    }

    /**
     * Find, open and read the enrollment data file from MDD and only insert new data into the
     * enrollment database.
     */
    public ListenableFuture<DownloadStatus> readAndInsertEnrolmentDataFromMdd() {
        LogUtil.d("Reading MDD data from file.");
        Pair<ClientFile, String> FileGroupAndBuildIdPair = getEnrollmentDataFile();
        if (FileGroupAndBuildIdPair == null || FileGroupAndBuildIdPair.first == null) {
            return Futures.immediateFuture(DownloadStatus.NO_FILE_AVAILABLE);
        }

        ClientFile enrollmentDataFile = FileGroupAndBuildIdPair.first;
        String fileGroupBuildId = FileGroupAndBuildIdPair.second;
        SharedPreferences sharedPrefs =
                mContext.getSharedPreferences(
                        ENROLLMENT_FILE_READ_STATUS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        if (sharedPrefs.getBoolean(fileGroupBuildId, false)) {
            LogUtil.d(
                    "Enrollment data build id = %s has been saved into DB. Skip adding same data.",
                    fileGroupBuildId);
            return Futures.immediateFuture(DownloadStatus.SKIP);
        }

        if (readDownloadedFile(enrollmentDataFile)) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.clear().putBoolean(fileGroupBuildId, true);
            if (!editor.commit()) {
                // TODO(b/280579966): Add logging using CEL.
                LogUtil.e("Saving to the enrollment file read status sharedpreference failed");
            }
            LogUtil.d("Inserted new enrollment data build id = %s into DB.", fileGroupBuildId);
            return Futures.immediateFuture(DownloadStatus.SUCCESS);
        } else {
            return Futures.immediateFuture(DownloadStatus.PARSING_FAILED);
        }
    }

    private boolean readDownloadedFile(ClientFile enrollmentDataFile) {
        LogUtil.d("Inserting MDD data into DB.");
        try {
            InputStream inputStream =
                    mFileStorage.open(
                            Uri.parse(enrollmentDataFile.getFileUri()), ReadStreamOpener.create());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            bufferedReader.readLine();
            String line = null;
            // While loop runs from the second line.
            EnrollmentDao enrollmentDao = EnrollmentDao.getInstance(mContext);
            while ((line = bufferedReader.readLine()) != null) {
                // Constructs EnrollmentData object and save it into DB.
                String[] data = line.split(",");
                if (data.length == 8) {
                    String enrollmentId = data[0];
                    LogUtil.d("Adding enrollmentId - %s", enrollmentId);
                    EnrollmentData enrollmentData =
                            new EnrollmentData.Builder()
                                    .setEnrollmentId(enrollmentId)
                                    .setCompanyId(data[1])
                                    .setSdkNames(data[2])
                                    .setAttributionSourceRegistrationUrl(
                                            data[3].contains(" ")
                                                    ? Arrays.asList(data[3].split(" "))
                                                    : Arrays.asList(data[3]))
                                    .setAttributionTriggerRegistrationUrl(
                                            data[4].contains(" ")
                                                    ? Arrays.asList(data[4].split(" "))
                                                    : Arrays.asList(data[4]))
                                    .setAttributionReportingUrl(
                                            data[5].contains(" ")
                                                    ? Arrays.asList(data[5].split(" "))
                                                    : Arrays.asList(data[5]))
                                    .setRemarketingResponseBasedRegistrationUrl(
                                            data[6].contains(" ")
                                                    ? Arrays.asList(data[6].split(" "))
                                                    : Arrays.asList(data[6]))
                                    .setEncryptionKeyUrl(
                                            data[7].contains(" ")
                                                    ? Arrays.asList(data[7].split(" "))
                                                    : Arrays.asList(data[7]))
                                    .build();
                    enrollmentDao.insert(enrollmentData);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @VisibleForTesting
    public enum DownloadStatus {
        SUCCESS,
        NO_FILE_AVAILABLE,
        PARSING_FAILED,
        // Skip reading and inserting same enrollment data to DB if the data has been saved
        // previously.
        SKIP;
    }

    private Pair<ClientFile, String> getEnrollmentDataFile() {
        GetFileGroupRequest getFileGroupRequest =
                GetFileGroupRequest.newBuilder().setGroupName(GROUP_NAME).build();
        try {
            ListenableFuture<ClientFileGroup> fileGroupFuture =
                    mMobileDataDownload.getFileGroup(getFileGroupRequest);
            ClientFileGroup fileGroup = fileGroupFuture.get();
            if (fileGroup == null) {
                LogUtil.d("MDD has not downloaded the Enrollment Data Files yet.");
                return null;
            }
            String fileGroupBuildId = String.valueOf(fileGroup.getBuildId());
            ClientFile enrollmentDataFile = null;
            for (ClientFile file : fileGroup.getFileList()) {
                if (file.getFileId().equals(DOWNLOADED_ENROLLMENT_DATA_FILE_ID)) {
                    enrollmentDataFile = file;
                }
            }
            return Pair.create(enrollmentDataFile, fileGroupBuildId);

        } catch (ExecutionException | InterruptedException e) {
            LogUtil.e(e, "Unable to load MDD file group.");
            return null;
        }
    }
}
