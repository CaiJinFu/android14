/**
 * Copyright (c) 2023, The Android Open Source Project
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

#pragma once

#include <perfetto/base/task_runner.h>
#include <perfetto/tracing.h>

#include <string>
#include <unordered_map>

#include "netdbpf/NetworkTracePoller.h"

// For PacketTrace struct definition
#include "netd.h"

namespace android {
namespace bpf {

// BundleKeys are PacketTraces where timestamp and length are ignored.
using BundleKey = PacketTrace;

// BundleKeys are hashed using all fields except timestamp/length.
struct BundleHash {
  std::size_t operator()(const BundleKey& a) const;
};

// BundleKeys are equal if all fields except timestamp/length are equal.
struct BundleEq {
  bool operator()(const BundleKey& a, const BundleKey& b) const;
};

// Track the bundles we've interned and their corresponding intern id (iid). We
// use IncrementalState (rather than state in the Handler) so that we stay in
// sync with Perfetto's periodic state clearing (which helps recover from packet
// loss). When state is cleared, the state object is replaced with a new default
// constructed instance.
struct NetworkTraceState {
  bool cleared = true;
  std::unordered_map<BundleKey, uint64_t, BundleHash, BundleEq> iids;
};

// Inject our custom incremental state type using type traits.
struct NetworkTraceTraits : public perfetto::DefaultDataSourceTraits {
  using IncrementalStateType = NetworkTraceState;
};

// NetworkTraceHandler implements the android.network_packets data source. This
// class is registered with Perfetto and is instantiated when tracing starts and
// destroyed when tracing ends. There is one instance per trace session.
class NetworkTraceHandler
    : public perfetto::DataSource<NetworkTraceHandler, NetworkTraceTraits> {
 public:
  // Registers this DataSource.
  static void RegisterDataSource();

  // Connects to the system Perfetto daemon and registers the trace handler.
  static void InitPerfettoTracing();

  // When isTest is true, skip non-hermetic code.
  NetworkTraceHandler(bool isTest = false) : mIsTest(isTest) {}

  // perfetto::DataSource overrides:
  void OnSetup(const SetupArgs& args) override;
  void OnStart(const StartArgs&) override;
  void OnStop(const StopArgs&) override;

  // Writes the packets as Perfetto TracePackets, creating packets as needed
  // using the provided callback (which allows easy testing).
  void Write(const std::vector<PacketTrace>& packets,
             NetworkTraceHandler::TraceContext& ctx);

 private:
  // Convert a PacketTrace into a Perfetto trace packet.
  void Fill(const PacketTrace& src,
            ::perfetto::protos::pbzero::NetworkPacketEvent* event);

  // Fills in contextual information either inline or via interning.
  ::perfetto::protos::pbzero::NetworkPacketBundle* FillWithInterning(
      NetworkTraceState* state, const BundleKey& key,
      ::perfetto::protos::pbzero::TracePacket* dst);

  static internal::NetworkTracePoller sPoller;
  bool mStarted;
  bool mIsTest;

  // Values from config, see proto for details.
  uint32_t mPollMs;
  uint32_t mInternLimit;
  uint32_t mAggregationThreshold;
  bool mDropLocalPort;
  bool mDropRemotePort;
  bool mDropTcpFlags;
};

}  // namespace bpf
}  // namespace android
