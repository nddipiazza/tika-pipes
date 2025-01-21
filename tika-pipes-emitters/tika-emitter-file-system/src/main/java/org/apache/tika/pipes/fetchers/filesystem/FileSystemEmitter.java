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
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamReadConstraints;
import org.apache.commons.io.output.CloseShieldWriter;
import org.pf4j.Extension;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.pipes.core.emitter.Emitter;
import org.apache.tika.pipes.core.emitter.EmitterConfig;
import org.apache.tika.pipes.core.emitter.OnExistBehavior;
import org.apache.tika.pipes.core.emitter.TikaEmitterException;

@Extension
public class FileSystemEmitter implements Emitter {

    @Override
    public void emit(EmitterConfig emitterConfig, String emitKey, Map<String, Object> metadata, InputStream is) throws IOException {
        FileSystemEmitterConfig config = (FileSystemEmitterConfig) emitterConfig;
        Path basePath = Paths.get(config.getBasePath());
        String fileExtension = config.getFileExtension();
        OnExistBehavior onExists = OnExistBehavior.valueOf(config
                .getOnExists()
                .toUpperCase());
        boolean prettyPrint = config.isPrettyPrint();
        Path output;
        if (metadata == null || metadata.isEmpty()) {
            throw new TikaEmitterException("metadata must not be null or empty");
        }
        if (fileExtension != null && !fileExtension.isEmpty()) {
            emitKey += "." + fileExtension;
        }
        output = basePath.resolve(emitKey);
        if (!Files.isDirectory(output.getParent())) {
            Files.createDirectories(output.getParent());
        }
        if (onExists == OnExistBehavior.REPLACE) {
            Files.copy(is, output, StandardCopyOption.REPLACE_EXISTING);
        } else if (onExists == OnExistBehavior.EXCEPTION) {
            Files.copy(is, output);
        } else if (onExists == OnExistBehavior.SKIP) {
            if (!Files.isRegularFile(output)) {
                try {
                    Files.copy(is, output);
                } catch (FileAlreadyExistsException e) {
                    //swallow
                }
            }
        }
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            toJson(metadata, writer, prettyPrint);
        }
    }

    private void toJson(Map<String, Object> metadata, Writer writer, boolean prettyPrint) throws IOException {
        if (metadata == null) {
            writer.write("null");
            return;
        }
        try (JsonGenerator jsonGenerator = new JsonFactory()
                .setStreamReadConstraints(StreamReadConstraints
                        .builder()
                        .maxStringLength(TikaConfig.getMaxJsonStringFieldLength())
                        .build())
                .createGenerator(new CloseShieldWriter(writer))) {
            if (prettyPrint) {
                jsonGenerator.useDefaultPrettyPrinter();
            }
            jsonGenerator.writeStartObject();
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                jsonGenerator.writeObjectField(entry.getKey(), entry.getValue());
            }
            jsonGenerator.writeEndObject();
        }
    }
}
