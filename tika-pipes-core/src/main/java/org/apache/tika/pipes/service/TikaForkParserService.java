package org.apache.tika.pipes.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.fork.ForkParser;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.pipes.fetcher.Fetcher;
import org.apache.tika.sax.BodyContentHandler;

@Service
public class TikaForkParserService {

    @Autowired
    private ForkParser forkParser;

    @Autowired
    private Executor forkParserExecutor;

    @Async("forkParserExecutor")
    public String parseFileAsync(Fetcher fetcher, InputStream inputStream) throws IOException, TikaException, SAXException {
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        forkParser.parse(inputStream, handler, metadata, new ParseContext());
        return handler.toString();
    }
}
