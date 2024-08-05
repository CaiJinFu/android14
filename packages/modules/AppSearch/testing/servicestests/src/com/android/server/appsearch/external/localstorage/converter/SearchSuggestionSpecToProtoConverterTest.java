/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.appsearch.external.localstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import android.app.appsearch.SearchSuggestionSpec;

import com.android.server.appsearch.external.localstorage.util.PrefixUtil;
import com.android.server.appsearch.icing.proto.NamespaceDocumentUriGroup;
import com.android.server.appsearch.icing.proto.SchemaTypeConfigProto;
import com.android.server.appsearch.icing.proto.SuggestionSpecProto;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

public class SearchSuggestionSpecToProtoConverterTest {

    @Test
    public void testToProto() throws Exception {
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/ 123)
                        .setRankingStrategy(
                                SearchSuggestionSpec.SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .addFilterDocumentIds("namespace1", ImmutableList.of("doc1", "doc2"))
                        .addFilterSchemas("typeA", "typeB")
                        .build();

        String prefix1 = PrefixUtil.createPrefix("package", "database");
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSuggestionSpecToProtoConverter converter =
                new SearchSuggestionSpecToProtoConverter(
                        /*queryExpression=*/ "prefix",
                        searchSuggestionSpec,
                        /*prefixes=*/ ImmutableSet.of(prefix1),
                        /*namespaceMap=*/ ImmutableMap.of(
                                prefix1,
                                ImmutableSet.of(prefix1 + "namespace1", prefix1 + "namespace2")),
                        /*schemaMap=*/ ImmutableMap.of(
                                prefix1,
                                ImmutableMap.of(
                                        prefix1 + "typeA", configProto,
                                        prefix1 + "typeB", configProto)));

        SuggestionSpecProto proto = converter.toSearchSuggestionSpecProto();

        assertThat(proto.getPrefix()).isEqualTo("prefix");
        assertThat(proto.getNumToReturn()).isEqualTo(123);
        assertThat(proto.getSchemaTypeFiltersList())
                .containsExactly("package$database/typeA", "package$database/typeB");
        assertThat(proto.getNamespaceFiltersList())
                .containsExactly("package$database/namespace1", "package$database/namespace2");
        assertThat(proto.getDocumentUriFiltersList())
                .containsExactly(
                        NamespaceDocumentUriGroup.newBuilder()
                                .setNamespace("package$database/namespace1")
                                .addDocumentUris("doc1")
                                .addDocumentUris("doc2")
                                .build());
    }
}
