/*
 * Copyright (c) 2017-2018 Globo.com
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globocom.grou.groot.jetty.generator;

import com.globocom.grou.groot.jetty.generator.util.MonitoringThreadPoolExecutor;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.toolchain.perf.HistogramSnapshot;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HTTP1WebsiteLoadGeneratorTest extends WebsiteLoadGeneratorTest {
    @Before
    public void prepare() throws Exception {
        prepareServer(new HttpConnectionFactory(), new TestHandler());
    }

    @Test
    public void testHTTP1() throws Exception {
        MonitoringThreadPoolExecutor executor = new MonitoringThreadPoolExecutor( 1024, 60, TimeUnit.SECONDS);

        AtomicLong requests = new AtomicLong();
        Histogram treeHistogram = new AtomicHistogram(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(10), 3);
        Histogram rootHistogram = new AtomicHistogram(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(10), 3);
        LoadGenerator loadGenerator = prepareLoadGenerator(new HTTP1ClientTransportBuilder())
                .warmupIterationsPerThread(10)
                .iterationsPerThread(100)
//                .warmupIterationsPerThread(1000)
//                .runFor(2, TimeUnit.MINUTES)
                .usersPerThread(100)
                .channelsPerUser(6)
                .resourceRate(20)
                .executor(executor)
                .resourceListener((Resource.TreeListener)info -> {
                    rootHistogram.recordValue(info.getResponseTime() - info.getRequestTime());
                    treeHistogram.recordValue(info.getTreeTime() - info.getRequestTime());
                })
                .requestListener(new Request.Listener.Adapter() {
                    @Override
                    public void onQueued(Request request) {
                        requests.incrementAndGet();
                    }
                })
                .requestListener(new Request.Listener.Adapter() {
                    @Override
                    public void onBegin(Request request) {
                        requests.decrementAndGet();
                    }
                })
                .build();

        serverStats.statsReset();
        loadGenerator.begin().join();
        long elapsed = serverStats.getStatsOnMs();

        Assert.assertEquals(0, requests.get());

        int serverRequests = serverStats.getRequests();
        System.err.printf("%nserver - requests: %d, rate: %.3f, max_request_time: %d%n%n",
                serverRequests,
                elapsed > 0 ? serverRequests * 1000F / elapsed : 0F,
                serverStats.getRequestTimeMax());

        HistogramSnapshot treeSnapshot = new HistogramSnapshot(treeHistogram, 20, "tree response time", "us", TimeUnit.NANOSECONDS::toMicros);
        System.err.println(treeSnapshot);
        HistogramSnapshot rootSnapshot = new HistogramSnapshot(rootHistogram, 20, "root response time", "us", TimeUnit.NANOSECONDS::toMicros);
        System.err.println(rootSnapshot);

        System.err.printf("client thread pool - max_threads: %d, max_queue_size: %d, max_queue_latency: %dms%n%n",
                executor.getMaxActiveThreads(),
                executor.getMaxQueueSize(),
                TimeUnit.NANOSECONDS.toMillis(executor.getMaxQueueLatency())
        );

        executor.shutdown();
    }
}
