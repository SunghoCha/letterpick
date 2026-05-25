package com.sungho.letterpick.newsletter.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailOperationsSearchConditionTest {

    @Test
    @DisplayName("이메일 운영 검색 조건은 수신 시각 조건 없이 생성할 수 있다")
    void empty_returns_search_condition_without_received_range() {
        // when
        EmailOperationsSearchCondition condition = EmailOperationsSearchCondition.empty();

        // then
        assertThat(condition.hasReceivedRange()).isFalse();
        assertThat(condition.receivedFrom()).isNull();
        assertThat(condition.receivedTo()).isNull();
    }

    @Test
    @DisplayName("이메일 운영 검색 조건 생성 시 시작 시각과 종료 시각이 같으면 예외가 발생한다")
    void constructor_throws_exception_when_received_from_is_equal_to_received_to() {
        // given
        Instant receivedAt = Instant.parse("2050-05-12T15:00:00Z");

        // when & then
        assertThatThrownBy(() -> EmailOperationsSearchCondition.receivedAtRange(receivedAt, receivedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("receivedFrom must be before receivedTo");
    }
}
