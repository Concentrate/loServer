package nettyhandler;

import cn.hutool.core.io.resource.ResourceUtil;
import config.NServerConfig;
import httpconcept.Request;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Created by liudeyu on 2019/5/8.
 */
public class HttpNettyInputHandler extends ChannelInboundHandlerAdapter {


    private NServerConfig config;

    public HttpNettyInputHandler(NServerConfig config) {
        this.config = config;
    }

    public HttpNettyInputHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(!(msg instanceof FullHttpRequest)){
            return;
        }
        FullHttpRequest fullHttpRequest=(FullHttpRequest)msg;
        Request request=new Request(ctx,fullHttpRequest);



    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
