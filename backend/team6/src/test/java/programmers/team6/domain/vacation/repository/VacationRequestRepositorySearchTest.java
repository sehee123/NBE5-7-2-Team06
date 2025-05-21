package programmers.team6.domain.vacation.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import programmers.team6.domain.admin.dto.AdminVacationSearchCondition;
import programmers.team6.domain.admin.dto.VacationRequestSearchResponse;
import programmers.team6.domain.admin.enums.Quarter;
import programmers.team6.domain.admin.repository.AdminVacationRequestSearchCustom;
import programmers.team6.domain.member.entity.Code;
import programmers.team6.domain.member.entity.Dept;
import programmers.team6.domain.member.entity.Member;
import programmers.team6.domain.member.enums.Role;
import programmers.team6.domain.member.repository.CodeRepository;
import programmers.team6.domain.member.repository.DeptRepository;
import programmers.team6.domain.member.repository.MemberRepository;
import programmers.team6.domain.vacation.entity.ApprovalStep;
import programmers.team6.domain.vacation.entity.VacationRequest;
import programmers.team6.domain.vacation.enums.ApprovalStatus;
import programmers.team6.domain.vacation.enums.VacationRequestStatus;

@Disabled
@SpringBootTest
@Transactional
	/**
	 * 현재 조회 및 필터 검색 과정을 테스트하기엔 생성 기능이 제공되지 않아 임시로 테스트 데이터를 작성하고 진행하였음
	 * 추후 필히 제거 요망, 검색 기능 테스트
	 * 조회시 검색 기능을 테스트하기 위해 테스트마다 데이터를 추가 생성하기엔 시간이 많이 걸려 로컬 환경에서 임의로 데이터 생성하고 테스트 진행
	 */
class VacationRequestRepositorySearchTest {
	@Autowired
	private VacationRequestRepository vacationRequestRepository;
	@Autowired
	private ApprovalStepRepository approvalStepRepository;
	@Autowired
	private CodeRepository codeRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private DeptRepository deptRepository;
	@Autowired
	AdminVacationRequestSearchCustom adminVacationRequestSearchCustom;

	// @Test
	// @Rollback(value = false)
	// void reset() {
	// 	vacationRequestRepository.deleteAll();
	// 	approvalStepRepository.deleteAll();
	// 	codeRepository.deleteAll();
	// 	memberRepository.deleteAll();
	// }

	@Test
	@Rollback(value = false)
	void init(@Autowired EntityManager entityManager) {
		Code positionCode = codeRepository.save(new Code("POSITION", "pos", "posName"));

		for (int i = 0; i < 3; i++) {
			Dept dept = deptRepository.save(Dept.builder().deptName("dept " + i).build());
			Member member = memberRepository.save(
				new Member("member " + i, dept, positionCode, LocalDateTime.now(), Role.USER));
			dept.toBuilder().deptLeader(member);

			List<Member> approvers = new ArrayList<>();
			for (int j = 0; j < 5; j++) {
				approvers.add(new Member(String.format("approver %d %d", i, j), dept,
					new Code("POSITION", "pos " + (10 * i + j + 1), "posName " + (10 * i + j)), LocalDateTime.now(),
					Role.USER));
			}
			memberRepository.saveAll(approvers);

			Code code = codeRepository.save(new Code("VACATION", "vac " + i, "vacName " + i));

			// 2023 , 2024 , 2025
			for (int year = 2023; year < 2026; year++) {
				// 1 2 3 4
				for (int month = 1; month < 5; month++) {
					// 1 2 3
					for (int day = 1; day < 4; day++) {
						VacationRequest vr = vacationRequestRepository.save(
							new VacationRequest(member, LocalDate.of(year, month, day).atStartOfDay(),
								LocalDate.of(year, month, day + 1).atStartOfDay(), null, code,
								VacationRequestStatus.APPROVED, 1));
						List<ApprovalStep> approvalSteps = new ArrayList<>();
						for (int j = 0; j < 3; j++) {
							if (day == 1) {
								approvalSteps.add(
									new ApprovalStep(approvers.get(j), vr, ApprovalStatus.APPROVED, j, null));
								vr.update(vr.getType(), vr.getFrom(), vr.getTo(), VacationRequestStatus.APPROVED, null);
							} else {
								approvalSteps.add(
									new ApprovalStep(approvers.get(j), vr, ApprovalStatus.PENDING, j, null));
							}
						}
						approvalStepRepository.saveAll(approvalSteps);
					}
				}
			}

		}
		entityManager.flush();
		entityManager.close();
	}

	@Test
	void testDefault() {
		// given
		AdminVacationSearchCondition defaultCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, null, null), null);

		AdminVacationSearchCondition latestCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition("member 0", null, null, null), null);

		// when
		Page<VacationRequestSearchResponse> defaultResult = adminVacationRequestSearchCustom.search(defaultCondition,
			PageRequest.of(0, Integer.MAX_VALUE));
		Page<VacationRequestSearchResponse> defaultPageableResult = adminVacationRequestSearchCustom.search(
			defaultCondition, PageRequest.of(0, 3));
		Page<VacationRequestSearchResponse> latestResult = adminVacationRequestSearchCustom.search(latestCondition,
			PageRequest.of(0, Integer.MAX_VALUE));

		// then
		assertThat(defaultResult).hasSize(3 * 4 * 3 * 3);
		assertThat(defaultPageableResult).hasSize(3);

		for (VacationRequestSearchResponse vacationRequestReadResponse : defaultPageableResult) {
			assertThat(vacationRequestReadResponse.id()).isNotNull();
			assertThat(vacationRequestReadResponse.type()).isNotNull();
			assertThat(vacationRequestReadResponse.from()).isNotNull();
			assertThat(vacationRequestReadResponse.to()).isNotNull();
			assertThat(vacationRequestReadResponse.applicantName()).isNotNull();
			assertThat(vacationRequestReadResponse.approverNames()).isNotNull();
			assertThat(vacationRequestReadResponse.deptName()).isNotNull();
			assertThat(vacationRequestReadResponse.status()).isNotNull();
		}

		List<VacationRequestSearchResponse> content = latestResult.getContent();
		for (int i = 0; i < content.size() - 1; i++) {
			assertThat(content.get(i).from()).isAfter(content.get(i + 1).from());
			assertThat(content.get(i).to()).isAfter(content.get(i + 1).to());
		}
	}

	@Test
	void testDate() {
		// given
		AdminVacationSearchCondition dateCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(LocalDate.of(2025, 1, 1).atStartOfDay(),
				LocalDate.of(2026, 1, 1).atStartOfDay(), null, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, null, null), null);
		AdminVacationSearchCondition yearCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, 2025, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, null, null), null);
		AdminVacationSearchCondition yearAndQuarterCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, 2025, Quarter.Q1),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, null, null), null);

		// when
		Page<VacationRequestSearchResponse> dateResult = adminVacationRequestSearchCustom.search(dateCondition,
			PageRequest.of(0, Integer.MAX_VALUE));
		Page<VacationRequestSearchResponse> yearResult = adminVacationRequestSearchCustom.search(yearCondition,
			PageRequest.of(0, Integer.MAX_VALUE));
		Page<VacationRequestSearchResponse> yearAndQuarterResult = adminVacationRequestSearchCustom.search(
			yearAndQuarterCondition, PageRequest.of(0, Integer.MAX_VALUE));

		// then
		assertThat(dateResult).hasSize(3 * 4 * 3);
		assertThat(yearResult).hasSize(3 * 4 * 3);
		assertThat(yearAndQuarterResult).hasSize(3 * 3 * 3);
	}

	@Test
	void testApplicantName() {
		// given
		AdminVacationSearchCondition applicantNameCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition("0", null, null, null), null);

		// when
		Page<VacationRequestSearchResponse> result = adminVacationRequestSearchCustom.search(applicantNameCondition,
			PageRequest.of(0, Integer.MAX_VALUE));

		// then
		for (VacationRequestSearchResponse vacationRequestReadResponse : result) {
			assertThat(vacationRequestReadResponse.applicantName()).contains("0");
		}
		assertThat(result).hasSize(3 * 4 * 3);
	}

	@Test
	void testDeptName() {
		// given
		AdminVacationSearchCondition deptNameCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, "0", null, null), null);

		Page<VacationRequestSearchResponse> result = adminVacationRequestSearchCustom.search(deptNameCondition,
			PageRequest.of(0, Integer.MAX_VALUE));

		for (VacationRequestSearchResponse vacationRequestReadResponse : result) {
			assertThat(vacationRequestReadResponse.deptName()).contains("0");
		}
		assertThat(result).hasSize(3 * 4 * 3);
	}

	@Test
	void testCodeId() {
		// given
		AdminVacationSearchCondition applicantVacationTypeCodeIdCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, null, 7L), null);
		AdminVacationSearchCondition applicantPositionCodeIdCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, 1L, null), null);

		// when
		Page<VacationRequestSearchResponse> applicantVacationTypeCodeIdResult = adminVacationRequestSearchCustom.search(
			applicantVacationTypeCodeIdCondition, PageRequest.of(0, Integer.MAX_VALUE));
		Page<VacationRequestSearchResponse> applicantPositionCodeIdResult = adminVacationRequestSearchCustom.search(
			applicantPositionCodeIdCondition, PageRequest.of(0, Integer.MAX_VALUE));

		// then
		assertThat(applicantVacationTypeCodeIdResult).hasSize(3 * 4 * 3);
		for (VacationRequestSearchResponse vacationRequestReadResponse : applicantVacationTypeCodeIdResult) {
			assertThat(vacationRequestReadResponse.applicantName()).isEqualTo("member 0");
		}
		assertThat(applicantPositionCodeIdResult).hasSize(3 * 4 * 3 * 3);
		for (VacationRequestSearchResponse vacationRequestReadResponse : applicantPositionCodeIdResult) {
			assertThat(vacationRequestReadResponse.applicantName()).startsWith("member");
		}
	}

	@Test
	void testVacationRequestStatus() {
		// given
		AdminVacationSearchCondition vacationRequestStatusCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, null, null),
			VacationRequestStatus.APPROVED);

		// when
		Page<VacationRequestSearchResponse> vacationRequestStatusResult = adminVacationRequestSearchCustom.search(
			vacationRequestStatusCondition, PageRequest.of(0, Integer.MAX_VALUE));

		// then
		assertThat(vacationRequestStatusResult).hasSize(4 * 3 * 3);
		for (VacationRequestSearchResponse vacationRequestReadResponse : vacationRequestStatusResult) {
			assertThat(vacationRequestReadResponse.status()).isEqualTo(VacationRequestStatus.APPROVED);
		}
	}

	@Test
	void testApproverNames() {
		// given
		AdminVacationSearchCondition approverNameCondition = new AdminVacationSearchCondition(
			new AdminVacationSearchCondition.DateRangeCondition(null, null, null, null),
			new AdminVacationSearchCondition.ApplicantCondition(null, null, null, null), null);

		// when
		Page<VacationRequestSearchResponse> approverNameResult = adminVacationRequestSearchCustom.search(
			approverNameCondition, PageRequest.of(0, Integer.MAX_VALUE));

		// then
		assertThat(approverNameResult).hasSize(3 * 4 * 3);
		for (VacationRequestSearchResponse vacationRequestReadResponse : approverNameResult) {
			assertThat(vacationRequestReadResponse.approverNames()).hasSize(3);
			assertThat(vacationRequestReadResponse.approverNames().get(0).contains("approver 0")
				|| vacationRequestReadResponse.approverNames().get(1).contains("approver 0")
				|| vacationRequestReadResponse.approverNames().get(2).contains("approver 0")).isTrue();
		}
	}
}
