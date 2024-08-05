/*
 * Copyright 2020 The Android Open Source Project
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

package android.app.appsearch;

import android.annotation.NonNull;
import android.annotation.SuppressLint;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

/**
 * Provides a connection to a single AppSearch database.
 *
 * <p>An {@link AppSearchSessionShim} instance provides access to database operations such as
 * setting a schema, adding documents, and searching.
 *
 * <p>Instances of this interface are usually obtained from a storage implementation, e.g. {@code
 * AppSearchManager.createSearchSessionAsync()} or {@code
 * PlatformStorage.createSearchSessionAsync()}.
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @see GlobalSearchSessionShim
 */
public interface AppSearchSessionShim extends Closeable {

    /**
     * Sets the schema that represents the organizational structure of data within the AppSearch
     * database.
     *
     * <p>Upon creating an {@link AppSearchSessionShim}, {@link #setSchema} should be called. If the
     * schema needs to be updated, or it has not been previously set, then the provided schema will
     * be saved and persisted to disk. Otherwise, {@link #setSchema} is handled efficiently as a
     * no-op call.
     *
     * @param request the schema to set or update the AppSearch database to.
     * @return a {@link ListenableFuture} which resolves to a {@link SetSchemaResponse} object.
     */
    @NonNull
    ListenableFuture<SetSchemaResponse> setSchemaAsync(@NonNull SetSchemaRequest request);

    /**
     * Retrieves the schema most recently successfully provided to {@link #setSchema}.
     *
     * @return The pending {@link GetSchemaResponse} of performing this operation.
     */
    // This call hits disk; async API prevents us from treating these calls as properties.
    @SuppressLint("KotlinPropertyAccess")
    @NonNull
    ListenableFuture<GetSchemaResponse> getSchemaAsync();

    /**
     * Retrieves the set of all namespaces in the current database with at least one document.
     *
     * @return The pending result of performing this operation.
     */
    @NonNull
    ListenableFuture<Set<String>> getNamespacesAsync();

    /**
     * Indexes documents into the {@link AppSearchSessionShim} database.
     *
     * <p>Each {@link GenericDocument} object must have a {@code schemaType} field set to an {@link
     * AppSearchSchema} type that has been previously registered by calling the {@link #setSchema}
     * method.
     *
     * @param request containing documents to be indexed.
     * @return a {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}. The
     *     keys of the returned {@link AppSearchBatchResult} are the IDs of the input documents. The
     *     values are either {@code null} if the corresponding document was successfully indexed, or
     *     a failed {@link AppSearchResult} otherwise.
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, Void>> putAsync(
            @NonNull PutDocumentsRequest request);

    /**
     * Gets {@link GenericDocument} objects by document IDs in a namespace from the {@link
     * AppSearchSessionShim} database.
     *
     * @param request a request containing a namespace and IDs to get documents for.
     * @return A {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}. The
     *     keys of the {@link AppSearchBatchResult} represent the input document IDs from the {@link
     *     GetByDocumentIdRequest} object. The values are either the corresponding {@link
     *     GenericDocument} object for the ID on success, or an {@link AppSearchResult} object on
     *     failure. For example, if an ID is not found, the value for that ID will be set to an
     *     {@link AppSearchResult} object with result code: {@link
     *     AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByDocumentIdAsync(
            @NonNull GetByDocumentIdRequest request);

    /**
     * Retrieves documents from the open {@link AppSearchSessionShim} that match a given query
     * string and type of search provided.
     *
     * <p>Query strings can be empty, contain one term with no operators, or contain multiple terms
     * and operators.
     *
     * <p>For query strings that are empty, all documents that match the {@link SearchSpec} will be
     * returned.
     *
     * <p>For query strings with a single term and no operators, documents that match the provided
     * query string and {@link SearchSpec} will be returned.
     *
     * <p>The following operators are supported:
     *
     * <ul>
     *   <li>AND (implicit)
     *       <p>AND is an operator that matches documents that contain <i>all</i> provided terms.
     *       <p><b>NOTE:</b> A space between terms is treated as an "AND" operator. Explicitly
     *       including "AND" in a query string will treat "AND" as a term, returning documents that
     *       also contain "AND".
     *       <p>Example: "apple AND banana" matches documents that contain the terms "apple", "and",
     *       "banana".
     *       <p>Example: "apple banana" matches documents that contain both "apple" and "banana".
     *       <p>Example: "apple banana cherry" matches documents that contain "apple", "banana", and
     *       "cherry".
     *   <li>OR
     *       <p>OR is an operator that matches documents that contain <i>any</i> provided term.
     *       <p>Example: "apple OR banana" matches documents that contain either "apple" or
     *       "banana".
     *       <p>Example: "apple OR banana OR cherry" matches documents that contain any of "apple",
     *       "banana", or "cherry".
     *   <li>Exclusion (-)
     *       <p>Exclusion (-) is an operator that matches documents that <i>do not</i> contain the
     *       provided term.
     *       <p>Example: "-apple" matches documents that do not contain "apple".
     *   <li>Grouped Terms
     *       <p>For queries that require multiple operators and terms, terms can be grouped into
     *       subqueries. Subqueries are contained within an open "(" and close ")" parenthesis.
     *       <p>Example: "(donut OR bagel) (coffee OR tea)" matches documents that contain either
     *       "donut" or "bagel" and either "coffee" or "tea".
     *   <li>Property Restricts
     *       <p>For queries that require a term to match a specific {@link AppSearchSchema} property
     *       of a document, a ":" must be included between the property name and the term.
     *       <p>Example: "subject:important" matches documents that contain the term "important" in
     *       the "subject" property.
     * </ul>
     *
     * <p>The above description covers the query operators that are supported on all versions of
     * AppSearch. Additional operators and their required features are described below.
     *
     * <p>{@link Features#LIST_FILTER_QUERY_LANGUAGE}: This feature covers the expansion of the
     * query language to conform to the definition of the list filters language (https://aip
     * .dev/160). This includes:
     *
     * <ul>
     *   <li>addition of explicit 'AND' and 'NOT' operators
     *   <li>property restricts are allowed with groupings (ex. "prop:(a OR b)")
     *   <li>addition of custom functions to control matching
     * </ul>
     *
     * <p>The newly added custom functions covered by this feature are:
     *
     * <ul>
     *   <li>createList(String...)
     *   <li>search(String, List<String>)
     *   <li>propertyDefined(String)
     * </ul>
     *
     * <p>createList takes a variable number of strings and returns a list of strings. It is for use
     * with search.
     *
     * <p>search takes a query string that will be parsed according to the supported query language
     * and an optional list of strings that specify the properties to be restricted to. This exists
     * as a convenience for multiple property restricts. So, for example, the query `(subject:foo OR
     * body:foo) (subject:bar OR body:bar)` could be rewritten as `search("foo bar",
     * createList("subject", "bar"))`.
     *
     * <p>propertyDefined takes a string specifying the property of interest and matches all
     * documents of any type that defines the specified property (ex.
     * `propertyDefined("sender.name")`). Note that propertyDefined will match so long as the
     * document's type defines the specified property. It does NOT require that the document
     * actually hold any values for this property.
     *
     * <p>{@link Features#NUMERIC_SEARCH}: This feature covers numeric search expressions. In the
     * query language, the values of properties that have {@link
     * AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE} set can be matched with a numeric
     * search expression (the property, a supported comparator and an integer value). Supported
     * comparators are <, <=, ==, >= and >.
     *
     * <p>Ex. `price < 10` will match all documents that has a numeric value in its price property
     * that is less than 10.
     *
     * <p>{@link Features#VERBATIM_SEARCH}: This feature covers the verbatim string operator
     * (quotation marks).
     *
     * <p>Ex. `"foo/bar" OR baz` will ensure that 'foo/bar' is treated as a single 'verbatim' token.
     *
     * <p>The availability of each of these features can be checked by calling {@link
     * Features#isFeatureSupported} with the desired feature.
     *
     * <p>Additional search specifications, such as filtering by {@link AppSearchSchema} type or
     * adding projection, can be set by calling the corresponding {@link SearchSpec.Builder} setter.
     *
     * <p>This method is lightweight. The heavy work will be done in {@link
     * SearchResultsShim#getNextPage}.
     *
     * @param queryExpression query string to search.
     * @param searchSpec spec for setting document filters, adding projection, setting term match
     *     type, etc.
     * @return a {@link SearchResultsShim} object for retrieved matched documents.
     */
    @NonNull
    SearchResultsShim search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec);

    /**
     * Retrieves suggested Strings that could be used as {@code queryExpression} in {@link
     * #search(String, SearchSpec)} API.
     *
     * <p>The {@code suggestionQueryExpression} can contain one term with no operators, or contain
     * multiple terms and operators. Operators will be considered as a normal term. Please see the
     * operator examples below. The {@code suggestionQueryExpression} must end with a valid term,
     * the suggestions are generated based on the last term. If the input {@code
     * suggestionQueryExpression} doesn't have a valid token, AppSearch will return an empty result
     * list. Please see the invalid examples below.
     *
     * <p>Example: if there are following documents with content stored in AppSearch.
     *
     * <ul>
     *   <li>document1: "term1"
     *   <li>document2: "term1 term2"
     *   <li>document3: "term1 term2 term3"
     *   <li>document4: "org"
     * </ul>
     *
     * <p>Search suggestions with the single term {@code suggestionQueryExpression} "t", the
     * suggested results are:
     *
     * <ul>
     *   <li>"term1" - Use it to be queryExpression in {@link #search} could get 3 {@link
     *       SearchResult}s, which contains document 1, 2 and 3.
     *   <li>"term2" - Use it to be queryExpression in {@link #search} could get 2 {@link
     *       SearchResult}s, which contains document 2 and 3.
     *   <li>"term3" - Use it to be queryExpression in {@link #search} could get 1 {@link
     *       SearchResult}, which contains document 3.
     * </ul>
     *
     * <p>Search suggestions with the multiple term {@code suggestionQueryExpression} "org t", the
     * suggested result will be "org term1" - The last token is completed by the suggested String.
     *
     * <p>Operators in {@link #search} are supported.
     *
     * <p><b>NOTE:</b> Exclusion and Grouped Terms in the last term is not supported.
     *
     * <p>example: "apple -f": This Api will throw an {@link
     * android.app.appsearch.exceptions.AppSearchException} with {@link
     * AppSearchResult#RESULT_INVALID_ARGUMENT}.
     *
     * <p>example: "apple (f)": This Api will return an empty results.
     *
     * <p>Invalid example: All these input {@code suggestionQueryExpression} don't have a valid last
     * token, AppSearch will return an empty result list.
     *
     * <ul>
     *   <li>"" - Empty {@code suggestionQueryExpression}.
     *   <li>"(f)" - Ending in a closed brackets.
     *   <li>"f:" - Ending in an operator.
     *   <li>"f " - Ending in trailing space.
     * </ul>
     *
     * @param suggestionQueryExpression the non empty query string to search suggestions
     * @param searchSuggestionSpec spec for setting document filters
     * @return The pending result of performing this operation which resolves to a List of {@link
     *     SearchSuggestionResult} on success. The returned suggestion Strings are ordered by the
     *     number of {@link SearchResult} you could get by using that suggestion in {@link #search}.
     * @see #search(String, SearchSpec)
     */
    @NonNull
    ListenableFuture<List<SearchSuggestionResult>> searchSuggestionAsync(
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec);

    /**
     * Reports usage of a particular document by namespace and ID.
     *
     * <p>A usage report represents an event in which a user interacted with or viewed a document.
     *
     * <p>For each call to {@link #reportUsage}, AppSearch updates usage count and usage recency *
     * metrics for that particular document. These metrics are used for ordering {@link #search}
     * results by the {@link SearchSpec#RANKING_STRATEGY_USAGE_COUNT} and {@link
     * SearchSpec#RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP} ranking strategies.
     *
     * <p>Reporting usage of a document is optional.
     *
     * @param request The usage reporting request.
     * @return The pending result of performing this operation which resolves to {@code null} on
     *     success.
     */
    @NonNull
    ListenableFuture<Void> reportUsageAsync(@NonNull ReportUsageRequest request);

    /**
     * Removes {@link GenericDocument} objects by document IDs in a namespace from the {@link
     * AppSearchSessionShim} database.
     *
     * <p>Removed documents will no longer be surfaced by {@link #search} or {@link
     * #getByDocumentId} calls.
     *
     * <p>Once the database crosses the document count or byte usage threshold, removed documents
     * will be deleted from disk.
     *
     * @param request {@link RemoveByDocumentIdRequest} with IDs in a namespace to remove from the
     *     index.
     * @return a {@link ListenableFuture} which resolves to an {@link AppSearchBatchResult}. The
     *     keys of the {@link AppSearchBatchResult} represent the input IDs from the {@link
     *     RemoveByDocumentIdRequest} object. The values are either {@code null} on success, or a
     *     failed {@link AppSearchResult} otherwise. IDs that are not found will return a failed
     *     {@link AppSearchResult} with a result code of {@link AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    ListenableFuture<AppSearchBatchResult<String, Void>> removeAsync(
            @NonNull RemoveByDocumentIdRequest request);

    /**
     * Removes {@link GenericDocument}s from the index by Query. Documents will be removed if they
     * match the {@code queryExpression} in given namespaces and schemaTypes which is set via {@link
     * SearchSpec.Builder#addFilterNamespaces} and {@link SearchSpec.Builder#addFilterSchemas}.
     *
     * <p>An empty {@code queryExpression} matches all documents.
     *
     * <p>An empty set of namespaces or schemaTypes matches all namespaces or schemaTypes in the
     * current database.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec Spec containing schemaTypes, namespaces and query expression indicates how
     *     document will be removed. All specific about how to scoring, ordering, snippeting and
     *     resulting will be ignored.
     * @return The pending result of performing this operation.
     * @throws IllegalArgumentException if the {@link SearchSpec} contains a {@link JoinSpec}.
     *     {@link JoinSpec} lets you join docs that are not owned by the caller, so the semantics of
     *     failures from this method would be complex.
     */
    @NonNull
    ListenableFuture<Void> removeAsync(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec);

    /**
     * Gets the storage info for this {@link AppSearchSessionShim} database.
     *
     * <p>This may take time proportional to the number of documents and may be inefficient to call
     * repeatedly.
     *
     * @return a {@link ListenableFuture} which resolves to a {@link StorageInfo} object.
     */
    @NonNull
    ListenableFuture<StorageInfo> getStorageInfoAsync();

    /**
     * Flush all schema and document updates, additions, and deletes to disk if possible.
     *
     * <p>The request is not guaranteed to be handled and may be ignored by some implementations of
     * AppSearchSessionShim.
     *
     * @return The pending result of performing this operation. {@link
     *     android.app.appsearch.exceptions.AppSearchException} with {@link
     *     AppSearchResult#RESULT_INTERNAL_ERROR} will be set to the future if we hit error when
     *     save to disk.
     */
    @NonNull
    ListenableFuture<Void> requestFlushAsync();

    /**
     * Returns the {@link Features} to check for the availability of certain features for this
     * session.
     */
    @NonNull
    Features getFeatures();

    /**
     * Closes the {@link AppSearchSessionShim} to persist all schema and document updates,
     * additions, and deletes to disk.
     */
    @Override
    void close();
}
