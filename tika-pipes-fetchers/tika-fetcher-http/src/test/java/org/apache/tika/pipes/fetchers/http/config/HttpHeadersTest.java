package org.apache.tika.pipes.fetchers.http.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpHeadersTest {
    @Test
    void testHeaderJackson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());
        HttpHeaders value = new HttpHeaders();

        value.getHeaders().put("hi", "nick");
        value.getHeaders().putAll("hiagain", List.of("nick", "nick"));
        String str = objectMapper.writeValueAsString(value);

        HttpHeaders parsed = objectMapper.readValue(str, HttpHeaders.class);

        Assertions.assertEquals(parsed.getHeaders().get("hi"), value.getHeaders().get("hi"));
        Assertions.assertEquals(parsed.getHeaders().get("hiagain"), value.getHeaders().get("hiagain"));
    }
}
