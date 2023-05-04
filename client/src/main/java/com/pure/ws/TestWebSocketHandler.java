package com.pure.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

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
    @OnMessage(maxMessageSize = 1024 * 1024 * 100)
    public void onMessage(ByteBuffer buffer) {
        log.info("[received]: {}", buffer);

        saveVideoBytes(buffer);

        receivers.forEach(receiver -> {
            try {
                receiver.getBasicRemote().sendBinary(buffer);
                // receiver.getBasicRemote().sendText(handleVideoBytes(buffer));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void saveVideoBytes(ByteBuffer buffer) {
        String filename = generateMp4(buffer);
        convertMp4ToM3u8(filename);
    }

    private void convertMp4ToM3u8(String filename) {
    }

    public String generateMp4(ByteBuffer buffer) {
        String filename = new Date().getTime() + ".mp4";
        byte[] videoBytes = buffer.array();
        try (FileOutputStream fos = new FileOutputStream(filename + ".mp4")) {
            fos.write(videoBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

    public String handleVideoBytes(ByteBuffer buffer) {
        byte[] videoBytes = buffer.array();
        List<byte[]> tsChunks = splitVideos(videoBytes, 10);
        String m3u8Content = generateM3u8(tsChunks);
        log.info("m3u8Content: {}", m3u8Content);

        return m3u8Content;
    }

    public List<byte[]> splitVideos(byte[] videoBytes, int chunkDuration) {
        List<byte[]> chunks = new ArrayList<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(videoBytes)) {

            byte[] buffer = new byte[chunkDuration * 1024 * 1024]; // 获取 chunkDuration 秒的视频数据
            int bytesRead = 0;
            int chunkIndex = 0;
            while ((bytesRead = bais.read(buffer)) > 0) {
                byte[] chunkBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkBytes, 0, bytesRead);
                chunks.add(chunkBytes);
                chunkIndex++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return chunks;
    }

    public String generateM3u8(List<byte[]> tsChunks) {
        StringBuffer builder = new StringBuffer("#EXTM3U");

        int chunkIndex = 0;
        for (byte[] chunkBytes : tsChunks) {
            String base64Str = Base64.getEncoder().encodeToString(chunkBytes);
            builder.append("#EXTINF:10.0,").append("chunk").append(chunkIndex).append(".ts");
            builder.append("data:video/MP2T;base64,").append(base64Str).append("\n");
            chunkIndex++;
        }

        return builder.toString();
    }

    public void sendData() {}

}
