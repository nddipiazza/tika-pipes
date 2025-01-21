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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.pf4j.Extension;

import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.pipes.core.iterators.PipeIterator;
import org.apache.tika.pipes.core.iterators.TikaPipeIteratorException;

@Extension
public class CsvPipeIterator implements PipeIterator {
    private final String fetchKeyColumn;
    private final Reader reader;
    private final Iterator<CSVRecord> records;

    public CsvPipeIterator(CsvPipeIteratorConfig config) {
        try {
            Path csvPath = config.getCsvPath();
            fetchKeyColumn = config.getFetchKeyColumn();
            CSVFormat csvFormat = CSVFormat.valueOf(config
                    .getCsvFormat()
                    .toUpperCase());
            reader = Files.newBufferedReader(csvPath, Charset.forName(config.getCharset()));
            records = csvFormat.parse(reader).getRecords().iterator();
        } catch (IOException e) {
            throw new TikaPipeIteratorException("Could not initialize reader", e);
        }
    }

    @Override
    public boolean hasNext() {
        return records.hasNext();
    }

    @Override
    public List<FetchAndParseRequest> next() {
        CSVRecord record = records.next();
        return List.of(FetchAndParseRequest
                .newBuilder()
                .setFetchKey(record.get(fetchKeyColumn))
                .setMetadataJson("{}")
                .build());
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
