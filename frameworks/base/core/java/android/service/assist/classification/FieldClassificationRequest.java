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

package android.service.assist.classification;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.app.assist.AssistStructure;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.DataClass;

/**
 * Represents a request to detect fields on an activity.
 * @hide
 */
@SystemApi
@DataClass(
        genToString = true
)
public final class FieldClassificationRequest implements Parcelable {
    private final @NonNull AssistStructure mAssistStructure;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/core/java/android/service/assist/classification/FieldClassificationRequest.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    public FieldClassificationRequest(
            @NonNull AssistStructure assistStructure) {
        this.mAssistStructure = assistStructure;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mAssistStructure);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public @NonNull AssistStructure getAssistStructure() {
        return mAssistStructure;
    }

    @Override
    @DataClass.Generated.Member
    public String toString() {
        // You can override field toString logic by defining methods like:
        // String fieldNameToString() { ... }

        return "FieldClassificationRequest { " +
                "assistStructure = " + mAssistStructure +
        " }";
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        dest.writeTypedObject(mAssistStructure, flags);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ FieldClassificationRequest(@NonNull Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        AssistStructure assistStructure = (AssistStructure) in.readTypedObject(AssistStructure.CREATOR);

        this.mAssistStructure = assistStructure;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mAssistStructure);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<FieldClassificationRequest> CREATOR
            = new Parcelable.Creator<FieldClassificationRequest>() {
        @Override
        public FieldClassificationRequest[] newArray(int size) {
            return new FieldClassificationRequest[size];
        }

        @Override
        public FieldClassificationRequest createFromParcel(@NonNull Parcel in) {
            return new FieldClassificationRequest(in);
        }
    };

    @DataClass.Generated(
            time = 1675320491692L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/core/java/android/service/assist/classification/FieldClassificationRequest.java",
            inputSignatures = "private final @android.annotation.NonNull android.app.assist.AssistStructure mAssistStructure\nclass FieldClassificationRequest extends java.lang.Object implements [android.os.Parcelable]\n@com.android.internal.util.DataClass(genToString=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}