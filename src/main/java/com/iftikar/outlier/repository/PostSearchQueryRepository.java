package com.iftikar.outlier.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Search-specific JPQL queries. The query text is assembled only from fixed
 * fragments; all user-provided values are bound as query parameters.
 */
@Repository
@RequiredArgsConstructor
public class PostSearchQueryRepository {

    private final EntityManager entityManager;

    /** Posts with at least one tag containing any supplied term. */
    public List<PostSearchRow> findPostsByMatchingTags(
            List<String> terms,
            SearchCursor cursor,
            int limit
    ) {
        String tagMatch = tagMatchPredicate("p", terms);

        return executePostQuery(
                "SELECT p.id, p.createdAt FROM Post p WHERE " + tagMatch,
                terms,
                cursor,
                limit
        );
    }

    /**
     * Posts with a matching technology but without any matching tag.
     * This keeps the hashtag results in two non-overlapping lists.
     */
    public List<PostSearchRow> findPostsByMatchingTechnologyOnly(
            List<String> terms,
            SearchCursor cursor,
            int limit
    ) {
        String technologyMatch = technologyMatchPredicate("p", terms);
        String tagMatch = tagMatchPredicate("p", terms);

        return executePostQuery(
                "SELECT p.id, p.createdAt FROM Post p WHERE "
                        + technologyMatch
                        + " AND NOT "
                        + tagMatch,
                terms,
                cursor,
                limit
        );
    }

    /** Posts matching either a tag or a technology. */
    public List<PostSearchRow> findPostsByTagsOrTechnologies(
            List<String> terms,
            SearchCursor cursor,
            int limit
    ) {
        String tagMatch = tagMatchPredicate("p", terms);
        String technologyMatch = technologyMatchPredicate("p", terms);

        return executePostQuery(
                "SELECT p.id, p.createdAt FROM Post p WHERE ("
                        + tagMatch
                        + " OR "
                        + technologyMatch
                        + ")",
                terms,
                cursor,
                limit
        );
    }

    /** Posts belonging to the exact username, ignoring letter case. */
    public List<PostSearchRow> findPostsByUsername(
            String username,
            SearchCursor cursor,
            int limit
    ) {
        StringBuilder jpql = new StringBuilder(
                "SELECT p.id, p.createdAt FROM Post p "
                        + "WHERE LOWER(p.user.username) = :username"
        );

        appendCursorPredicate(jpql, cursor);
        jpql.append(" ORDER BY p.createdAt DESC, p.id DESC");

        TypedQuery<Object[]> query = entityManager.createQuery(
                jpql.toString(),
                Object[].class
        );

        query.setParameter("username", username.toLowerCase(Locale.ROOT));
        setCursorParameters(query, cursor);
        query.setMaxResults(limit);

        return mapRows(query.getResultList());
    }

    private List<PostSearchRow> executePostQuery(
            String baseJpql,
            List<String> terms,
            SearchCursor cursor,
            int limit
    ) {
        StringBuilder jpql = new StringBuilder(baseJpql);

        appendCursorPredicate(jpql, cursor);
        jpql.append(" ORDER BY p.createdAt DESC, p.id DESC");

        TypedQuery<Object[]> query = entityManager.createQuery(
                jpql.toString(),
                Object[].class
        );

        setTermParameters(query, terms);
        setCursorParameters(query, cursor);
        query.setMaxResults(limit);

        return mapRows(query.getResultList());
    }

    /**
     * EXISTS query for matching tags. The outer post alias is always "p".
     */
    private String tagMatchPredicate(
            String postAlias,
            List<String> terms
    ) {
        return "EXISTS ("
                + "SELECT pTag.id FROM Post pTag JOIN pTag.tags tag "
                + "WHERE pTag.id = " + postAlias + ".id "
                + "AND (" + containsAnyPredicate("tag", terms) + ")"
                + ")";
    }

    /**
     * EXISTS query for matching technology names. PostTechnology is the
     * relationship entity between Post and Technology in this project.
     */
    private String technologyMatchPredicate(
            String postAlias,
            List<String> terms
    ) {
        return "EXISTS ("
                + "SELECT pt.post.id FROM PostTechnology pt "
                + "WHERE pt.post.id = " + postAlias + ".id "
                + "AND ("
                + containsAnyPredicate("pt.technology.name", terms)
                + ")"
                + ")";
    }

    /** Creates case-insensitive substring comparisons for all search terms. */
    private String containsAnyPredicate(
            String fieldExpression,
            List<String> terms
    ) {
        List<String> conditions = new ArrayList<>(terms.size());

        for (int index = 0; index < terms.size(); index++) {
            conditions.add(
                    "LOWER(" + fieldExpression + ") LIKE :term" + index
                            + " ESCAPE '!'"
            );
        }

        return String.join(" OR ", conditions);
    }

    private void appendCursorPredicate(
            StringBuilder jpql,
            SearchCursor cursor
    ) {
        if (cursor == null) {
            return;
        }

        jpql.append(
                " AND (p.createdAt < :cursorCreatedAt "
                        + "OR (p.createdAt = :cursorCreatedAt "
                        + "AND p.id < :cursorPostId))"
        );
    }

    private void setTermParameters(
            TypedQuery<Object[]> query,
            List<String> terms
    ) {
        for (int index = 0; index < terms.size(); index++) {
            String escapedTerm = escapeLikeLiteral(
                    terms.get(index).toLowerCase(Locale.ROOT)
            );

            query.setParameter(
                    "term" + index,
                    "%" + escapedTerm + "%"
            );
        }
    }

    private void setCursorParameters(
            TypedQuery<Object[]> query,
            SearchCursor cursor
    ) {
        if (cursor == null) {
            return;
        }

        query.setParameter("cursorCreatedAt", cursor.createdAt());
        query.setParameter("cursorPostId", cursor.postId());
    }

    /**
     * Escape LIKE metacharacters so user input is treated as literal text.
     * '!' is the ESCAPE character in the JPQL predicates above.
     */
    private String escapeLikeLiteral(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private List<PostSearchRow> mapRows(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new PostSearchRow(
                        (String) row[0],
                        (Instant) row[1]
                ))
                .toList();
    }

    public record PostSearchRow(
            String postId,
            Instant createdAt
    ) {}

    public record SearchCursor(
            Instant createdAt,
            String postId
    ) {}
}
