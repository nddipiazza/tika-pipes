rpcurl -plaintext -d '{"fetcher_id": "httpFetcherTest1","plugin_id":"org.apache.tika.pipes.fetcher.fs.FileSystemFetcher", "fetcher_config_json": "{\"basePath\": \"/home/ndipiazza/Downloads/docx\"}"}' localhost:50052 tika.Tika/SaveFetcher
