package com.sungho.letterpick.trending.score.adapter.sqs;

import com.sungho.letterpick.trending.score.application.TrendingScoreMessageProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrendingScoreSqsListenerTest {

    private static final String SQS_MESSAGE_BODY = "trending score event json";
    private static final String QUEUE_NAME = "letterpick-test-trending-score-events";
    private static final String LISTENER_ENABLED_PROPERTY = "letterpick.trending.sqs-listener.enabled=true";
    private static final String LISTENER_DISABLED_PROPERTY = "letterpick.trending.sqs-listener.enabled=false";
    private static final String QUEUE_NAME_PROPERTY_NAME = "letterpick.trending.score-events-queue";
    private static final String QUEUE_NAME_PROPERTY =
            QUEUE_NAME_PROPERTY_NAME + "=" + QUEUE_NAME;
    private static final String BLANK_QUEUE_NAME_PROPERTY = "letterpick.trending.score-events-queue= ";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(TrendingScoreMessageProcessor.class, () -> mock(TrendingScoreMessageProcessor.class))
            .withUserConfiguration(TrendingScoreSqsListener.class);

    @Test
    @DisplayName("SQS message body를 score event processor에 전달한다")
    void receive_delegates_message_body_to_processor() {
        // given
        TrendingScoreMessageProcessor processor = mock(TrendingScoreMessageProcessor.class);
        TrendingScoreSqsListener listener = new TrendingScoreSqsListener(processor, QUEUE_NAME);

        // when
        listener.receive(SQS_MESSAGE_BODY);

        // then
        verify(processor).process(SQS_MESSAGE_BODY);
    }

    @Test
    @DisplayName("queue name 설정이 비어 있으면 listener 생성 시 실패한다")
    void constructor_rejects_blank_queue_name() {
        // given
        TrendingScoreMessageProcessor processor = mock(TrendingScoreMessageProcessor.class);

        // when & then
        assertThatIllegalStateException()
                .isThrownBy(() -> new TrendingScoreSqsListener(processor, " "))
                .withMessageContaining("trending score queue name is not configured");
    }

    @Test
    @DisplayName("queue name 설정에 앞뒤 공백이 있으면 listener 생성 시 실패한다")
    void constructor_rejects_padded_queue_name() {
        // given
        TrendingScoreMessageProcessor processor = mock(TrendingScoreMessageProcessor.class);

        // when & then
        assertThatIllegalStateException()
                .isThrownBy(() -> new TrendingScoreSqsListener(processor, " " + QUEUE_NAME + " "))
                .withMessageContaining("trending score queue name is not configured");
    }

    @Test
    @DisplayName("트렌딩 SQS listener 설정이 켜져 있으면 score listener bean을 등록한다")
    void register_sqs_listener_when_enabled() {
        // when
        contextRunner
                .withPropertyValues(LISTENER_ENABLED_PROPERTY, QUEUE_NAME_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(TrendingScoreSqsListener.class);
                });
    }

    @Test
    @DisplayName("트렌딩 SQS listener 설정이 없으면 score listener bean을 등록하지 않는다")
    void does_not_register_sqs_listener_when_enabled_property_is_missing() {
        // when
        contextRunner
                .withPropertyValues(QUEUE_NAME_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(TrendingScoreSqsListener.class);
                });
    }

    @Test
    @DisplayName("트렌딩 SQS listener 설정이 꺼져 있으면 score listener bean을 등록하지 않는다")
    void does_not_register_sqs_listener_when_disabled() {
        // when
        contextRunner
                .withPropertyValues(LISTENER_DISABLED_PROPERTY, QUEUE_NAME_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(TrendingScoreSqsListener.class);
                });
    }

    @Test
    @DisplayName("listener 설정이 켜져 있고 queue name 설정이 비어 있으면 context 생성이 실패한다")
    void fail_to_register_sqs_listener_when_queue_name_is_blank() {
        // when
        contextRunner
                .withPropertyValues(LISTENER_ENABLED_PROPERTY, BLANK_QUEUE_NAME_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("trending score queue name is not configured: "
                                    + "letterpick.trending.score-events-queue");
                });
    }

    @Test
    @DisplayName("listener 설정이 켜져 있고 queue name 설정에 앞뒤 공백이 있으면 context 생성이 실패한다")
    void fail_to_register_sqs_listener_when_queue_name_is_padded() {
        // when
        contextRunner
                .withInitializer(context -> context.getEnvironment()
                        .getPropertySources()
                        .addFirst(new MapPropertySource(
                                "paddedQueueName",
                                Map.of(QUEUE_NAME_PROPERTY_NAME, " " + QUEUE_NAME + " ")
                        )))
                .withPropertyValues(LISTENER_ENABLED_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("trending score queue name is not configured: "
                                    + QUEUE_NAME_PROPERTY_NAME);
                });
    }
}
