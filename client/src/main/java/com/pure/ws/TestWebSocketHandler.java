package com.pure.ws;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayInputStream;
import java.io.File;
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

    final static String TMP_DIR = System.getProperty("java.io.tmpdir");

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

        String mLink = bytesToM3u8(buffer);

//        String fileName = saveVideoBytes(buffer);
//        String link = getLink(fileName);

        receivers.forEach(receiver -> {
            try {
                // receiver.getBasicRemote().sendBinary(buffer);
                receiver.getBasicRemote().sendText(mLink);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String bytesToM3u8(ByteBuffer buffer) {
        File tmpFile = new File("tmp");

        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
            fos.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }

        FFmpegFrameGrabber fg = new FFmpegFrameGrabber(tmpFile);

        // 创建一个 FFmpeg 视频编码器
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tmpFile, fg.getImageWidth(), fg.getImageHeight(), fg.getAudioChannels());
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setVideoOption("preset", "veryfast");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFormat("hls");
        recorder.setVideoBitrate(900 * 1024);
        recorder.setAudioBitrate(128 * 1024);
        recorder.setFrameRate(fg.getFrameRate());
        recorder.setVideoQuality(26);
        recorder.setGopSize((int)Math.round(fg.getFrameRate()));


        tmpFile.delete();

        return null;
    }

    private String getLink(String fileName) {
        return "http://localhost:8888/video/" + fileName + ".m3u8";
    }

    private String saveVideoBytes(ByteBuffer buffer) {
        String mp4FileName = generateMp4(buffer);
        String m3FileName = convertMp4ToM3u8(mp4FileName);

        return m3FileName;
    }

    public String generateMp4(ByteBuffer buffer) {
        String filePath = "/Users/gnl/Tmp/statics/video/";
        String outputSuffix =  ".mp4";
        String filename = new Date().getTime() + "";
        String outputFileName = filePath + filename + outputSuffix;

        byte[] videoBytes = buffer.array();
        try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
            fos.write(videoBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

    public String convertMp4ToM3u8(String filename) {
        try {
            String filePath = "/Users/gnl/Tmp/statics/video/";
            String inputSuffix =  ".mp4";
            String inputFileName = filePath + filename + inputSuffix;

            String m3Suffix = ".m3u8";
            String tsSuffix = ".ts";

            String outputM3u8 = filePath + filename + m3Suffix;
            // String outputTs = filePath + filename + "_%d" + tsSuffix; // "output_%d.ts"

            // ffmpeg -i input.mp4 -codec copy -hls_list_size 0 output.m3u8
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", inputFileName, "-codec", "copy", "-hls_list_size", "0", outputM3u8);
            Process process = pb.start();
            process.waitFor();

            return filename;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

}
