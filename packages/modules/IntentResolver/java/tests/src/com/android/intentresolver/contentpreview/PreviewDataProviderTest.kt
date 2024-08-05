/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.intentresolver.contentpreview

import android.content.ContentInterface
import android.content.Intent
import android.database.MatrixCursor
import android.media.MediaMetadata
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.Lifecycle
import com.android.intentresolver.TestLifecycleOwner
import com.android.intentresolver.mock
import com.android.intentresolver.whenever
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewDataProviderTest {
    private val contentResolver = mock<ContentInterface>()
    private val mimeTypeClassifier = DefaultMimeTypeClassifier

    private val lifecycleOwner = TestLifecycleOwner()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        lifecycleOwner.state = Lifecycle.State.CREATED
    }

    @After
    fun cleanup() {
        lifecycleOwner.state = Lifecycle.State.DESTROYED
        Dispatchers.resetMain()
    }

    @Test
    fun test_nonSendIntentAction_resolvesToTextPreviewUiSynchronously() {
        val targetIntent = Intent(Intent.ACTION_VIEW)
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_TEXT)
        verify(contentResolver, never()).getType(any())
    }

    @Test
    fun test_sendSingleTextFileWithoutPreview_resolvesToFilePreviewUi() {
        val uri = Uri.parse("content://org.pkg.app/notes.txt")
        val targetIntent = Intent(Intent.ACTION_SEND)
            .apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/plain"
            }
        whenever(contentResolver.getType(uri)).thenReturn("text/plain")
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        assertThat(testSubject.uriCount).isEqualTo(1)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri)
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_sendIntentWithoutUris_resolvesToTextPreviewUiSynchronously() {
        val targetIntent = Intent(Intent.ACTION_SEND)
            .apply {
                type = "image/png"
            }
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_TEXT)
        verify(contentResolver, never()).getType(any())
    }

    @Test
    fun test_sendSingleImage_resolvesToImagePreviewUi() {
        val uri = Uri.parse("content://org.pkg.app/image.png")
        val targetIntent = Intent(Intent.ACTION_SEND)
            .apply {
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        whenever(contentResolver.getType(uri)).thenReturn("image/png")
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        assertThat(testSubject.uriCount).isEqualTo(1)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri)
        assertThat(testSubject.firstFileInfo?.previewUri).isEqualTo(uri)
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_sendSingleNonImage_resolvesToFilePreviewUi() {
        val uri = Uri.parse("content://org.pkg.app/paper.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND)
            .apply {
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        whenever(contentResolver.getType(uri)).thenReturn("application/pdf")
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        assertThat(testSubject.uriCount).isEqualTo(1)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri)
        assertThat(testSubject.firstFileInfo?.previewUri).isNull()
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_sendSingleImageWithFailingGetType_resolvesToFilePreviewUi() {
        val uri = Uri.parse("content://org.pkg.app/image.png")
        val targetIntent =
            Intent(Intent.ACTION_SEND)
                .apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
        whenever(contentResolver.getType(uri)).thenThrow(SecurityException("test failure"))
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        assertThat(testSubject.uriCount).isEqualTo(1)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri)
        assertThat(testSubject.firstFileInfo?.previewUri).isNull()
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_sendSingleImageWithFailingMetadata_resolvesToFilePreviewUi() {
        val uri = Uri.parse("content://org.pkg.app/image.png")
        val targetIntent =
            Intent(Intent.ACTION_SEND)
                .apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
        whenever(contentResolver.getStreamTypes(uri, "*/*"))
            .thenThrow(SecurityException("test failure"))
        whenever(contentResolver.query(uri, METADATA_COLUMNS, null, null))
            .thenThrow(SecurityException("test failure"))
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        assertThat(testSubject.uriCount).isEqualTo(1)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri)
        assertThat(testSubject.firstFileInfo?.previewUri).isNull()
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_SingleNonImageUriWithImageTypeInGetStreamTypes_useImagePreviewUi() {
        val uri = Uri.parse("content://org.pkg.app/paper.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND)
            .apply {
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        whenever(contentResolver.getStreamTypes(uri, "*/*"))
            .thenReturn(arrayOf("application/pdf", "image/png"))
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        assertThat(testSubject.uriCount).isEqualTo(1)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri)
        assertThat(testSubject.firstFileInfo?.previewUri).isEqualTo(uri)
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_SingleNonImageUriWithThumbnailFlag_useImagePreviewUi() {
        testMetadataToImagePreview(
            columns = arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            values =
                arrayOf(
                    DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL or
                        DocumentsContract.Document.FLAG_SUPPORTS_METADATA
                )
        )
    }

    @Test
    fun test_SingleNonImageUriWithMetadataIconUri_useImagePreviewUi() {
        testMetadataToImagePreview(
            columns = arrayOf(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI),
            values = arrayOf("content://org.pkg.app/test.pdf?thumbnail"),
        )
    }

    private fun testMetadataToImagePreview(columns: Array<String>, values: Array<Any>) {
        val uri = Uri.parse("content://org.pkg.app/test.pdf")
        val targetIntent = Intent(Intent.ACTION_SEND)
            .apply {
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        whenever(contentResolver.getType(uri)).thenReturn("application/pdf")
        whenever(contentResolver.query(uri, METADATA_COLUMNS, null, null))
            .thenReturn(MatrixCursor(columns).apply { addRow(values) })
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        assertThat(testSubject.uriCount).isEqualTo(1)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri)
        assertThat(testSubject.firstFileInfo?.previewUri).isNotNull()
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_multipleImageUri_useImagePreviewUi() {
        val uri1 = Uri.parse("content://org.pkg.app/test.png")
        val uri2 = Uri.parse("content://org.pkg.app/test.jpg")
        val targetIntent =
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .apply {
                    putExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList<Uri>().apply {
                            add(uri1)
                            add(uri2)
                        }
                    )
                }
        whenever(contentResolver.getType(uri1)).thenReturn("image/png")
        whenever(contentResolver.getType(uri2)).thenReturn("image/jpeg")
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        assertThat(testSubject.uriCount).isEqualTo(2)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri1)
        assertThat(testSubject.firstFileInfo?.previewUri).isEqualTo(uri1)
        // preview type can be determined by the first URI type
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_SomeImageUri_useImagePreviewUi() {
        val uri1 = Uri.parse("content://org.pkg.app/test.png")
        val uri2 = Uri.parse("content://org.pkg.app/test.pdf")
        whenever(contentResolver.getType(uri1)).thenReturn("image/png")
        whenever(contentResolver.getType(uri2)).thenReturn("application/pdf")
        val targetIntent =
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .apply {
                    putExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList<Uri>().apply {
                            add(uri1)
                            add(uri2)
                        }
                    )
                }
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        assertThat(testSubject.uriCount).isEqualTo(2)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri1)
        assertThat(testSubject.firstFileInfo?.previewUri).isEqualTo(uri1)
        // preview type can be determined by the first URI type
        verify(contentResolver, times(1)).getType(any())
    }

    @Test
    fun test_someNonImageUriWithPreview_useImagePreviewUi() {
        val uri1 = Uri.parse("content://org.pkg.app/test.mp4")
        val uri2 = Uri.parse("content://org.pkg.app/test.pdf")
        val targetIntent =
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .apply {
                    putExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList<Uri>().apply {
                            add(uri1)
                            add(uri2)
                        }
                    )
                }
        whenever(contentResolver.getType(uri1)).thenReturn("video/mpeg4")
        whenever(contentResolver.getStreamTypes(uri1, "*/*")).thenReturn(arrayOf("image/png"))
        whenever(contentResolver.getType(uri2)).thenReturn("application/pdf")
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_IMAGE)
        assertThat(testSubject.uriCount).isEqualTo(2)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri1)
        assertThat(testSubject.firstFileInfo?.previewUri).isEqualTo(uri1)
        verify(contentResolver, times(2)).getType(any())
    }

    @Test
    fun test_allNonImageUrisWithoutPreview_useFilePreviewUi() {
        val uri1 = Uri.parse("content://org.pkg.app/test.html")
        val uri2 = Uri.parse("content://org.pkg.app/test.pdf")
        val targetIntent =
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .apply {
                    putExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList<Uri>().apply {
                            add(uri1)
                            add(uri2)
                        }
                    )
                }
        whenever(contentResolver.getType(uri1)).thenReturn("text/html")
        whenever(contentResolver.getType(uri2)).thenReturn("application/pdf")
        val testSubject =
            PreviewDataProvider(targetIntent, contentResolver, mimeTypeClassifier, dispatcher)

        assertThat(testSubject.previewType).isEqualTo(ContentPreviewType.CONTENT_PREVIEW_FILE)
        assertThat(testSubject.uriCount).isEqualTo(2)
        assertThat(testSubject.firstFileInfo?.uri).isEqualTo(uri1)
        assertThat(testSubject.firstFileInfo?.previewUri).isNull()
        verify(contentResolver, times(2)).getType(any())
    }
}
