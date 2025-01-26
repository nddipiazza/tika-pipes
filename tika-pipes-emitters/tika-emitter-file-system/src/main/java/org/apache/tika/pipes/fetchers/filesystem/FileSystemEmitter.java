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
package org.apache.tika.pipes.fetchers.filesystem;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pf4j.Extension;

import org.apache.tika.FetchAndParseReply;
import org.apache.tika.pipes.core.emitter.Emitter;
import org.apache.tika.pipes.core.emitter.EmitterConfig;
import org.apache.tika.pipes.core.emitter.OnExistBehavior;

@Extension
public class FileSystemEmitter implements Emitter {
    @Override
    public String getPluginId() {
        return "filesystem-emitter";
    }

    @Override
    public void emit(EmitterConfig emitterConfig, List<FetchAndParseReply> fetchAndParseReplies)
            throws IOException {
        FileSystemEmitterConfig config = (FileSystemEmitterConfig) emitterConfig;
        Path basePath = Paths.get(config.getBasePath());
        String fileExtension = config.getFileExtension();
        OnExistBehavior onExists = OnExistBehavior.valueOf(config
                .getOnExists()
                .toUpperCase());
        for (FetchAndParseReply fetchAndParseReply : fetchAndParseReplies) {
            Path output;
            String emitKey = fetchAndParseReply.getFetchKey();
            if (fileExtension != null && !fileExtension.isEmpty()) {
                emitKey += "." + fileExtension;
            }
            if (basePath != null) {
                output = basePath.resolve(emitKey);
            } else {
                output = Paths.get(emitKey);
            }
            if (!Files.isDirectory(output.getParent())) {
                Files.createDirectories(output.getParent());
            }
            if (onExists == OnExistBehavior.SKIP && Files.isRegularFile(output)) {
                continue;
            } else if (onExists == OnExistBehavior.EXCEPTION && Files.isRegularFile(output)) {
                throw new FileAlreadyExistsException(output.toString());
            }
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(writer, fetchAndParseReply.getMetadataList());
            }
        }
    }
}
