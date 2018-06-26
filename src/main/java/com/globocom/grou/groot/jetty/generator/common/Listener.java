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

package com.globocom.grou.groot.jetty.generator.common;

import com.globocom.grou.groot.jetty.generator.LoadGenerator;
import java.util.EventListener;

public interface Listener extends EventListener {

    interface BeginListener extends Listener {

        void onBegin(LoadGenerator generator);
    }

    interface EndListener extends Listener {

        void onEnd(LoadGenerator generator);
    }
}
