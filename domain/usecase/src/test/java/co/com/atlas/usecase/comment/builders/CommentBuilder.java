package co.com.atlas.usecase.comment.builders;

import co.com.atlas.model.comment.Comment;

import java.time.Instant;

/**
 * Builder de test para Comment.
 */
public class CommentBuilder {

    private Long id = 1L;
    private Long postId = 1L;
    private Long authorId = 10L;
    private Long parentId = null;
    private String content = "Comentario de prueba";
    private Boolean isApproved = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = null;
    private Instant deletedAt = null;

    public static CommentBuilder aComment() {
        return new CommentBuilder();
    }

    public CommentBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public CommentBuilder withPostId(Long postId) {
        this.postId = postId;
        return this;
    }

    public CommentBuilder withAuthorId(Long authorId) {
        this.authorId = authorId;
        return this;
    }

    public CommentBuilder withParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public CommentBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public CommentBuilder withIsApproved(Boolean isApproved) {
        this.isApproved = isApproved;
        return this;
    }

    public Comment build() {
        return Comment.builder()
                .id(id)
                .postId(postId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content)
                .isApproved(isApproved)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
