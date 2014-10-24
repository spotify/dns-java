/*
 * Copyright (c) 2012-2014 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotify.dns;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * Utility functions that are shared between tests.
 */
public class DnsTestUtil {
  static List<LookupResult> nodes(String... nodeNames) {
    return Lists.transform(
        Arrays.asList(nodeNames),
        new Function<String, LookupResult>() {
          @Override
          public LookupResult apply(String input) {
            return LookupResult.create(input, 8080, 1, 2, 999);
          }
        }
    );
  }
}
