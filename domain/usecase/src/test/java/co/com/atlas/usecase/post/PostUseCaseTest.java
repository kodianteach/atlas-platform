package co.com.atlas.usecase.post;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.gateways.PostRepository;
import co.com.atlas.usecase.post.builders.PostBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostUseCaseTest {

    @Mock private PostRepository postRepository;

    private PostUseCase postUseCase;

    private static final Long POST_ID = 1L;
    private static final Long ORG_ID = 100L;

    @BeforeEach
    void setUp() {
        postUseCase = new PostUseCase(postRepository);
    }

    @Test
    void shouldCreatePostSuccessfully() {
        Post input = PostBuilder.aPost().withId(null).build();
        Post saved = PostBuilder.aPost().withId(POST_ID).withStatus(PostStatus.DRAFT).build();

        when(postRepository.save(any(Post.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(postUseCase.create(input))
                .assertNext(result -> {
                    assertThat(result.getStatus()).isEqualTo(PostStatus.DRAFT);
                    assertThat(result.getId()).isEqualTo(POST_ID);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectPostWithTitleExceeding150Chars() {
        String longTitle = "A".repeat(151);
        Post input = PostBuilder.aPost().withTitle(longTitle).build();

        StepVerifier.create(postUseCase.create(input))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("150"))
                .verify();
    }

    @Test
    void shouldRejectPostWithContentExceeding5000Chars() {
        String longContent = "A".repeat(5001);
        Post input = PostBuilder.aPost().withContent(longContent).build();

        StepVerifier.create(postUseCase.create(input))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("5000"))
                .verify();
    }

    @Test
    void shouldRejectPostWithEmptyTitle() {
        Post input = PostBuilder.aPost().withTitle("").build();

        StepVerifier.create(postUseCase.create(input))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("requerido"))
                .verify();
    }

    @Test
    void shouldPublishPostSuccessfully() {
        Post draftPost = PostBuilder.aPost()
                .withId(POST_ID)
                .withOrganizationId(ORG_ID)
                .withStatus(PostStatus.DRAFT)
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(draftPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(postUseCase.publish(POST_ID, ORG_ID))
                .assertNext(result -> {
                    assertThat(result.getStatus()).isEqualTo(PostStatus.PUBLISHED);
                    assertThat(result.getPublishedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectPublishFromDifferentOrganization() {
        Post post = PostBuilder.aPost()
                .withId(POST_ID)
                .withOrganizationId(ORG_ID)
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(post));

        Long differentOrgId = 999L;
        StepVerifier.create(postUseCase.publish(POST_ID, differentOrgId))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("No tiene acceso"))
                .verify();
    }

    @Test
    void shouldArchivePostSuccessfully() {
        Post publishedPost = PostBuilder.aPost()
                .withId(POST_ID)
                .withStatus(PostStatus.PUBLISHED)
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(publishedPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(postUseCase.archive(POST_ID))
                .assertNext(result -> assertThat(result.getStatus()).isEqualTo(PostStatus.ARCHIVED))
                .verifyComplete();
    }

    @Test
    void shouldSoftDeletePost() {
        Post post = PostBuilder.aPost()
                .withId(POST_ID)
                .withOrganizationId(ORG_ID)
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(postUseCase.delete(POST_ID, ORG_ID))
                .verifyComplete();
    }
}
