package com.pure.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.http.HttpResponse;
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

    // static final Map<String, byte[]> cache = new HashMap<>();
    static final List<String> cache = new ArrayList<>();

    @GetMapping("/str")
    public String str() {
        return "TestController ==> str()";
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
    public void videoUpload(MultipartFile file) throws IOException {
        if (Objects.nonNull(file)) {

            String fileName = file.getOriginalFilename();
            Assert.notNull(file, "file name should not be null");

            String[] split = fileName.split("\\.");
            String extName = split[1];
            Path fileUploadPath = Paths.get("./upload/" + extName);

            if (!Files.exists(fileUploadPath)) {
                Files.createDirectories(fileUploadPath);
            }
            long uploadDate = new Date().getTime();
            file.transferTo(fileUploadPath.resolve(uploadDate + "-" + fileName));
            log.info("file uploaded {}", fileName);
            return;
        }

        log.info("No file upload");
    }

    @PostMapping("/blobUpload")
    public void blobUpload(@RequestBody String base64Body) throws IOException {
        if (Objects.nonNull(base64Body)) {
            log.info("blob uploaded {}", base64Body);
            cache.add(base64Body);
            return;
        }

        log.info("No blob upload");
    }

    @GetMapping("/blobs")
    public ResponseEntity<List<String>> getBlob() {
        HttpHeaders headers = new HttpHeaders();
        return new ResponseEntity<>(cache, headers, HttpStatus.OK);
    }

//    @PostMapping("/blobUpload")
//    public void blobUpload(@RequestBody byte[] videoArrayBuffer) throws IOException {
//        if (Objects.nonNull(videoArrayBuffer)) {
//            log.info("blob uploaded {}", videoArrayBuffer);
//            cache.put("upload", videoArrayBuffer);
//            return;
//        }
//
//        log.info("No blob upload");
//    }

//    @GetMapping("/blobs")
//    public ResponseEntity<byte[]> getBlob() {
//        byte[] body = cache.get("upload");
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//        headers.setContentLength(body.length);
//        return new ResponseEntity<>(body, headers, HttpStatus.OK);
//    }
}
