
/*
 * Copyright (c) 2017-2017 Globo.com
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

package com.globocom.grou.groot.jbender.executors.http;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import co.paralleluniverse.strands.SuspendableCallable;
import com.globocom.grou.groot.jbender.executors.RequestExecutor;
import com.globocom.grou.groot.jbender.executors.Validator;
import com.globocom.grou.groot.jbender.util.ParameterizedRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Executor base class offering a Comsat-based implementation of an HTTP request executor.
 */
public class FiberApacheHttpClientRequestExecutor<X extends HttpRequestBase> implements RequestExecutor<X, CloseableHttpResponse>, AutoCloseable {
  // Inspired by https://github.com/puniverse/photon/blob/master/src/main/java/co/paralleluniverse/photon/Photon.java

  private final Validator<CloseableHttpResponse> validator;
  private final FiberHttpClient client;

  public FiberApacheHttpClientRequestExecutor(final Validator<CloseableHttpResponse> resValidator, final int maxConnections, final int timeout, final int parallelism) throws IOReactorException {
    final DefaultConnectingIOReactor ioreactor = new DefaultConnectingIOReactor(IOReactorConfig.custom().
      setConnectTimeout(timeout).
      setIoThreadCount(parallelism).
      setSoTimeout(timeout).
      build());

    final PoolingNHttpClientConnectionManager mngr = new PoolingNHttpClientConnectionManager(ioreactor);
    mngr.setDefaultMaxPerRoute(maxConnections);
    mngr.setMaxTotal(maxConnections);

    final CloseableHttpAsyncClient ahc = HttpAsyncClientBuilder.create().
      setConnectionManager(mngr).
      setDefaultRequestConfig(RequestConfig.custom().setLocalAddress(null).build()).build();

    client = new FiberHttpClient(ahc);
    validator = resValidator;
  }

  public FiberApacheHttpClientRequestExecutor(final Validator<CloseableHttpResponse> resValidator, final int maxConnections, final int timeout) throws IOReactorException {
    this(resValidator, maxConnections, timeout, Runtime.getRuntime().availableProcessors());
  }

  public FiberApacheHttpClientRequestExecutor(final Validator<CloseableHttpResponse> resValidator, final int maxConnections) throws IOReactorException {
    this(resValidator, maxConnections, 0);
  }

  public FiberApacheHttpClientRequestExecutor(final int maxConnections) throws IOReactorException {
    this(null, maxConnections, 0);
  }

  // TODO Figure out meaningful and sensible default for maxConnections and add no-args constructor

  @Override
  public CloseableHttpResponse execute(final long nanoTime, final HttpRequestBase request) throws SuspendExecution, InterruptedException {
    // TODO See if timeout can be configured per-request
    final CloseableHttpResponse ret;
    try {
      ret = new Fiber<>((SuspendableCallable<CloseableHttpResponse>) () -> {
        try {
          final HttpContext context = ((ParameterizedRequest)request).getContext();
          return client.execute(request, context);
        } catch (final IOException e) {
          throw Exceptions.rethrowUnwrap(e);
        }
      }).start().get();
    } catch (final ExecutionException e) {
      throw Exceptions.rethrowUnwrap(e);
    }
    if (validator != null) {
      validator.validate(ret);
    }
    return ret;
  }

  @Override
  public void close() throws IOException {
    client.close();
  }
}
