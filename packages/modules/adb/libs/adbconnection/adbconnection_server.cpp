/*
 * Copyright (C) 2019 The Android Open Source Project
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

#include "adbconnection/server.h"

#include <sys/epoll.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>

#include <algorithm>
#include <array>
#include <vector>

#include <android-base/logging.h>
#include <android-base/unique_fd.h>

#include "adbconnection/process_info.h"

using android::base::unique_fd;

#define JDWP_CONTROL_NAME "\0jdwp-control"
#define JDWP_CONTROL_NAME_LEN (sizeof(JDWP_CONTROL_NAME) - 1)

static_assert(JDWP_CONTROL_NAME_LEN <= sizeof(reinterpret_cast<sockaddr_un*>(0)->sun_path));

// Listen for incoming jdwp clients forever.
void adbconnection_listen(void (*callback)(int fd, ProcessInfo process)) {
  sockaddr_un addr = {};
  socklen_t addrlen = JDWP_CONTROL_NAME_LEN + sizeof(addr.sun_family);

  addr.sun_family = AF_UNIX;
  memcpy(addr.sun_path, JDWP_CONTROL_NAME, JDWP_CONTROL_NAME_LEN);

  unique_fd s(socket(AF_UNIX, SOCK_SEQPACKET | SOCK_NONBLOCK | SOCK_CLOEXEC, 0));
  if (s < 0) {
    PLOG(ERROR) << "failed to create JDWP control socket";
    return;
  }

  if (bind(s.get(), reinterpret_cast<sockaddr*>(&addr), addrlen) < 0) {
    PLOG(ERROR) << "failed to bind JDWP control socket";
    return;
  }

  if (listen(s.get(), 4) < 0) {
    PLOG(ERROR) << "failed to listen on JDWP control socket";
    return;
  }

  std::vector<unique_fd> pending_connections;

  unique_fd epfd(epoll_create1(EPOLL_CLOEXEC));
  std::array<epoll_event, 16> events;

  events[0].events = EPOLLIN;
  events[0].data.fd = -1;
  if (epoll_ctl(epfd.get(), EPOLL_CTL_ADD, s.get(), &events[0]) != 0) {
    PLOG(FATAL) << "failed to register socket " << s.get() << " with epoll fd";
  }

  while (true) {
    int epoll_rc = TEMP_FAILURE_RETRY(epoll_wait(epfd.get(), events.data(), events.size(), -1));
    if (epoll_rc == -1) {
      PLOG(FATAL) << "epoll_wait failed";
    }

    for (int i = 0; i < epoll_rc; ++i) {
      const epoll_event& event = events[i];
      if (event.data.fd == -1) {
        unique_fd client(
            TEMP_FAILURE_RETRY(accept4(s.get(), nullptr, nullptr, SOCK_NONBLOCK | SOCK_CLOEXEC)));

        if (client == -1) {
          PLOG(WARNING) << "failed to accept client on JDWP control socket";
          continue;
        }

        epoll_event register_event;
        register_event.events = EPOLLIN;
        register_event.data.fd = client.get();

        if (epoll_ctl(epfd.get(), EPOLL_CTL_ADD, client.get(), &register_event) != 0) {
          PLOG(FATAL) << "failed to register JDWP client " << client.get() << " with epoll";
        }

        pending_connections.emplace_back(std::move(client));
      } else {
        // n^2, but the backlog should be short.
        auto it = std::find_if(pending_connections.begin(), pending_connections.end(),
                               [&](const unique_fd& fd) { return fd.get() == event.data.fd; });

        if (it == pending_connections.end()) {
          LOG(FATAL) << "failed to find JDWP client (" << event.data.fd
                     << ") in pending connections";
        }

        ProcessInfo process;
        int rc = TEMP_FAILURE_RETRY(recv(it->get(), &process, sizeof(process), MSG_DONTWAIT));
        if (rc != sizeof(process)) {
          LOG(ERROR) << "received data of incorrect size from JDWP client: read " << rc
                     << ", expected " << sizeof(process);
        } else {
          callback(it->release(), process);
        }

        if (epoll_ctl(epfd.get(), EPOLL_CTL_DEL, event.data.fd, nullptr) != 0) {
          PLOG(FATAL) << "failed to delete fd " << event.data.fd << " from JDWP epoll fd";
        }

        pending_connections.erase(it);
      }
    }
  }
}