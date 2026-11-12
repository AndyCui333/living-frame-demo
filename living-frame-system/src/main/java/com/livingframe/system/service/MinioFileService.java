package com.livingframe.system.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.system.config.MinioProperties;
import com.livingframe.system.dto.AvatarResource;
import com.livingframe.system.dto.FileConfigResponse;
import com.livingframe.system.dto.MinioObjectVO;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public FileConfigResponse config() {
        return new FileConfigResponse(properties.getDefaultBucket());
    }

    public List<String> listBuckets() {
        try {
            List<String> names = new ArrayList<>();
            for (Bucket bucket : minioClient.listBuckets()) {
                names.add(bucket.name());
            }
            String defaultBucket = properties.getDefaultBucket();
            if (StrUtil.isNotBlank(defaultBucket) && !names.contains(defaultBucket)) {
                createBucket(defaultBucket);
                names.add(defaultBucket);
            }
            names.sort(String::compareTo);
            return names;
        } catch (Exception ex) {
            throw minioError("获取 Bucket 列表", ex);
        }
    }

    public void createBucket(String bucketName) {
        String bucket = normalizeBucket(bucketName);
        try {
            if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                throw new BusinessException("Bucket 已存在: " + bucket);
            }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw minioError("创建 Bucket", ex);
        }
    }

    public void deleteBucket(String bucketName) {
        String bucket = normalizeBucket(bucketName);
        if (bucket.equals(properties.getDefaultBucket())) {
            throw new BusinessException("默认 Bucket 不能删除");
        }
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
        } catch (Exception ex) {
            throw minioError("删除 Bucket（Bucket 必须为空）", ex);
        }
    }

    public List<MinioObjectVO> listObjects(String bucketName, String prefix) {
        String bucket = resolveBucket(bucketName);
        String normalizedPrefix = normalizePrefix(prefix);
        try {
            requireBucket(bucket);
            List<MinioObjectVO> records = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(normalizedPrefix)
                    .delimiter("/")
                    .recursive(false)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                if (item.isDir()) {
                    records.add(new MinioObjectVO(
                            objectName,
                            relativeName(normalizedPrefix, objectName),
                            0,
                            null,
                            true));
                } else {
                    records.add(new MinioObjectVO(
                            objectName,
                            relativeName(normalizedPrefix, objectName),
                            item.size(),
                            item.lastModified() == null ? null
                                    : item.lastModified().withZoneSameInstant(ZoneId.systemDefault()).format(FORMATTER),
                            false));
                }
            }
            records.sort(Comparator.comparing(MinioObjectVO::dir).reversed()
                    .thenComparing(MinioObjectVO::name, String.CASE_INSENSITIVE_ORDER));
            return records;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw minioError("读取文件列表", ex);
        }
    }

    public String upload(String bucketName, String prefix, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要上传的文件");
        }
        String bucket = resolveBucket(bucketName);
        String objectName = normalizePrefix(prefix) + sanitizeFileName(file.getOriginalFilename());
        try {
            requireBucket(bucket);
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(StrUtil.blankToDefault(file.getContentType(), "application/octet-stream"))
                        .build());
            }
            return objectName;
        } catch (Exception ex) {
            throw minioError("上传文件", ex);
        }
    }

    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像图片不能超过 5MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!AVATAR_EXTENSIONS.contains(extension)) {
            throw new BusinessException("头像仅支持 JPG、PNG、GIF、WebP 格式");
        }
        String contentType = StrUtil.blankToDefault(file.getContentType(), contentTypeOf(extension));
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("上传文件不是图片");
        }
        String bucket = properties.getDefaultBucket();
        String objectName = "avatar/"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/" + IdUtil.fastSimpleUUID() + "." + extension;
        try {
            requireBucket(bucket);
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }
            return objectName;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw minioError("上传头像", ex);
        }
    }

    public AvatarResource avatar(String objectName) {
        String object = requireObjectName(objectName);
        if (!object.startsWith("avatar/")) {
            throw new BusinessException("非法头像路径");
        }
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getDefaultBucket())
                    .object(object)
                    .build());
            if (stat.size() > MAX_AVATAR_SIZE) {
                throw new BusinessException("头像文件过大");
            }
            try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getDefaultBucket())
                    .object(object)
                    .build())) {
                String contentType = StrUtil.blankToDefault(stat.contentType(), contentTypeOf(extensionOf(object)));
                return new AvatarResource(inputStream.readAllBytes(), contentType);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw minioError("读取头像", ex);
        }
    }

    public String downloadUrl(String bucketName, String objectName) {
        String bucket = resolveBucket(bucketName);
        String object = requireObjectName(objectName);
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(object)
                    .expiry((int) properties.getPresignedExpirySeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception ex) {
            throw minioError("生成下载地址", ex);
        }
    }

    public void deleteObject(String bucketName, String objectName) {
        String bucket = resolveBucket(bucketName);
        String object = requireObjectName(objectName);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .build());
        } catch (Exception ex) {
            throw minioError("删除文件", ex);
        }
    }

    private String resolveBucket(String bucketName) {
        return StrUtil.isBlank(bucketName)
                ? properties.getDefaultBucket()
                : normalizeBucket(bucketName);
    }

    private void requireBucket(String bucket) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            throw new BusinessException("Bucket 不存在: " + bucket);
        }
    }

    private String normalizeBucket(String bucketName) {
        String bucket = StrUtil.blankToDefault(bucketName, "").trim();
        if (StrUtil.isBlank(bucket)) {
            throw new BusinessException("Bucket 不能为空");
        }
        if (!bucket.matches("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$")) {
            throw new BusinessException("Bucket 名称不合法，只能包含小写字母、数字、点和短横线");
        }
        return bucket;
    }

    private String normalizePrefix(String prefix) {
        String value = StrUtil.blankToDefault(prefix, "").replace('\\', '/').trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.contains("..")) {
            throw new BusinessException("非法目录路径");
        }
        if (!value.isEmpty() && !value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    private String requireObjectName(String objectName) {
        String value = StrUtil.blankToDefault(objectName, "").replace('\\', '/').trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isEmpty() || value.contains("..")) {
            throw new BusinessException("非法文件路径");
        }
        return value;
    }

    private String sanitizeFileName(String originalName) {
        String name = StrUtil.blankToDefault(originalName, "").replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\r\\n\\t]", "").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            name = "file-" + IdUtil.fastSimpleUUID();
        }
        return name;
    }

    private String relativeName(String prefix, String objectName) {
        if (StrUtil.isNotBlank(prefix) && objectName.startsWith(prefix)) {
            String relative = objectName.substring(prefix.length());
            return relative.endsWith("/") ? relative.substring(0, relative.length() - 1) : relative;
        }
        return objectName;
    }

    private String extensionOf(String name) {
        String value = StrUtil.blankToDefault(name, "");
        int dot = value.lastIndexOf('.');
        return dot < 0 ? "" : value.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String contentTypeOf(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private BusinessException minioError(String action, Exception ex) {
        log.error("MinIO {} 失败: {}", action, ex.getMessage());
        return new BusinessException("MinIO " + action + "失败：" + StrUtil.maxLength(ex.getMessage(), 300));
    }
}
