package com.user_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {
    String uploadFile(MultipartFile file, String folder);
    void deleteFile(String fileUrl);
}