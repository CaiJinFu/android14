/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.net.ipsec.test.ike;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.net.InetAddresses;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.LongSparseArray;

import com.android.internal.net.TestUtils;
import com.android.internal.net.ipsec.test.ike.message.IkeHeader;
import com.android.internal.net.ipsec.test.ike.net.IkeConnectionController;
import com.android.internal.util.HexDump;

import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentCaptor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class IkeSocketTestBase {
    protected static final int REMOTE_RECV_BUFF_SIZE = 2048;
    protected static final int TIMEOUT = 1000;

    protected static final String NON_ESP_MARKER_HEX_STRING = "00000000";
    protected static final String IKE_REQ_MESSAGE_HEX_STRING =
            "5f54bf6d8b48e6e100000000000000002120220800000000"
                    + "00000150220000300000002c010100040300000c0100000c"
                    + "800e00800300000803000002030000080400000200000008"
                    + "020000022800008800020000b4a2faf4bb54878ae21d6385"
                    + "12ece55d9236fc5046ab6cef82220f421f3ce6361faf3656"
                    + "4ecb6d28798a94aad7b2b4b603ddeaaa5630adb9ece8ac37"
                    + "534036040610ebdd92f46bef84f0be7db860351843858f8a"
                    + "cf87056e272377f70c9f2d81e29c7b0ce4f291a3a72476bb"
                    + "0b278fd4b7b0a4c26bbeb08214c707137607958729000024"
                    + "c39b7f368f4681b89fa9b7be6465abd7c5f68b6ed5d3b4c7"
                    + "2cb4240eb5c464122900001c00004004e54f73b7d83f6beb"
                    + "881eab2051d8663f421d10b02b00001c00004005d915368c"
                    + "a036004cb578ae3e3fb268509aeab1900000002069936922"
                    + "8741c6d4ca094c93e242c9de19e7b7c60000000500000500";

    protected static final long LOCAL_SPI = 0x0L;
    protected static final long REMOTE_SPI = 0x5f54bf6d8b48e6e1L;

    protected static final String DATA_ONE = "one 1";
    protected static final String DATA_TWO = "two 2";

    protected static final InetAddress IPV4_LOOPBACK =
            InetAddresses.parseNumericAddress("127.0.0.1");
    protected static final InetAddress IPV6_LOOPBACK = InetAddresses.parseNumericAddress("::1");

    protected final LongSparseArray mSpiToIkeSocketCallbackMap =
            new LongSparseArray<IkeSocket.Callback>();

    protected static final int DUMMY_DSCP = 36;
    protected final IkeSocketConfig mSpyIkeSocketConfig =
            spy(new IkeSocketConfig(mock(IkeConnectionController.class), DUMMY_DSCP));
    protected final IkeSocket.Callback mMockIkeSocketCallback = mock(IkeSocket.Callback.class);

    protected byte[] mDataOne;
    protected byte[] mDataTwo;
    protected FileDescriptor mDummyRemoteServerFd;
    protected InetAddress mLocalAddress;

    @Before
    public void setUp() throws Exception {
        mSpiToIkeSocketCallbackMap.put(LOCAL_SPI, mMockIkeSocketCallback);
        mDummyRemoteServerFd = getBoundUdpSocket(IPV4_LOOPBACK);

        mDataOne = DATA_ONE.getBytes("UTF-8");
        mDataTwo = DATA_TWO.getBytes("UTF-8");
    }

    @After
    public void tearDown() throws Exception {
        Os.close(mDummyRemoteServerFd);
    }

    protected abstract void setPacketReceiver(IkeSocket.IPacketReceiver packetReceiver);

    protected static FileDescriptor getBoundUdpSocket(InetAddress address) throws Exception {
        FileDescriptor sock =
                Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
        Os.bind(sock, address, IkeSocket.SERVER_PORT_UDP_ENCAPSULATED);
        return sock;
    }

    protected boolean isFdOpen(FileDescriptor fd) {
        try {
            Os.getsockname(fd);
            return true;
        } catch (ErrnoException ignored) {
            return false;
        }
    }

    protected void verifyCloseFd(FileDescriptor fd) {
        try {
            Os.sendto(
                    fd,
                    ByteBuffer.wrap(mDataOne),
                    0,
                    InetAddress.getLoopbackAddress(),
                    IkeSocket.SERVER_PORT_UDP_ENCAPSULATED);
            fail("Expected to fail because fd is closed");
        } catch (ErrnoException | IOException expected) {
        }
    }

    protected byte[] receive(FileDescriptor mfd) throws Exception {
        byte[] receiveBuffer = new byte[REMOTE_RECV_BUFF_SIZE];
        AtomicInteger bytesRead = new AtomicInteger(-1);
        Thread receiveThread =
                new Thread(
                        () -> {
                            while (bytesRead.get() < 0) {
                                try {
                                    bytesRead.set(
                                            Os.recvfrom(
                                                    mDummyRemoteServerFd,
                                                    receiveBuffer,
                                                    0,
                                                    REMOTE_RECV_BUFF_SIZE,
                                                    0,
                                                    null));
                                } catch (Exception e) {
                                    Log.e(
                                            "IkeSocketTest",
                                            "Error encountered reading from socket",
                                            e);
                                }
                            }
                        });

        receiveThread.start();
        receiveThread.join(TIMEOUT);

        return Arrays.copyOfRange(receiveBuffer, 0, bytesRead.get());
    }

    protected void sendToIkeSocket(
            FileDescriptor fd, byte[] data, InetAddress destAddress, int port) throws Exception {
        Os.sendto(fd, data, 0, data.length, 0, destAddress, port);
    }

    protected static class DummyPacketReceiver implements IkeSocket.IPacketReceiver {
        byte[] mReceivedData = null;
        final TestCountDownLatch mLatch;

        DummyPacketReceiver(TestCountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void handlePacket(byte[] revbuf, LongSparseArray<IkeSocket.Callback> spiToCallback) {
            mReceivedData = Arrays.copyOfRange(revbuf, 0, revbuf.length);
            mLatch.countDown();
        }
    }

    protected static class TestCountDownLatch {
        private CountDownLatch mLatch;

        TestCountDownLatch() {
            reset();
        }

        private void reset() {
            mLatch = new CountDownLatch(1);
        }

        void countDown() {
            mLatch.countDown();
        }

        void await() {
            try {
                if (!mLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    fail("Time out");
                }
            } catch (InterruptedException e) {
                fail(e.toString());
            }
            reset();
        }
    }

    protected interface IkeSocketFactory {
        IkeSocket getIkeSocket(IkeSocketConfig sockConfig, IkeSocket.Callback ikeSocketCallback)
                throws ErrnoException, IOException;
    }

    private static void verifySocketConfigIsApplied(
            IkeSocketConfig spySockConfig, IkeSocket ikeSocket) throws Exception {
        verify(spySockConfig).getDscp();
    }

    protected void verifyGetAndCloseIkeSocketSameConfig(
            IkeSocketFactory ikeUdpSocketFactory, int expectedServerPort) throws Exception {
        IkeSocket.Callback mockIkeSocketCbOne = mock(IkeSocket.Callback.class);
        IkeSocket.Callback mockIkeSocketCbTwo = mock(IkeSocket.Callback.class);

        IkeSocket ikeSocketOne =
                ikeUdpSocketFactory.getIkeSocket(mSpyIkeSocketConfig, mockIkeSocketCbOne);
        assertEquals(expectedServerPort, ikeSocketOne.getIkeServerPort());
        assertEquals(1, ikeSocketOne.mRegisteredCallbacks.size());

        IkeSocket ikeSocketTwo =
                ikeUdpSocketFactory.getIkeSocket(mSpyIkeSocketConfig, mockIkeSocketCbTwo);
        assertEquals(expectedServerPort, ikeSocketTwo.getIkeServerPort());
        assertEquals(2, ikeSocketTwo.mRegisteredCallbacks.size());
        assertEquals(ikeSocketOne, ikeSocketTwo);

        verifySocketConfigIsApplied(mSpyIkeSocketConfig, ikeSocketOne);

        ikeSocketOne.releaseReference(mockIkeSocketCbOne);
        assertEquals(1, ikeSocketOne.mRegisteredCallbacks.size());
        assertTrue(isFdOpen(ikeSocketOne.getFd()));

        ikeSocketTwo.releaseReference(mockIkeSocketCbTwo);
        assertEquals(0, ikeSocketTwo.mRegisteredCallbacks.size());
        verifyCloseFd(ikeSocketTwo.getFd());
    }

    protected void verifyGetAndCloseIkeSocketDifferentConfig(
            IkeSocketFactory ikeUdpSocketFactory, int expectedServerPort) throws Exception {
        IkeSocket.Callback mockIkeSocketCbOne = mock(IkeSocket.Callback.class);
        IkeSocket.Callback mockIkeSocketCbTwo = mock(IkeSocket.Callback.class);

        IkeSocketConfig spySockConfigOne =
                spy(new IkeSocketConfig(mock(IkeConnectionController.class), DUMMY_DSCP));
        IkeSocketConfig spySockConfigTwo =
                spy(new IkeSocketConfig(mock(IkeConnectionController.class), DUMMY_DSCP));

        IkeSocket ikeSocketOne =
                ikeUdpSocketFactory.getIkeSocket(spySockConfigOne, mockIkeSocketCbOne);
        assertEquals(expectedServerPort, ikeSocketOne.getIkeServerPort());
        assertEquals(1, ikeSocketOne.mRegisteredCallbacks.size());

        IkeSocket ikeSocketTwo =
                ikeUdpSocketFactory.getIkeSocket(spySockConfigTwo, mockIkeSocketCbTwo);
        assertEquals(expectedServerPort, ikeSocketTwo.getIkeServerPort());
        assertEquals(1, ikeSocketTwo.mRegisteredCallbacks.size());

        assertNotEquals(ikeSocketOne, ikeSocketTwo);

        ArgumentCaptor<FileDescriptor> fdCaptorOne = ArgumentCaptor.forClass(FileDescriptor.class);
        ArgumentCaptor<FileDescriptor> fdCaptorTwo = ArgumentCaptor.forClass(FileDescriptor.class);

        verifySocketConfigIsApplied(spySockConfigOne, ikeSocketOne);
        verifySocketConfigIsApplied(spySockConfigTwo, ikeSocketTwo);

        FileDescriptor fdOne = ikeSocketOne.getFd();
        FileDescriptor fdTwo = ikeSocketTwo.getFd();
        assertNotNull(fdOne);
        assertNotNull(fdTwo);
        assertNotEquals(fdOne, fdTwo);

        ikeSocketOne.releaseReference(mockIkeSocketCbOne);
        assertEquals(0, ikeSocketOne.mRegisteredCallbacks.size());
        verifyCloseFd(ikeSocketOne.getFd());

        ikeSocketTwo.releaseReference(mockIkeSocketCbTwo);
        assertEquals(0, ikeSocketTwo.mRegisteredCallbacks.size());
        verifyCloseFd(ikeSocketTwo.getFd());
    }

    protected void verifyHandlePacket(
            byte[] receivedPacket, IkeSocket.IPacketReceiver packetReceiver) throws Exception {
        packetReceiver.handlePacket(receivedPacket, mSpiToIkeSocketCallbackMap);

        byte[] expectedIkePacketBytes = TestUtils.hexStringToByteArray(IKE_REQ_MESSAGE_HEX_STRING);
        ArgumentCaptor<IkeHeader> ikeHeaderCaptor = ArgumentCaptor.forClass(IkeHeader.class);
        verify(mMockIkeSocketCallback)
                .onIkePacketReceived(ikeHeaderCaptor.capture(), eq(expectedIkePacketBytes));

        IkeHeader capturedIkeHeader = ikeHeaderCaptor.getValue();
        assertEquals(REMOTE_SPI, capturedIkeHeader.ikeInitiatorSpi);
        assertEquals(LOCAL_SPI, capturedIkeHeader.ikeResponderSpi);
    }

    protected void verifyIkeUdpSocketReceivePacket(
            IkeSocketFactory ikeUdpSocketFactory, IkeSocket.IPacketReceiver packetReceiver)
            throws Exception {
        verifyIkeUdpSocketReceivePacket(
                ikeUdpSocketFactory, packetReceiver, IKE_REQ_MESSAGE_HEX_STRING);
    }

    protected void verifyIkeUdpSocketReceivePacket(
            IkeSocketFactory ikeUdpSocketFactory,
            IkeSocket.IPacketReceiver packetReceiver,
            String messageToProcessHexString)
            throws Exception {
        IkeSocket.Callback mockIkeSocketCallback = mock(IkeSocket.Callback.class);
        IkeUdpSocket ikeSocket =
                (IkeUdpSocket)
                        ikeUdpSocketFactory.getIkeSocket(
                                mSpyIkeSocketConfig, mockIkeSocketCallback);
        assertNotNull(ikeSocket);

        // Set up state
        ikeSocket.registerIke(LOCAL_SPI, mockIkeSocketCallback);
        IkeSocket.IPacketReceiver mockPacketReceiver = mock(IkeSocket.IPacketReceiver.class);
        setPacketReceiver(mockPacketReceiver);
        try {
            // Send a packet
            byte[] pktBytes = HexDump.hexStringToByteArray(messageToProcessHexString);
            ikeSocket.handlePacket(pktBytes, pktBytes.length);

            verify(mockPacketReceiver).handlePacket(eq(pktBytes), any());

        } finally {
            ikeSocket.releaseReference(mockIkeSocketCallback);
            setPacketReceiver(packetReceiver);
        }
    }
}
