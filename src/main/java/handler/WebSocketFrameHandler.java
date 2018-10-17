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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import directory.AttributeKeyDirectory;
import directory.UserSesssionDirectory;

/**
 * Echoes uppercase content of text frames.
 */
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(
            WebSocketFrameHandler.class);

    // @Override
    // public void channelActive(ChannelHandlerContext ctx) throws Exception {
    //     UserSesssionDirectory.group.add(ctx.channel());
    // }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        UserSesssionDirectory.group.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // ping and pong frames already handled

        if (frame instanceof TextWebSocketFrame) {
            // Send the uppercase string back.
            UserSesssionDirectory.group.size();
            String request = ((TextWebSocketFrame) frame).text();
            JSONObject jsonObject = JSON.parseObject(request);
            String type = jsonObject.getString("type");
            String message = jsonObject.getString("message");
            String msg =
                    "<div style='color: red'>" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + " " + ctx
                            .channel().attr(AttributeKeyDirectory.CHATNAME).get() + ":</div><div style='color: black'>"
                            + message + "</div>";
            logger.info("{} received {}", ctx.channel(), request);
            if ("normal".equals(type)) {
                String channelId = jsonObject.getString("channelId");
                Channel channel = UserSesssionDirectory.group.stream()
                        .filter(x -> channelId.equals(x.id().asShortText())).findFirst().orElse(null);
                if (channel != null)
                    channel.writeAndFlush(new TextWebSocketFrame(msg));
            }
            if ("group".equals(type)) {
                UserSesssionDirectory.group.writeAndFlush(new TextWebSocketFrame(msg));
            }
            // ctx.channel().writeAndFlush(new TextWebSocketFrame(request.toUpperCase(Locale.US)));
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

}
