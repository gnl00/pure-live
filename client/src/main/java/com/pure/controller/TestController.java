package com.pure.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * TODO
 *
 * @author gnl
 * @since 2023/5/2
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    static int unit = 1024;
    static int b = 1;
    static int kb = b * unit;
    static int mb = kb * unit;

    static ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024 * 20);

    @GetMapping("/str")
    public String str() {
        return "TestController ==> str()";
    }

    @GetMapping("/json")
    public Map<String, Object> json() {
        Map<String, Object> json = new HashMap<>();
        json.put("data", "this is the data content");
        return json;
    }

    @GetMapping("/bytes")
    public byte[] bytes() {
        return new byte[]{1, 2, 3, 4, 5};
    }

    @PostMapping("/upload")
    public void fileUpload(MultipartFile file) throws IOException {
        if (Objects.nonNull(file)) {
            Path fileUploadPath = Paths.get("./upload/");
            if (!Files.exists(fileUploadPath)) {
                Files.createDirectories(fileUploadPath);
            }
            long uploadDate = new Date().getTime();
            String fileName = file.getOriginalFilename();
            file.transferTo(fileUploadPath.resolve(uploadDate + "-" + fileName));
            log.info("file uploaded {}", fileName);
            return;
        }

        log.info("No file upload");
    }

    @PostMapping("/videoUpload")
    public void videoUpload(@RequestBody byte[] arrayBuffer) {
        if (Objects.nonNull(arrayBuffer)) {
            byteBuffer.put(arrayBuffer);

            log.info("blob uploaded size {},  head {}, end {}", arrayBuffer.length, arrayBuffer[0], arrayBuffer[arrayBuffer.length - 1]);
            return;
        }

        log.info("No file upload");
    }

    @GetMapping("/videoBuffer")
    public ResponseEntity<byte[]> videoBuffer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(byteBuffer.array(), headers, HttpStatus.OK);
    }

//    @PostMapping("/blobsUpload")
//    public void blobUpload(@RequestBody String base64Body) throws IOException {
//        if (Objects.nonNull(base64Body)) {
//            log.info("blob uploaded {}", base64Body);
//            cache.add(base64Body);
//            return;
//        }
//
//        log.info("No blob upload");
//    }
//
//    @GetMapping("/blobs")
//    public ResponseEntity<List<String>> getBlob() {
//        HttpHeaders headers = new HttpHeaders();
//        return new ResponseEntity<>(cache, headers, HttpStatus.OK);
//    }

    @PostMapping("/blobsUpload")
    public void blobUpload(@RequestBody byte[] arrayBuffer) {
        if (Objects.nonNull(arrayBuffer)) {
            byteBuffer.put(arrayBuffer);
            log.info("blob uploaded size {},  head {}, end {}", arrayBuffer.length, arrayBuffer[0], arrayBuffer[arrayBuffer.length - 1]);
            return;
        }

        log.info("No blob upload");
    }

    @GetMapping("/blobs")
    public ResponseEntity<byte []> getBlob() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(byteBuffer.array(), headers, HttpStatus.OK);
    }
}
