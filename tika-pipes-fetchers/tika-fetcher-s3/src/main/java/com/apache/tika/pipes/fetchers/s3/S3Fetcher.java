/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apache.tika.pipes.fetchers.s3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.apache.tika.pipes.fetchers.s3.config.S3FetcherConfig;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import org.apache.tika.exception.FileTooLongException;
import org.apache.tika.io.FilenameUtils;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.pipes.fetchers.core.Fetcher;
import org.apache.tika.pipes.fetchers.core.FetcherConfig;
import org.apache.tika.utils.StringUtils;

@Extension
@Slf4j
public class S3Fetcher implements Fetcher {
    private static final String PREFIX = "s3";

    //Do not retry if there's an AmazonS3Exception with this error code
    private static final Set<String> NO_RETRY_ERROR_CODES = new HashSet<>();
    private static final ConcurrentMap<S3FetcherConfig, AmazonS3> clientMap = new ConcurrentHashMap<>();

    //Keep this private so that we can change as needed.
    //Not sure if it is better to have an accept list (only throttle on too many requests)
    //or this deny list...don't throttle for these s3 exceptions
    static {
        NO_RETRY_ERROR_CODES.add("AccessDenied");
        NO_RETRY_ERROR_CODES.add("NoSuchKey");
        NO_RETRY_ERROR_CODES.add("ExpiredToken");
        NO_RETRY_ERROR_CODES.add("InvalidAccessKeyId");
        NO_RETRY_ERROR_CODES.add("InvalidRange");
        NO_RETRY_ERROR_CODES.add("InvalidRequest");
    }

    @Override
    public InputStream fetch(FetcherConfig fetcherConfig, String fetchKey, Map<String, Object> fetchMetadata, Map<String, Object> responseMetadata) {
        S3FetcherConfig s3FetcherConfig = (S3FetcherConfig) fetcherConfig;
        List<Long> throttleSeconds = s3FetcherConfig.getThrottleSeconds();
        int tries = 0;
        IOException ex;
        do {
            AmazonS3 s3Client = clientMap.computeIfAbsent(s3FetcherConfig, k -> new S3ClientManager(s3FetcherConfig).getS3Client());
            String prefix = s3FetcherConfig.getPrefix();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(prefix) && !prefix.endsWith("/")) {
                prefix += "/";
            }
            String theFetchKey = StringUtils.isBlank(prefix) ? fetchKey : prefix + fetchKey;
            try {
                long start = System.currentTimeMillis();
                InputStream is = _fetch(s3Client, s3FetcherConfig, theFetchKey, fetchMetadata, responseMetadata);
                long elapsed = System.currentTimeMillis() - start;
                log.debug("total to fetch {}", elapsed);
                return is;
            } catch (AmazonS3Exception e) {
                if (e.getErrorCode() != null && NO_RETRY_ERROR_CODES.contains(e.getErrorCode())) {
                    log.warn("Hit a no retry error code for key {}. Not retrying." + tries, theFetchKey, e);
                    throw new RuntimeException(e);
                }
                log.warn("client exception fetching on retry=" + tries, e);
                ex = new IOException(e);
            } catch (AmazonClientException e) {
                log.warn("client exception fetching on retry=" + tries, e);
                ex = new IOException(e);
            } catch (IOException e) {
                log.warn("client exception fetching on retry=" + tries, e);
                ex = e;
            }
            log.warn("sleeping for {} seconds before retry", throttleSeconds.get(tries));
            try {
                Thread.sleep(throttleSeconds.get(tries));
            } catch (InterruptedException e) {
                throw new RuntimeException("interrupted");
            }

        } while (++tries < throttleSeconds.size());

        throw new RuntimeException(ex);
    }

    private InputStream _fetch(AmazonS3 s3Client, S3FetcherConfig s3FetcherConfig, String fetchKey, Map<String, Object> fetchMetadata, Map<String, Object> responseMetadata) throws IOException {
        Long startRange = (Long) fetchMetadata.get("startRange");
        Long endRange = (Long) fetchMetadata.get("endRange");
        TemporaryResources tmp = null;
        String bucket = s3FetcherConfig.getBucket();
        try {
            long start = System.currentTimeMillis();
            GetObjectRequest objectRequest = new GetObjectRequest(bucket, fetchKey);
            if (startRange != null && endRange != null && startRange > -1 && endRange > -1) {
                objectRequest.withRange(startRange, endRange);
            }
            S3Object s3Object = s3Client.getObject(objectRequest);
            long length = s3Object
                    .getObjectMetadata()
                    .getContentLength();
            responseMetadata.put(Metadata.CONTENT_LENGTH, Long.toString(length));
            long maxLength = s3FetcherConfig.getMaxLength();
            if (maxLength > -1) {
                if (length > maxLength) {
                    throw new FileTooLongException(length, maxLength);
                }
            }
            log.debug("took {} ms to fetch file's metadata", System.currentTimeMillis() - start);

            if (s3FetcherConfig.isExtractUserMetadata()) {
                for (Map.Entry<String, String> e : s3Object
                        .getObjectMetadata()
                        .getUserMetadata()
                        .entrySet()) {
                    fetchMetadata.put(PREFIX + ":" + e.getKey(), e.getValue());
                }
            }
            if (!s3FetcherConfig.isSpoolToTemp()) {
                return TikaInputStream.get(s3Object.getObjectContent());
            } else {
                start = System.currentTimeMillis();
                tmp = new TemporaryResources();
                Path tmpPath = tmp.createTempFile(FilenameUtils.getSuffixFromPath(fetchKey));
                Files.copy(s3Object.getObjectContent(), tmpPath, StandardCopyOption.REPLACE_EXISTING);
                Metadata metadata = new Metadata();
                TikaInputStream tis = TikaInputStream.get(tmpPath, metadata, tmp);
                log.debug("took {} ms to fetch metadata and copy to local tmp file", System.currentTimeMillis() - start);
                return tis;
            }
        } catch (Throwable e) {
            if (tmp != null) {
                tmp.close();
            }
            throw e;
        }
    }
}
