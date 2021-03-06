/**
 * Copyright (C) 2016 Hurence (support@hurence.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hurence.logisland.agent.rest.client;

import com.hurence.logisland.agent.rest.model.Property;

import java.util.ArrayList;
import java.util.List;


public class MockConfigsApiClient implements ConfigsApiClient {


    public MockConfigsApiClient(String zkPort, String kafkaPort) {

        properties.add(new Property().key("kafka.metadata.broker.list").value("localhost:" + kafkaPort));
        properties.add(new Property().key("kafka.zookeeper.quorum").value("localhost:" + zkPort));
        properties.add(new Property().key("kafka.topic.autoCreate").value("true"));
        properties.add(new Property().key("kafka.topic.default.partitions").value("1"));
        properties.add(new Property().key("kafka.topic.default.replicationFactor").value("1"));
    }







    List<Property> properties = new ArrayList<>();


    @Override
    public List<Property> getConfigs() {
        return properties;
    }
}
