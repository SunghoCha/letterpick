package com.sungho.letterpick.newsletter.application.exception;

import com.sungho.letterpick.common.exception.BusinessException;
import com.sungho.letterpick.common.exception.CommonErrorCode;

public class InvalidEmailOperationsSearchConditionException extends BusinessException {

    public InvalidEmailOperationsSearchConditionException() {
        super(CommonErrorCode.INVALID_INPUT);
    }
}
