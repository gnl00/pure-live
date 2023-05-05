package com.pure;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author gnl
 * @since 2023/5/4
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // mp4ToM3u8();

        FileInputStream  fis = new FileInputStream("test.mp4");
        byte[] videoBytes = fis.readAllBytes();
        ByteBuffer videoBuffer = ByteBuffer.wrap(videoBytes);
        fis.close();

        bytesToM3u8(videoBuffer);
    }


     // javacv + ffmpeg-platform
     public static void bytesToM3u8(ByteBuffer buffer) throws FrameGrabber.Exception, FrameRecorder.Exception {
        File tmpFile = new File("tmp");

        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
            fos.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 设置 FFmpeg 的配置信息
        FFmpegFrameGrabber fg = new FFmpegFrameGrabber(tmpFile);
        Map<String, String> options = new HashMap<>();
        options.put("-y", "-"); // 覆盖输出文件
        options.put("-threads", "8"); // 线程数
        options.put("-i", "-"); // 从标准输入流读入
        options.put("-c:v", "libx264");
        options.put("-profile:v", "high");
        options.put("-level", "3.1");
        options.put("-preset", "slow"); // 编码质量为最慢
        options.put("-crf", "28"); // 编码质量为 28
        options.put("-sc_threshold", "0"); // 不过滤场景切换
        options.put("-g", "25");
        // options.put("-hls_time", "10"); // 10 秒一个分片
        options.put("-hls_list_size", "0"); // 不限制 m3u8 列表文件的大小
        options.put("-start_number", "0"); // 分片开始的序号
        options.put("-hls_segment_filename", "%03d.ts"); // ts 分片保存目录和文件名格式
        options.put("output.m3u8", "-"); // m3u8 文件
        fg.setOptions(options);

        // 初始化 FFmpeg
        fg.start();

        // 创建一个 FFmpeg 视频编码器
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("output.m3u8", fg.getImageWidth(), fg.getImageHeight(), fg.getAudioChannels());
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setVideoOption("preset", "slow");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFormat("hls");
        recorder.setVideoBitrate(900 * 1024);
        recorder.setAudioBitrate(128 * 1024);
        recorder.setFrameRate(fg.getFrameRate());
        recorder.setVideoQuality(26);
        recorder.setGopSize((int)Math.round(fg.getFrameRate()));

        // 初始化编码器
        recorder.start();

        // 每一帧图像进行编码并写入到 FFmpeg 中，直到读取完所有帧
        Frame frame = null;
        int count = 0;
        while ((frame = fg.grab()) != null) {
            recorder.setTimestamp(fg.getTimestamp());
            recorder.record(frame);
            count++;
        }

        System.out.println("Total number of frames: " + count);

        // release
        recorder.close();
        fg.close();

        tmpFile.delete();
     }

    public static void mp4ToM3u8() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", "test.mp4", "-c:v", "libx264", "-hls_time", "10", "-hls_list_size", "0", "-hls_segment_filename", "output_%d.ts", "output.m3u8");
        Process process = pb.start();
        process.waitFor();
    }
}
