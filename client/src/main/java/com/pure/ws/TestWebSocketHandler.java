package com.pure.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TODO
 *
 * @author gnl
 * @since 2023/5/3
 */
@Slf4j
@Component
@ServerEndpoint("/ws/test/{receiver}")
public class TestWebSocketHandler {

    static List<Session> receivers = new ArrayList<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("receiver") String param) {

        if (Objects.nonNull(param) && param.equals("1")) {
            log.info("receiver");
            receivers.add(session);
            log.info("receivers size {}", receivers.size());
        }

        log.info("new link join");
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        log.info("One link closed, reason: {}", reason.getReasonPhrase());
        receivers.remove(session);
        log.info("receivers size {}", receivers.size());
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    // 默认支持的最大消息为 8192=8kb
    @OnMessage(maxMessageSize = 5120000)
    public void onMessage(ByteBuffer buffer) {
        log.info("[received]: {}", buffer);

        receivers.forEach(receiver -> {
            try {
                receiver.getBasicRemote().sendBinary(buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void sendData() {}

}
