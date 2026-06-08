package com.sungho.letterpick.trending.lifecycle.adapter.sqs;

import com.sungho.letterpick.trending.application.TrendingMessageProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrendingLifecycleSqsListenerTest {

    private static final String SQS_MESSAGE_BODY = "trending lifecycle event json";
    private static final String QUEUE_NAME = "letterpick-test-trending-lifecycle-events";
    private static final String LISTENER_ENABLED_PROPERTY = "letterpick.trending.sqs-listener.enabled=true";
    private static final String LISTENER_DISABLED_PROPERTY = "letterpick.trending.sqs-listener.enabled=false";
    private static final String QUEUE_NAME_PROPERTY =
            "letterpick.trending.lifecycle-events-queue=" + QUEUE_NAME;
    private static final String BLANK_QUEUE_NAME_PROPERTY = "letterpick.trending.lifecycle-events-queue= ";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(TrendingMessageProcessor.class, () -> mock(TrendingMessageProcessor.class))
            .withUserConfiguration(TrendingLifecycleSqsListener.class);

    @Test
    @DisplayName("SQS message body와 queue name을 lifecycle event processor에 전달한다")
    void receive_delegates_message_body_and_queue_name_to_processor() {
        // given
        TrendingMessageProcessor processor = mock(TrendingMessageProcessor.class);
        TrendingLifecycleSqsListener listener = new TrendingLifecycleSqsListener(processor, QUEUE_NAME);

        // when
        listener.receive(SQS_MESSAGE_BODY);

        // then
        verify(processor).process(SQS_MESSAGE_BODY, QUEUE_NAME);
    }

    @Test
    @DisplayName("queue name 설정이 비어 있으면 listener 생성 시 실패한다")
    void constructor_rejects_blank_queue_name() {
        // given
        TrendingMessageProcessor processor = mock(TrendingMessageProcessor.class);

        // when & then
        assertThatIllegalStateException()
                .isThrownBy(() -> new TrendingLifecycleSqsListener(processor, " "))
                .withMessageContaining("trending lifecycle queue name is not configured");
    }

    @Test
    @DisplayName("트렌딩 lifecycle SQS listener 설정이 켜져 있으면 listener bean을 등록한다")
    void register_sqs_listener_when_enabled() {
        // when
        contextRunner
                .withPropertyValues(LISTENER_ENABLED_PROPERTY, QUEUE_NAME_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(TrendingLifecycleSqsListener.class);
                });
    }

    @Test
    @DisplayName("트렌딩 lifecycle SQS listener 설정이 없으면 listener bean을 등록하지 않는다")
    void does_not_register_sqs_listener_when_enabled_property_is_missing() {
        // when
        contextRunner
                .withPropertyValues(QUEUE_NAME_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(TrendingLifecycleSqsListener.class);
                });
    }

    @Test
    @DisplayName("트렌딩 lifecycle SQS listener 설정이 꺼져 있으면 listener bean을 등록하지 않는다")
    void does_not_register_sqs_listener_when_disabled() {
        // when
        contextRunner
                .withPropertyValues(LISTENER_DISABLED_PROPERTY, QUEUE_NAME_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(TrendingLifecycleSqsListener.class);
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
                            .hasRootCauseMessage("trending lifecycle queue name is not configured: "
                                    + "letterpick.trending.lifecycle-events-queue");
                });
    }
}
