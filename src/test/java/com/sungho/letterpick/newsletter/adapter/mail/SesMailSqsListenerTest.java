package com.sungho.letterpick.newsletter.adapter.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SesMailSqsListenerTest {

    private static final String SQS_MESSAGE_BODY = "ses notification json";
    private static final String LISTENER_ENABLED_PROPERTY = "letterpick.mail.sqs-listener.enabled=true";

    @Mock
    private SesMailReceiveProcessor processor;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(SesMailReceiveProcessor.class, () -> mock(SesMailReceiveProcessor.class))
            .withUserConfiguration(SesMailSqsListener.class);

    @Test
    @DisplayName("SQS message body를 SES mail receive processor에 전달한다")
    void receive_delegates_sqs_message_body_to_processor() {
        // given
        SesMailSqsListener listener = new SesMailSqsListener(processor);

        // when
        listener.receive(SQS_MESSAGE_BODY);

        // then
        verify(processor).process(SQS_MESSAGE_BODY);
    }

    @Test
    @DisplayName("메일 수신 SQS listener 설정이 켜져 있으면 listener bean을 등록한다")
    void register_sqs_listener_when_enabled() {
        // when
        contextRunner
                .withPropertyValues(LISTENER_ENABLED_PROPERTY)
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(SesMailSqsListener.class);
                });
    }

    @Test
    @DisplayName("메일 수신 SQS listener 설정이 꺼져 있으면 listener bean을 등록하지 않는다")
    void does_not_register_sqs_listener_when_disabled() {
        // when
        contextRunner.run(context -> {
            // then
            assertThat(context).doesNotHaveBean(SesMailSqsListener.class);
        });
    }
}
