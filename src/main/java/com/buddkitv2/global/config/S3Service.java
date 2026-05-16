package com.buddkitv2.global.config;

import com.buddkitv2.global.exception.FileUploadException;
import com.buddkitv2.global.exception.FileSizeExceededException;
import com.buddkitv2.global.exception.InvalidFileTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png");

    public String upload(MultipartFile file, String directory) {
        validate(file);
        String key = directory + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new FileUploadException();
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new FileSizeExceededException();
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidFileTypeException();
        }
    }
}
