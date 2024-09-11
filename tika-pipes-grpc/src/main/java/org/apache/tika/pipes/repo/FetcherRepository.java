package org.apache.tika.pipes.repo;

import fetcher.FetcherConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FetcherRepository extends MongoRepository<FetcherConfig, String> {
    FetcherConfig findByFetcherId(String fetcherId);
}
