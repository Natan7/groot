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

package com.globocom.grou.groot.jetty.listeners.responsetime;

import com.globocom.grou.groot.jetty.generator.common.Resource;

import java.util.concurrent.atomic.LongAdder;

/**
 *
 */
public class ResponsePerStatus implements Resource.NodeListener {

    private final LongAdder responses1xx = new LongAdder();

    private final LongAdder responses2xx = new LongAdder();

    private final LongAdder responses3xx = new LongAdder();

    private final LongAdder responses4xx = new LongAdder();

    private final LongAdder responses5xx = new LongAdder();

    @Override
    public void onResourceNode(Resource.Info info) {
        switch (info.getStatus() / 100) {
            case 1:
                responses1xx.increment();
                break;
            case 2:
                responses2xx.increment();
                break;
            case 3:
                responses3xx.increment();
                break;
            case 4:
                responses4xx.increment();
                break;
            case 5:
                responses5xx.increment();
                break;
            default:
                break;
        }
    }

    public long getResponses1xx() {
        return responses1xx.longValue();
    }

    public long getResponses2xx() {
        return responses2xx.longValue();
    }

    public long getResponses3xx() {
        return responses3xx.longValue();
    }

    public long getResponses4xx() {
        return responses4xx.longValue();
    }

    public long getResponses5xx() {
        return responses5xx.longValue();
    }
}
