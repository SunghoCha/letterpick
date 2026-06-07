package com.sungho.letterpick.member.application.provided;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.common.auth.SocialProvider;
import com.sungho.letterpick.member.application.MemberModifyService;
import com.sungho.letterpick.member.application.NewsletterInboxAddressGenerator;
import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.MemberFixture;
import com.sungho.letterpick.member.domain.MemberStatus;
import com.sungho.letterpick.member.domain.Nickname;
import com.sungho.letterpick.member.domain.exception.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import({LetterPickTestConfiguration.class, MemberModifyService.class,
        NewsletterInboxAddressGenerator.class})
class MemberModifierTest {

    @Autowired
    MemberModifier memberModifier;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach // 제거 가능할듯
    void cleanUp() {
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("회원 가입이 정상적으로 이루어진다")
    void register() {
        // given
        MemberRegisterRequest request = new MemberRegisterRequest(
                "email@test.com", "nickname", SocialProvider.GOOGLE, "google-sub-1");
        // when
        Member member = memberModifier.register(request);
        // then
        assertThat(member.getId()).isNotNull();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getEmail().address()).isEqualTo("email@test.com");
        assertThat(member.getNickname().name()).isEqualTo("nickname");
        assertThat(member.getNewsletterInboxAddress().address())
                .matches("^[a-z0-9]{12}@inbound\\.letterpick\\.test$");
        assertThat(member.getCreatedAt()).isNotNull();
        assertThat(member.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원 가입 시 이메일이 중복이면 실패한다")
    void registerFailsWhenEmailDuplicated() {
        // given
        MemberRegisterRequest request = new MemberRegisterRequest(
                "email@test.com", "nickname", SocialProvider.GOOGLE, "google-sub-1");
        MemberRegisterRequest otherRequest = new MemberRegisterRequest(
                "email@test.com", "other", SocialProvider.GOOGLE, "google-sub-2");
        memberModifier.register(request);
        // then
        assertThatThrownBy(() -> memberModifier.register(otherRequest))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("회원 가입 시 닉네임이 중복이면 실패한다")
    void registerFailsWhenNicknameDuplicated() {
        // given
        MemberRegisterRequest request = new MemberRegisterRequest(
                "email@test.com", "nickname", SocialProvider.GOOGLE, "google-sub-1");
        MemberRegisterRequest otherRequest = new MemberRegisterRequest(
                "other@test.com", "nickname", SocialProvider.GOOGLE, "google-sub-2");
        // when
        memberModifier.register(request);
        // then
        assertThatThrownBy(() -> memberModifier.register(otherRequest))
                .isInstanceOf(DuplicateNicknameException.class)
                .hasMessageContaining(MemberErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("회원 닉네임 수정이 정상적으로 이루어진다")
    void changeNickname() {
        // given
        Member member = MemberFixture.createMember();
        Member savedMember = memberRepository.save(member);
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("수정된닉네임");
        // when
        memberModifier.changeNickname(savedMember.getId(), request);

        // then
        Member changedMember = memberRepository.findById(savedMember.getId()).orElseThrow();
        assertThat(changedMember.getNickname()).isEqualTo(new Nickname(request.nickname()));

    }

    @Test
    @DisplayName("존재하지 않는 회원의 닉네임을 변경하면 실패한다")
    void changeNicknameFailsWhenMemberNotFound() {
        // given
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest("수정된닉네임");
        // then
        assertThatThrownBy(() -> memberModifier.changeNickname(1L, request))
                .isInstanceOf(MemberNotFoundException.class);

    }

    @Test
    @DisplayName("다른 회원이 사용 중인 닉네임으로 변경하면 실패한다")
    void changeNicknameFailsWhenNicknameDuplicated() {
        // given
        Member member = memberRepository.save(MemberFixture.createMember("test1@email.com", "닉네임1"));
        Member otherMember = memberRepository.save(MemberFixture.createMember("test2@email.com", "닉네임2"));
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest(member.getNickname().name());
        // then
        assertThatThrownBy(() -> memberModifier.changeNickname(otherMember.getId(), request))
                .isInstanceOf(DuplicateNicknameException.class);

    }

    @Test
    @DisplayName("현재와 동일한 닉네임으로 변경하면 변경 없이 정상 처리된다")
    void changeNicknameKeepsWhenSameNickname() {
        // given
        Member member = memberRepository.save(MemberFixture.createMember("test1@email.com", "닉네임1"));
        MemberNicknameChangeRequest request = new MemberNicknameChangeRequest(member.getNickname().name());
        // when
        memberModifier.changeNickname(member.getId(), request);
        // then
        Member savedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(savedMember.getNickname()).isEqualTo(member.getNickname());

    }

    @Test
    @DisplayName("ACTIVE 아닌 회원이 닉네임을 변경하면 실패한다")
    void changeNicknameFailsWhenNotActive() {
        // given
        Member member = MemberFixture.createMember();
        member.suspend();
        Member savedMember = memberRepository.save(member);
        MemberNicknameChangeRequest request =
                new MemberNicknameChangeRequest("새로운닉네임");
        // then
        assertThatThrownBy(() -> memberModifier.changeNickname(savedMember.getId(), request))
                .isInstanceOf(MemberStatusException.class);
    }

    @Test
    @DisplayName("DEACTIVATED 회원이 닉네임을 변경하면 실패한다")
    void changeNicknameFailsWhenDeactivated() {
        // given
        Member member = MemberFixture.createMember();
        member.withdraw();
        Member savedMember = memberRepository.save(member);
        MemberNicknameChangeRequest request =
                new MemberNicknameChangeRequest("새로운닉네임");
        // then
        assertThatThrownBy(() -> memberModifier.changeNickname(savedMember.getId(), request))
                .isInstanceOf(MemberStatusException.class);
    }

    @Test
    @DisplayName("회원 탈퇴가 정상적으로 이루어진다")
    void withdraw() {
        // given
        Member savedMember = memberRepository.save(MemberFixture.createMember());
        // when
        memberModifier.withdraw(savedMember.getId());
        // then
        Member foundMember = memberRepository.findById(savedMember.getId()).orElseThrow();
        assertThat(foundMember.getStatus()).isEqualTo(MemberStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("존재하지 않는 회원이 탈퇴하면 실패한다")
    void withdrawFailsWhenMemberNotFound() {
        // then
        assertThatThrownBy(() -> memberModifier.withdraw(1L)).isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("ACTIVE 아닌 회원이 탈퇴하면 실패한다")
    void withdrawFailsWhenNotActive() {
        // given
        Member member = MemberFixture.createMember();
        member.suspend();
        Member savedMember = memberRepository.save(member);

        // then
        assertThatThrownBy(() -> memberModifier.withdraw(savedMember.getId()))
                .isInstanceOf(MemberStatusException.class);

    }

    @Test
    @DisplayName("관리자가 회원을 정지시킨다")
    void suspend() {
        // given
        Member member = MemberFixture.createMember();
        Member savedMember = memberRepository.save(member);
        MemberSuspendRequest request = new MemberSuspendRequest(savedMember.getId());
        // when
        memberModifier.suspend(request);
        // then
        Member foundMember = memberRepository.findById(savedMember.getId()).orElseThrow();
        assertThat(foundMember.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }
    
    @Test
    @DisplayName("존재하지 않는 회원을 정지시키면 실패한다")
    void suspendFailsWhenMemberNotFound() {
        // given
        MemberSuspendRequest request = new MemberSuspendRequest(1L);
        // then
        assertThatThrownBy(() -> memberModifier.suspend(request))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("ACTIVE 아닌 회원을 정지시키면 실패한다")
    void suspendFailsWhenNotActive() {
        // given
        Member member = MemberFixture.createMember();
        member.suspend();
        Member savedMember = memberRepository.save(member);
        MemberSuspendRequest request = new MemberSuspendRequest(savedMember.getId());
        // then
        assertThatThrownBy(() -> memberModifier.suspend(request))
                .isInstanceOf(MemberStatusException.class);
    }

    @Test
    @DisplayName("관리자가 정지 상태의 회원을 탈퇴 처리한다")
    void withdrawByAdmin() {
        // given
        Member member = MemberFixture.createMember();
        member.suspend();
        Member savedMember = memberRepository.save(member);
        MemberWithdrawByAdminRequest request = new MemberWithdrawByAdminRequest(savedMember.getId());
        // when
        memberModifier.withdrawByAdmin(request);
        // then
        Member foundMember = memberRepository.findById(savedMember.getId()).orElseThrow();
        assertThat(foundMember.getStatus()).isEqualTo(MemberStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("존재하지 않는 회원을 관리자가 탈퇴 처리하면 실패한다")
    void withdrawByAdminFailsWhenMemberNotFound() {
        // given
        MemberWithdrawByAdminRequest request = new MemberWithdrawByAdminRequest(1L);
        // then
        assertThatThrownBy(() -> memberModifier.withdrawByAdmin(request))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("SUSPENDED 아닌 회원을 관리자가 탈퇴 처리하면 실패한다")
    void withdrawByAdminFailsWhenNotSuspended() {
        // given
        Member member = MemberFixture.createMember();
        Member savedMember = memberRepository.save(member);
        MemberWithdrawByAdminRequest request = new MemberWithdrawByAdminRequest(savedMember.getId());
        // then
        assertThatThrownBy(() -> memberModifier.withdrawByAdmin(request))
                .isInstanceOf(MemberStatusException.class);
    }
}
