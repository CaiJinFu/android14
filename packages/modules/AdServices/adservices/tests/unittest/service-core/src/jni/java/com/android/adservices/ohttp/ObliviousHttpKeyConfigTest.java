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

package com.android.adservices.ohttp;

import com.google.common.io.BaseEncoding;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;

public class ObliviousHttpKeyConfigTest {

    @Test
    public void create_configuresKeyCorrectly() throws InvalidKeySpecException {
        String keyConfigHex =
                "01002031e1f05a740102115220e9af918f738674aec95f54db6e04eb705aae8e798155"
                        + "00080001000100010003";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        ObliviousHttpKeyConfig keyConfig = ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes);

        Assert.assertEquals(keyConfig.keyid(), 0X01);
        Assert.assertEquals(keyConfig.kemid(), 0X0020);
        String publicKeyHex = BaseEncoding.base16().lowerCase().encode(keyConfig.getPublicKey());
        Assert.assertEquals(
                publicKeyHex, "31e1f05a740102115220e9af918f738674aec95f54db6e04eb705aae8e798155");
        Assert.assertEquals(keyConfig.kdfId(), 0X0001);
        Assert.assertEquals(keyConfig.aeadId(), 0X0001);
    }

    @Test
    public void create_emptyKeyConfig_throwsError() throws InvalidKeySpecException {
        String keyConfigHex = "";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        Assert.assertThrows(
                InvalidKeySpecException.class,
                () -> ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes));
    }

    @Test
    public void create_wrongKemIdLength_throwsError() throws InvalidKeySpecException {
        String keyConfigHex = "0100";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        Assert.assertThrows(
                InvalidKeySpecException.class,
                () -> ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes));
    }

    @Test
    public void create_unsupportedKemId_throwsError() throws InvalidKeySpecException {
        String keyConfigHex = "010002";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        Assert.assertThrows(
                InvalidKeySpecException.class,
                () -> ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes));
    }

    @Test
    public void create_wrongPublicKeyLength_throwsError() throws InvalidKeySpecException {
        String keyConfigHex =
                "01000231e1f05a740102115220e9af918f738674aec95f54db6e04eb705aae8e7981";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        Assert.assertThrows(
                InvalidKeySpecException.class,
                () -> ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes));
    }

    @Test
    public void create_wrongAlgorithmsLength_throwsError() throws InvalidKeySpecException {
        String keyConfigHex =
                "01000231e1f05a740102115220e9af918f738674aec95f54db6e04eb705aae8e79815500";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        Assert.assertThrows(
                InvalidKeySpecException.class,
                () -> ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes));
    }

    @Test
    public void create_incompleteAlgorithmsList_throwsError() throws InvalidKeySpecException {
        String keyConfigHex =
                "01002031e1f05a740102115220e9af918f738674aec95f54db6e04eb705aae8e798155"
                        + "000800010001";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        Assert.assertThrows(
                InvalidKeySpecException.class,
                () -> ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes));
    }

    @Test
    public void serializeOhttpPayloadHeader_returnsCorrectHeader() throws InvalidKeySpecException {
        String keyConfigHex =
                "01002031e1f05a740102115220e9af918f738674aec95f54db6e04eb705aae8e798155"
                        + "00080001000100010003";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        ObliviousHttpKeyConfig keyConfig = ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes);
        byte[] ohttpPayloadHeader = keyConfig.serializeOhttpPayloadHeader();

        Assert.assertEquals(
                BaseEncoding.base16().lowerCase().encode(ohttpPayloadHeader), "01002000010001");
    }

    @Test
    public void createRecipientKeyInfoBytes_returnsCorrectInfo()
            throws InvalidKeySpecException, IOException {
        String keyConfigHex =
                "01002031e1f05a740102115220e9af918f738674aec95f54db6e04eb705aae8e798155"
                        + "00080001000100010003";
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(keyConfigHex);

        ObliviousHttpKeyConfig keyConfig = ObliviousHttpKeyConfig.fromSerializedKeyConfig(bytes);
        RecipientKeyInfo response = keyConfig.createRecipientKeyInfo();

        Assert.assertEquals(
                BaseEncoding.base16().lowerCase().encode(response.getBytes()),
                "6d6573736167652f626874747020726571756573740001002000010001");
    }
}
