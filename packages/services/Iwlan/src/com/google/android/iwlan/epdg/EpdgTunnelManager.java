/*
 * Copyright 2020 The Android Open Source Project
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

package com.google.android.iwlan.epdg;

import static android.net.ipsec.ike.ike3gpp.Ike3gppData.DATA_TYPE_NOTIFY_BACKOFF_TIMER;
import static android.net.ipsec.ike.ike3gpp.Ike3gppData.DATA_TYPE_NOTIFY_N1_MODE_INFORMATION;
import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.IpSecManager;
import android.net.IpSecTransform;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.eap.EapAkaInfo;
import android.net.eap.EapInfo;
import android.net.eap.EapSessionConfig;
import android.net.ipsec.ike.ChildSaProposal;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.ChildSessionConfiguration;
import android.net.ipsec.ike.ChildSessionParams;
import android.net.ipsec.ike.IkeFqdnIdentification;
import android.net.ipsec.ike.IkeIdentification;
import android.net.ipsec.ike.IkeKeyIdIdentification;
import android.net.ipsec.ike.IkeRfc822AddrIdentification;
import android.net.ipsec.ike.IkeSaProposal;
import android.net.ipsec.ike.IkeSession;
import android.net.ipsec.ike.IkeSessionCallback;
import android.net.ipsec.ike.IkeSessionConfiguration;
import android.net.ipsec.ike.IkeSessionConnectionInfo;
import android.net.ipsec.ike.IkeSessionParams;
import android.net.ipsec.ike.IkeTrafficSelector;
import android.net.ipsec.ike.SaProposal;
import android.net.ipsec.ike.TunnelModeChildSessionParams;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeIOException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.ike3gpp.Ike3gppBackoffTimer;
import android.net.ipsec.ike.ike3gpp.Ike3gppData;
import android.net.ipsec.ike.ike3gpp.Ike3gppExtension;
import android.net.ipsec.ike.ike3gpp.Ike3gppN1ModeInformation;
import android.net.ipsec.ike.ike3gpp.Ike3gppParams;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.NetworkSliceInfo;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import com.google.android.iwlan.ErrorPolicyManager;
import com.google.android.iwlan.IwlanError;
import com.google.android.iwlan.IwlanHelper;
import com.google.android.iwlan.IwlanTunnelMetricsImpl;
import com.google.android.iwlan.TunnelMetricsInterface;
import com.google.android.iwlan.TunnelMetricsInterface.OnClosedMetrics;
import com.google.android.iwlan.TunnelMetricsInterface.OnOpenedMetrics;
import com.google.android.iwlan.exceptions.IwlanSimNotReadyException;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EpdgTunnelManager {

    private final Context mContext;
    private final int mSlotId;
    private Handler mHandler;

    private static final int EVENT_TUNNEL_BRINGUP_REQUEST = 0;
    private static final int EVENT_TUNNEL_BRINGDOWN_REQUEST = 1;
    private static final int EVENT_CHILD_SESSION_OPENED = 2;
    private static final int EVENT_CHILD_SESSION_CLOSED = 3;
    private static final int EVENT_IKE_SESSION_CLOSED = 5;
    private static final int EVENT_EPDG_ADDRESS_SELECTION_REQUEST_COMPLETE = 6;
    private static final int EVENT_IPSEC_TRANSFORM_CREATED = 7;
    private static final int EVENT_IPSEC_TRANSFORM_DELETED = 8;
    private static final int EVENT_UPDATE_NETWORK = 9;
    private static final int EVENT_IKE_SESSION_OPENED = 10;
    private static final int EVENT_IKE_SESSION_CONNECTION_INFO_CHANGED = 11;
    private static final int EVENT_IKE_3GPP_DATA_RECEIVED = 12;
    private static final int IKE_HARD_LIFETIME_SEC_MINIMUM = 300;
    private static final int IKE_HARD_LIFETIME_SEC_MAXIMUM = 86400;
    private static final int IKE_SOFT_LIFETIME_SEC_MINIMUM = 120;
    private static final int CHILD_HARD_LIFETIME_SEC_MINIMUM = 300;
    private static final int CHILD_HARD_LIFETIME_SEC_MAXIMUM = 14400;
    private static final int CHILD_SOFT_LIFETIME_SEC_MINIMUM = 120;
    private static final int LIFETIME_MARGIN_SEC_MINIMUM = (int) TimeUnit.MINUTES.toSeconds(1L);
    private static final int IKE_RETRANS_TIMEOUT_MS_MIN = 500;

    private static final int IKE_RETRANS_TIMEOUT_MS_MAX = (int) TimeUnit.MINUTES.toMillis(30L);

    private static final int IKE_RETRANS_MAX_ATTEMPTS_MAX = 10;
    private static final int IKE_DPD_DELAY_SEC_MIN = 20;
    private static final int IKE_DPD_DELAY_SEC_MAX = 1800; // 30 minutes
    private static final int NATT_KEEPALIVE_DELAY_SEC_MIN = 10;
    private static final int NATT_KEEPALIVE_DELAY_SEC_MAX = 120;

    private static final int DEVICE_IMEI_LEN = 15;
    private static final int DEVICE_IMEISV_SUFFIX_LEN = 2;

    private static final int TRAFFIC_SELECTOR_START_PORT = 0;
    private static final int TRAFFIC_SELECTOR_END_PORT = 65535;
    private static final String TRAFFIC_SELECTOR_IPV4_START_ADDR = "0.0.0.0";
    private static final String TRAFFIC_SELECTOR_IPV4_END_ADDR = "255.255.255.255";
    private static final String TRAFFIC_SELECTOR_IPV6_START_ADDR = "::";
    private static final String TRAFFIC_SELECTOR_IPV6_END_ADDR =
            "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff";

    // "192.0.2.0" is selected from RFC5737, "IPv4 Address Blocks Reserved for Documentation"
    private static final InetAddress DUMMY_ADDR = InetAddresses.parseNumericAddress("192.0.2.0");

    private static final Map<Integer, EpdgTunnelManager> mTunnelManagerInstances =
            new ConcurrentHashMap<>();

    private Queue<TunnelRequestWrapper> mPendingBringUpRequests = new LinkedList<>();

    private final EpdgInfo mValidEpdgInfo = new EpdgInfo();
    @Nullable private InetAddress mEpdgAddress;

    // The most recently updated system default network as seen by IwlanDataService.
    @Nullable private Network mDefaultNetwork;
    // The latest Network provided to the IKE session. Only for debugging purposes.
    @Nullable private Network mIkeSessionNetwork;

    private int mTransactionId = 0;
    private boolean mHasConnectedToEpdg;
    private final IkeSessionCreator mIkeSessionCreator;

    private Map<String, TunnelConfig> mApnNameToTunnelConfig = new ConcurrentHashMap<>();
    private final Map<String, Integer> mApnNameToCurrentToken = new ConcurrentHashMap<>();

    private final String TAG;

    @Nullable private byte[] mNextReauthId = null;
    private long mEpdgServerSelectionDuration = 0;
    private long mEpdgServerSelectionStartTime = 0;
    private long mIkeTunnelEstablishmentStartTime = 0;

    private static final Set<Integer> VALID_DH_GROUPS;
    private static final Set<Integer> VALID_KEY_LENGTHS;
    private static final Set<Integer> VALID_PRF_ALGOS;
    private static final Set<Integer> VALID_INTEGRITY_ALGOS;
    private static final Set<Integer> VALID_ENCRYPTION_ALGOS;

    private static final String CONFIG_TYPE_DH_GROUP = "dh group";
    private static final String CONFIG_TYPE_KEY_LEN = "algorithm key length";
    private static final String CONFIG_TYPE_PRF_ALGO = "prf algorithm";
    private static final String CONFIG_TYPE_INTEGRITY_ALGO = "integrity algorithm";
    private static final String CONFIG_TYPE_ENCRYPT_ALGO = "encryption algorithm";

    static {
        VALID_DH_GROUPS =
                Set.of(
                        SaProposal.DH_GROUP_1024_BIT_MODP,
                        SaProposal.DH_GROUP_1536_BIT_MODP,
                        SaProposal.DH_GROUP_2048_BIT_MODP);
        VALID_KEY_LENGTHS =
                Set.of(
                        SaProposal.KEY_LEN_AES_128,
                        SaProposal.KEY_LEN_AES_192,
                        SaProposal.KEY_LEN_AES_256);

        VALID_ENCRYPTION_ALGOS =
                Set.of(
                        SaProposal.ENCRYPTION_ALGORITHM_AES_CBC,
                        SaProposal.ENCRYPTION_ALGORITHM_AES_CTR);

        VALID_INTEGRITY_ALGOS =
                Set.of(
                        SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96,
                        SaProposal.INTEGRITY_ALGORITHM_AES_XCBC_96,
                        SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128,
                        SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_384_192,
                        SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_512_256);

        VALID_PRF_ALGOS =
                Set.of(
                        SaProposal.PSEUDORANDOM_FUNCTION_HMAC_SHA1,
                        SaProposal.PSEUDORANDOM_FUNCTION_AES128_XCBC,
                        SaProposal.PSEUDORANDOM_FUNCTION_SHA2_256,
                        SaProposal.PSEUDORANDOM_FUNCTION_SHA2_384,
                        SaProposal.PSEUDORANDOM_FUNCTION_SHA2_512);
    }

    private final EpdgSelector.EpdgSelectorCallback mSelectorCallback =
            new EpdgSelector.EpdgSelectorCallback() {
                @Override
                public void onServerListChanged(int transactionId, List<InetAddress> validIPList) {
                    sendSelectionRequestComplete(
                            validIPList, new IwlanError(IwlanError.NO_ERROR), transactionId);
                }

                @Override
                public void onError(int transactionId, IwlanError epdgSelectorError) {
                    sendSelectionRequestComplete(null, epdgSelectorError, transactionId);
                }
            };

    @VisibleForTesting
    class TunnelConfig {
        @NonNull final TunnelCallback mTunnelCallback;
        @NonNull final TunnelMetricsInterface mTunnelMetrics;
        // TODO: Change this to TunnelLinkProperties after removing autovalue
        private List<InetAddress> mPcscfAddrList;
        private List<InetAddress> mDnsAddrList;
        private List<LinkAddress> mInternalAddrList;

        private final InetAddress mSrcIpv6Address;
        private final int mSrcIpv6AddressPrefixLen;
        private NetworkSliceInfo mSliceInfo;
        private boolean mIsBackoffTimeValid = false;
        private long mBackoffTime;

        private IkeSessionState mIkeSessionState;

        public IkeSessionState getIkeSessionState() {
            return mIkeSessionState;
        }

        public void setIkeSessionState(IkeSessionState ikeSessionState) {
            mIkeSessionState = ikeSessionState;
        }

        public NetworkSliceInfo getSliceInfo() {
            return mSliceInfo;
        }

        public void setSliceInfo(NetworkSliceInfo si) {
            mSliceInfo = si;
        }

        public boolean isBackoffTimeValid() {
            return mIsBackoffTimeValid;
        }

        public long getBackoffTime() {
            return mBackoffTime;
        }

        public void setBackoffTime(long backoffTime) {
            mIsBackoffTimeValid = true;
            mBackoffTime = backoffTime;
        }

        @NonNull final IkeSession mIkeSession;
        IwlanError mError;
        private IpSecManager.IpSecTunnelInterface mIface;

        public TunnelConfig(
                IkeSession ikeSession,
                TunnelCallback tunnelCallback,
                TunnelMetricsInterface tunnelMetrics,
                InetAddress srcIpv6Addr,
                int srcIpv6PrefixLength) {
            mTunnelCallback = tunnelCallback;
            mTunnelMetrics = tunnelMetrics;
            mIkeSession = ikeSession;
            mError = new IwlanError(IwlanError.NO_ERROR);
            mSrcIpv6Address = srcIpv6Addr;
            mSrcIpv6AddressPrefixLen = srcIpv6PrefixLength;

            setIkeSessionState(IkeSessionState.IKE_SESSION_INIT_IN_PROGRESS);
        }

        @NonNull
        TunnelCallback getTunnelCallback() {
            return mTunnelCallback;
        }

        @NonNull
        TunnelMetricsInterface getTunnelMetrics() {
            return mTunnelMetrics;
        }

        List<InetAddress> getPcscfAddrList() {
            return mPcscfAddrList;
        }

        void setPcscfAddrList(List<InetAddress> pcscfAddrList) {
            mPcscfAddrList = pcscfAddrList;
        }

        public List<InetAddress> getDnsAddrList() {
            return mDnsAddrList;
        }

        public void setDnsAddrList(List<InetAddress> dnsAddrList) {
            this.mDnsAddrList = dnsAddrList;
        }

        public List<LinkAddress> getInternalAddrList() {
            return mInternalAddrList;
        }

        boolean isPrefixSameAsSrcIP(LinkAddress laddr) {
            if (laddr.isIpv6() && (laddr.getPrefixLength() == mSrcIpv6AddressPrefixLen)) {
                IpPrefix assignedPrefix = new IpPrefix(laddr.getAddress(), laddr.getPrefixLength());
                IpPrefix srcPrefix = new IpPrefix(mSrcIpv6Address, mSrcIpv6AddressPrefixLen);
                return assignedPrefix.equals(srcPrefix);
            }
            return false;
        }

        public void setInternalAddrList(List<LinkAddress> internalAddrList) {
            mInternalAddrList = new ArrayList<LinkAddress>(internalAddrList);
            if (getSrcIpv6Address() != null) {
                // check if we can reuse src ipv6 address (i.e. if prefix is same)
                for (LinkAddress assignedAddr : internalAddrList) {
                    if (isPrefixSameAsSrcIP(assignedAddr)) {
                        // the assigned IPv6 address is same as pre-Handover IPv6
                        // addr. Just reuse the pre-Handover Address so the IID is
                        // preserved
                        mInternalAddrList.remove(assignedAddr);

                        // add original address
                        mInternalAddrList.add(
                                new LinkAddress(mSrcIpv6Address, mSrcIpv6AddressPrefixLen));

                        Log.d(
                                TAG,
                                "Network assigned IP replaced OLD:"
                                        + internalAddrList
                                        + " NEW:"
                                        + mInternalAddrList);
                        break;
                    }
                }
            }
        }

        @NonNull
        public IkeSession getIkeSession() {
            return mIkeSession;
        }

        public IwlanError getError() {
            return mError;
        }

        public void setError(IwlanError error) {
            this.mError = error;
        }

        public IpSecManager.IpSecTunnelInterface getIface() {
            return mIface;
        }

        public void setIface(IpSecManager.IpSecTunnelInterface iface) {
            mIface = iface;
        }

        public InetAddress getSrcIpv6Address() {
            return mSrcIpv6Address;
        }

        public boolean hasTunnelOpened() {
            return mInternalAddrList != null
                    && !mInternalAddrList.isEmpty() /* The child session is opened */
                    && mIface != null; /* The tunnel interface is bring up */
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TunnelConfig { ");

            if (mSliceInfo != null) {
                sb.append("mSliceInfo: ").append(mSliceInfo).append(", ");
            }

            if (mIsBackoffTimeValid) {
                sb.append("mBackoffTime: ").append(mBackoffTime).append(", ");
            }
            sb.append(" }");
            return sb.toString();
        }
    }

    @VisibleForTesting
    class TmIkeSessionCallback implements IkeSessionCallback {

        private final String mApnName;
        private final int mToken;

        TmIkeSessionCallback(String apnName, int token) {
            this.mApnName = apnName;
            this.mToken = token;
        }

        @Override
        public void onOpened(IkeSessionConfiguration sessionConfiguration) {
            Log.d(TAG, "Ike session opened for apn: " + mApnName + " with token: " + mToken);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IKE_SESSION_OPENED,
                            new IkeSessionOpenedData(mApnName, mToken, sessionConfiguration)));
        }

        @Override
        public void onClosed() {
            Log.d(TAG, "Ike session closed for apn: " + mApnName + " with token: " + mToken);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IKE_SESSION_CLOSED,
                            new SessionClosedData(mApnName, mToken, null /* ikeException */)));
        }

        @Override
        public void onClosedWithException(IkeException exception) {
            mNextReauthId = null;
            onSessionClosedWithException(exception, mApnName, mToken, EVENT_IKE_SESSION_CLOSED);
        }

        @Override
        public void onError(IkeProtocolException exception) {
            Log.d(TAG, "Ike session onError for apn: " + mApnName + " with token: " + mToken);

            mNextReauthId = null;

            Log.d(
                    TAG,
                    "ErrorType:"
                            + exception.getErrorType()
                            + " ErrorData:"
                            + exception.getMessage());
        }

        @Override
        public void onIkeSessionConnectionInfoChanged(
                IkeSessionConnectionInfo ikeSessionConnectionInfo) {
            Network network = ikeSessionConnectionInfo.getNetwork();
            Log.d(
                    TAG,
                    "Ike session connection info changed for apn: "
                            + mApnName
                            + " with token: "
                            + mToken
                            + " Network: "
                            + network);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IKE_SESSION_CONNECTION_INFO_CHANGED,
                            new IkeSessionConnectionInfoData(
                                    mApnName, mToken, ikeSessionConnectionInfo)));
        }
    }

    @VisibleForTesting
    class TmIke3gppCallback implements Ike3gppExtension.Ike3gppDataListener {
        private final String mApnName;
        private final int mToken;

        private TmIke3gppCallback(String apnName, int token) {
            mApnName = apnName;
            mToken = token;
        }

        @Override
        public void onIke3gppDataReceived(List<Ike3gppData> payloads) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IKE_3GPP_DATA_RECEIVED,
                            new Ike3gppDataReceived(mApnName, mToken, payloads)));
        }
    }

    @VisibleForTesting
    class TmChildSessionCallback implements ChildSessionCallback {

        private final String mApnName;
        private final int mToken;

        TmChildSessionCallback(String apnName, int token) {
            this.mApnName = apnName;
            this.mToken = token;
        }

        @Override
        public void onOpened(ChildSessionConfiguration sessionConfiguration) {
            Log.d(TAG, "onOpened child session for apn: " + mApnName + " with token: " + mToken);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_CHILD_SESSION_OPENED,
                            new TunnelOpenedData(
                                    mApnName,
                                    mToken,
                                    sessionConfiguration.getInternalDnsServers(),
                                    sessionConfiguration.getInternalAddresses())));
        }

        @Override
        public void onClosed() {
            Log.d(TAG, "onClosed child session for apn: " + mApnName + " with token: " + mToken);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_CHILD_SESSION_CLOSED,
                            new SessionClosedData(mApnName, mToken, null /* ikeException */)));
        }

        @Override
        public void onClosedWithException(IkeException exception) {
            onSessionClosedWithException(exception, mApnName, mToken, EVENT_CHILD_SESSION_CLOSED);
        }

        @Override
        public void onIpSecTransformsMigrated(
                IpSecTransform inIpSecTransform, IpSecTransform outIpSecTransform) {
            // migration is similar to addition
            Log.d(TAG, "Transforms migrated for apn: " + mApnName + " with token: " + mToken);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IPSEC_TRANSFORM_CREATED,
                            new IpsecTransformData(
                                    inIpSecTransform,
                                    IpSecManager.DIRECTION_IN,
                                    mApnName,
                                    mToken)));
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IPSEC_TRANSFORM_CREATED,
                            new IpsecTransformData(
                                    outIpSecTransform,
                                    IpSecManager.DIRECTION_OUT,
                                    mApnName,
                                    mToken)));
        }

        @Override
        public void onIpSecTransformCreated(IpSecTransform ipSecTransform, int direction) {
            Log.d(
                    TAG,
                    "Transform created, direction: "
                            + direction
                            + ", apn: "
                            + mApnName
                            + ", token: "
                            + mToken);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IPSEC_TRANSFORM_CREATED,
                            new IpsecTransformData(ipSecTransform, direction, mApnName, mToken)));
        }

        @Override
        public void onIpSecTransformDeleted(IpSecTransform ipSecTransform, int direction) {
            Log.d(
                    TAG,
                    "Transform deleted, direction: "
                            + direction
                            + ", apn: "
                            + mApnName
                            + ", token: "
                            + mToken);
            mHandler.sendMessage(
                    mHandler.obtainMessage(
                            EVENT_IPSEC_TRANSFORM_DELETED,
                            new IpsecTransformData(ipSecTransform, direction, mApnName, mToken)));
        }
    }

    private EpdgTunnelManager(Context context, int slotId) {
        mContext = context;
        mSlotId = slotId;
        mIkeSessionCreator = new IkeSessionCreator();
        TAG = EpdgTunnelManager.class.getSimpleName() + "[" + mSlotId + "]";
        initHandler();
    }

    @VisibleForTesting
    void initHandler() {
        mHandler = new TmHandler(getLooper());
    }

    @VisibleForTesting
    Looper getLooper() {
        HandlerThread handlerThread = new HandlerThread("EpdgTunnelManagerThread");
        handlerThread.start();
        return handlerThread.getLooper();
    }

    /**
     * Gets a EpdgTunnelManager instance.
     *
     * @param context application context
     * @param subId subscription ID for the tunnel
     * @return tunnel manager instance corresponding to the sub id
     */
    public static EpdgTunnelManager getInstance(@NonNull Context context, int subId) {
        return mTunnelManagerInstances.computeIfAbsent(
                subId, k -> new EpdgTunnelManager(context, subId));
    }

    @VisibleForTesting
    public static void resetAllInstances() {
        mTunnelManagerInstances.clear();
    }

    public interface TunnelCallback {
        /**
         * Called when the tunnel is opened.
         *
         * @param apnName apn for which the tunnel was opened
         * @param linkProperties link properties of the tunnel
         */
        void onOpened(@NonNull String apnName, @NonNull TunnelLinkProperties linkProperties);
        /**
         * Called when the tunnel is closed OR if bringup fails
         *
         * @param apnName apn for which the tunnel was closed
         * @param error IwlanError carrying details of the error
         */
        void onClosed(@NonNull String apnName, @NonNull IwlanError error);
    }

    /**
     * Close tunnel for an apn. Confirmation of closing will be delivered in TunnelCallback that was
     * provided in {@link #bringUpTunnel}. If no tunnel was available, callback will be delivered
     * using client-provided provided tunnelCallback and iwlanTunnelMetrics
     *
     * @param apnName apn name
     * @param forceClose if true, results in local cleanup of tunnel
     * @param tunnelCallback Used if no current or pending IWLAN tunnel exists
     * @param iwlanTunnelMetrics Used to report metrics if no current or pending IWLAN tunnel exists
     */
    public void closeTunnel(
            @NonNull String apnName,
            boolean forceClose,
            @NonNull TunnelCallback tunnelCallback,
            @NonNull IwlanTunnelMetricsImpl iwlanTunnelMetrics) {
        mHandler.sendMessage(
                mHandler.obtainMessage(
                        EVENT_TUNNEL_BRINGDOWN_REQUEST,
                        new TunnelBringdownRequest(
                                apnName, forceClose, tunnelCallback, iwlanTunnelMetrics)));
    }

    /**
     * Update the local Network. This will trigger a revaluation for every tunnel for which tunnel
     * manager has state.
     *
     * @param network the network to be updated
     * @param network the linkProperties to be updated
     */
    public void updateNetwork(Network network, LinkProperties linkProperties) {
        UpdateNetworkWrapper updateNetworkWrapper =
                new UpdateNetworkWrapper(network, linkProperties);
        mHandler.sendMessage(mHandler.obtainMessage(EVENT_UPDATE_NETWORK, updateNetworkWrapper));
    }
    /**
     * Bring up epdg tunnel. Only one bring up request per apn is expected. All active tunnel
     * requests and tunnels are expected to be on the same network.
     *
     * @param setupRequest {@link TunnelSetupRequest} tunnel configurations
     * @param tunnelCallback {@link TunnelCallback} interface to notify clients about the tunnel
     *     state
     * @return true if params are valid and no existing tunnel. False otherwise.
     */
    public boolean bringUpTunnel(
            @NonNull TunnelSetupRequest setupRequest,
            @NonNull TunnelCallback tunnelCallback,
            @NonNull TunnelMetricsInterface tunnelMetrics) {
        String apnName = setupRequest.apnName();

        if (getTunnelSetupRequestApnName(setupRequest) == null) {
            Log.e(TAG, "APN is null.");
            return false;
        }

        if (isTunnelConfigContainExistApn(apnName)) {
            Log.e(TAG, "Tunnel exists for apn:" + apnName);
            return false;
        }

        if (!isValidApnProtocol(setupRequest.apnIpProtocol())) {
            Log.e(TAG, "Invalid protocol for APN");
            return false;
        }

        int pduSessionId = setupRequest.pduSessionId();
        if (pduSessionId < 0 || pduSessionId > 15) {
            Log.e(TAG, "Invalid pduSessionId: " + pduSessionId);
            return false;
        }

        TunnelRequestWrapper tunnelRequestWrapper =
                new TunnelRequestWrapper(setupRequest, tunnelCallback, tunnelMetrics);

        mHandler.sendMessage(
                mHandler.obtainMessage(EVENT_TUNNEL_BRINGUP_REQUEST, tunnelRequestWrapper));

        return true;
    }

    private void onBringUpTunnel(
            TunnelSetupRequest setupRequest,
            TunnelCallback tunnelCallback,
            TunnelMetricsInterface tunnelMetrics) {
        String apnName = setupRequest.apnName();
        IkeSessionParams ikeSessionParams;

        Log.d(
                TAG,
                "Bringing up tunnel for apn: "
                        + apnName
                        + " ePDG: "
                        + mEpdgAddress.getHostAddress());

        final int token = incrementAndGetCurrentTokenForApn(apnName);

        try {
            ikeSessionParams = buildIkeSessionParams(setupRequest, apnName, token);
        } catch (IwlanSimNotReadyException e) {
            IwlanError iwlanError = new IwlanError(IwlanError.SIM_NOT_READY_EXCEPTION);
            reportIwlanError(apnName, iwlanError);
            tunnelCallback.onClosed(apnName, iwlanError);
            tunnelMetrics.onClosed(new OnClosedMetrics.Builder().setApnName(apnName).build());
            return;
        }

        mIkeTunnelEstablishmentStartTime = System.currentTimeMillis();
        IkeSession ikeSession =
                getIkeSessionCreator()
                        .createIkeSession(
                                mContext,
                                ikeSessionParams,
                                buildChildSessionParams(setupRequest),
                                Executors.newSingleThreadExecutor(),
                                getTmIkeSessionCallback(apnName, token),
                                new TmChildSessionCallback(apnName, token));

        boolean isSrcIpv6Present = setupRequest.srcIpv6Address().isPresent();
        putApnNameToTunnelConfig(
                apnName,
                ikeSession,
                tunnelCallback,
                tunnelMetrics,
                isSrcIpv6Present ? setupRequest.srcIpv6Address().get() : null,
                setupRequest.srcIpv6AddressPrefixLength());
    }

    /**
     * Proxy to allow testing
     *
     * @hide
     */
    @VisibleForTesting
    static class IkeSessionCreator {
        /** Creates a IKE session */
        public IkeSession createIkeSession(
                @NonNull Context context,
                @NonNull IkeSessionParams ikeSessionParams,
                @NonNull ChildSessionParams firstChildSessionParams,
                @NonNull Executor userCbExecutor,
                @NonNull IkeSessionCallback ikeSessionCallback,
                @NonNull ChildSessionCallback firstChildSessionCallback) {
            return new IkeSession(
                    context,
                    ikeSessionParams,
                    firstChildSessionParams,
                    userCbExecutor,
                    ikeSessionCallback,
                    firstChildSessionCallback);
        }
    }

    private ChildSessionParams buildChildSessionParams(TunnelSetupRequest setupRequest) {
        int proto = setupRequest.apnIpProtocol();
        int hardTimeSeconds =
                getConfig(CarrierConfigManager.Iwlan.KEY_CHILD_SA_REKEY_HARD_TIMER_SEC_INT);
        int softTimeSeconds =
                getConfig(CarrierConfigManager.Iwlan.KEY_CHILD_SA_REKEY_SOFT_TIMER_SEC_INT);
        if (!isValidChildSessionLifetime(hardTimeSeconds, softTimeSeconds)) {
            if (hardTimeSeconds > CHILD_HARD_LIFETIME_SEC_MAXIMUM
                    && softTimeSeconds > CHILD_SOFT_LIFETIME_SEC_MINIMUM) {
                hardTimeSeconds = CHILD_HARD_LIFETIME_SEC_MAXIMUM;
                softTimeSeconds = CHILD_HARD_LIFETIME_SEC_MAXIMUM - LIFETIME_MARGIN_SEC_MINIMUM;
            } else {
                hardTimeSeconds =
                        IwlanHelper.getDefaultConfig(
                                CarrierConfigManager.Iwlan.KEY_CHILD_SA_REKEY_HARD_TIMER_SEC_INT);
                softTimeSeconds =
                        IwlanHelper.getDefaultConfig(
                                CarrierConfigManager.Iwlan.KEY_CHILD_SA_REKEY_SOFT_TIMER_SEC_INT);
            }
            Log.d(
                    TAG,
                    "Invalid child session lifetime values, set hard: "
                            + hardTimeSeconds
                            + ", soft: "
                            + softTimeSeconds);
        }

        TunnelModeChildSessionParams.Builder childSessionParamsBuilder =
                new TunnelModeChildSessionParams.Builder()
                        .setLifetimeSeconds(hardTimeSeconds, softTimeSeconds);

        childSessionParamsBuilder.addChildSaProposal(buildChildSaProposal());

        boolean handoverIPv4Present = setupRequest.srcIpv4Address().isPresent();
        boolean handoverIPv6Present = setupRequest.srcIpv6Address().isPresent();
        if (handoverIPv4Present || handoverIPv6Present) {
            if (handoverIPv4Present) {
                childSessionParamsBuilder.addInternalAddressRequest(
                        (Inet4Address) setupRequest.srcIpv4Address().get());
                childSessionParamsBuilder.addInternalDnsServerRequest(AF_INET);
                childSessionParamsBuilder.addInboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv4());
                childSessionParamsBuilder.addOutboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv4());
            }
            if (handoverIPv6Present) {
                childSessionParamsBuilder.addInternalAddressRequest(
                        (Inet6Address) setupRequest.srcIpv6Address().get(),
                        setupRequest.srcIpv6AddressPrefixLength());
                childSessionParamsBuilder.addInternalDnsServerRequest(AF_INET6);
                childSessionParamsBuilder.addInboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv6());
                childSessionParamsBuilder.addOutboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv6());
            }
        } else {
            // non-handover case
            if (proto == ApnSetting.PROTOCOL_IP || proto == ApnSetting.PROTOCOL_IPV4V6) {
                childSessionParamsBuilder.addInternalAddressRequest(AF_INET);
                childSessionParamsBuilder.addInternalDnsServerRequest(AF_INET);
                childSessionParamsBuilder.addInboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv4());
                childSessionParamsBuilder.addOutboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv4());
            }
            if (proto == ApnSetting.PROTOCOL_IPV6 || proto == ApnSetting.PROTOCOL_IPV4V6) {
                childSessionParamsBuilder.addInternalAddressRequest(AF_INET6);
                childSessionParamsBuilder.addInternalDnsServerRequest(AF_INET6);
                childSessionParamsBuilder.addInboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv6());
                childSessionParamsBuilder.addOutboundTrafficSelectors(
                        getDefaultTrafficSelectorIpv6());
            }
        }

        return childSessionParamsBuilder.build();
    }

    private static IkeTrafficSelector getDefaultTrafficSelectorIpv4() {
        return new IkeTrafficSelector(
                TRAFFIC_SELECTOR_START_PORT,
                TRAFFIC_SELECTOR_END_PORT,
                InetAddresses.parseNumericAddress(TRAFFIC_SELECTOR_IPV4_START_ADDR),
                InetAddresses.parseNumericAddress(TRAFFIC_SELECTOR_IPV4_END_ADDR));
    }

    private static IkeTrafficSelector getDefaultTrafficSelectorIpv6() {
        return new IkeTrafficSelector(
                TRAFFIC_SELECTOR_START_PORT,
                TRAFFIC_SELECTOR_END_PORT,
                InetAddresses.parseNumericAddress(TRAFFIC_SELECTOR_IPV6_START_ADDR),
                InetAddresses.parseNumericAddress(TRAFFIC_SELECTOR_IPV6_END_ADDR));
    }

    private int numPdnTunnels() {
        return mApnNameToTunnelConfig.size();
    }

    // Returns the IMEISV or device IMEI, in that order of priority.
    private @Nullable String getMobileDeviceIdentity() {
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        telephonyManager =
                Objects.requireNonNull(telephonyManager)
                        .createForSubscriptionId(IwlanHelper.getSubId(mContext, mSlotId));
        if (telephonyManager == null) {
            return null;
        }
        // Queries the 15-digit device IMEI.
        String imei = telephonyManager.getImei();
        if (imei == null || imei.length() != DEVICE_IMEI_LEN) {
            Log.i(TAG, "Unable to query valid Mobile Device Identity (IMEI)!");
            return null;
        }
        String imeisv_suffix = telephonyManager.getDeviceSoftwareVersion();
        if (imeisv_suffix == null || imeisv_suffix.length() != DEVICE_IMEISV_SUFFIX_LEN) {
            // Unable to retrieve 2-digit suffix for IMEISV, so returns device IMEI.
            return imei;
        }
        // Splices the first 14 digit of device IMEI with 2-digit SV suffix to form IMEISV.
        return imei.substring(0, imei.length() - 1) + imeisv_suffix;
    }

    private IkeSessionParams buildIkeSessionParams(
            TunnelSetupRequest setupRequest, String apnName, int token)
            throws IwlanSimNotReadyException {
        int hardTimeSeconds =
                getConfig(CarrierConfigManager.Iwlan.KEY_IKE_REKEY_HARD_TIMER_SEC_INT);
        int softTimeSeconds =
                getConfig(CarrierConfigManager.Iwlan.KEY_IKE_REKEY_SOFT_TIMER_SEC_INT);
        if (!isValidIkeSessionLifetime(hardTimeSeconds, softTimeSeconds)) {
            if (hardTimeSeconds > IKE_HARD_LIFETIME_SEC_MAXIMUM
                    && softTimeSeconds > IKE_SOFT_LIFETIME_SEC_MINIMUM) {
                hardTimeSeconds = IKE_HARD_LIFETIME_SEC_MAXIMUM;
                softTimeSeconds = IKE_HARD_LIFETIME_SEC_MAXIMUM - LIFETIME_MARGIN_SEC_MINIMUM;
            } else {
                hardTimeSeconds =
                        IwlanHelper.getDefaultConfig(
                                CarrierConfigManager.Iwlan.KEY_IKE_REKEY_HARD_TIMER_SEC_INT);
                softTimeSeconds =
                        IwlanHelper.getDefaultConfig(
                                CarrierConfigManager.Iwlan.KEY_IKE_REKEY_SOFT_TIMER_SEC_INT);
            }
            Log.d(
                    TAG,
                    "Invalid ike session lifetime values, set hard: "
                            + hardTimeSeconds
                            + ", soft: "
                            + softTimeSeconds);
        }

        IkeSessionParams.Builder builder =
                new IkeSessionParams.Builder(mContext)
                        // permanently hardcode DSCP to 46 (Expedited Forwarding class)
                        // See https://www.iana.org/assignments/dscp-registry/dscp-registry.xhtml
                        // This will make WiFi prioritize IKE signallig under WMM AC_VO
                        .setDscp(46)
                        .setServerHostname(mEpdgAddress.getHostAddress())
                        .setLocalIdentification(getLocalIdentification())
                        .setRemoteIdentification(getId(setupRequest.apnName(), false))
                        .setAuthEap(null, getEapConfig())
                        .addIkeSaProposal(buildIkeSaProposal())
                        .setNetwork(mDefaultNetwork)
                        .addIkeOption(IkeSessionParams.IKE_OPTION_ACCEPT_ANY_REMOTE_ID)
                        .addIkeOption(IkeSessionParams.IKE_OPTION_MOBIKE)
                        .addIkeOption(IkeSessionParams.IKE_OPTION_REKEY_MOBILITY)
                        .setLifetimeSeconds(hardTimeSeconds, softTimeSeconds)
                        .setRetransmissionTimeoutsMillis(getRetransmissionTimeoutsFromConfig())
                        .setDpdDelaySeconds(getDpdDelayFromConfig());

        if (numPdnTunnels() == 0) {
            builder.addIkeOption(IkeSessionParams.IKE_OPTION_INITIAL_CONTACT);
            Log.d(TAG, "IKE_OPTION_INITIAL_CONTACT");
        }

        if ((int) getConfig(CarrierConfigManager.Iwlan.KEY_EPDG_AUTHENTICATION_METHOD_INT)
                == CarrierConfigManager.Iwlan.AUTHENTICATION_METHOD_EAP_ONLY) {
            builder.addIkeOption(IkeSessionParams.IKE_OPTION_EAP_ONLY_AUTH);
        }

        if (setupRequest.requestPcscf()) {
            int proto = setupRequest.apnIpProtocol();
            if (proto == ApnSetting.PROTOCOL_IP || proto == ApnSetting.PROTOCOL_IPV4V6) {
                builder.addPcscfServerRequest(AF_INET);
            }
            if (proto == ApnSetting.PROTOCOL_IPV6 || proto == ApnSetting.PROTOCOL_IPV4V6) {
                builder.addPcscfServerRequest(AF_INET6);
            }
        }

        // If MOBIKE is configured, ePDGs may force IPv6 UDP encapsulation- as specified by
        // RFC 4555- which Android connectivity stack presently does not support.
        if (mEpdgAddress instanceof Inet6Address) {
            builder.removeIkeOption(IkeSessionParams.IKE_OPTION_MOBIKE);
        }

        Ike3gppParams.Builder builder3gppParams = null;

        // TODO(b/239753287): Telus carrier requests DEVICE_IDENTITY, but errors out when parsing
        //  the response. Temporarily disabled.
        if (false) {
            String imei = getMobileDeviceIdentity();
            if (imei != null) {
                if (builder3gppParams == null) {
                    builder3gppParams = new Ike3gppParams.Builder();
                }
                Log.d(TAG, "DEVICE_IDENTITY set in Ike3gppParams");
                builder3gppParams.setMobileDeviceIdentity(imei);
            }
        }

        if (isN1ModeSupported()) {
            if (setupRequest.pduSessionId() != 0) {
                // Configures the PduSession ID in N1_MODE_CAPABILITY payload
                // to notify the server that UE supports N1_MODE
                builder3gppParams = new Ike3gppParams.Builder();
                builder3gppParams.setPduSessionId((byte) setupRequest.pduSessionId());
            }
        }

        if (builder3gppParams != null) {
            Ike3gppExtension extension =
                    new Ike3gppExtension(
                            builder3gppParams.build(), new TmIke3gppCallback(apnName, token));
            builder.setIke3gppExtension(extension);
        }

        int nattKeepAliveTimer =
                getConfig(CarrierConfigManager.Iwlan.KEY_NATT_KEEP_ALIVE_TIMER_SEC_INT);
        if (nattKeepAliveTimer < NATT_KEEPALIVE_DELAY_SEC_MIN
                || nattKeepAliveTimer > NATT_KEEPALIVE_DELAY_SEC_MAX) {
            Log.d(TAG, "Falling back to default natt keep alive timer");
            nattKeepAliveTimer =
                    IwlanHelper.getDefaultConfig(
                            CarrierConfigManager.Iwlan.KEY_NATT_KEEP_ALIVE_TIMER_SEC_INT);
        }
        builder.setNattKeepAliveDelaySeconds(nattKeepAliveTimer);

        return builder.build();
    }

    private boolean isValidChildSessionLifetime(int hardLifetimeSeconds, int softLifetimeSeconds) {
        return hardLifetimeSeconds >= CHILD_HARD_LIFETIME_SEC_MINIMUM
                && hardLifetimeSeconds <= CHILD_HARD_LIFETIME_SEC_MAXIMUM
                && softLifetimeSeconds >= CHILD_SOFT_LIFETIME_SEC_MINIMUM
                && hardLifetimeSeconds - softLifetimeSeconds >= LIFETIME_MARGIN_SEC_MINIMUM;
    }

    private boolean isValidIkeSessionLifetime(int hardLifetimeSeconds, int softLifetimeSeconds) {
        return hardLifetimeSeconds >= IKE_HARD_LIFETIME_SEC_MINIMUM
                && hardLifetimeSeconds <= IKE_HARD_LIFETIME_SEC_MAXIMUM
                && softLifetimeSeconds >= IKE_SOFT_LIFETIME_SEC_MINIMUM
                && hardLifetimeSeconds - softLifetimeSeconds >= LIFETIME_MARGIN_SEC_MINIMUM;
    }

    private <T> T getConfig(String configKey) {
        return IwlanHelper.getConfig(configKey, mContext, mSlotId);
    }

    private IkeSaProposal buildIkeSaProposal() {
        IkeSaProposal.Builder saProposalBuilder = new IkeSaProposal.Builder();

        int[] dhGroups = getConfig(CarrierConfigManager.Iwlan.KEY_DIFFIE_HELLMAN_GROUPS_INT_ARRAY);
        for (int dhGroup : dhGroups) {
            if (validateConfig(dhGroup, VALID_DH_GROUPS, CONFIG_TYPE_DH_GROUP)) {
                saProposalBuilder.addDhGroup(dhGroup);
            }
        }

        int[] encryptionAlgos =
                getConfig(
                        CarrierConfigManager.Iwlan
                                .KEY_SUPPORTED_IKE_SESSION_ENCRYPTION_ALGORITHMS_INT_ARRAY);
        for (int encryptionAlgo : encryptionAlgos) {
            validateConfig(encryptionAlgo, VALID_ENCRYPTION_ALGOS, CONFIG_TYPE_ENCRYPT_ALGO);

            if (encryptionAlgo == SaProposal.ENCRYPTION_ALGORITHM_AES_CBC) {
                int[] aesCbcKeyLens =
                        getConfig(
                                CarrierConfigManager.Iwlan
                                        .KEY_IKE_SESSION_AES_CBC_KEY_SIZE_INT_ARRAY);
                for (int aesCbcKeyLen : aesCbcKeyLens) {
                    if (validateConfig(aesCbcKeyLen, VALID_KEY_LENGTHS, CONFIG_TYPE_KEY_LEN)) {
                        saProposalBuilder.addEncryptionAlgorithm(encryptionAlgo, aesCbcKeyLen);
                    }
                }
            }

            if (encryptionAlgo == SaProposal.ENCRYPTION_ALGORITHM_AES_CTR) {
                int[] aesCtrKeyLens =
                        getConfig(
                                CarrierConfigManager.Iwlan
                                        .KEY_IKE_SESSION_AES_CTR_KEY_SIZE_INT_ARRAY);
                for (int aesCtrKeyLen : aesCtrKeyLens) {
                    if (validateConfig(aesCtrKeyLen, VALID_KEY_LENGTHS, CONFIG_TYPE_KEY_LEN)) {
                        saProposalBuilder.addEncryptionAlgorithm(encryptionAlgo, aesCtrKeyLen);
                    }
                }
            }
        }

        int[] integrityAlgos =
                getConfig(CarrierConfigManager.Iwlan.KEY_SUPPORTED_INTEGRITY_ALGORITHMS_INT_ARRAY);
        for (int integrityAlgo : integrityAlgos) {
            if (validateConfig(integrityAlgo, VALID_INTEGRITY_ALGOS, CONFIG_TYPE_INTEGRITY_ALGO)) {
                saProposalBuilder.addIntegrityAlgorithm(integrityAlgo);
            }
        }

        int[] prfAlgos =
                getConfig(CarrierConfigManager.Iwlan.KEY_SUPPORTED_PRF_ALGORITHMS_INT_ARRAY);
        for (int prfAlgo : prfAlgos) {
            if (validateConfig(prfAlgo, VALID_PRF_ALGOS, CONFIG_TYPE_PRF_ALGO)) {
                saProposalBuilder.addPseudorandomFunction(prfAlgo);
            }
        }

        return saProposalBuilder.build();
    }

    private boolean validateConfig(int config, Set<Integer> validConfigValues, String configType) {
        if (validConfigValues.contains(config)) {
            return true;
        }

        Log.e(TAG, "Invalid config value for " + configType + ":" + config);
        return false;
    }

    private ChildSaProposal buildChildSaProposal() {
        ChildSaProposal.Builder saProposalBuilder = new ChildSaProposal.Builder();

        // IKE library doesn't add KE payload if dh groups are not set in child session params.
        // Use the same groups as that of IKE session.
        if (getConfig(CarrierConfigManager.Iwlan.KEY_ADD_KE_TO_CHILD_SESSION_REKEY_BOOL)) {
            int[] dhGroups =
                    getConfig(CarrierConfigManager.Iwlan.KEY_DIFFIE_HELLMAN_GROUPS_INT_ARRAY);
            for (int dhGroup : dhGroups) {
                if (validateConfig(dhGroup, VALID_DH_GROUPS, CONFIG_TYPE_DH_GROUP)) {
                    saProposalBuilder.addDhGroup(dhGroup);
                }
            }
        }

        int[] encryptionAlgos =
                getConfig(
                        CarrierConfigManager.Iwlan
                                .KEY_SUPPORTED_CHILD_SESSION_ENCRYPTION_ALGORITHMS_INT_ARRAY);
        for (int encryptionAlgo : encryptionAlgos) {
            if (validateConfig(encryptionAlgo, VALID_ENCRYPTION_ALGOS, CONFIG_TYPE_ENCRYPT_ALGO)) {
                if (ChildSaProposal.getSupportedEncryptionAlgorithms().contains(encryptionAlgo)) {
                    if (encryptionAlgo == SaProposal.ENCRYPTION_ALGORITHM_AES_CBC) {
                        int[] aesCbcKeyLens =
                                getConfig(
                                        CarrierConfigManager.Iwlan
                                                .KEY_CHILD_SESSION_AES_CBC_KEY_SIZE_INT_ARRAY);
                        for (int aesCbcKeyLen : aesCbcKeyLens) {
                            if (validateConfig(
                                    aesCbcKeyLen, VALID_KEY_LENGTHS, CONFIG_TYPE_KEY_LEN)) {
                                saProposalBuilder.addEncryptionAlgorithm(
                                        encryptionAlgo, aesCbcKeyLen);
                            }
                        }
                    }

                    if (encryptionAlgo == SaProposal.ENCRYPTION_ALGORITHM_AES_CTR) {
                        int[] aesCtrKeyLens =
                                getConfig(
                                        CarrierConfigManager.Iwlan
                                                .KEY_CHILD_SESSION_AES_CTR_KEY_SIZE_INT_ARRAY);
                        for (int aesCtrKeyLen : aesCtrKeyLens) {
                            if (validateConfig(
                                    aesCtrKeyLen, VALID_KEY_LENGTHS, CONFIG_TYPE_KEY_LEN)) {
                                saProposalBuilder.addEncryptionAlgorithm(
                                        encryptionAlgo, aesCtrKeyLen);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Device does not support encryption alog:  " + encryptionAlgo);
                }
            }
        }

        int[] integrityAlgos =
                getConfig(CarrierConfigManager.Iwlan.KEY_SUPPORTED_INTEGRITY_ALGORITHMS_INT_ARRAY);
        for (int integrityAlgo : integrityAlgos) {
            if (validateConfig(integrityAlgo, VALID_INTEGRITY_ALGOS, CONFIG_TYPE_INTEGRITY_ALGO)) {
                if (ChildSaProposal.getSupportedIntegrityAlgorithms().contains(integrityAlgo)) {
                    saProposalBuilder.addIntegrityAlgorithm(integrityAlgo);
                } else {
                    Log.w(TAG, "Device does not support integrity alog:  " + integrityAlgo);
                }
            }
        }

        return saProposalBuilder.build();
    }

    private IkeIdentification getLocalIdentification() throws IwlanSimNotReadyException {
        String nai;

        nai = IwlanHelper.getNai(mContext, mSlotId, mNextReauthId);

        if (nai == null) {
            throw new IwlanSimNotReadyException("Nai is null.");
        }

        Log.d(TAG, "getLocalIdentification: Nai: " + nai);
        return getId(nai, true);
    }

    private IkeIdentification getId(String id, boolean isLocal) {
        String idTypeConfig =
                isLocal
                        ? CarrierConfigManager.Iwlan.KEY_IKE_LOCAL_ID_TYPE_INT
                        : CarrierConfigManager.Iwlan.KEY_IKE_REMOTE_ID_TYPE_INT;
        int idType = getConfig(idTypeConfig);
        switch (idType) {
            case CarrierConfigManager.Iwlan.ID_TYPE_FQDN:
                return new IkeFqdnIdentification(id);
            case CarrierConfigManager.Iwlan.ID_TYPE_KEY_ID:
                return new IkeKeyIdIdentification(id.getBytes(StandardCharsets.US_ASCII));
            case CarrierConfigManager.Iwlan.ID_TYPE_RFC822_ADDR:
                return new IkeRfc822AddrIdentification(id);
            default:
                throw new IllegalArgumentException("Invalid local Identity type: " + idType);
        }
    }

    private EapSessionConfig getEapConfig() throws IwlanSimNotReadyException {
        int subId = IwlanHelper.getSubId(mContext, mSlotId);
        String nai = IwlanHelper.getNai(mContext, mSlotId, null);

        if (nai == null) {
            throw new IwlanSimNotReadyException("Nai is null.");
        }

        EapSessionConfig.EapAkaOption option = null;
        if (mNextReauthId != null) {
            option = new EapSessionConfig.EapAkaOption.Builder().setReauthId(mNextReauthId).build();
        }

        Log.d(TAG, "getEapConfig: Nai: " + nai);
        return new EapSessionConfig.Builder()
                .setEapAkaConfig(subId, TelephonyManager.APPTYPE_USIM, option)
                .setEapIdentity(nai.getBytes(StandardCharsets.US_ASCII))
                .build();
    }

    private void onSessionClosedWithException(
            IkeException exception, String apnName, int token, int sessionType) {
        Log.e(
                TAG,
                "Closing tunnel with exception for apn: "
                        + apnName
                        + " with token: "
                        + token
                        + " sessionType:"
                        + sessionType);
        exception.printStackTrace();

        mHandler.sendMessage(
                mHandler.obtainMessage(
                        sessionType, new SessionClosedData(apnName, token, exception)));
    }

    private boolean isEpdgSelectionOrFirstTunnelBringUpInProgress() {
        // Tunnel config is created but not connected to an ePDG. i.e., The first bring-up request
        // in progress.
        // No bring-up request in progress but pending queue is not empty. i.e. ePDG selection in
        // progress
        return (!mHasConnectedToEpdg && !mApnNameToTunnelConfig.isEmpty())
                || !mPendingBringUpRequests.isEmpty();
    }

    private IwlanError getErrorFromIkeException(
            IkeException ikeException, IkeSessionState ikeSessionState) {
        IwlanError error;
        if (ikeException instanceof IkeIOException) {
            error = new IwlanError(ikeSessionState.getErrorType(), ikeException);
        } else {
            error = new IwlanError(ikeException);
        }
        Log.e(TAG, "Closing tunnel: error: " + error + " state: " + ikeSessionState);
        return error;
    }

    private final class TmHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what = " + eventToString(msg.what));

            String apnName;
            TunnelConfig tunnelConfig;
            OnClosedMetrics.Builder onClosedMetricsBuilder;
            switch (msg.what) {
                case EVENT_CHILD_SESSION_OPENED:
                case EVENT_IKE_SESSION_CLOSED:
                case EVENT_IPSEC_TRANSFORM_CREATED:
                case EVENT_IPSEC_TRANSFORM_DELETED:
                case EVENT_CHILD_SESSION_CLOSED:
                case EVENT_IKE_SESSION_OPENED:
                case EVENT_IKE_SESSION_CONNECTION_INFO_CHANGED:
                case EVENT_IKE_3GPP_DATA_RECEIVED:
                    IkeEventData ikeEventData = (IkeEventData) msg.obj;
                    if (isObsoleteToken(ikeEventData.mApnName, ikeEventData.mToken)) {
                        Log.d(
                                TAG,
                                eventToString(msg.what)
                                        + " for obsolete token "
                                        + ikeEventData.mToken);
                        return;
                    }
            }

            long mIkeTunnelEstablishmentDuration;
            switch (msg.what) {
                case EVENT_TUNNEL_BRINGUP_REQUEST:
                    TunnelRequestWrapper tunnelRequestWrapper = (TunnelRequestWrapper) msg.obj;
                    TunnelSetupRequest setupRequest = tunnelRequestWrapper.getSetupRequest();
                    IwlanError bringUpError = null;

                    onClosedMetricsBuilder =
                            new OnClosedMetrics.Builder().setApnName(setupRequest.apnName());

                    if (IwlanHelper.getSubId(mContext, mSlotId)
                            == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        Log.e(TAG, "SIM isn't ready");
                        bringUpError = new IwlanError(IwlanError.SIM_NOT_READY_EXCEPTION);
                        reportIwlanError(setupRequest.apnName(), bringUpError);
                    } else if (Objects.isNull(mDefaultNetwork)) {
                        Log.e(TAG, "The default network is not ready");
                        bringUpError = new IwlanError(IwlanError.IKE_INTERNAL_IO_EXCEPTION);
                        reportIwlanError(setupRequest.apnName(), bringUpError);
                    } else if (!canBringUpTunnel(setupRequest.apnName())) {
                        Log.d(TAG, "Cannot bring up tunnel as retry time has not passed");
                        bringUpError = getLastError(setupRequest.apnName());
                    }

                    if (Objects.nonNull(bringUpError)) {
                        tunnelRequestWrapper
                                .getTunnelCallback()
                                .onClosed(setupRequest.apnName(), bringUpError);
                        tunnelRequestWrapper
                                .getTunnelMetrics()
                                .onClosed(onClosedMetricsBuilder.build());
                        return;
                    }

                    if (mHasConnectedToEpdg) {
                        // Service the request immediately when epdg address is available
                        onBringUpTunnel(
                                setupRequest,
                                tunnelRequestWrapper.getTunnelCallback(),
                                tunnelRequestWrapper.getTunnelMetrics());
                        break;
                    }

                    if (!isEpdgSelectionOrFirstTunnelBringUpInProgress()) {
                        // No tunnel bring-up in progress. Select the ePDG address first
                        selectEpdgAddress(setupRequest);
                    }

                    // Another bring-up or ePDG selection is in progress, pending this request.
                    mPendingBringUpRequests.add(tunnelRequestWrapper);
                    break;

                case EVENT_EPDG_ADDRESS_SELECTION_REQUEST_COMPLETE:
                    EpdgSelectorResult selectorResult = (EpdgSelectorResult) msg.obj;
                    printRequestQueue("EVENT_EPDG_ADDRESS_SELECTION_REQUEST_COMPLETE");

                    if (selectorResult.getTransactionId() != mTransactionId) {
                        Log.e(TAG, "Mismatched transactionId");
                        break;
                    }

                    if (mPendingBringUpRequests.isEmpty()) {
                        Log.d(TAG, "Empty request queue");
                        break;
                    }

                    if (selectorResult.getEpdgError().getErrorType() == IwlanError.NO_ERROR
                            && selectorResult.getValidIpList() != null) {
                        tunnelRequestWrapper = mPendingBringUpRequests.remove();
                        validateAndSetEpdgAddress(selectorResult.getValidIpList());
                        onBringUpTunnel(
                                tunnelRequestWrapper.getSetupRequest(),
                                tunnelRequestWrapper.getTunnelCallback(),
                                tunnelRequestWrapper.getTunnelMetrics());
                    } else {
                        IwlanError error =
                                (selectorResult.getEpdgError().getErrorType()
                                                == IwlanError.NO_ERROR)
                                        ? new IwlanError(
                                                IwlanError.EPDG_SELECTOR_SERVER_SELECTION_FAILED)
                                        : selectorResult.getEpdgError();
                        failAllPendingRequests(error);
                    }
                    break;

                case EVENT_CHILD_SESSION_OPENED:
                    TunnelOpenedData tunnelOpenedData = (TunnelOpenedData) msg.obj;
                    apnName = tunnelOpenedData.mApnName;
                    tunnelConfig = mApnNameToTunnelConfig.get(apnName);

                    tunnelConfig.setDnsAddrList(tunnelOpenedData.mInternalDnsServers);
                    tunnelConfig.setInternalAddrList(tunnelOpenedData.mInternalAddresses);

                    IpSecManager.IpSecTunnelInterface tunnelInterface = tunnelConfig.getIface();

                    for (LinkAddress address : tunnelConfig.getInternalAddrList()) {
                        try {
                            tunnelInterface.addAddress(
                                    address.getAddress(), address.getPrefixLength());
                        } catch (IOException e) {
                            Log.e(TAG, "Adding internal addresses to interface failed.");
                        }
                    }

                    TunnelLinkProperties linkProperties =
                            TunnelLinkProperties.builder()
                                    .setInternalAddresses(tunnelConfig.getInternalAddrList())
                                    .setDnsAddresses(tunnelConfig.getDnsAddrList())
                                    .setPcscfAddresses(tunnelConfig.getPcscfAddrList())
                                    .setIfaceName(tunnelConfig.getIface().getInterfaceName())
                                    .setSliceInfo(tunnelConfig.getSliceInfo())
                                    .build();
                    tunnelConfig.getTunnelCallback().onOpened(apnName, linkProperties);

                    reportIwlanError(apnName, new IwlanError(IwlanError.NO_ERROR));

                    mIkeTunnelEstablishmentDuration =
                            System.currentTimeMillis() - mIkeTunnelEstablishmentStartTime;
                    mIkeTunnelEstablishmentStartTime = 0;
                    tunnelConfig
                            .getTunnelMetrics()
                            .onOpened(
                                    new OnOpenedMetrics.Builder()
                                            .setApnName(apnName)
                                            .setEpdgServerAddress(mEpdgAddress)
                                            .setEpdgServerSelectionDuration(
                                                    (int) mEpdgServerSelectionDuration)
                                            .setIkeTunnelEstablishmentDuration(
                                                    (int) mIkeTunnelEstablishmentDuration)
                                            .build());

                    onConnectedToEpdg(true);
                    mValidEpdgInfo.resetIndex();
                    printRequestQueue("EVENT_CHILD_SESSION_OPENED");
                    serviceAllPendingRequests();
                    tunnelConfig.setIkeSessionState(IkeSessionState.CHILD_SESSION_OPENED);
                    break;

                case EVENT_IKE_SESSION_CLOSED:
                    printRequestQueue("EVENT_IKE_SESSION_CLOSED");
                    SessionClosedData sessionClosedData = (SessionClosedData) msg.obj;
                    apnName = sessionClosedData.mApnName;

                    tunnelConfig = mApnNameToTunnelConfig.get(apnName);
                    if (tunnelConfig == null) {
                        Log.e(TAG, "No callback found for apn: " + apnName);
                        return;
                    }

                    // If IKE session closed exceptionally, we retrieve IwlanError directly from the
                    // exception; otherwise, it is still possible that we triggered an IKE session
                    // close due to an error (e.g. IwlanError.TUNNEL_TRANSFORM_FAILED), or because
                    // the Child session closed exceptionally; in which case, we attempt to retrieve
                    // the stored error (if any) from TunnelConfig.
                    IwlanError iwlanError;
                    if (sessionClosedData.mIkeException != null) {
                        iwlanError =
                                getErrorFromIkeException(
                                        sessionClosedData.mIkeException,
                                        tunnelConfig.getIkeSessionState());
                    } else {
                        // If IKE session opened, then closed before child session (and IWLAN
                        // tunnel) opened.
                        // Iwlan reports IKE_SESSION_CLOSED_BEFORE_CHILD_SESSION_OPENED
                        // instead of NO_ERROR
                        if (!tunnelConfig.hasTunnelOpened()) {
                            iwlanError = new IwlanError(
                                    IwlanError.IKE_SESSION_CLOSED_BEFORE_CHILD_SESSION_OPENED);
                        } else {
                            iwlanError = tunnelConfig.getError();
                        }
                    }

                    IpSecManager.IpSecTunnelInterface iface = tunnelConfig.getIface();
                    if (iface != null) {
                        iface.close();
                    }

                    if (!tunnelConfig.hasTunnelOpened()) {
                        if (tunnelConfig.isBackoffTimeValid()) {
                            reportIwlanError(apnName, iwlanError, tunnelConfig.getBackoffTime());
                        } else {
                            reportIwlanError(apnName, iwlanError);
                        }
                    }

                    Log.d(TAG, "Tunnel Closed: " + iwlanError);
                    tunnelConfig.setIkeSessionState(IkeSessionState.NO_IKE_SESSION);
                    tunnelConfig.getTunnelCallback().onClosed(apnName, iwlanError);
                    onClosedMetricsBuilder = new OnClosedMetrics.Builder().setApnName(apnName);

                    if (!mHasConnectedToEpdg) {
                        failAllPendingRequests(iwlanError);
                        tunnelConfig.getTunnelMetrics().onClosed(onClosedMetricsBuilder.build());
                    } else {
                        mIkeTunnelEstablishmentDuration =
                                mIkeTunnelEstablishmentStartTime > 0
                                        ? System.currentTimeMillis()
                                                - mIkeTunnelEstablishmentStartTime
                                        : 0;
                        mIkeTunnelEstablishmentStartTime = 0;

                        onClosedMetricsBuilder
                                .setEpdgServerAddress(mEpdgAddress)
                                .setEpdgServerSelectionDuration((int) mEpdgServerSelectionDuration)
                                .setIkeTunnelEstablishmentDuration(
                                        (int) mIkeTunnelEstablishmentDuration);
                        tunnelConfig.getTunnelMetrics().onClosed(onClosedMetricsBuilder.build());
                    }

                    mApnNameToTunnelConfig.remove(apnName);
                    if (mApnNameToTunnelConfig.size() == 0 && mPendingBringUpRequests.isEmpty()) {
                        onConnectedToEpdg(false);
                    }

                    break;

                case EVENT_UPDATE_NETWORK:
                    UpdateNetworkWrapper updatedNetwork = (UpdateNetworkWrapper) msg.obj;
                    mDefaultNetwork = updatedNetwork.getNetwork();
                    LinkProperties defaultLinkProperties = updatedNetwork.getLinkProperties();
                    String paraString = "Network: " + mDefaultNetwork;

                    if (mHasConnectedToEpdg) {
                        if (Objects.isNull(mDefaultNetwork)) {
                            Log.w(TAG, "The default network has been removed.");
                        } else if (Objects.isNull(defaultLinkProperties)) {
                            Log.w(
                                    TAG,
                                    "The default network's LinkProperties is not ready ."
                                            + paraString);
                        } else if (!defaultLinkProperties.isReachable(mEpdgAddress)) {
                            Log.w(
                                    TAG,
                                    "The default network link "
                                            + defaultLinkProperties
                                            + " doesn't have a route to the ePDG "
                                            + mEpdgAddress
                                            + paraString);
                        } else if (Objects.equals(mDefaultNetwork, mIkeSessionNetwork)) {
                            Log.w(
                                    TAG,
                                    "The default network has not changed from the IKE session"
                                            + " network. "
                                            + paraString);
                        } else {
                            mApnNameToTunnelConfig.forEach(
                                    (apn, config) -> {
                                        Log.d(
                                                TAG,
                                                "The Underlying Network is updating for APN (+"
                                                        + apn
                                                        + "). "
                                                        + paraString);
                                        config.getIkeSession().setNetwork(mDefaultNetwork);
                                        config.setIkeSessionState(
                                                IkeSessionState.IKE_MOBILITY_IN_PROGRESS);
                                    });
                            mIkeSessionNetwork = mDefaultNetwork;
                        }
                    }
                    break;

                case EVENT_TUNNEL_BRINGDOWN_REQUEST:
                    TunnelBringdownRequest bringdownRequest = (TunnelBringdownRequest) msg.obj;
                    apnName = bringdownRequest.mApnName;
                    boolean forceClose = bringdownRequest.mForceClose;
                    tunnelConfig = mApnNameToTunnelConfig.get(apnName);
                    if (tunnelConfig == null) {
                        Log.w(
                                TAG,
                                "Bringdown request: No tunnel exists for apn: "
                                        + apnName
                                        + "forced: "
                                        + forceClose);
                    } else {
                        if (forceClose) {
                            tunnelConfig.getIkeSession().kill();
                        } else {
                            tunnelConfig.getIkeSession().close();
                        }
                    }
                    int numClosed = closePendingRequestsForApn(apnName);
                    if (numClosed > 0) {
                        Log.d(TAG, "Closed " + numClosed + " pending requests for apn: " + apnName);
                    }
                    if (tunnelConfig == null && numClosed == 0) {
                        // IwlanDataService expected to close a (pending or up) tunnel but was not
                        // found. Recovers state in IwlanDataService through TunnelCallback.
                        iwlanError = new IwlanError(IwlanError.TUNNEL_NOT_FOUND);
                        reportIwlanError(apnName, iwlanError);
                        bringdownRequest.mTunnelCallback.onClosed(apnName, iwlanError);
                        bringdownRequest.mIwlanTunnelMetrics.onClosed(
                                new OnClosedMetrics.Builder().setApnName(apnName).build());
                    }
                    break;

                case EVENT_IPSEC_TRANSFORM_CREATED:
                    IpsecTransformData transformData = (IpsecTransformData) msg.obj;
                    apnName = transformData.getApnName();
                    IpSecManager ipSecManager = mContext.getSystemService(IpSecManager.class);
                    tunnelConfig = mApnNameToTunnelConfig.get(apnName);

                    if (tunnelConfig.getIface() == null) {
                        try {
                            tunnelConfig.setIface(
                                    ipSecManager.createIpSecTunnelInterface(
                                            DUMMY_ADDR /* unused */,
                                            DUMMY_ADDR /* unused */,
                                            mDefaultNetwork));
                        } catch (IpSecManager.ResourceUnavailableException | IOException e) {
                            Log.e(TAG, "Failed to create tunnel interface. " + e);
                            closeIkeSession(
                                    apnName, new IwlanError(IwlanError.TUNNEL_TRANSFORM_FAILED));
                            return;
                        }
                    }

                    try {
                        assert ipSecManager != null;
                        ipSecManager.applyTunnelModeTransform(
                                tunnelConfig.getIface(),
                                transformData.getDirection(),
                                transformData.getTransform());
                    } catch (IOException | IllegalArgumentException e) {
                        // If the IKE session was closed before the transform could be applied, the
                        // IpSecService will throw an IAE on processing the IpSecTunnelInterface id.
                        Log.e(TAG, "Failed to apply tunnel transform." + e);
                        closeIkeSession(
                                apnName, new IwlanError(IwlanError.TUNNEL_TRANSFORM_FAILED));
                    }
                    if (tunnelConfig.getIkeSessionState()
                            == IkeSessionState.IKE_MOBILITY_IN_PROGRESS) {
                        tunnelConfig.setIkeSessionState(IkeSessionState.CHILD_SESSION_OPENED);
                    }
                    break;

                case EVENT_IPSEC_TRANSFORM_DELETED:
                    transformData = (IpsecTransformData) msg.obj;
                    IpSecTransform transform = transformData.getTransform();
                    transform.close();
                    break;

                case EVENT_CHILD_SESSION_CLOSED:
                    sessionClosedData = (SessionClosedData) msg.obj;
                    apnName = sessionClosedData.mApnName;

                    tunnelConfig = mApnNameToTunnelConfig.get(apnName);
                    if (tunnelConfig == null) {
                        Log.d(TAG, "No tunnel callback for apn: " + apnName);
                        return;
                    }
                    if (sessionClosedData.mIkeException != null) {
                        tunnelConfig.setError(
                                getErrorFromIkeException(
                                        sessionClosedData.mIkeException,
                                        tunnelConfig.getIkeSessionState()));
                    }
                    tunnelConfig.getIkeSession().close();
                    break;

                case EVENT_IKE_SESSION_OPENED:
                    IkeSessionOpenedData ikeSessionOpenedData = (IkeSessionOpenedData) msg.obj;
                    apnName = ikeSessionOpenedData.mApnName;
                    IkeSessionConfiguration sessionConfiguration =
                            ikeSessionOpenedData.mIkeSessionConfiguration;

                    tunnelConfig = mApnNameToTunnelConfig.get(apnName);
                    tunnelConfig.setPcscfAddrList(sessionConfiguration.getPcscfServers());

                    boolean enabledFastReauth =
                            getConfig(
                                    CarrierConfigManager.Iwlan
                                            .KEY_SUPPORTS_EAP_AKA_FAST_REAUTH_BOOL);
                    Log.d(
                            TAG,
                            "CarrierConfigManager.Iwlan.KEY_SUPPORTS_EAP_AKA_FAST_REAUTH_BOOL "
                                    + enabledFastReauth);

                    if (enabledFastReauth) {
                        EapInfo eapInfo = sessionConfiguration.getEapInfo();
                        if (eapInfo instanceof EapAkaInfo) {
                            mNextReauthId = ((EapAkaInfo) eapInfo).getReauthId();
                            Log.d(TAG, "Update ReauthId: " + Arrays.toString(mNextReauthId));
                        } else {
                            mNextReauthId = null;
                        }
                    }
                    break;

                case EVENT_IKE_SESSION_CONNECTION_INFO_CHANGED:
                    IkeSessionConnectionInfoData ikeSessionConnectionInfoData =
                            (IkeSessionConnectionInfoData) msg.obj;
                    Network network =
                            ikeSessionConnectionInfoData.mIkeSessionConnectionInfo.getNetwork();
                    apnName = ikeSessionConnectionInfoData.mApnName;

                    ConnectivityManager connectivityManager =
                            mContext.getSystemService(ConnectivityManager.class);
                    if (Objects.requireNonNull(connectivityManager).getLinkProperties(network)
                            == null) {
                        Log.e(TAG, "Network " + network + " has null LinkProperties!");
                        return;
                    }

                    tunnelConfig = mApnNameToTunnelConfig.get(apnName);
                    tunnelInterface = tunnelConfig.getIface();
                    try {
                        tunnelInterface.setUnderlyingNetwork(network);
                    } catch (IOException | IllegalArgumentException e) {
                        Log.e(
                                TAG,
                                "Failed to update underlying network for apn: "
                                        + apnName
                                        + " exception: "
                                        + e);
                    }
                    break;

                case EVENT_IKE_3GPP_DATA_RECEIVED:
                    Ike3gppDataReceived ike3gppDataReceived = (Ike3gppDataReceived) msg.obj;
                    apnName = ike3gppDataReceived.mApnName;
                    List<Ike3gppData> ike3gppData = ike3gppDataReceived.mIke3gppData;
                    if (ike3gppData != null && !ike3gppData.isEmpty()) {
                        tunnelConfig = mApnNameToTunnelConfig.get(apnName);
                        for (Ike3gppData payload : ike3gppData) {
                            if (payload.getDataType() == DATA_TYPE_NOTIFY_N1_MODE_INFORMATION) {
                                Log.d(TAG, "Got payload DATA_TYPE_NOTIFY_N1_MODE_INFORMATION");
                                NetworkSliceInfo si =
                                        NetworkSliceSelectionAssistanceInformation.getSliceInfo(
                                                ((Ike3gppN1ModeInformation) payload).getSnssai());
                                if (si != null) {
                                    tunnelConfig.setSliceInfo(si);
                                    Log.d(TAG, "SliceInfo: " + si);
                                }
                            } else if (payload.getDataType() == DATA_TYPE_NOTIFY_BACKOFF_TIMER) {
                                Log.d(TAG, "Got payload DATA_TYPE_NOTIFY_BACKOFF_TIMER");
                                long backoffTime =
                                        decodeBackoffTime(
                                                ((Ike3gppBackoffTimer) payload).getBackoffTimer());
                                if (backoffTime > 0) {
                                    tunnelConfig.setBackoffTime(backoffTime);
                                    Log.d(TAG, "Backoff Timer: " + backoffTime);
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Null or empty payloads received:");
                    }
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }

        TmHandler(Looper looper) {
            super(looper);
        }
    }

    private void closeIkeSession(String apnName, IwlanError error) {
        TunnelConfig tunnelConfig = mApnNameToTunnelConfig.get(apnName);
        tunnelConfig.setError(error);
        tunnelConfig.getIkeSession().close();
    }

    private void selectEpdgAddress(TunnelSetupRequest setupRequest) {
        ++mTransactionId;
        mEpdgServerSelectionStartTime = System.currentTimeMillis();

        final int ipPreference =
                IwlanHelper.getConfig(
                        CarrierConfigManager.Iwlan.KEY_EPDG_ADDRESS_IP_TYPE_PREFERENCE_INT,
                        mContext,
                        mSlotId);

        IpPreferenceConflict ipPreferenceConflict =
                isIpPreferenceConflictsWithNetwork(ipPreference);
        if (ipPreferenceConflict.mIsConflict) {
            sendSelectionRequestComplete(
                    null, new IwlanError(ipPreferenceConflict.mErrorType), mTransactionId);
            return;
        }

        int protoFilter = EpdgSelector.PROTO_FILTER_IPV4V6;
        int epdgAddressOrder = EpdgSelector.SYSTEM_PREFERRED;
        switch (ipPreference) {
            case CarrierConfigManager.Iwlan.EPDG_ADDRESS_IPV4_PREFERRED:
                epdgAddressOrder = EpdgSelector.IPV4_PREFERRED;
                break;
            case CarrierConfigManager.Iwlan.EPDG_ADDRESS_IPV6_PREFERRED:
                epdgAddressOrder = EpdgSelector.IPV6_PREFERRED;
                break;
            case CarrierConfigManager.Iwlan.EPDG_ADDRESS_IPV4_ONLY:
                protoFilter = EpdgSelector.PROTO_FILTER_IPV4;
                break;
            case CarrierConfigManager.Iwlan.EPDG_ADDRESS_IPV6_ONLY:
                protoFilter = EpdgSelector.PROTO_FILTER_IPV6;
                break;
            case CarrierConfigManager.Iwlan.EPDG_ADDRESS_SYSTEM_PREFERRED:
                break;
            default:
                Log.w(TAG, "Invalid Ip preference : " + ipPreference);
        }

        EpdgSelector epdgSelector = getEpdgSelector();
        IwlanError epdgError =
                epdgSelector.getValidatedServerList(
                        mTransactionId,
                        protoFilter,
                        epdgAddressOrder,
                        setupRequest.isRoaming(),
                        setupRequest.isEmergency(),
                        mDefaultNetwork,
                        mSelectorCallback);

        if (epdgError.getErrorType() != IwlanError.NO_ERROR) {
            Log.e(TAG, "Epdg address selection failed with error:" + epdgError);
            sendSelectionRequestComplete(null, epdgError, mTransactionId);
        }
    }

    @VisibleForTesting
    EpdgSelector getEpdgSelector() {
        return EpdgSelector.getSelectorInstance(mContext, mSlotId);
    }

    @VisibleForTesting
    int closePendingRequestsForApn(String apnName) {
        int numRequestsClosed = 0;
        int queueSize = mPendingBringUpRequests.size();
        if (queueSize == 0) {
            return numRequestsClosed;
        }

        for (int count = 0; count < queueSize; count++) {
            TunnelRequestWrapper requestWrapper = mPendingBringUpRequests.remove();
            if (requestWrapper.getSetupRequest().apnName().equals(apnName)) {
                requestWrapper
                        .getTunnelCallback()
                        .onClosed(apnName, new IwlanError(IwlanError.NO_ERROR));

                requestWrapper
                        .getTunnelMetrics()
                        .onClosed(
                                new OnClosedMetrics.Builder()
                                        .setApnName(apnName)
                                        .setEpdgServerAddress(mEpdgAddress)
                                        .build());
                numRequestsClosed++;
            } else {
                mPendingBringUpRequests.add(requestWrapper);
            }
        }
        return numRequestsClosed;
    }

    @VisibleForTesting
    void validateAndSetEpdgAddress(List<InetAddress> selectorResultList) {
        List<InetAddress> addrList = mValidEpdgInfo.getAddrList();
        if (addrList == null || !addrList.equals(selectorResultList)) {
            Log.d(TAG, "Update ePDG address list.");
            mValidEpdgInfo.setAddrList(selectorResultList);
            addrList = mValidEpdgInfo.getAddrList();
        }

        int index = mValidEpdgInfo.getIndex();
        Log.d(
                TAG,
                "Valid ePDG Address List: "
                        + Arrays.toString(addrList.toArray())
                        + ", index = "
                        + index);
        mEpdgAddress = addrList.get(index);
        mValidEpdgInfo.incrementIndex();
    }

    private void serviceAllPendingRequests() {
        while (!mPendingBringUpRequests.isEmpty()) {
            Log.d(TAG, "serviceAllPendingRequests");
            TunnelRequestWrapper request = mPendingBringUpRequests.remove();
            onBringUpTunnel(
                    request.getSetupRequest(),
                    request.getTunnelCallback(),
                    request.getTunnelMetrics());
        }
    }

    private void failAllPendingRequests(IwlanError error) {
        while (!mPendingBringUpRequests.isEmpty()) {
            Log.d(TAG, "failAllPendingRequests");
            TunnelRequestWrapper request = mPendingBringUpRequests.remove();
            TunnelSetupRequest setupRequest = request.getSetupRequest();
            reportIwlanError(setupRequest.apnName(), error);
            request.getTunnelCallback().onClosed(setupRequest.apnName(), error);
            request.getTunnelMetrics()
                    .onClosed(
                            new OnClosedMetrics.Builder()
                                    .setApnName(setupRequest.apnName())
                                    .setEpdgServerAddress(mEpdgAddress)
                                    .build());
        }
    }

    // Prints mPendingBringUpRequests
    private void printRequestQueue(String info) {
        Log.d(TAG, info);
        Log.d(
                TAG,
                "mPendingBringUpRequests: " + Arrays.toString(mPendingBringUpRequests.toArray()));
    }

    // Update Network wrapper
    private static final class UpdateNetworkWrapper {
        private final Network mNetwork;
        private final LinkProperties mLinkProperties;

        private UpdateNetworkWrapper(Network network, LinkProperties linkProperties) {
            mNetwork = network;
            mLinkProperties = linkProperties;
        }

        public Network getNetwork() {
            return mNetwork;
        }

        public LinkProperties getLinkProperties() {
            return mLinkProperties;
        }
    }

    // Tunnel request + tunnel callback
    private static final class TunnelRequestWrapper {
        private final TunnelSetupRequest mSetupRequest;

        private final TunnelCallback mTunnelCallback;
        private final TunnelMetricsInterface mTunnelMetrics;

        private TunnelRequestWrapper(
                TunnelSetupRequest setupRequest,
                TunnelCallback tunnelCallback,
                TunnelMetricsInterface tunnelMetrics) {
            mTunnelCallback = tunnelCallback;
            mSetupRequest = setupRequest;
            mTunnelMetrics = tunnelMetrics;
        }

        public TunnelSetupRequest getSetupRequest() {
            return mSetupRequest;
        }

        public TunnelCallback getTunnelCallback() {
            return mTunnelCallback;
        }

        public TunnelMetricsInterface getTunnelMetrics() {
            return mTunnelMetrics;
        }
    }

    private static final class TunnelBringdownRequest {
        final String mApnName;
        final boolean mForceClose;
        final TunnelCallback mTunnelCallback;
        final IwlanTunnelMetricsImpl mIwlanTunnelMetrics;

        private TunnelBringdownRequest(
                String apnName,
                boolean forceClose,
                TunnelCallback tunnelCallback,
                IwlanTunnelMetricsImpl iwlanTunnelMetrics) {
            mApnName = apnName;
            mForceClose = forceClose;
            mTunnelCallback = tunnelCallback;
            mIwlanTunnelMetrics = iwlanTunnelMetrics;
        }
    }

    private static final class EpdgSelectorResult {
        private final List<InetAddress> mValidIpList;

        public List<InetAddress> getValidIpList() {
            return mValidIpList;
        }

        public IwlanError getEpdgError() {
            return mEpdgError;
        }

        public int getTransactionId() {
            return mTransactionId;
        }

        private final IwlanError mEpdgError;
        private final int mTransactionId;

        private EpdgSelectorResult(
                List<InetAddress> validIpList, IwlanError epdgError, int transactionId) {
            mValidIpList = validIpList;
            mEpdgError = epdgError;
            mTransactionId = transactionId;
        }
    }

    // Data received from IkeSessionStateMachine on successful EVENT_CHILD_SESSION_OPENED.
    private static final class TunnelOpenedData extends IkeEventData {
        final List<InetAddress> mInternalDnsServers;
        final List<LinkAddress> mInternalAddresses;

        private TunnelOpenedData(
                String apnName,
                int token,
                List<InetAddress> internalDnsServers,
                List<LinkAddress> internalAddresses) {
            super(apnName, token);
            mInternalDnsServers = internalDnsServers;
            mInternalAddresses = internalAddresses;
        }
    }

    // Data received from IkeSessionStateMachine on successful EVENT_IKE_SESSION_OPENED.
    private static final class IkeSessionOpenedData extends IkeEventData {
        final IkeSessionConfiguration mIkeSessionConfiguration;

        private IkeSessionOpenedData(
                String apnName, int token, IkeSessionConfiguration ikeSessionConfiguration) {
            super(apnName, token);
            mIkeSessionConfiguration = ikeSessionConfiguration;
        }
    }

    private static final class IkeSessionConnectionInfoData extends IkeEventData {
        final IkeSessionConnectionInfo mIkeSessionConnectionInfo;

        private IkeSessionConnectionInfoData(
                String apnName, int token, IkeSessionConnectionInfo ikeSessionConnectionInfo) {
            super(apnName, token);
            mIkeSessionConnectionInfo = ikeSessionConnectionInfo;
        }
    }

    private static final class Ike3gppDataReceived extends IkeEventData {
        final List<Ike3gppData> mIke3gppData;

        private Ike3gppDataReceived(String apnName, int token, List<Ike3gppData> ike3gppData) {
            super(apnName, token);
            mIke3gppData = ike3gppData;
        }
    }

    // Data received from IkeSessionStateMachine if either IKE session or Child session have been
    // closed, normally or exceptionally.
    private static final class SessionClosedData extends IkeEventData {
        final IkeException mIkeException;

        private SessionClosedData(String apnName, int token, IkeException ikeException) {
            super(apnName, token);
            mIkeException = ikeException;
        }
    }

    private static final class IpsecTransformData extends IkeEventData {
        private final IpSecTransform mTransform;
        private final int mDirection;

        private IpsecTransformData(
                IpSecTransform transform, int direction, String apnName, int token) {
            super(apnName, token);
            mTransform = transform;
            mDirection = direction;
        }

        public IpSecTransform getTransform() {
            return mTransform;
        }

        public int getDirection() {
            return mDirection;
        }

        public String getApnName() {
            return super.mApnName;
        }
    }

    private abstract static class IkeEventData {
        final String mApnName;
        final int mToken;

        private IkeEventData(String apnName, int token) {
            mApnName = apnName;
            mToken = token;
        }
    }

    private static final class EpdgInfo {
        private List<InetAddress> mAddrList;
        private int mIndex;

        private EpdgInfo() {
            mAddrList = null;
            mIndex = 0;
        }

        public List<InetAddress> getAddrList() {
            return mAddrList;
        }

        public void setAddrList(@NonNull List<InetAddress> AddrList) {
            mAddrList = AddrList;
            resetIndex();
        }

        public int getIndex() {
            return mIndex;
        }

        public void incrementIndex() {
            if (getIndex() >= getAddrList().size() - 1) {
                resetIndex();
            } else {
                mIndex++;
            }
        }

        public void resetIndex() {
            mIndex = 0;
        }
    }

    private static class IpPreferenceConflict {
        final boolean mIsConflict;
        final int mErrorType;

        private IpPreferenceConflict(boolean isConflict, int errorType) {
            mIsConflict = isConflict;
            mErrorType = errorType;
        }

        private IpPreferenceConflict() {
            mIsConflict = false;
            mErrorType = IwlanError.NO_ERROR;
        }
    }

    private int[] getRetransmissionTimeoutsFromConfig() {
        int[] timeList = getConfig(CarrierConfigManager.Iwlan.KEY_RETRANSMIT_TIMER_MSEC_INT_ARRAY);
        boolean isValid =
                timeList != null
                        && timeList.length != 0
                        && timeList.length <= IKE_RETRANS_MAX_ATTEMPTS_MAX;
        for (int time : Objects.requireNonNull(timeList)) {
            if (time < IKE_RETRANS_TIMEOUT_MS_MIN || time > IKE_RETRANS_TIMEOUT_MS_MAX) {
                isValid = false;
                break;
            }
        }
        if (!isValid) {
            timeList =
                    IwlanHelper.getDefaultConfig(
                            CarrierConfigManager.Iwlan.KEY_RETRANSMIT_TIMER_MSEC_INT_ARRAY);
        }
        Log.d(TAG, "getRetransmissionTimeoutsFromConfig: " + Arrays.toString(timeList));
        return timeList;
    }

    private int getDpdDelayFromConfig() {
        int dpdDelay = getConfig(CarrierConfigManager.Iwlan.KEY_DPD_TIMER_SEC_INT);
        if (dpdDelay < IKE_DPD_DELAY_SEC_MIN || dpdDelay > IKE_DPD_DELAY_SEC_MAX) {
            dpdDelay =
                    IwlanHelper.getDefaultConfig(CarrierConfigManager.Iwlan.KEY_DPD_TIMER_SEC_INT);
        }
        return dpdDelay;
    }

    /**
     * Decodes backoff time as per TS 124 008 10.5.7.4a Bits 5 to 1 represent the binary coded timer
     * value
     *
     * <p>Bits 6 to 8 defines the timer value unit for the GPRS timer as follows: Bits 8 7 6 0 0 0
     * value is incremented in multiples of 10 minutes 0 0 1 value is incremented in multiples of 1
     * hour 0 1 0 value is incremented in multiples of 10 hours 0 1 1 value is incremented in
     * multiples of 2 seconds 1 0 0 value is incremented in multiples of 30 seconds 1 0 1 value is
     * incremented in multiples of 1 minute 1 1 0 value is incremented in multiples of 1 hour 1 1 1
     * value indicates that the timer is deactivated.
     *
     * @param backoffTimeByte Byte value obtained from ike
     * @return long time value in seconds. -1 if the timer needs to be deactivated.
     */
    private static long decodeBackoffTime(byte backoffTimeByte) {
        final int BACKOFF_TIME_VALUE_MASK = 0x1F;
        final int BACKOFF_TIMER_UNIT_MASK = 0xE0;
        final Long[] BACKOFF_TIMER_UNIT_INCREMENT_SECS = {
            10L * 60L, // 10 mins
            60L * 60L, // 1 hour
            10L * 60L * 60L, // 10 hours
            2L, // 2 seconds
            30L, // 30 seconds
            60L, // 1 minute
            60L * 60L, // 1 hour
        };

        long time = backoffTimeByte & BACKOFF_TIME_VALUE_MASK;
        int timerUnit = (backoffTimeByte & BACKOFF_TIMER_UNIT_MASK) >> 5;
        if (timerUnit >= BACKOFF_TIMER_UNIT_INCREMENT_SECS.length) {
            return -1;
        }
        time *= BACKOFF_TIMER_UNIT_INCREMENT_SECS[timerUnit];
        return time;
    }

    @VisibleForTesting
    String getTunnelSetupRequestApnName(TunnelSetupRequest setupRequest) {
        return setupRequest.apnName();
    }

    @VisibleForTesting
    void putApnNameToTunnelConfig(
            String apnName,
            IkeSession ikeSession,
            TunnelCallback tunnelCallback,
            TunnelMetricsInterface tunnelMetrics,
            InetAddress srcIpv6Addr,
            int srcIPv6AddrPrefixLen) {
        mApnNameToTunnelConfig.put(
                apnName,
                new TunnelConfig(
                        ikeSession,
                        tunnelCallback,
                        tunnelMetrics,
                        srcIpv6Addr,
                        srcIPv6AddrPrefixLen));
        Log.d(TAG, "Added apn: " + apnName + " to TunnelConfig");
    }

    @VisibleForTesting
    int incrementAndGetCurrentTokenForApn(String apnName) {
        final int currentToken =
                mApnNameToCurrentToken.compute(
                        apnName, (apn, token) -> token == null ? 0 : token + 1);
        Log.d(TAG, "Added token: " + currentToken + " for apn: " + apnName);
        return currentToken;
    }

    @VisibleForTesting
    boolean isN1ModeSupported() {
        int[] nrCarrierCaps =
                getConfig(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        Log.d(TAG, "KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY : " + Arrays.toString(nrCarrierCaps));
        if (Arrays.stream(nrCarrierCaps)
                .anyMatch(cap -> cap == CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA)) {
            return true;
        } else return false;
    }

    @VisibleForTesting
    boolean isTunnelConfigContainExistApn(String apnName) {
        return mApnNameToTunnelConfig.containsKey(apnName);
    }

    @VisibleForTesting
    List<InetAddress> getAddressForNetwork(Network network, Context context) {
        return IwlanHelper.getAllAddressesForNetwork(network, context);
    }

    @VisibleForTesting
    IkeSessionCreator getIkeSessionCreator() {
        return mIkeSessionCreator;
    }

    @VisibleForTesting
    void sendSelectionRequestComplete(
            List<InetAddress> validIPList, IwlanError result, int transactionId) {
        mEpdgServerSelectionDuration = System.currentTimeMillis() - mEpdgServerSelectionStartTime;
        mEpdgServerSelectionStartTime = 0;
        EpdgSelectorResult epdgSelectorResult =
                new EpdgSelectorResult(validIPList, result, transactionId);
        mHandler.sendMessage(
                mHandler.obtainMessage(
                        EVENT_EPDG_ADDRESS_SELECTION_REQUEST_COMPLETE, epdgSelectorResult));
    }

    static boolean isValidApnProtocol(int proto) {
        return (proto == ApnSetting.PROTOCOL_IP
                || proto == ApnSetting.PROTOCOL_IPV4V6
                || proto == ApnSetting.PROTOCOL_IPV6);
    }

    boolean isObsoleteToken(String apnName, int token) {
        if (!mApnNameToCurrentToken.containsKey(apnName)) {
            return true;
        }
        return token != mApnNameToCurrentToken.get(apnName);
    }

    private static String eventToString(int event) {
        switch (event) {
            case EVENT_TUNNEL_BRINGUP_REQUEST:
                return "EVENT_TUNNEL_BRINGUP_REQUEST";
            case EVENT_TUNNEL_BRINGDOWN_REQUEST:
                return "EVENT_TUNNEL_BRINGDOWN_REQUEST";
            case EVENT_CHILD_SESSION_OPENED:
                return "EVENT_CHILD_SESSION_OPENED";
            case EVENT_CHILD_SESSION_CLOSED:
                return "EVENT_CHILD_SESSION_CLOSED";
            case EVENT_IKE_SESSION_CLOSED:
                return "EVENT_IKE_SESSION_CLOSED";
            case EVENT_EPDG_ADDRESS_SELECTION_REQUEST_COMPLETE:
                return "EVENT_EPDG_ADDRESS_SELECTION_REQUEST_COMPLETE";
            case EVENT_IPSEC_TRANSFORM_CREATED:
                return "EVENT_IPSEC_TRANSFORM_CREATED";
            case EVENT_IPSEC_TRANSFORM_DELETED:
                return "EVENT_IPSEC_TRANSFORM_DELETED";
            case EVENT_UPDATE_NETWORK:
                return "EVENT_UPDATE_NETWORK";
            case EVENT_IKE_SESSION_OPENED:
                return "EVENT_IKE_SESSION_OPENED";
            case EVENT_IKE_SESSION_CONNECTION_INFO_CHANGED:
                return "EVENT_IKE_SESSION_CONNECTION_INFO_CHANGED";
            case EVENT_IKE_3GPP_DATA_RECEIVED:
                return "EVENT_IKE_3GPP_DATA_RECEIVED";
            default:
                return "Unknown(" + event + ")";
        }
    }

    @VisibleForTesting
    TmIkeSessionCallback getTmIkeSessionCallback(String apnName, int token) {
        return new TmIkeSessionCallback(apnName, token);
    }

    @VisibleForTesting
    void onConnectedToEpdg(boolean hasConnected) {
        mHasConnectedToEpdg = hasConnected;
        if (mHasConnectedToEpdg) {
            mIkeSessionNetwork = mDefaultNetwork;
        } else {
            mIkeSessionNetwork = null;
            mEpdgAddress = null;
        }
    }

    @VisibleForTesting
    TunnelConfig getTunnelConfigForApn(String apnName) {
        return mApnNameToTunnelConfig.get(apnName);
    }

    @VisibleForTesting
    int getCurrentTokenForApn(String apnName) {
        if (!mApnNameToCurrentToken.containsKey(apnName)) {
            throw new IllegalArgumentException("There is no token for apn: " + apnName);
        }
        return mApnNameToCurrentToken.get(apnName);
    }

    @VisibleForTesting
    long reportIwlanError(String apnName, IwlanError error) {
        return ErrorPolicyManager.getInstance(mContext, mSlotId).reportIwlanError(apnName, error);
    }

    @VisibleForTesting
    long reportIwlanError(String apnName, IwlanError error, long backOffTime) {
        return ErrorPolicyManager.getInstance(mContext, mSlotId)
                .reportIwlanError(apnName, error, backOffTime);
    }

    @VisibleForTesting
    IwlanError getLastError(String apnName) {
        return ErrorPolicyManager.getInstance(mContext, mSlotId).getLastError(apnName);
    }

    @VisibleForTesting
    boolean canBringUpTunnel(String apnName) {
        return ErrorPolicyManager.getInstance(mContext, mSlotId).canBringUpTunnel(apnName);
    }

    @VisibleForTesting
    void setEpdgAddress(InetAddress inetAddress) {
        mEpdgAddress = inetAddress;
    }

    @VisibleForTesting
    IpPreferenceConflict isIpPreferenceConflictsWithNetwork(
            @CarrierConfigManager.Iwlan.EpdgAddressIpPreference int ipPreference) {
        List<InetAddress> localAddresses = getAddressForNetwork(mDefaultNetwork, mContext);
        if (localAddresses == null || localAddresses.size() == 0) {
            Log.e(TAG, "No local addresses available for Network " + mDefaultNetwork);
            return new IpPreferenceConflict(true, IwlanError.EPDG_SELECTOR_SERVER_SELECTION_FAILED);
        } else if (!IwlanHelper.hasIpv6Address(localAddresses)
                && ipPreference == CarrierConfigManager.Iwlan.EPDG_ADDRESS_IPV6_ONLY) {
            Log.e(
                    TAG,
                    "ePDG IP preference: "
                            + ipPreference
                            + " conflicts with source IP type: "
                            + EpdgSelector.PROTO_FILTER_IPV4);
            return new IpPreferenceConflict(true, IwlanError.EPDG_ADDRESS_ONLY_IPV6_ALLOWED);
        } else if (!IwlanHelper.hasIpv4Address(localAddresses)
                && ipPreference == CarrierConfigManager.Iwlan.EPDG_ADDRESS_IPV4_ONLY) {
            // b/209938719 allows Iwlan to support VoWiFi for IPv4 ePDG server while on IPv6 WiFi.
            // Iwlan will receive a IPv4 address which is embedded in stacked IPv6 address. By using
            // this IPv4 address, UE will connect to IPv4 ePDG server through XLAT. However, there
            // are issues on connecting ePDG server through XLAT. Will allow IPV4_ONLY on IPv6 WiFi
            // after the issues are resolved.
            Log.e(
                    TAG,
                    "ePDG IP preference: "
                            + ipPreference
                            + " conflicts with source IP type: "
                            + EpdgSelector.PROTO_FILTER_IPV6);
            return new IpPreferenceConflict(true, IwlanError.EPDG_ADDRESS_ONLY_IPV4_ALLOWED);
        }
        return new IpPreferenceConflict();
    }

    public void dump(PrintWriter pw) {
        pw.println("---- EpdgTunnelManager ----");
        pw.println("mHasConnectedToEpdg: " + mHasConnectedToEpdg);
        pw.println("mIkeSessionNetwork: " + mIkeSessionNetwork);
        if (mEpdgAddress != null) {
            pw.println("mEpdgAddress: " + mEpdgAddress);
        }
        pw.println("mApnNameToTunnelConfig:\n");
        for (Map.Entry<String, TunnelConfig> entry : mApnNameToTunnelConfig.entrySet()) {
            pw.println("APN: " + entry.getKey());
            pw.println("TunnelConfig: " + entry.getValue());
            pw.println();
        }
        pw.println("---------------------------");
    }
}
