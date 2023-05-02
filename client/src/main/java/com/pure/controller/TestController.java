package com.pure.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;

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
            String[] split = fileName.split(".");
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
}
