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
package com.hurence.logisland.kafka.store;


import com.hurence.logisland.kafka.registry.KafkaRegistry;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class KafkaRegistryTest {
  @Test
  public void testGetPortForIdentityPrecedence() {
    List<String> listeners = new LinkedList<String>();
    listeners.add("http://localhost:456");

    int port = KafkaRegistry.getPortForIdentity(123, listeners);
    assertEquals("Expected listeners to take precedence over port.", 456, port);
  }

  @Test
  public void testGetPortForIdentityNoListeners() {
    List<String> listeners = new LinkedList<String>();
    int port = KafkaRegistry.getPortForIdentity(123, listeners);
    assertEquals("Expected port to take the configured port value", 123, port);
  }

  @Test
  public void testGetPortForIdentityMultipleListeners() {
    List<String> listeners = new LinkedList<String>();
    listeners.add("http://localhost:123");
    listeners.add("https://localhost:456");

    int port = KafkaRegistry.getPortForIdentity(-1, listeners);
    assertEquals("Expected first listener's port to be returned", 123, port);
  }
}
