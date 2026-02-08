package co.com.atlas.r2dbc.post;

import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.PostType;
import co.com.atlas.model.post.gateways.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class PostRepositoryAdapter implements PostRepository {

    private final PostReactiveRepository repository;

    @Override
    public Mono<Post> save(Post post) {
        return repository.save(toEntity(post))
                .map(this::toDomain);
    }

    @Override
    public Mono<Post> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Flux<Post> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Post> findPublishedByOrganizationId(Long organizationId) {
        return repository.findPublishedByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Post> findPinnedByOrganizationId(Long organizationId) {
        return repository.findPinnedByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }

    private Post toDomain(PostEntity entity) {
        return Post.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .authorId(entity.getAuthorId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType() != null ? PostType.valueOf(entity.getType()) : null)
                .allowComments(entity.getAllowComments())
                .isPinned(entity.getIsPinned())
                .status(entity.getStatus() != null ? PostStatus.valueOf(entity.getStatus()) : null)
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private PostEntity toEntity(Post post) {
        return PostEntity.builder()
                .id(post.getId())
                .organizationId(post.getOrganizationId())
                .authorId(post.getAuthorId())
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType() != null ? post.getType().name() : null)
                .allowComments(post.getAllowComments())
                .isPinned(post.getIsPinned())
                .status(post.getStatus() != null ? post.getStatus().name() : null)
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .deletedAt(post.getDeletedAt())
                .build();
    }
}
