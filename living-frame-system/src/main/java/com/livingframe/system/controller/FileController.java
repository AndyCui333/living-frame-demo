package com.livingframe.system.controller;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.CreateBucketRequest;
import com.livingframe.system.dto.FileConfigResponse;
import com.livingframe.system.dto.MinioObjectVO;
import com.livingframe.system.service.MinioFileService;
import com.livingframe.system.vo.FileDownloadVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/system/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioFileService fileService;

    @GetMapping("/config")
    @PreAuthorize("@ss.hasPermi('system:file:list')")
    public R<FileConfigResponse> config() {
        return R.ok(fileService.config());
    }

    @GetMapping("/buckets")
    @PreAuthorize("@ss.hasPermi('system:file:list')")
    public R<List<String>> buckets() {
        return R.ok(fileService.listBuckets());
    }

    @PostMapping("/buckets")
    @OperationLog("创建 MinIO Bucket")
    @PreAuthorize("@ss.hasPermi('system:file:bucket')")
    public R<Void> createBucket(@Valid @RequestBody CreateBucketRequest request) {
        fileService.createBucket(request.bucketName());
        return R.ok();
    }

    @DeleteMapping("/buckets/{bucket}")
    @OperationLog("删除 MinIO Bucket")
    @PreAuthorize("@ss.hasPermi('system:file:bucket')")
    public R<Void> deleteBucket(@PathVariable String bucket) {
        fileService.deleteBucket(bucket);
        return R.ok();
    }

    @GetMapping("/objects")
    @PreAuthorize("@ss.hasPermi('system:file:list')")
    public R<List<MinioObjectVO>> objects(@RequestParam(required = false) String bucket,
                                          @RequestParam(required = false) String prefix) {
        return R.ok(fileService.listObjects(bucket, prefix));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog("上传 MinIO 文件")
    @PreAuthorize("@ss.hasPermi('system:file:upload')")
    public R<String> upload(@RequestParam(required = false) String bucket,
                            @RequestParam(required = false) String prefix,
                            @RequestParam("file") MultipartFile file) {
        return R.ok(fileService.upload(bucket, prefix, file));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.uploadAvatar(file));
    }

    @GetMapping("/download")
    @PreAuthorize("@ss.hasPermi('system:file:list')")
    public R<FileDownloadVO> download(@RequestParam(required = false) String bucket,
                                      @RequestParam String objectName) {
        return R.ok(new FileDownloadVO(fileService.downloadUrl(bucket, objectName)));
    }

    /**
     * 同源媒体代理。使用短时签名 token，浏览器可以直接在 img/video/iframe 中加载，
     * 不再暴露 MinIO 内网 HTTP 地址，也不会触发 Mixed Content。
     */
    @RequestMapping(value = "/media", method = {org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> media(@RequestParam String token,
                                                       @RequestHeader HttpHeaders headers,
                                                       HttpServletRequest request) {
        MinioFileService.MediaObject media = fileService.resolveMedia(token);
        long size = media.size();
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);
        long start = 0;
        long end = size - 1;
        if (range != null && size > 0) {
            start = range.getRangeStart(size);
            end = range.getRangeEnd(size);
            if (start >= size) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + size)
                        .build();
            }
        }
        long length = size == 0 ? 0 : end - start + 1;
        boolean partial = range != null;
        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(partial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(media.objectName()))
                .contentType(MediaType.parseMediaType(media.contentType()))
                .contentLength(length);
        if (partial) {
            builder.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + size);
        }
        if (size == 0 || "HEAD".equalsIgnoreCase(request.getMethod())) {
            return builder.build();
        }
        long offset = start;
        long readLength = length;
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = fileService.openMedia(media, offset, readLength)) {
                inputStream.transferTo(outputStream);
            }
        };
        return builder.body(body);
    }

    private String contentDisposition(String objectName) {
        String name = objectName.substring(objectName.lastIndexOf('/') + 1);
        String encoded = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "inline; filename*=UTF-8''" + encoded;
    }

    @DeleteMapping("/objects")
    @OperationLog("删除 MinIO 文件")
    @PreAuthorize("@ss.hasPermi('system:file:delete')")
    public R<Void> deleteObject(@RequestParam(required = false) String bucket,
                                @RequestParam String objectName) {
        fileService.deleteObject(bucket, objectName);
        return R.ok();
    }
}
