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
package org.apache.tika.pipes.fetchers.atlassianjwt;

import org.pf4j.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtlassianJwtFetcherPlugin extends Plugin {
    private static final Logger LOG = LoggerFactory.getLogger(AtlassianJwtFetcherPlugin.class);
    
    @Override
    public void start() {
        LOG.info("Starting Atlassian JWT Fetcher Plugin");
        super.start();
    }

    @Override
    public void stop() {
        LOG.info("Stopping Atlassian JWT Fetcher Plugin");
        super.stop();
    }

    @Override
    public void delete() {
        LOG.info("Deleting Atlassian JWT Fetcher Plugin");
        super.delete();
    }
}