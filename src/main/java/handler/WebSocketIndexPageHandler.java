/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package handler;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import directory.AttributeKeyDirectory;
import directory.UserDirectory;
import directory.UserSesssionDirectory;
import server.WebSocketServerIndexPage;

/**
 * Outputs index page content.
 */
public class WebSocketIndexPageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private Logger logger = LoggerFactory.getLogger(WebSocketIndexPageHandler.class);

    private final String websocketPath;

    public WebSocketIndexPageHandler(String websocketPath) {
        this.websocketPath = websocketPath;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        // Allow only GET methods.
        if (req.method() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
        String path = queryStringDecoder.path();

        // Send the index page
        if ("/".equals(path) || "/index.html".equals(req.uri())) {
            String webSocketLocation = getWebSocketLocation(ctx.pipeline(), req, websocketPath);
            ByteBuf content = WebSocketServerIndexPage.getContent(webSocketLocation);
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            HttpUtil.setContentLength(res, content.readableBytes());

            sendHttpResponse(ctx, req, res);
        } else if ("/login".equals(path)) {
            logger.debug("user login entry");

            Map<String, List<String>> parameters = queryStringDecoder.parameters();
            if (!parameters.containsKey("username")) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            } else {
                ByteBuf content = Unpooled.copiedBuffer("{\"result\":false}", CharsetUtil.UTF_8);
                String username = parameters.get("username").get(0);
                if (StringUtils.isNotBlank(username)) {
                    String s = UserDirectory.getUsers().get(username);
                    if (StringUtils.isNotBlank(s)) {
                        logger.debug("user is {}", s);
                        content = Unpooled
                                .copiedBuffer("{\"result\":true,\"chatName\":\"" + s + "\"}", CharsetUtil.UTF_8);
                    }
                }
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
                res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
                res.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                res.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, "Origin, X-Requested-With, Content-Type, Accept");
                res.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT,DELETE");
                HttpUtil.setContentLength(res, content.readableBytes());
                sendHttpResponse(ctx, req, res);
            }
        } else if ("/onlineUser".equals(path)) {
            logger.debug("online user entry");
            JSONArray ja = new JSONArray();
            for (Channel channel : UserSesssionDirectory.group) {
                JSONObject jo = new JSONObject();
                jo.put("chatName", channel.attr(AttributeKeyDirectory.CHATNAME).get());
                jo.put("userId", channel.attr(AttributeKeyDirectory.USERID).get());
                jo.put("channelId", channel.id().asShortText());
                ja.add(jo);
            }
            ByteBuf content = Unpooled.copiedBuffer("{\"result\":true,\"online\":" + ja.toString() + "}", CharsetUtil.UTF_8);
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
            res.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            res.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, "Origin, X-Requested-With, Content-Type, Accept");
            res.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT,DELETE");
            HttpUtil.setContentLength(res, content.readableBytes());
            sendHttpResponse(ctx, req, res);
        } else if (path.contains(".html")) {
            try {
                ByteBuf content = WebSocketServerIndexPage.getHtmlContent(path);
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

                res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                HttpUtil.setContentLength(res, content.readableBytes());
                sendHttpResponse(ctx, req, res);
            } catch (Exception e) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            }
        } else {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(ChannelPipeline cp, HttpRequest req, String path) {
        String protocol = "ws";
        if (cp.get(SslHandler.class) != null) {
            // SSL in use so use Secure WebSockets
            protocol = "wss";
        }
        return protocol + "://" + req.headers().get(HttpHeaderNames.HOST) + path;
    }
}
