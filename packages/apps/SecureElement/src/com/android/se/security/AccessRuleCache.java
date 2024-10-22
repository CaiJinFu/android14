/*
 * Copyright (C) 2017 The Android Open Source Project
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
/*
 * Copyright (c) 2015-2017, The Linux Foundation.
 */

/*
 * Copyright 2012 Giesecke & Devrient GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.se.security;

import android.os.Build;
import android.util.Log;

import com.android.se.security.gpac.AID_REF_DO;
import com.android.se.security.gpac.AR_DO;
import com.android.se.security.gpac.Hash_REF_DO;
import com.android.se.security.gpac.PKG_REF_DO;
import com.android.se.security.gpac.REF_DO;

import java.io.PrintWriter;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stores all the access rules from the Secure Element */
public class AccessRuleCache {
    private static final boolean DEBUG = Build.isDebuggable();
    private final String mTag = "SecureElement-AccessRuleCache";
    // Previous "RefreshTag"
    // 2012-09-25
    // the refresh tag has to be valid as long as AxxController is valid
    // a pure static element would cause that rules are not read any longer once the
    // AxxController is
    // recreated.
    private byte[] mRefreshTag = null;
    private Map<REF_DO, ChannelAccess> mRuleCache = new HashMap<REF_DO, ChannelAccess>();
    private ArrayList<REF_DO> mCarrierPrivilegeCache = new ArrayList<REF_DO>();

    private static AID_REF_DO getAidRefDo(byte[] aid) {
        byte[] defaultAid = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};
        if (aid == null || Arrays.equals(aid, defaultAid)) {
            return new AID_REF_DO(AID_REF_DO.TAG_DEFAULT_APPLICATION);
        } else {
            return new AID_REF_DO(AID_REF_DO.TAG, aid);
        }
    }

    private static ChannelAccess mapArDo2ChannelAccess(AR_DO arDo) {
        ChannelAccess channelAccess = new ChannelAccess();

        // Missing access rule attribute shall be interpreted as ALWAYS or NEVER
        // after the result of the rule conflict resolution and combination is processed.
        // See Table G-1 in GP SEAC v1.1 Annex G.
        //
        // GP SEAC v1.0 also indicates the same rule in Annex D.
        // Combined rule of APDU (ALWAYS) and NFC (ALWAYS) shall be APDU (ALWAYS) + NFC (ALWAYS).

        // check apdu access allowance
        if (arDo.getApduArDo() != null) {
            if (arDo.getApduArDo().isApduAllowed()) {
                channelAccess.setAccess(ChannelAccess.ACCESS.ALLOWED, "");
                // check the apdu filter
                ArrayList<byte[]> apduHeaders = arDo.getApduArDo().getApduHeaderList();
                ArrayList<byte[]> filterMasks = arDo.getApduArDo().getFilterMaskList();
                if (apduHeaders != null && filterMasks != null && apduHeaders.size() > 0
                        && apduHeaders.size() == filterMasks.size()) {
                    ApduFilter[] accessConditions = new ApduFilter[apduHeaders.size()];
                    for (int i = 0; i < apduHeaders.size(); i++) {
                        accessConditions[i] = new ApduFilter(apduHeaders.get(i),
                                filterMasks.get(i));
                    }
                    channelAccess.setUseApduFilter(true);
                    channelAccess.setApduFilter(accessConditions);
                } else {
                    // general APDU access
                    channelAccess.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
                }
            } else {
                // apdu access is not allowed at all.
                channelAccess.setAccess(ChannelAccess.ACCESS.DENIED,
                        "NEVER is explicitly specified as the APDU access rule policy");
                channelAccess.setApduAccess(ChannelAccess.ACCESS.DENIED);
            }
        } else {
            // It is too early to interpret the missing APDU access rule attribute as NEVER.
        }

        // check for NFC Event allowance
        if (arDo.getNfcArDo() != null) {
            channelAccess.setNFCEventAccess(
                    arDo.getNfcArDo().isNfcAllowed()
                            ? ChannelAccess.ACCESS.ALLOWED
                            : ChannelAccess.ACCESS.DENIED);
        } else {
            // It is too early to interpret the missing NFC access rule attribute. Keep UNDEFINED.
        }

        return channelAccess;
    }

    /** Clears access rule cache and refresh tag. */
    public void reset() {
        mRefreshTag = null;
        mRuleCache.clear();
        mCarrierPrivilegeCache.clear();
    }

    /** Clears access rule cache only. */
    public void clearCache() {
        mRuleCache.clear();
        mCarrierPrivilegeCache.clear();
    }

    /** Adds the Rule to the Cache */
    public void putWithMerge(REF_DO refDo, AR_DO arDo) {
        if (refDo.isCarrierPrivilegeRefDo()) {
            mCarrierPrivilegeCache.add(refDo);
            return;
        }
        ChannelAccess channelAccess = mapArDo2ChannelAccess(arDo);
        putWithMerge(refDo, channelAccess);
    }

    /** Adds the Rule to the Cache */
    public void putWithMerge(REF_DO refDo, ChannelAccess channelAccess) {
        if (refDo.isCarrierPrivilegeRefDo()) {
            mCarrierPrivilegeCache.add(refDo);
            return;
        }
        if (mRuleCache.containsKey(refDo)) {
            ChannelAccess ca = mRuleCache.get(refDo);

            // if new ac condition is more restrictive then use their settings
            // DENIED > ALLOWED > UNDEFINED

            if (ca.getAccess() != ChannelAccess.ACCESS.DENIED) {
                if (channelAccess.getAccess() == ChannelAccess.ACCESS.DENIED) {
                    ca.setAccess(ChannelAccess.ACCESS.DENIED, channelAccess.getReason());
                } else if (channelAccess.getAccess() == ChannelAccess.ACCESS.ALLOWED) {
                    ca.setAccess(ChannelAccess.ACCESS.ALLOWED, "");
                }
            }

            // Only the rule with the highest priority shall be applied if the rules conflict.
            // NFC (NEVER) > NFC (ALWAYS) > No NFC attribute

            if (ca.getNFCEventAccess() != ChannelAccess.ACCESS.DENIED) {
                if (channelAccess.getNFCEventAccess() == ChannelAccess.ACCESS.DENIED) {
                    ca.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
                } else if (channelAccess.getNFCEventAccess() == ChannelAccess.ACCESS.ALLOWED) {
                    ca.setNFCEventAccess(ChannelAccess.ACCESS.ALLOWED);
                }
            }

            // Only the rule with the highest priority shall be applied if the rules conflict.
            // APDU (NEVER) > APDU (Filter) > APDU (ALWAYS) > No APDU attribute

            if (ca.getApduAccess() != ChannelAccess.ACCESS.DENIED) {
                if (channelAccess.getApduAccess() == ChannelAccess.ACCESS.DENIED) {
                    ca.setApduAccess(ChannelAccess.ACCESS.DENIED);
                } else if (ca.isUseApduFilter() || channelAccess.isUseApduFilter()) {
                    // In order to differentiate APDU (Filter) from APDU (ALWAYS) clearly,
                    // check if the combined rule will have APDU filter here
                    // and avoid changing APDU access from UNDEFINED in APDU (Filter) case.
                    // APDU filters combination itself will be done in the next process below.
                } else if (channelAccess.getApduAccess() == ChannelAccess.ACCESS.ALLOWED) {
                    ca.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
                }
            }

            // put APDU filter together if resulting APDU access is not denied.
            if (ca.getApduAccess() != ChannelAccess.ACCESS.DENIED) {
                if (channelAccess.isUseApduFilter()) {
                    Log.i(mTag, "Merged Access Rule:  APDU filter together");
                    ca.setUseApduFilter(true);
                    ApduFilter[] filter = ca.getApduFilter();
                    ApduFilter[] filter2 = channelAccess.getApduFilter();
                    if (filter == null || filter.length == 0) {
                        ca.setApduFilter(filter2);
                    } else if (filter2 == null || filter2.length == 0) {
                        ca.setApduFilter(filter);
                    } else {
                        ApduFilter[] sum = new ApduFilter[filter.length + filter2.length];
                        int i = 0;
                        for (ApduFilter f : filter) {
                            sum[i++] = f;
                        }
                        for (ApduFilter f : filter2) {
                            sum[i++] = f;
                        }
                        ca.setApduFilter(sum);
                    }
                }
            } else {
                // if APDU access is not allowed the remove also all apdu filter.
                ca.setUseApduFilter(false);
                ca.setApduFilter(null);
            }
            if (DEBUG) {
                Log.i(mTag, "Merged Access Rule: " + refDo.toString() + ", " + ca.toString());
            }
            return;
        }
        if (DEBUG) {
            Log.i(mTag, "Add Access Rule: " + refDo.toString() + ", " + channelAccess.toString());
        }
        mRuleCache.put(refDo, channelAccess);
    }

    /** Find Access Rule for the given AID and Application */
    public ChannelAccess findAccessRule(byte[] aid, List<byte[]> appCertHashes)
            throws AccessControlException {
        ChannelAccess ca = findAccessRuleInternal(aid, appCertHashes);
        if (ca != null) {
            if ((ca.getApduAccess() == ChannelAccess.ACCESS.UNDEFINED) && !ca.isUseApduFilter()) {
                // Rule for APDU access does not exist.
                // All the APDU access requests shall never be allowed in this case.
                // This missing rule resolution is valid for both ARA and ARF
                // if the supported GP SEAC version is v1.1 or later.
                ca.setAccess(ChannelAccess.ACCESS.DENIED, "No APDU access rule is available");
                ca.setApduAccess(ChannelAccess.ACCESS.DENIED);
            }
            if (ca.getNFCEventAccess() == ChannelAccess.ACCESS.UNDEFINED) {
                // Missing NFC access rule shall be treated as ALLOWED
                // if relevant APDU access rule is ALLOWED or APDU filter is specified.
                if (ca.isUseApduFilter()) {
                    ca.setNFCEventAccess(ChannelAccess.ACCESS.ALLOWED);
                } else {
                    ca.setNFCEventAccess(ca.getApduAccess());
                }
            }
            // Note that the GP SEAC v1.1 has not been supported as GSMA TS.26 does not require it.
        }
        return ca;
    }

    private ChannelAccess findAccessRuleInternal(byte[] aid, List<byte[]> appCertHashes)
            throws AccessControlException {

        // TODO: check difference between DeviceCertHash and Certificate Chain (EndEntityCertHash,
        // IntermediateCertHash (1..n), RootCertHash)
        // The DeviceCertificate is equal to the EndEntityCertificate.
        // The android systems seems always to deliver only the EndEntityCertificate, but this
        // seems not
        // to be sure.
        // thats why we implement the whole chain.


        /* Search Rule A ( Certificate(s); AID ) */
        AID_REF_DO aid_ref_do = getAidRefDo(aid);
        REF_DO ref_do;
        Hash_REF_DO hash_ref_do;
        for (byte[] appCertHash : appCertHashes) {
            hash_ref_do = new Hash_REF_DO(appCertHash);
            ref_do = new REF_DO(aid_ref_do, hash_ref_do);

            if (mRuleCache.containsKey(ref_do)) {
                if (DEBUG) {
                    Log.i(mTag, "findAccessRule() Case A " + ref_do.toString() + ", "
                            + mRuleCache.get(ref_do).toString());
                }
                return mRuleCache.get(ref_do);
            }
        }
        // no rule found,
        // now we have to check if the given AID
        // is used together with another specific hash value (another device application)
        if (searchForRulesWithSpecificAidButOtherHash(aid_ref_do) != null) {
            if (DEBUG) {
                Log.i(mTag, "Conflict Resolution Case A returning access rule \'NEVER\'.");
            }
            ChannelAccess ca = new ChannelAccess();
            ca.setApduAccess(ChannelAccess.ACCESS.DENIED);
            ca.setAccess(ChannelAccess.ACCESS.DENIED,
                    "AID has a specific access rule with a different hash. (Case A)");
            ca.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
            return ca;
        }

        // SearchRule B ( <AllDeviceApplications>; AID)
        aid_ref_do = getAidRefDo(aid);
        hash_ref_do = new Hash_REF_DO(); // empty hash ref
        ref_do = new REF_DO(aid_ref_do, hash_ref_do);

        if (mRuleCache.containsKey(ref_do)) {
            if (DEBUG) {
                Log.i(mTag, "findAccessRule() Case B " + ref_do.toString() + ", "
                        + mRuleCache.get(ref_do).toString());
            }
            return mRuleCache.get(ref_do);
        }

        // Search Rule C ( Certificate(s); <AllSEApplications> )
        aid_ref_do = new AID_REF_DO(AID_REF_DO.TAG);
        for (byte[] appCertHash : appCertHashes) {
            hash_ref_do = new Hash_REF_DO(appCertHash);
            ref_do = new REF_DO(aid_ref_do, hash_ref_do);

            if (mRuleCache.containsKey(ref_do)) {
                if (DEBUG) {
                    Log.i(mTag, "findAccessRule() Case C " + ref_do.toString() + ", "
                            + mRuleCache.get(ref_do).toString());
                }
                return mRuleCache.get(ref_do);
            }
        }

        // no rule found,
        // now we have to check if the all AID DO
        // is used together with another Hash
        if (searchForRulesWithAllAidButOtherHash() != null) {
            if (DEBUG) {
                Log.i(mTag, "Conflict Resolution Case C returning access rule \'NEVER\'.");
            }
            ChannelAccess ca = new ChannelAccess();
            ca.setApduAccess(ChannelAccess.ACCESS.DENIED);
            ca.setAccess(
                    ChannelAccess.ACCESS.DENIED,
                    "An access rule with a different hash and all AIDs was found. (Case C)");
            ca.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
            return ca;
        }

        // SearchRule D ( <AllDeviceApplications>; <AllSEApplications>)
        aid_ref_do = new AID_REF_DO(AID_REF_DO.TAG);
        hash_ref_do = new Hash_REF_DO();
        ref_do = new REF_DO(aid_ref_do, hash_ref_do);

        if (mRuleCache.containsKey(ref_do)) {
            if (DEBUG) {
                Log.i(mTag, "findAccessRule() Case D " + ref_do.toString() + ", "
                        + mRuleCache.get(ref_do).toString());
            }
            return mRuleCache.get(ref_do);
        }

        if (DEBUG) Log.i(mTag, "findAccessRule() not found");
        return null;
    }

    /*
     * The GP_SE_AC spec says:
     * According to the rule conflict resolution process defined in section 3.2.1, if a specific
     * rule exists
     * that associates another device application with the SE application identified by AID (e.g.
      * there is
     * a rule associating AID with the hash of another device application), then the ARA-M (when
     * using GET DATA [Specific]) or the Access Control Enforcer (when using GET DATA [All]) shall
     * set the result of SearchRuleFor(DeviceApplicationCertificate, AID) to NEVER (i.e. precedence
     * of specific rules over generic rules)
     *
     * In own words:
     * Search the rules cache for a rule that contains the wanted AID but with another specific
     * Hash value.
     */
    private REF_DO searchForRulesWithSpecificAidButOtherHash(AID_REF_DO aidRefDo) {

        // AID has to be specific
        if (aidRefDo == null) {
            return null;
        }

        // The specified AID_REF_DO does not have any AID and it is not for the default AID.
        if (aidRefDo.getTag() == AID_REF_DO.TAG && aidRefDo.getAid().length == 0) {
            return null;
        }

        Set<REF_DO> keySet = mRuleCache.keySet();
        Iterator<REF_DO> iter = keySet.iterator();
        while (iter.hasNext()) {
            REF_DO ref_do = iter.next();
            if (aidRefDo.equals(ref_do.getAidDo())) {
                if (ref_do.getHashDo() != null
                        && ref_do.getHashDo().getHash().length > 0) {
                    // this ref_do contains the search AID and a specific hash value
                    return ref_do;
                }
            }
        }
        return null;
    }

    /*
     * The GP_SE_AC spec says:
     * According to the rule conflict resolution process defined in section 3.2.1, if a specific
     * rule exists
     * that associates another device application with the SE application identified by AID (e.g.
      * there is
     * a rule associating AID with the hash of another device application), then the ARA-M (when
     * using GET DATA [Specific]) or the Access Control Enforcer (when using GET DATA [All]) shall
     * set the result of SearchRuleFor(DeviceApplicationCertificate, AID) to NEVER (i.e. precedence
     * of specific rules over generic rules)
     *
     * In own words:
     * Search the rules cache for a rule that contains a Hash with an all SE AID (4F 00).
     */
    private Object searchForRulesWithAllAidButOtherHash() {

        AID_REF_DO aid_ref_do = new AID_REF_DO(AID_REF_DO.TAG);

        Set<REF_DO> keySet = mRuleCache.keySet();
        Iterator<REF_DO> iter = keySet.iterator();
        while (iter.hasNext()) {
            REF_DO ref_do = iter.next();
            if (aid_ref_do.equals(ref_do.getAidDo())) {
                // aid tlv is equal
                if (ref_do.getHashDo() != null
                        && ref_do.getHashDo().getHash().length > 0) {
                    // return ref_do if
                    // a HASH value is available and has a length > 0 (SHA1_LEN)
                    return ref_do;
                }
            }
        }
        return null;
    }

    /** Check if the carrier privilege exists for the given package */
    public boolean checkCarrierPrivilege(String packageName, List<byte[]> appCertHashes) {
        for (byte[] hash : appCertHashes) {
            for (REF_DO ref_do : mCarrierPrivilegeCache) {
                Hash_REF_DO hash_ref_do = ref_do.getHashDo();
                PKG_REF_DO pkg_ref_do = ref_do.getPkgDo();
                if (Hash_REF_DO.equals(hash_ref_do, new Hash_REF_DO(hash))) {
                    // If PKG_REF_DO exists then package name should match, otherwise allow
                    if (pkg_ref_do != null) {
                        if (packageName.equals(pkg_ref_do.getPackageName())) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Check if the given Refresh Tag is equal to the last known */
    public boolean isRefreshTagEqual(byte[] refreshTag) {
        if (refreshTag == null || mRefreshTag == null) return false;

        return Arrays.equals(refreshTag, mRefreshTag);
    }

    public byte[] getRefreshTag() {
        return mRefreshTag;
    }

    /** Sets the Refresh Tag */
    public void setRefreshTag(byte[] refreshTag) {
        mRefreshTag = refreshTag;
    }

    /** Debug information to be used by dumpsys */
    public void dump(PrintWriter writer) {
        writer.println(mTag + ":");

        /* Dump the refresh tag */
        writer.print("Current refresh tag is: ");
        if (mRefreshTag == null) {
            writer.print("<null>");
        } else {
            for (byte oneByte : mRefreshTag) writer.printf("%02X:", oneByte);
        }
        writer.println();

        /* Dump the rules cache */
        writer.println("Rules:");
        int i = 0;
        for (Map.Entry<REF_DO, ChannelAccess> entry : mRuleCache.entrySet()) {
            i++;
            writer.print("rule " + i + ": ");
            writer.println(entry.getKey().toString() + " -> " + entry.getValue().toString());
        }
        writer.println();

        /* Dump the Carrier Privilege cache */
        writer.println("Carrier Privilege:");
        i = 0;
        for (REF_DO ref_do  : mCarrierPrivilegeCache) {
            i++;
            writer.print("carrier privilege " + i + ": ");
            writer.println(ref_do.toString());
        }
        writer.println();
    }
}
