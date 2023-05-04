package com.pure;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TODO
 *
 * @author gnl
 * @since 2023/5/4
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", "input.mp4", "-c:v", "libx264", "-hls_time", "10", "-hls_list_size", "0", "-hls_segment_filename", "output_%d.ts", "output.m3u8");
        Process process = pb.start();
        process.waitFor();

//        byte[] m3u8Data = Files.readAllBytes(Path.of("output.m3u8"));
//        try (FileOutputStream fos = new FileOutputStream("foutput.m3u8")) {
//            fos.write(m3u8Data);
//        }
    }
}
