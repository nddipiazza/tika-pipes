package org.apache.tika.pipes.fetchers.filesystem;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import org.pf4j.Extension;

import org.apache.tika.pipes.core.iterators.DefaultPipeIteratorConfig;

@Extension
@Getter
@Setter
public class CsvPipeIteratorConfig extends DefaultPipeIteratorConfig {
    private String charset = StandardCharsets.UTF_8.name();
    private Path csvPath;
    private String fetchKeyColumn;
    private String csvFormat;
}
