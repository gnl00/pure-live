package com.pure.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    static Session latestSession;

    @OnOpen
    public void onOpen(Session session, @PathParam("receiver") String param) {

        if (Objects.nonNull(param) && param.equals("1")) {
            log.info(param);
            this.latestSession = session;
        }

        log.info("new link join");
    }

    @OnClose
    public void onClose(CloseReason reason) {
        log.info("One link closed, reason: {}", reason.getReasonPhrase());
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnMessage
    public void onMessage(ByteBuffer buffer) throws IOException {
        log.info("[received]: {}", buffer);

        latestSession.getBasicRemote().sendBinary(buffer);
    }

    public void sendData() {}

}
