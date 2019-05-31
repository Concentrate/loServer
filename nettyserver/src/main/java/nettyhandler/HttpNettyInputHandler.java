package nettyhandler;

import cn.hutool.core.io.resource.ResourceUtil;
import config.NServerConfig;
import config.NServerSetting;
import httpconcept.Request;
import httpconcept.Response;
import httphandler.Action;
import httphandler.Filter;
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
        if (!(msg instanceof FullHttpRequest)) {
            return;
        }
        FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
        Request request = new Request(ctx, fullHttpRequest);
        Response response = Response.build(ctx, request);
        boolean isPass = filter(request, response);
        if (isPass) {
            try {
                handleAction(request, response);

            } catch (Exception e) {
                NServerSetting.getSetTextAction(e.getMessage());
            }
            if (!response.isSent()) {
                response.send();
            }
        }
    }


    public void handleAction(Request request, Response response) {
        Action action = NServerSetting.getAction(request.getPath());
        if (action == null) {
            Action rootAc = NServerSetting.getRootAction();
            rootAc.handleAction(request, response);
        } else {
            action.handleAction(request, response);
        }
    }

    private boolean filter(Request request, Response response) {
        Filter filter = NServerSetting.getRootFilter();
        if (filter != null) {
            if (!filter.filter(request)) {
                return false;
            }
        }
        Filter cusFilter = NServerSetting.getFilter(request.getPath());
        if (cusFilter != null) {
            if (!cusFilter.filter(request)) {
                return false;
            }
        }

        return true;

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
