package com.goservi.serviceoffer.controller;


import com.goservi.user.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Subida de archivos a Cloudinary")
public class FileController {

    private final CloudinaryService cloudinaryService;

    @Operation(summary = "Subir múltiples archivos")
    @PostMapping("/upload-multiple")
    public ResponseEntity<List<String>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files) {

        List<String> urls = Arrays.stream(files)
                .map(file -> cloudinaryService.upload(file, "services/gallery"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(urls);
    }

    @Operation(summary = "Subir un solo archivo")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadSingle(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        String url = cloudinaryService.upload(file, folder);
        return ResponseEntity.ok(url);
    }
}