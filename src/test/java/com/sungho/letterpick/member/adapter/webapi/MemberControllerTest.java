package com.sungho.letterpick.member.adapter.webapi;

import com.sungho.letterpick.common.auth.WithLoginUser;
import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.member.application.provided.MemberFinder;
import com.sungho.letterpick.member.application.provided.MemberModifier;
import com.sungho.letterpick.member.application.provided.MemberNicknameChangeRequest;
import com.sungho.letterpick.member.application.provided.MemberView;
import com.sungho.letterpick.member.domain.MemberRole;
import com.sungho.letterpick.member.domain.MemberStatus;
import com.sungho.letterpick.member.domain.exception.DuplicateNicknameException;
import com.sungho.letterpick.member.domain.exception.MemberNotFoundException;
import com.sungho.letterpick.member.domain.exception.MemberStatusException;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcConfig.class)
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MemberModifier memberModifier;

    @MockitoBean
    MemberFinder memberFinder;

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("GET /api/v1/members/me 요청 시 200과 회원 기본 정보가 반환된다")
    void findMember_returns_200_and_member_information() throws Exception {
        MemberView memberView = new MemberView(
                42L,
                "member@example.com",
                "새닉네임",
                MemberStatus.ACTIVE,
                MemberRole.ADMIN,
                "k8x3p9q2m4z1@letterpick.com"
        );
        given(memberFinder.findMember(42L)).willReturn(memberView);

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(42L))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.newsletterInboxAddress").value("k8x3p9q2m4z1@letterpick.com"));

        verify(memberFinder).findMember(42L);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("내 정보 조회 시 회원을 찾지 못하면 404가 반환된다")
    void findMember_returns_404_when_member_not_found() throws Exception {
        given(memberFinder.findMember(42L)).willThrow(new MemberNotFoundException());

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEM-003"));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("DELETE /api/v1/members/me 요청 시 204가 반환되고 서비스에 탈퇴가 위임된다")
    void withdraw_returns_204_and_delegates_to_service() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isNoContent());

        verify(memberModifier).withdraw(42L);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("탈퇴 시 회원을 찾지 못하면 404가 반환된다")
    void withdraw_returns_404_when_member_not_found() throws Exception {
        doThrow(new MemberNotFoundException()).when(memberModifier).withdraw(42L);

        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEM-003"));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("탈퇴 시 허용되지 않는 상태이면 409가 반환된다")
    void withdraw_returns_409_when_status_violation() throws Exception {
        doThrow(new MemberStatusException()).when(memberModifier).withdraw(42L);

        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEM-004"));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("PATCH /api/v1/members/me 요청 시 204가 반환되고 서비스에 닉네임 변경이 위임된다")
    void changeNickname_returns_204_and_delegates_to_service() throws Exception {
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("새닉네임");

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(memberModifier).changeNickname(42L, request);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("닉네임 변경 시 회원을 찾지 못하면 404가 반환된다")
    void changeNickname_returns_404_when_member_not_found() throws Exception {
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("새닉네임");
        doThrow(new MemberNotFoundException())
                .when(memberModifier).changeNickname(42L, request);

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEM-003"));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("닉네임 변경 시 닉네임이 중복되면 409가 반환된다")
    void changeNickname_returns_409_when_nickname_duplicated() throws Exception {
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("중복닉네임");
        doThrow(new DuplicateNicknameException())
                .when(memberModifier).changeNickname(42L, request);

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEM-002"));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("닉네임 변경 중 nickname unique constraint 충돌 시 409")
    void changeNickname_returns_409_when_nickname_unique_constraint_violated() throws Exception {
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("중복닉네임");
        doThrow(dataIntegrityViolation("uk_member_nickname"))
                .when(memberModifier).changeNickname(42L, request);

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEM-002"));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("닉네임 변경 시 허용되지 않는 상태이면 409가 반환된다")
    void changeNickname_returns_409_when_status_violation() throws Exception {
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("새닉네임");
        doThrow(new MemberStatusException())
                .when(memberModifier).changeNickname(42L, request);

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEM-004"));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("닉네임 변경 시 blank 입력이면 400이 반환된다")
    void changeNickname_returns_400_when_nickname_blank() throws Exception {
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("");

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private DataIntegrityViolationException dataIntegrityViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "Unique constraint violation",
                new ConstraintViolationException(
                        "Unique constraint violation",
                        new SQLException("constraint violation"),
                        constraintName
                )
        );
    }
}
