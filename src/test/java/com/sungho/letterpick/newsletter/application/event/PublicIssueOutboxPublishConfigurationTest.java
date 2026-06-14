package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.common.outbox.OutboxMessageRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PublicIssueOutboxPublishConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OutboxMessageRelay.class, () -> mock(OutboxMessageRelay.class))
            .withUserConfiguration(PublicIssueOutboxPublishConfiguration.class);

    @Test
    @DisplayName("outbox 발행 설정이 켜져 있으면 공개 이슈 즉시 발행 listener를 등록한다")
    void registerListenersWhenOutboxPublishEnabled() {
        // when
        contextRunner
                .withPropertyValues("letterpick.outbox.publish.enabled=true")
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(PublicIssueAvailableOutboxPublishListener.class);
                    assertThat(context).hasSingleBean(PublicIssueRemovedOutboxPublishListener.class);
                });
    }

    @Test
    @DisplayName("outbox 발행 설정이 꺼져 있으면 공개 이슈 즉시 발행 listener를 등록하지 않는다")
    void doesNotRegisterListenersWhenOutboxPublishDisabled() {
        // when
        contextRunner.run(context -> {
            // then
            assertThat(context).doesNotHaveBean(PublicIssueAvailableOutboxPublishListener.class);
            assertThat(context).doesNotHaveBean(PublicIssueRemovedOutboxPublishListener.class);
        });
    }
}
