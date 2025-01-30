package org.apache.tika.pipes.core.emitter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;


@Getter
@Setter
@Builder
public class EmitOutput {
    private String fetchKey;
    private List<Map<String, List<Object>>> metadata;
}
