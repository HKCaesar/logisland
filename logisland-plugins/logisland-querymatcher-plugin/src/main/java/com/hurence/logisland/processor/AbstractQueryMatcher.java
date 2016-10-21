/**
 * Copyright (C) 2016 Hurence (bailet.thomas@gmail.com)
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
package com.hurence.logisland.processor;

import java.util.Collections;
import java.util.List;

/**
 * Created by fprunier on 15/04/16.
 */
public abstract class AbstractQueryMatcher {

    public static String EVENT_MATCH_TYPE_NAME = "querymatch";

    private List<MatchingRule> rules = Collections.emptyList();

    protected List<MatchingRule> getRules() {
        return rules;
    }

    public void setRules(List<MatchingRule> rules) {
        this.rules = rules;
    }
}
