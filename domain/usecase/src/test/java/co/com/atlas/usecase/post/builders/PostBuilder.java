package co.com.atlas.usecase.post.builders;

import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.PostType;

import java.time.Instant;

/**
 * Builder de test para Post.
 */
public class PostBuilder {

    private Long id = 1L;
    private Long organizationId = 100L;
    private Long authorId = 10L;
    private String title = "Publicación de prueba";
    private String content = "Contenido de la publicación de prueba";
    private PostType type = PostType.ANNOUNCEMENT;
    private Boolean allowComments = true;
    private Boolean isPinned = false;
    private PostStatus status = PostStatus.DRAFT;
    private Instant publishedAt = null;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = null;
    private Instant deletedAt = null;

    public static PostBuilder aPost() {
        return new PostBuilder();
    }

    public PostBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public PostBuilder withOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public PostBuilder withAuthorId(Long authorId) {
        this.authorId = authorId;
        return this;
    }

    public PostBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public PostBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public PostBuilder withType(PostType type) {
        this.type = type;
        return this;
    }

    public PostBuilder withAllowComments(Boolean allowComments) {
        this.allowComments = allowComments;
        return this;
    }

    public PostBuilder withStatus(PostStatus status) {
        this.status = status;
        return this;
    }

    public PostBuilder withPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }

    public Post build() {
        return Post.builder()
                .id(id)
                .organizationId(organizationId)
                .authorId(authorId)
                .title(title)
                .content(content)
                .type(type)
                .allowComments(allowComments)
                .isPinned(isPinned)
                .status(status)
                .publishedAt(publishedAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
