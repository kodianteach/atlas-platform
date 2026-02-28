package co.com.atlas.model.post.gateways;

import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.common.PostPollFilter;
import co.com.atlas.model.post.Post;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Gateway para repositorio de Posts.
 */
public interface PostRepository {
    
    Mono<Post> save(Post post);
    
    Mono<Post> findById(Long id);
    
    Flux<Post> findByOrganizationId(Long organizationId);
    
    Flux<Post> findPublishedByOrganizationId(Long organizationId);
    
    Flux<Post> findPinnedByOrganizationId(Long organizationId);
    
    Mono<Void> deleteById(Long id);

    Mono<PageResponse<Post>> findByFilters(Long organizationId, PostPollFilter filter);

    Mono<Map<String, Long>> countByStatusAndOrganization(Long organizationId);
}
