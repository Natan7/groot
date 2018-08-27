package com.globocom.grou.groot.channel.handler;

import com.globocom.grou.groot.monit.MonitorService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ExceptionChannelInboundHandlerAdapter extends ChannelInboundHandlerAdapter {

    private final MonitorService monitorService;

    public ExceptionChannelInboundHandlerAdapter(final MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        monitorService.fail(cause);
        ctx.close();
    }
}
