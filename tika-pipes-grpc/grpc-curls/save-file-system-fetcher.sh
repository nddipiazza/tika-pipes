grpcurl -plaintext -d '{"fetcher_id": "fileSystemTest1","plugin_id":"org.apache.tika.pipes.fetchers.filesystem.FileSystemFetcher", "fetcher_config_json": "{\"basePath\": \"/home/ndipiazza/Downloads/docx\"}"}' localhost:9090 tika.Tika/SaveFetcher