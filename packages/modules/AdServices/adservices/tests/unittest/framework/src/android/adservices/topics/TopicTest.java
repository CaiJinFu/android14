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

import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link android.adservices.topics.Topic} */
public class TopicTest {
    private Topic mTopic1;
    private Topic mTopic2;

    @Before
    public void setup() throws Exception {
        generateTopics();
    }

    @Test
    public void testGetters() {
        assertThat(mTopic1.getTopicId()).isEqualTo(1);
        assertThat(mTopic1.getModelVersion()).isEqualTo(1L);
        assertThat(mTopic1.getTaxonomyVersion()).isEqualTo(1L);
    }

    @Test
    public void testToString() {
        String expectedTopicString = "Topic{mTaxonomyVersion=1, mModelVersion=1, mTopicCode=1}";
        assertThat(mTopic1.toString()).isEqualTo(expectedTopicString);
        assertThat(mTopic2.toString()).isEqualTo(expectedTopicString);
    }

    @Test
    public void testEquals() {
        assertThat(mTopic1).isEqualTo(mTopic2);
    }

    @Test
    public void testEquals_nullObject() {
        // To test code won't throw if comparing to a null object.
        assertThat(mTopic1).isNotEqualTo(null);
    }

    @Test
    public void testHashCode() {
        assertThat(mTopic1.hashCode()).isEqualTo(mTopic2.hashCode());
    }

    private void generateTopics() {
        mTopic1 = new Topic(/* mTaxonomyVersion */ 1L, /* mModelVersion */ 1L, /* mTopicId */ 1);
        mTopic2 = new Topic(/* mTaxonomyVersion */ 1L, /* mModelVersion */ 1L, /* mTopicId */ 1);
    }
}
