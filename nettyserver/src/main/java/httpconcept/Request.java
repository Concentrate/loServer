package httpconcept;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.log.Log;
import cn.hutool.log.StaticLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * Created by liudeyu on 2019/5/8.
 */
public class Request {
    private static final Log log = StaticLog.get();

    private Map<String, String> header = new HashMap<>();
    private String ip;
    private String path;
    private Map<String, Cookie> cookieMap = new HashMap<>();
    private Map<String, Object> paras = new HashMap<>();

    private FullHttpRequest nettyRequest;

    public Request(ChannelHandlerContext context, FullHttpRequest fullHttpRequest) {
        nettyRequest = fullHttpRequest;
        decodeHeadersAndCookie(fullHttpRequest.headers());
        path = URLUtil.getPath(fullHttpRequest.uri());
        decodeUrlPara(new QueryStringDecoder(fullHttpRequest.uri()));
        if (fullHttpRequest.method() == HttpMethod.POST) {
            HttpPostRequestDecoder postRequestDecoder = new HttpPostRequestDecoder(fullHttpRequest);
            decodePostData(postRequestDecoder);
        }

        putIp(context);

    }


    public String getPath() {
        return path;
    }

    private void putIp(ChannelHandlerContext context) {
        String reverIp = header.get("X-Forwarded-For");
        if (StringUtil.isNullOrEmpty(reverIp)) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) context.channel().remoteAddress();
            ip = inetSocketAddress.getAddress().getHostAddress();
        } else {
            ip = NetUtil.getMultistageReverseProxyIp(reverIp);
        }
    }

    private void decodePostData(HttpPostRequestDecoder postRequestDecoder) {
        if (CollectionUtil.isEmpty(postRequestDecoder.getBodyHttpDatas())) {
            return;
        }
        for (InterfaceHttpData tmp : postRequestDecoder.getBodyHttpDatas()) {
            putPostParam(tmp);
        }
    }

    private void putPostParam(InterfaceHttpData tmp) {
        InterfaceHttpData.HttpDataType dataType = tmp.getHttpDataType();
        if (dataType == InterfaceHttpData.HttpDataType.Attribute) {
            Attribute attribute = (Attribute) tmp;
            try {
                paras.put(attribute.getName(), CollectionUtil.newArrayList(attribute.getValue()));
            } catch (IOException e) {
                e.printStackTrace();

            }
        } else if (dataType == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) tmp;
            if (fileUpload.isCompleted()) {
                try {
                    paras.put(fileUpload.getName(), fileUpload.getFile());
                } catch (IOException e) {
                    e.printStackTrace();
                    log.debug(this.getClass().getSimpleName(), "get upload file error");
                }
            }
        }
    }

    private void decodeUrlPara(QueryStringDecoder queryStringDecoder) {
        if (queryStringDecoder.parameters() == null || queryStringDecoder.parameters().isEmpty()) {
            return;
        }
        for (String key : queryStringDecoder.parameters().keySet()) {
            List<String> values = queryStringDecoder.parameters().get(key);
            if (!CollectionUtil.isEmpty(values)) {
                paras.put(key, queryStringDecoder.parameters().get(key));
            }
        }
    }


    private void decodeHeadersAndCookie(HttpHeaders headers) {
        if (headers == null) {
            return;
        }
        Iterator<Map.Entry<String, String>> iterator = headers.iteratorAsString();
        while (iterator.hasNext()) {
            Map.Entry<String, String> tmpEntry = iterator.next();
            header.put(tmpEntry.getKey(), tmpEntry.getValue());
        }
        String cookieString = headers.get(HttpHeaderNames.COOKIE);
        if (!StringUtil.isNullOrEmpty(cookieString)) {
            Set<Cookie> tmpCookie = ServerCookieDecoder.LAX.decode(cookieString);
            for (Cookie b : tmpCookie) {
                cookieMap.put(b.name(), b);
            }
        }
    }

    private String getProtocalVersion(){
        return nettyRequest.getProtocolVersion().text();
    }


    public boolean isKeepAlive(){
        String connectVa=header.get(HttpHeaderNames.CONNECTION);
        if(HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(connectVa)){
            return false;
        }
        if(HttpVersion.HTTP_1_0.text().equals(getProtocalVersion())){
            if(!HttpHeaderValues.KEEP_ALIVE.contentEqualsIgnoreCase(connectVa)){
                return false;
            }
        }
        return true;
    }


}
