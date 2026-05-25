package com.sungho.letterpick.newsletter.adapter.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE;

import com.sungho.letterpick.newsletter.application.required.QueueStatusReadException;
import com.sungho.letterpick.newsletter.application.required.QueueStatusSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

@ExtendWith(MockitoExtension.class)
class SqsQueueStatusReaderTest {

    private static final String MAIN_QUEUE_NAME = "letterpick-mail-receive";
    private static final String DEAD_LETTER_QUEUE_NAME = "letterpick-mail-receive-dlq";
    private static final String MAIN_QUEUE_URL = "https://sqs.ap-northeast-2.amazonaws.com/123/main";
    private static final String DEAD_LETTER_QUEUE_URL = "https://sqs.ap-northeast-2.amazonaws.com/123/dlq";

    @Mock
    private SqsClient sqsClient;

    @Test
    @DisplayName("메인 큐와 DLQ attribute를 조회해 큐 상태 스냅샷으로 변환한다")
    void readQueueStatus_returns_queue_status_snapshot() {
        // given
        SqsQueueStatusReader reader = new SqsQueueStatusReader(
                sqsClient,
                MAIN_QUEUE_NAME,
                DEAD_LETTER_QUEUE_NAME
        );
        given(sqsClient.getQueueUrl(argThat((GetQueueUrlRequest request) ->
                request != null && MAIN_QUEUE_NAME.equals(request.queueName()))))
                .willReturn(GetQueueUrlResponse.builder()
                        .queueUrl(MAIN_QUEUE_URL)
                        .build());
        given(sqsClient.getQueueUrl(argThat((GetQueueUrlRequest request) ->
                request != null && DEAD_LETTER_QUEUE_NAME.equals(request.queueName()))))
                .willReturn(GetQueueUrlResponse.builder()
                        .queueUrl(DEAD_LETTER_QUEUE_URL)
                        .build());
        given(sqsClient.getQueueAttributes(argThat((GetQueueAttributesRequest request) ->
                request != null
                        && MAIN_QUEUE_URL.equals(request.queueUrl())
                        && Set.copyOf(request.attributeNames()).equals(Set.of(
                        APPROXIMATE_NUMBER_OF_MESSAGES,
                        APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                        APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED
                )))))
                .willReturn(GetQueueAttributesResponse.builder()
                        .attributes(Map.of(
                                APPROXIMATE_NUMBER_OF_MESSAGES, "3",
                                APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE, "2",
                                APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED, "1"
                        ))
                        .build());
        given(sqsClient.getQueueAttributes(argThat((GetQueueAttributesRequest request) ->
                request != null
                        && DEAD_LETTER_QUEUE_URL.equals(request.queueUrl())
                        && request.attributeNames().equals(List.of(APPROXIMATE_NUMBER_OF_MESSAGES)))))
                .willReturn(GetQueueAttributesResponse.builder()
                        .attributes(Map.of(
                                APPROXIMATE_NUMBER_OF_MESSAGES, "4"
                        ))
                        .build());

        // when
        QueueStatusSnapshot result = reader.readQueueStatus();

        // then
        assertThat(result.mainQueue().availableMessageCount()).isEqualTo(3L);
        assertThat(result.mainQueue().inFlightMessageCount()).isEqualTo(2L);
        assertThat(result.mainQueue().delayedMessageCount()).isEqualTo(1L);
        assertThat(result.deadLetterQueue().availableMessageCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("SQS 큐가 없으면 application required 예외로 변환한다")
    void readQueueStatus_wraps_queue_does_not_exist_exception() {
        // given
        SqsQueueStatusReader reader = new SqsQueueStatusReader(
                sqsClient,
                MAIN_QUEUE_NAME,
                DEAD_LETTER_QUEUE_NAME
        );
        given(sqsClient.getQueueUrl(argThat((GetQueueUrlRequest request) ->
                request != null && MAIN_QUEUE_NAME.equals(request.queueName()))))
                .willThrow(QueueDoesNotExistException.builder()
                        .message("queue does not exist")
                        .build());

        // when & then
        assertThatThrownBy(reader::readQueueStatus)
                .isInstanceOf(QueueStatusReadException.class)
                .hasMessage("SQS queue does not exist")
                .hasCauseInstanceOf(QueueDoesNotExistException.class);
    }
}
