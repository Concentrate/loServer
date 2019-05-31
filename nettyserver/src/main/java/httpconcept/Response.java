package httpconcept;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeUtility;
import com.sun.xml.internal.messaging.saaj.util.MimeHeadersUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.spdy.SpdyHeaders;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by liudeyu on 2019/5/8.
 */
public class Response {

    public final static String CONTENT_TEXT_TYPE = "text/plain";
    public final static String CONTENT_HTML_TYPE = "text/html";
    public final static String CONTENT_JSON_TYPE = "json/application";
    public static final String CONTENT_XML_TYPE = "text/xml";
    public static final String CONTENT_JAVASCRIPT_TYPE = "application/javascript";


    private Request request;
    private ChannelHandlerContext context;
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
    private HttpResponseStatus httpResponseStatus = HttpResponseStatus.OK;
    private String contentType = CONTENT_HTML_TYPE;
    private String charset = CharsetUtil.UTF_8.displayName();
    private HttpHeaders headers = new DefaultHttpHeaders();
    private Set<Cookie> cookies = new HashSet<>();
    private Object content;


    public Response(Request request, ChannelHandlerContext context) {
        this.request = request;
        this.context = context;
    }

    public Response setHttpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return this;
    }


    public Response setHttpResStatusWithCode(int code) {
        httpResponseStatus = HttpResponseStatus.valueOf(code);
        return this;
    }

    public Response setHttpStatus(HttpResponseStatus status) {
        this.httpResponseStatus = status;
        return this;
    }

    public Response setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }


    public Response setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public Response setHeader(String name, String value) {
        headers.set(name, value);
        return this;
    }

    public Response setContentLength(long contentLength) {
        setHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), contentLength + "");
        return this;
    }

    public Response setKeepAlive() {
        setHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
        return this;
    }


    public Response addCookie(Cookie cookie) {
        cookies.add(cookie);
        return this;
    }

    public Response addCookie(String name, String value) {
        addCookie(new DefaultCookie(name, value));
        return this;
    }

    /**
     * @param expireSecond is second,0 expire right now,-1 shutdown browser and clear
     */
    public Response addCookie(String name, String value, long expireSecond, String path, String domain) {
        Cookie cookie = new DefaultCookie(name, value);
        cookie.setMaxAge(expireSecond);
        if (!StringUtil.isNullOrEmpty(path)) {
            cookie.setPath(path);
        }
        if (!StringUtil.isNullOrEmpty(domain)) {
            cookie.setDomain(domain);
        }
        return this;
    }

    /**
     * path default is /
     */
    public Response addCookie(String name, String value, int maxAgeSecond) {
        addCookie(name, value, maxAgeSecond, "/", null);
        return this;
    }

    public Response setContent(String contentText) {
        this.content = Unpooled.copiedBuffer(contentText, Charset.forName(charset));
        return this;
    }

    public Response setTextContent(String contentText) {
        setContentType(CONTENT_TEXT_TYPE);
        setContent(contentText);
        return this;
    }

    public Response setJsonContent(String jsonText) {
        setContentType(CONTENT_JSON_TYPE);
        setContent(jsonText);
        return this;
    }


    public Response setContent(byte[] array) {
        content = Unpooled.copiedBuffer(array);
        return this;
    }

    public Response setContent(ByteBuf byteBuf) {
        content = byteBuf;
        return this;
    }


    public Response setContent(File file) {
        content = file;
        return this;
    }

    public Response addHttpHeaders(String name, String value) {
        headers.add(name, value);
        return this;
    }

    /**
     * without content
     */
    public DefaultHttpResponse toDefaultHttpResponse() {
        DefaultHttpResponse httpResponse = new DefaultFullHttpResponse(httpVersion, httpResponseStatus);
        HttpHeaders tmpHead = httpResponse.headers().add(headers);
        for (Cookie cookie : cookies) {
            tmpHead.add(HttpHeaderNames.COOKIE.toString(), ServerCookieEncoder.LAX.encode(cookie));
        }
        return httpResponse;
    }


    private DefaultFullHttpResponse toTextFullHttpResponse() {
        ByteBuf byteBuf = (ByteBuf) content;
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(httpVersion, httpResponseStatus, byteBuf);
        HttpHeaders tmpOne = response.headers().add(headers);
        tmpOne.set(HttpHeaderNames.CONTENT_TYPE.toString(), StrUtil.format("{};charset={}", contentType, charset));
        tmpOne.set(HttpHeaderNames.CONTENT_ENCODING.toString(), charset);
        tmpOne.set(HttpHeaderNames.CONTENT_LENGTH.toString(), byteBuf.readableBytes());
        for (Cookie cookie : cookies) {
            tmpOne.add(HttpHeaderNames.COOKIE.toString(), ServerCookieEncoder.LAX.encode(cookie));
        }
        return response;
    }


    private boolean isSent = false;

    public ChannelFuture send() {
        if (content instanceof File) {
            File file = (File) content;
            ChannelFuture tmp;
            try {
                tmp = sendFile(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                tmp = sendError(HttpResponseStatus.FORBIDDEN, "something error while send file");
            }
            isSent = true;
            return tmp;
        } else {
            isSent = true;
            return sendFull();
        }
    }


    private ChannelFuture sendError(HttpResponseStatus status, String msg) {
        if (context.channel().isActive()) {
            return setHttpStatus(status).setContent(msg).send();
        }
        return null;
    }

    private ChannelFuture sendFile(File file) throws FileNotFoundException {
        if (!FileUtil.exist(file)) {
            return null;
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        headers.set(HttpHeaderNames.CONTENT_LENGTH, file.length());
        String contentType = cn.hutool.http.HttpUtil.getMimeType(file.getName());
        if (!StrUtil.isBlank(contentType)) {
            setContentType(cn.hutool.http.HttpUtil.getMimeType(file.getName()));
        } else {
            headers.set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        }
        setContentType(contentType);
        context.write(toDefaultHttpResponse());
        context.write(new DefaultFileRegion(randomAccessFile.getChannel(), 0, file
                .length()), context.newProgressivePromise());
        return sendEmptyLast();
    }

    private ChannelFuture sendEmptyLast() {
        ChannelFuture future = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!request.isKeepAlive()) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
        return future;
    }


    private ChannelFuture sendFull() {
        if (request != null && request.isKeepAlive()) {
            setKeepAlive();
            return context.writeAndFlush(toTextFullHttpResponse());
        } else {
            return context.writeAndFlush(toTextFullHttpResponse()).addListener(ChannelFutureListener.CLOSE);
        }
    }


    public static Response build(ChannelHandlerContext context,Request request){
        return new Response(request,context);
    }


}
