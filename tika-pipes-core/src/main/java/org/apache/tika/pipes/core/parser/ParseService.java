package org.apache.tika.pipes.core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.pipes.HandlerConfig;
import org.apache.tika.pipes.core.exception.TikaServerParseException;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.RecursiveParserWrapperHandler;

@Component
@Slf4j
public class ParseService {
    @Value("${parser.taskTimeoutMillis:50000}")
    private Long taskTimeoutMillis;

    public static Map<String, Object> convertMetadataToMap(Metadata metadata) {
        Map<String, Object> metadataMap = new HashMap<>();
        String[] keys = metadata.names();
        for (String key : keys) {
            String[] values = metadata.getValues(key);
            if (values.length == 1) {
                metadataMap.put(key, values[0]);
            } else {
                metadataMap.put(key, values);
            }
        }
        return metadataMap;
    }

    private Parser createParser(TikaConfig tikaConfig) {
        return new AutoDetectParser(tikaConfig);
    }

    public List<Map<String, Object>> parseDocument(InputStream inputStream) {
        try {
            TikaConfig tikaConfig = new TikaConfig();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            Parser parser = createParser(tikaConfig);
            RecursiveParserWrapper wrapper = new RecursiveParserWrapper(parser);
            RecursiveParserWrapperHandler handler = getRecursiveParserWrapperHandler(context, tikaConfig);
            parse(wrapper, inputStream, handler, metadata, context);
            return handler
                    .getMetadataList()
                    .stream()
                    .map(ParseService::convertMetadataToMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Parse document failed", e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static RecursiveParserWrapperHandler getRecursiveParserWrapperHandler(ParseContext context, TikaConfig tikaConfig) {
        HandlerConfig handlerConfig = new HandlerConfig(BasicContentHandlerFactory.HANDLER_TYPE.TEXT, HandlerConfig.PARSE_MODE.RMETA, 50000, 50, false);
        BasicContentHandlerFactory.HANDLER_TYPE type = handlerConfig.getType();
        BasicContentHandlerFactory contentHandlerFactory = new BasicContentHandlerFactory(type, handlerConfig.getWriteLimit(), handlerConfig.isThrowOnWriteLimitReached(), context);
        return new RecursiveParserWrapperHandler(contentHandlerFactory, handlerConfig.getMaxEmbeddedResources(), tikaConfig.getMetadataFilter());
    }

    /**
     * Use this to call a parser and unify exception handling.
     * NOTE: This call to parse closes the InputStream. DO NOT surround
     * the call in an auto-close block.
     *
     * @param parser       parser to use
     * @param inputStream  inputStream (which is closed by this call!)
     * @param handler      handler to use
     * @param metadata     metadata
     * @param parseContext parse context
     * @throws IOException wrapper for all exceptions
     */
    private void parse(Parser parser, InputStream inputStream, ContentHandler handler, Metadata metadata, ParseContext parseContext) throws IOException {
        String fileName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);

        //long taskId = SERVER_STATUS.start(ServerStatus.TASK.PARSE, fileName, timeoutMillis);
        try {
            parser.parse(inputStream, handler, metadata, parseContext);
        } catch (SAXException e) {
            throw new TikaServerParseException(e);
        } catch (EncryptedDocumentException e) {
            log.warn("Encrypted document ({})", fileName, e);
            throw new TikaServerParseException(e);
        } catch (Exception e) {
            if (!WriteLimitReachedException.isWriteLimitReached(e)) {
                log.warn("Text extraction failed ({})", fileName, e);
            }
            throw new TikaServerParseException(e);
        } catch (OutOfMemoryError e) {
            log.warn("OOM ({})", fileName, e);
//            SERVER_STATUS.setStatus(ServerStatus.STATUS.ERROR);
            throw e;
        } finally {
//            SERVER_STATUS.complete(taskId);
            inputStream.close();
        }
    }
}