package com.ruoyi.file.service;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;

/** Secure internal reader for files created by the configured local file service. */
@Service
public class LocalFileDownloadService
{
    private static final long MAX_DOWNLOAD_SIZE = 50L * 1024L * 1024L;

    @Value("${file.domain}")
    private String domain;

    @Value("${file.path}")
    private String localFilePath;

    @Value("${file.prefix}")
    private String localFilePrefix;

    public ResponseEntity<byte[]> download(String fileUrl) throws Exception
    {
        URI configuredDomain = URI.create(domain);
        URI requested = URI.create(fileUrl);
        if (!sameOrigin(configuredDomain, requested))
        {
            throw new ServiceException("附件地址不属于当前文件服务");
        }

        String prefix = joinPath(configuredDomain.getPath(), localFilePrefix);
        String requestPath = requested.getPath();
        if (requestPath == null || !requestPath.startsWith(prefix + "/"))
        {
            throw new ServiceException("附件路径不合法");
        }

        String relative = requestPath.substring(prefix.length() + 1);
        Path root = Paths.get(localFilePath).toRealPath();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target))
        {
            throw new ServiceException("附件不存在或路径不合法");
        }
        target = target.toRealPath();
        if (!target.startsWith(root))
        {
            throw new ServiceException("附件路径不合法");
        }

        long size = Files.size(target);
        if (size > MAX_DOWNLOAD_SIZE)
        {
            throw new ServiceException("单个附件不能超过50MB");
        }
        String contentType = Files.probeContentType(target);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType));
        headers.setContentLength(size);
        return ResponseEntity.ok().headers(headers).body(Files.readAllBytes(target));
    }

    private boolean sameOrigin(URI left, URI right)
    {
        return equalsIgnoreCase(left.getScheme(), right.getScheme())
                && equalsIgnoreCase(left.getHost(), right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private int effectivePort(URI uri)
    {
        if (uri.getPort() >= 0)
        {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private boolean equalsIgnoreCase(String left, String right)
    {
        return left == null ? right == null : right != null && left.equalsIgnoreCase(right);
    }

    private String joinPath(String first, String second)
    {
        String left = first == null || "/".equals(first) ? "" : trimTrailingSlash(first);
        String right = second == null ? "" : second.trim();
        if (!right.startsWith("/"))
        {
            right = "/" + right;
        }
        return trimTrailingSlash(left + right);
    }

    private String trimTrailingSlash(String value)
    {
        while (value.length() > 1 && value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
