package org.apache.tika.pipes.core.iterators;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class PipeInput {
    private String fetchKey;
    private Map<String, Object> metadata;
}
