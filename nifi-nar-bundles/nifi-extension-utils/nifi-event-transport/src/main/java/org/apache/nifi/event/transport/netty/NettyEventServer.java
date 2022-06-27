/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.event.transport.netty;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.apache.nifi.event.transport.EventServer;
import org.apache.nifi.event.transport.configuration.ShutdownQuietPeriod;
import org.apache.nifi.event.transport.configuration.ShutdownTimeout;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Netty Event Server
 */
class NettyEventServer implements EventServer {
    private final EventLoopGroup group;

    private final Channel channel;

    private final Duration shutdownQuietPeriod;

    private final Duration shutdownTimeout;

    /**
     * Netty Event Server with Event Loop Group and bound Channel
     *
     * @param group Event Loop Group
     * @param channel Bound Channel
     */
    NettyEventServer(final EventLoopGroup group, final Channel channel) {
        this(group, channel, ShutdownQuietPeriod.DEFAULT.getDuration(), ShutdownTimeout.DEFAULT.getDuration());
    }

    /**
     * Netty Event Server with Event Loop Group, bound Channel, and Shutdown Configuration
     *
     * @param group Event Loop Group
     * @param channel Bound Channel
     * @param quietPeriod server shutdown quiet period
     * @param timeout server shutdown timeout
     */
    NettyEventServer(final EventLoopGroup group, final Channel channel, final Duration quietPeriod, final Duration timeout) {
        this.group = group;
        this.channel = channel;
        this.shutdownQuietPeriod = quietPeriod;
        this.shutdownTimeout = timeout;
    }

    /**
     * Close Channel and shutdown Event Loop Group
     */
    @Override
    public void shutdown() {
        try {
            if (channel.isOpen()) {
                channel.close().syncUninterruptibly();
            }
        } finally {
            group.shutdownGracefully(shutdownQuietPeriod.toMillis(), shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }
}
