package com.livingframe.system.controller;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.CreateBucketRequest;
import com.livingframe.system.dto.FileConfigResponse;
import com.livingframe.system.dto.MinioObjectVO;
import com.livingframe.system.service.MinioFileService;
import com.livingframe.system.vo.FileDownloadVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @DeleteMapping("/objects")
    @OperationLog("删除 MinIO 文件")
    @PreAuthorize("@ss.hasPermi('system:file:delete')")
    public R<Void> deleteObject(@RequestParam(required = false) String bucket,
                                @RequestParam String objectName) {
        fileService.deleteObject(bucket, objectName);
        return R.ok();
    }
}
