package com.goservi.user.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.goservi.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String upload(MultipartFile file, String folder) {
        try {
            Map result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "goservi/" + folder, "resource_type", "auto"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            log.error("Cloudinary upload error", e);
            throw new BadRequestException("Error al subir imagen: " + e.getMessage());
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Could not delete from Cloudinary: {}", e.getMessage());
        }
    }
}
