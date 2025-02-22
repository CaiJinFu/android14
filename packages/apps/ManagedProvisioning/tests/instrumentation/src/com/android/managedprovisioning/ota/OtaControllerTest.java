/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.managedprovisioning.ota;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Pair;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

import com.android.managedprovisioning.analytics.ProvisioningAnalyticsTracker;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import com.android.managedprovisioning.task.DisallowAddUserTask;
import com.android.managedprovisioning.task.InstallExistingPackageTask;
import com.android.managedprovisioning.task.MigrateSystemAppsSnapshotTask;
import com.android.managedprovisioning.task.UpdateInteractAcrossProfilesAppOpTask;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Unit tests for {@link OtaController}.
 */
@SmallTest
public class OtaControllerTest {
    private static final int MANAGED_PROFILE_USER_ID = 15;
    private static final int MANAGED_USER_USER_ID = 18;

    private static final String DUMMY_SYSTEM_IME_PACKAGE_NAME =
            "com.android.inputmethod.dummysystemime";

    private static final ComponentName ADMIN_COMPONENT = new ComponentName("com.test.admin",
            ".AdminReceiver");

    private static final IntFunction<ArraySet<String>> NO_MISSING_SYSTEM_IME_PROVIDER
            = userHandle -> new ArraySet<>();

    @Mock private Context mContext;
    @Mock private DevicePolicyManager mDevicePolicyManager;
    @Mock private PackageManager mPackageManager;
    @Mock private UserManager mUserManager;
    @Mock private ProvisioningAnalyticsTracker mProvisioningAnalyticsTracker;

    private TaskExecutor mTaskExecutor;

    private List<Pair<Integer, AbstractProvisioningTask>> mTasks = new ArrayList<>();
    private List<UserInfo> mUsers = new ArrayList<>();
    private List<UserInfo> mProfiles = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getResources())
                .thenReturn(InstrumentationRegistry.getTargetContext().getResources());

        when(mUserManager.getUsers()).thenReturn(mUsers);
        when(mUserManager.getProfiles(UserHandle.USER_SYSTEM)).thenReturn(mProfiles);

        mTaskExecutor = new FakeTaskExecutor();

        addSystemUser();
    }

    @Test
    public void testDeviceOwnerSystemUser() {
        OtaController controller = new OtaController(mContext, mTaskExecutor,
                NO_MISSING_SYSTEM_IME_PROVIDER,
                mProvisioningAnalyticsTracker);

        // GIVEN that there is a device owner on the system user
        setDeviceOwner(UserHandle.USER_SYSTEM, ADMIN_COMPONENT);

        // WHEN running the OtaController
        controller.run();

        // THEN the task list should contain these tasks.
        assertTaskList(
                Pair.create(UserHandle.USER_SYSTEM, MigrateSystemAppsSnapshotTask.class),
                Pair.create(UserHandle.USER_SYSTEM, DeleteNonRequiredAppsTask.class),
                Pair.create(UserHandle.USER_SYSTEM, DisallowAddUserTask.class),
                Pair.create(UserHandle.USER_SYSTEM, UpdateInteractAcrossProfilesAppOpTask.class));

        // THEN cross profile intent filters setter should be invoked for system user
        verify(mDevicePolicyManager).resetDefaultCrossProfileIntentFilters(UserHandle.USER_SYSTEM);
    }

    @Test
    public void testManagedProfileWithoutMissingSystemIme() {
        OtaController controller = new OtaController(mContext, mTaskExecutor,
                NO_MISSING_SYSTEM_IME_PROVIDER,
                mProvisioningAnalyticsTracker);

        // GIVEN that there is a managed profile
        addManagedProfile(MANAGED_PROFILE_USER_ID, ADMIN_COMPONENT);

        // WHEN running the OtaController
        controller.run();

        // THEN the task list should contain these tasks.
        assertTaskList(
                Pair.create(UserHandle.USER_SYSTEM, MigrateSystemAppsSnapshotTask.class),
                Pair.create(MANAGED_PROFILE_USER_ID, InstallExistingPackageTask.class),
                Pair.create(MANAGED_PROFILE_USER_ID, DeleteNonRequiredAppsTask.class),
                Pair.create(UserHandle.USER_SYSTEM, UpdateInteractAcrossProfilesAppOpTask.class));

        // THEN the cross profile intent filters should be reset
        verify(mDevicePolicyManager).resetDefaultCrossProfileIntentFilters(UserHandle.USER_SYSTEM);
        verify(mDevicePolicyManager, never()).resetDefaultCrossProfileIntentFilters(
                MANAGED_PROFILE_USER_ID);

        // THEN the DISALLOW_WALLPAPER restriction should be set
        verify(mUserManager).setUserRestriction(UserManager.DISALLOW_WALLPAPER, true,
                UserHandle.of(MANAGED_PROFILE_USER_ID));
    }

    @Test
    public void testManagedProfileWithMissingSystemIme() {
        IntFunction<ArraySet<String>> missingSystemImeProvider =
                (IntFunction<ArraySet<String>>) mock(IntFunction.class);

        OtaController controller = new OtaController(mContext, mTaskExecutor,
                missingSystemImeProvider,
                mProvisioningAnalyticsTracker);

        // GIVEN that there is a managed profile
        addManagedProfile(MANAGED_PROFILE_USER_ID, ADMIN_COMPONENT);

        // GIVEN that the managed profile does not have DUMMY_SYSTEM_IME_PACKAGE_NAME.
        ArraySet<String> missingImes = new ArraySet<>();
        missingImes.add(DUMMY_SYSTEM_IME_PACKAGE_NAME);
        when(missingSystemImeProvider.apply(MANAGED_PROFILE_USER_ID)).thenReturn(missingImes);

        // WHEN running the OtaController
        controller.run();

        // THEN the task list should contain these tasks.
        assertTaskList(
                Pair.create(UserHandle.USER_SYSTEM, MigrateSystemAppsSnapshotTask.class),
                Pair.create(MANAGED_PROFILE_USER_ID, InstallExistingPackageTask.class),
                Pair.create(MANAGED_PROFILE_USER_ID, InstallExistingPackageTask.class),

                Pair.create(MANAGED_PROFILE_USER_ID, DeleteNonRequiredAppsTask.class),
                Pair.create(UserHandle.USER_SYSTEM, UpdateInteractAcrossProfilesAppOpTask.class));

        // THEN the cross profile intent filters should be reset
        verify(mDevicePolicyManager).resetDefaultCrossProfileIntentFilters(UserHandle.USER_SYSTEM);
        verify(mDevicePolicyManager, never()).resetDefaultCrossProfileIntentFilters(
                MANAGED_PROFILE_USER_ID);

        // THEN the DISALLOW_WALLPAPER restriction should be set
        verify(mUserManager).setUserRestriction(UserManager.DISALLOW_WALLPAPER, true,
                UserHandle.of(MANAGED_PROFILE_USER_ID));

        // THEN the DUMMY_SYSTEM_IME_PACKAGE_NAME should be installed.
        assertInstallExistingPackageTask(MANAGED_PROFILE_USER_ID, DUMMY_SYSTEM_IME_PACKAGE_NAME);
    }

    @Test
    public void testManagedUser() {
        OtaController controller = new OtaController(mContext, mTaskExecutor,
                NO_MISSING_SYSTEM_IME_PROVIDER, mProvisioningAnalyticsTracker);

        // GIVEN that there is a managed profile
        addManagedUser(MANAGED_USER_USER_ID, ADMIN_COMPONENT);

        // WHEN running the OtaController
        controller.run();

        // THEN the task list should contain these tasks.
        assertTaskList(
                Pair.create(UserHandle.USER_SYSTEM, MigrateSystemAppsSnapshotTask.class),
                Pair.create(MANAGED_USER_USER_ID, DeleteNonRequiredAppsTask.class),
                Pair.create(UserHandle.USER_SYSTEM, UpdateInteractAcrossProfilesAppOpTask.class));
    }

    private class FakeTaskExecutor extends TaskExecutor {

        public FakeTaskExecutor() {
            super();
        }

        @Override
        public synchronized void execute(int userId, AbstractProvisioningTask task) {
            mTasks.add(Pair.create(userId, task));
        }
    }

    private void setDeviceOwner(int userId, ComponentName admin) {
        when(mDevicePolicyManager.getDeviceOwnerUserId()).thenReturn(userId);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(admin);
    }

    private void addManagedProfile(int userId, ComponentName admin) {
        UserInfo ui = new UserInfo(userId, null, UserInfo.FLAG_MANAGED_PROFILE);
        mUsers.add(ui);
        when(mDevicePolicyManager.getProfileOwnerAsUser(userId)).thenReturn(admin);
        when(mUserManager.getProfiles(userId)).thenReturn(Collections.singletonList(ui));
        mProfiles.add(ui);
    }

    private void addManagedUser(int userId, ComponentName admin) {
        UserInfo ui = new UserInfo(userId, null, 0);
        mUsers.add(ui);
        when(mDevicePolicyManager.getProfileOwnerAsUser(userId)).thenReturn(admin);
    }

    private void addSystemUser() {
        UserInfo ui = new UserInfo(UserHandle.USER_SYSTEM, null, UserInfo.FLAG_PRIMARY);
        mUsers.add(ui);
        mProfiles.add(ui);
    }

    private void assertTaskList(Pair<Integer, Class>... tasks) {
        assertEquals(tasks.length, mTasks.size());

        for (Pair<Integer, Class> task : tasks) {
            assertTaskListContains(task.first, task.second);
        }
    }

    private void assertTaskListContains(Integer userId, Class taskClass) {
        for (Pair<Integer, AbstractProvisioningTask> task : mTasks) {
            if (Objects.equals(userId, task.first) && taskClass.isInstance(task.second)) {
                return;
            }
        }
        fail("Task for class " + taskClass + " and userId " + userId + " not executed");
    }

    private void assertInstallExistingPackageTask(int userId, String packageName) {
        for (Pair<Integer, AbstractProvisioningTask> task : mTasks) {
            if (userId != task.first || !InstallExistingPackageTask.class.isInstance(task.second)) {
                continue;
            }
            InstallExistingPackageTask installExistingPackageTask =
                    (InstallExistingPackageTask) task.second;
            if (packageName.equals(installExistingPackageTask.getPackageName())) {
                return;
            }
        }
        fail("InstallExistingPackageTask for " + packageName + " and userId " + userId
                + " not executed");
    }
}
