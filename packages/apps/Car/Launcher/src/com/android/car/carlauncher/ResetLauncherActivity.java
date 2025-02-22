/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.car.carlauncher;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

import com.android.car.ui.AlertDialogBuilder;

import java.io.File;
/**
 * Activity that shows different dialogs from the device default theme.
 */
public class ResetLauncherActivity extends Activity {
    private AlertDialog mDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDialog = createResetLauncherDialog();
        mDialog.show();
    }
    private AlertDialog createResetLauncherDialog() {
        File filesDir =  getFilesDir();
        return new AlertDialogBuilder(/* context= */ this)
                .setTitle(getString(R.string.reset_appgrid_title))
                .setMessage(getString(R.string.reset_appgrid_dialogue_message))
                .setPositiveButton(getString(android.R.string.ok), (dialogInterface, which) -> {
                    File order = new File(filesDir, LauncherViewModel.ORDER_FILE_NAME);
                    order.delete();
                    finish();
                })
                .setNegativeButton(getString(android.R.string.cancel), (dialogInterface, which) -> {
                    finish();
                }).create();
    }
}
