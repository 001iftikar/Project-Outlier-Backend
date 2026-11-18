package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.MatchedTechStackInsteadResponse;
import com.iftikar.outlier.dto.PostFeedItemResponse;
import com.iftikar.outlier.dto.PostSearchResponse;
import com.iftikar.outlier.repository.PostSearchQueryRepository;
import com.iftikar.outlier.repository.PostSearchQueryRepository.PostSearchRow;
import com.iftikar.outlier.repository.PostSearchQueryRepository.SearchCursor;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostSearchServiceImpl implements PostSearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_QUERY_LENGTH = 120;
    private static final int MAX_SEARCH_TERMS = 8;
    private static final int MAX_TERM_LENGTH = 100;

    private final PostSearchQueryRepository postSearchQueryRepository;
    private final PostResponseAssembler postResponseAssembler;

    @Override
    @Transactional(readOnly = true)
    public PostSearchResponse searchPosts(
            String currentUserId,
            String rawQuery,
            String cursor,
            String matchedTechStackInsteadCursor,
            int size
    ) {
        validatePageSize(size);

        ParsedSearch parsedSearch = parseQuery(rawQuery);

        SearchCursor decodedCursor = decodeCursor(cursor);
        SearchCursor decodedTechStackCursor = decodeCursor(
                matchedTechStackInsteadCursor
        );

        return switch (parsedSearch.mode()) {
            case USERNAME -> searchByUsername(
                    currentUserId,
                    parsedSearch,
                    decodedCursor,
                    size
            );
            case TAGS -> searchByTags(
                    currentUserId,
                    parsedSearch,
                    decodedCursor,
                    decodedTechStackCursor,
                    size
            );
            case OMNI -> searchOmni(
                    currentUserId,
                    parsedSearch,
                    decodedCursor,
                    size
            );
        };
    }

    private PostSearchResponse searchByUsername(
            String currentUserId,
            ParsedSearch parsedSearch,
            SearchCursor cursor,
            int size
    ) {
        List<PostSearchRow> fetchedRows =
                postSearchQueryRepository.findPostsByUsername(
                        parsedSearch.username(),
                        cursor,
                        size + 1
                );

        SearchPage<PostSearchRow> page = createPage(fetchedRows, size);
        List<String> postIds = page.items().stream()
                .map(PostSearchRow::postId)
                .toList();

        List<PostFeedItemResponse> posts =
                postResponseAssembler.assemble(currentUserId, postIds);

        return new PostSearchResponse(
                SearchMode.USERNAME.name(),
                List.of(parsedSearch.username()),
                posts,
                List.of(),
                nextCursor(page.items(), page.hasMore()),
                page.hasMore(),
                null,
                false,
                false
        );
    }

    private PostSearchResponse searchByTags(
            String currentUserId,
            ParsedSearch parsedSearch,
            SearchCursor tagCursor,
            SearchCursor techStackCursor,
            int size
    ) {
        // The primary list contains posts whose tags match any search term.
        List<PostSearchRow> fetchedTagRows =
                postSearchQueryRepository.findPostsByMatchingTags(
                        parsedSearch.terms(),
                        tagCursor,
                        size + 1
                );

        SearchPage<PostSearchRow> tagPage = createPage(
                fetchedTagRows,
                size
        );

        List<String> taggedPostIds = tagPage.items().stream()
                .map(PostSearchRow::postId)
                .toList();

        List<PostFeedItemResponse> posts =
                postResponseAssembler.assemble(
                        currentUserId,
                        taggedPostIds
                );

        // The secondary list contains technology matches that did not also
        // match any searched tag. Its cursor is independent of the tag list.
        List<PostSearchRow> fetchedTechRows =
                postSearchQueryRepository.findPostsByMatchingTechnologyOnly(
                        parsedSearch.terms(),
                        techStackCursor,
                        size + 1
                );

        SearchPage<PostSearchRow> techPage = createPage(
                fetchedTechRows,
                size
        );

        List<String> techPostIds = techPage.items().stream()
                .map(PostSearchRow::postId)
                .toList();

        List<PostFeedItemResponse> techPosts =
                postResponseAssembler.assemble(
                        currentUserId,
                        techPostIds
                );

        List<MatchedTechStackInsteadResponse> matchedTechStackInstead =
                techPosts.stream()
                        .map(post -> new MatchedTechStackInsteadResponse(
                                post,
                                findMatchedTechnologies(
                                        post.techStack(),
                                        parsedSearch.terms()
                                )
                        ))
                        .toList();

        boolean usedTechStackFallback = tagCursor == null
                && posts.isEmpty()
                && !matchedTechStackInstead.isEmpty();

        return new PostSearchResponse(
                SearchMode.TAGS.name(),
                parsedSearch.terms(),
                posts,
                matchedTechStackInstead,
                nextCursor(tagPage.items(), tagPage.hasMore()),
                tagPage.hasMore(),
                nextCursor(techPage.items(), techPage.hasMore()),
                techPage.hasMore(),
                usedTechStackFallback
        );
    }

    private PostSearchResponse searchOmni(
            String currentUserId,
            ParsedSearch parsedSearch,
            SearchCursor cursor,
            int size
    ) {
        List<PostSearchRow> fetchedRows =
                postSearchQueryRepository.findPostsByTagsOrTechnologies(
                        parsedSearch.terms(),
                        cursor,
                        size + 1
                );

        SearchPage<PostSearchRow> page = createPage(fetchedRows, size);
        List<String> postIds = page.items().stream()
                .map(PostSearchRow::postId)
                .toList();

        List<PostFeedItemResponse> posts =
                postResponseAssembler.assemble(currentUserId, postIds);

        return new PostSearchResponse(
                SearchMode.OMNI.name(),
                parsedSearch.terms(),
                posts,
                List.of(),
                nextCursor(page.items(), page.hasMore()),
                page.hasMore(),
                null,
                false,
                false
        );
    }

    private List<String> findMatchedTechnologies(
            List<String> postTechnologies,
            List<String> terms
    ) {
        if (postTechnologies == null || postTechnologies.isEmpty()) {
            return List.of();
        }

        return postTechnologies.stream()
                .filter(technology -> {
                    String normalizedTechnology = technology.toLowerCase(
                            Locale.ROOT
                    );

                    return terms.stream().anyMatch(
                            normalizedTechnology::contains
                    );
                })
                .toList();
    }

    private <T> SearchPage<T> createPage(
            List<T> fetchedRows,
            int size
    ) {
        boolean hasMore = fetchedRows.size() > size;
        List<T> pageItems = hasMore
                ? fetchedRows.subList(0, size)
                : fetchedRows;

        return new SearchPage<>(pageItems, hasMore);
    }

    private String nextCursor(
            List<PostSearchRow> pageRows,
            boolean hasMore
    ) {
        if (!hasMore || pageRows.isEmpty()) {
            return null;
        }

        PostSearchRow lastRow = pageRows.get(pageRows.size() - 1);
        String rawCursor = lastRow.createdAt() + "|" + lastRow.postId();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    private SearchCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        if (cursor.length() > 512) {
            throw invalidCursor();
        }

        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            int separator = decoded.indexOf('|');

            if (separator <= 0 || separator == decoded.length() - 1) {
                throw invalidCursor();
            }

            Instant createdAt = Instant.parse(decoded.substring(0, separator));
            String postId = decoded.substring(separator + 1);

            if (postId.isBlank() || postId.length() > 64) {
                throw invalidCursor();
            }

            return new SearchCursor(createdAt, postId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private ParsedSearch parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw invalidQuery(
                    "Search query cannot be empty."
            );
        }

        String query = rawQuery.strip();

        if (query.length() > MAX_QUERY_LENGTH) {
            throw invalidQuery(
                    "Search query cannot exceed 120 characters."
            );
        }

        // @username searches that exact username, not tags or technologies.
        if (query.startsWith("@")) {
            String username = query.substring(1).strip();

            if (username.isEmpty()
                    || username.length() > 64
                    || username.chars().anyMatch(Character::isWhitespace)) {
                throw invalidQuery(
                        "Enter one username after @, without spaces."
                );
            }

            return new ParsedSearch(
                    SearchMode.USERNAME,
                    List.of(username.toLowerCase(Locale.ROOT)),
                    username.toLowerCase(Locale.ROOT)
            );
        }

        boolean tagsOnly = query.startsWith("#");
        String termsSource = tagsOnly
                ? query.substring(1).strip()
                : query;

        Set<String> uniqueTerms = new LinkedHashSet<>();

        for (String rawTerm : termsSource.split("\\s+")) {
            String term = rawTerm.strip();

            // Also supports input such as "#android #room".
            while (term.startsWith("#")) {
                term = term.substring(1);
            }

            if (term.isBlank()) {
                continue;
            }

            if (term.length() > MAX_TERM_LENGTH) {
                throw invalidQuery(
                        "Each search term cannot exceed 100 characters."
                );
            }

            uniqueTerms.add(term.toLowerCase(Locale.ROOT));

            if (uniqueTerms.size() > MAX_SEARCH_TERMS) {
                throw invalidQuery(
                        "Search can contain at most 8 terms."
                );
            }
        }

        if (uniqueTerms.isEmpty()) {
            throw invalidQuery(
                    "Enter at least one search term."
            );
        }

        return new ParsedSearch(
                tagsOnly ? SearchMode.TAGS : SearchMode.OMNI,
                new ArrayList<>(uniqueTerms),
                null
        );
    }

    private void validatePageSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    "INVALID_PAGE_SIZE",
                    "Page size must be between 1 and 50.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private ApiException invalidQuery(String message) {
        return new ApiException(
                "INVALID_SEARCH_QUERY",
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    private ApiException invalidCursor() {
        return new ApiException(
                "INVALID_SEARCH_CURSOR",
                "The supplied search cursor is invalid.",
                HttpStatus.BAD_REQUEST
        );
    }

    private enum SearchMode {
        USERNAME,
        TAGS,
        OMNI
    }

    private record ParsedSearch(
            SearchMode mode,
            List<String> terms,
            String username
    ) {}

    private record SearchPage<T>(
            List<T> items,
            boolean hasMore
    ) {}
}
