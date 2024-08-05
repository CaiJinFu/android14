/*
 * Copyright (C) 2022 The Android Open Source Project
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
package android.adservices.topics;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import androidx.test.filters.SmallTest;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link android.adservices.topics.GetTopicsResponse}
 */
@SmallTest
public final class GetTopicsResponseTest {

    @Test
    public void testGetTopicsResponseBuilder_nullableThrows() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    GetTopicsResponse unusedResponse = new GetTopicsResponse.Builder(null).build();
                });
    }

    @Test
    public void testGetTopicsResponseBuilder() {
        List<Topic> topics =
                List.of(
                        new Topic(
                                /* mTaxonomyVersion */ 1L, /* mModelVersion */
                                1L, /* mTopicId */
                                0));

        // Build GetTopicsResponse using topicList
        GetTopicsResponse response = new GetTopicsResponse.Builder(topics).build();

        // Validate the topicList is same to what we created
        assertEquals(topics, response.getTopics());
    }

    @Test
    public void testEquals() {
        List<Topic> topics =
                List.of(
                        new Topic(
                                /* mTaxonomyVersion */ 1L, /* mModelVersion */
                                1L, /* mTopicId */
                                0));
        GetTopicsResponse getTopicsResponse1 = new GetTopicsResponse.Builder(topics).build();
        GetTopicsResponse getTopicsResponse2 = new GetTopicsResponse.Builder(topics).build();

        assertThat(getTopicsResponse1.equals(getTopicsResponse2)).isTrue();
    }

    @Test
    public void testHashCode() {
        List<Topic> topics =
                List.of(
                        new Topic(
                                /* mTaxonomyVersion */ 1L, /* mModelVersion */
                                1L, /* mTopicId */
                                0));
        GetTopicsResponse getTopicsResponse1 = new GetTopicsResponse.Builder(topics).build();
        GetTopicsResponse getTopicsResponse2 = new GetTopicsResponse.Builder(topics).build();

        assertThat(getTopicsResponse1.hashCode()).isEqualTo(getTopicsResponse2.hashCode());
    }
}

