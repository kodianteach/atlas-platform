package co.com.atlas.usecase.comment;

import co.com.atlas.model.comment.Comment;
import co.com.atlas.model.comment.gateways.CommentRepository;
import co.com.atlas.model.comment.gateways.ContentModerationGateway;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.gateways.PostRepository;
import co.com.atlas.usecase.comment.builders.CommentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentUseCaseTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private ContentModerationGateway contentModerationGateway;

    private CommentUseCase commentUseCase;

    private static final Long POST_ID = 1L;
    private static final Long COMMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        commentUseCase = new CommentUseCase(commentRepository, postRepository, contentModerationGateway);
    }

    @Test
    void shouldCreateCommentWhenContentIsAppropriate() {
        // Arrange
        Post publishedPost = Post.builder()
                .id(POST_ID)
                .status(PostStatus.PUBLISHED)
                .allowComments(true)
                .build();

        Comment input = CommentBuilder.aComment()
                .withId(null)
                .withPostId(POST_ID)
                .withContent("Buen anuncio, gracias por la información")
                .build();

        Comment saved = CommentBuilder.aComment()
                .withId(COMMENT_ID)
                .withPostId(POST_ID)
                .withContent("Buen anuncio, gracias por la información")
                .withIsApproved(true)
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(publishedPost));
        when(contentModerationGateway.isContentAppropriate(anyString())).thenReturn(Mono.just(true));
        when(commentRepository.save(any(Comment.class))).thenReturn(Mono.just(saved));

        // Act & Assert
        StepVerifier.create(commentUseCase.create(input))
                .assertNext(result -> {
                    assertThat(result.getIsApproved()).isTrue();
                    assertThat(result.getContent()).contains("Buen anuncio");
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectCommentWithInappropriateContent() {
        // Arrange
        Post publishedPost = Post.builder()
                .id(POST_ID)
                .status(PostStatus.PUBLISHED)
                .allowComments(true)
                .build();

        Comment input = CommentBuilder.aComment()
                .withId(null)
                .withPostId(POST_ID)
                .withContent("Esto es una mierda total")
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(publishedPost));
        when(contentModerationGateway.isContentAppropriate(anyString())).thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(commentUseCase.create(input))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("no cumple las reglas"))
                .verify();
    }

    @Test
    void shouldRejectCommentOnUnpublishedPost() {
        Post draftPost = Post.builder()
                .id(POST_ID)
                .status(PostStatus.DRAFT)
                .allowComments(true)
                .build();

        Comment input = CommentBuilder.aComment().withPostId(POST_ID).build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(draftPost));

        StepVerifier.create(commentUseCase.create(input))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("no publicada"))
                .verify();
    }

    @Test
    void shouldRejectCommentWhenCommentsDisabled() {
        Post noCommentsPost = Post.builder()
                .id(POST_ID)
                .status(PostStatus.PUBLISHED)
                .allowComments(false)
                .build();

        Comment input = CommentBuilder.aComment().withPostId(POST_ID).build();

        when(postRepository.findById(POST_ID)).thenReturn(Mono.just(noCommentsPost));

        StepVerifier.create(commentUseCase.create(input))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("deshabilitados"))
                .verify();
    }

    @Test
    void shouldSoftDeleteComment() {
        Comment existing = CommentBuilder.aComment().withId(COMMENT_ID).build();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Mono.just(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(commentUseCase.delete(COMMENT_ID))
                .verifyComplete();
    }
}
