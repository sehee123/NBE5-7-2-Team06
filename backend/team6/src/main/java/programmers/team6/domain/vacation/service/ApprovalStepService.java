package programmers.team6.domain.vacation.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import programmers.team6.domain.member.entity.Dept;
import programmers.team6.domain.member.entity.Member;
import programmers.team6.domain.member.repository.DeptRepository;
import programmers.team6.domain.vacation.dto.ApprovalFirstStepDetailResponse;
import programmers.team6.domain.vacation.dto.ApprovalFirstStepSelectResponse;
import programmers.team6.domain.vacation.dto.ApprovalSecondStepDetailResponse;
import programmers.team6.domain.vacation.dto.ApprovalSecondStepSelectResponse;
import programmers.team6.domain.vacation.dto.ApprovalStepRejectRequest;
import programmers.team6.domain.vacation.dto.ApprovalStepSelectRequest;
import programmers.team6.domain.vacation.entity.ApprovalStep;
import programmers.team6.domain.vacation.entity.VacationInfo;
import programmers.team6.domain.vacation.entity.VacationInfoLog;
import programmers.team6.domain.vacation.entity.VacationRequest;
import programmers.team6.domain.vacation.repository.ApprovalStepRepository;
import programmers.team6.domain.vacation.repository.VacationInfoRepository;
import programmers.team6.domain.vacation.util.mapper.ApprovalStepMapper;
import programmers.team6.global.exception.code.NotFoundErrorCode;
import programmers.team6.global.exception.customException.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalStepService {

	private static final int STEP1 = 1;
	private static final int STEP2 = 2;

	private final ApprovalStepRepository approvalStepRepository;
	private final VacationInfoRepository vacationInfoRepository;
	private final DeptRepository deptRepository;
	private final VacationInfoLogPublisher vacationInfoLogPublisher;

	public Page<ApprovalFirstStepSelectResponse> findFirstStepByMemberId(Long memberId, Pageable pageable) {
		return approvalStepRepository.findFirstStepByMemberId(memberId, STEP1, pageable);
	}

	public Page<ApprovalFirstStepSelectResponse> findFirstStepByFilter(
		ApprovalStepSelectRequest request, Long memberId, Pageable pageable) {
		return approvalStepRepository.findFirstStepByFilter(memberId, request.type(),
			request.name(), request.from(), request.to(), request.status(), STEP1, pageable);
	}

	public Page<ApprovalSecondStepSelectResponse> findSecondStepByMemberId(Long memberId, Pageable pageable) {
		return approvalStepRepository.findSecondStepByMemberId(memberId, STEP2, pageable);
	}

	public Page<ApprovalSecondStepSelectResponse> findSecondStepByFilter(
		ApprovalStepSelectRequest request, Long memberId, Pageable pageable) {
		return approvalStepRepository.findSecondStepByFilter(memberId, request.type(),
			request.name(), request.from(), request.to(), request.status(), STEP2, pageable);
	}

	public ApprovalFirstStepDetailResponse findFirstStepDetailById(Long approvalStepId, Long memberId) {
		ApprovalStep findApprovalStep = approvalStepRepository.findByIdAndMember_IdAndStep(approvalStepId,
				memberId, STEP1)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_FIRST_APPROVAL_STEP));
		return ApprovalStepMapper.fromFirstStepEntity(findApprovalStep);
	}

	public ApprovalSecondStepDetailResponse findSecondStepDetailById(Long approvalStepId, Long memberId) {
		ApprovalStep findApprovalStep = approvalStepRepository.findByIdAndMember_IdAndStep(approvalStepId,
				memberId, STEP2)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_SECOND_APPROVAL_STEP));
		return ApprovalStepMapper.fromSecondStepEntity(findApprovalStep);
	}

	@Transactional
	public void approveFirstStep(Long approvalStepId, Long memberId) {
		ApprovalStep firstStepApproval = approvalStepRepository.findByIdAndMember_IdAndStep(approvalStepId,
				memberId, STEP1)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_FIRST_APPROVAL_STEP));

		firstStepApproval.validateApprovable();

		ApprovalStep secondStepApproval = approvalStepRepository.findByVacationRequest_IdAndStep(
				firstStepApproval.getVacationRequestId(), STEP2)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_SECOND_APPROVAL_STEP));

		firstStepApproval.approve();
		secondStepApproval.pending();

	}

	@Transactional
	public void rejectFirstStep(Long approvalStepId, Long memberId, ApprovalStepRejectRequest request) {
		ApprovalStep firstStepApproval = approvalStepRepository.findByIdAndMember_IdAndStep(approvalStepId,
				memberId, STEP1)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_FIRST_APPROVAL_STEP));

		firstStepApproval.validateRejectable();

		ApprovalStep secondStepApproval = approvalStepRepository.findByVacationRequest_IdAndStep(
				firstStepApproval.getVacationRequestId(), STEP2)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_SECOND_APPROVAL_STEP));

		firstStepApproval.reject(request.reason());
		secondStepApproval.reject();
		firstStepApproval.rejectVacation();

	}

	@Transactional
	public boolean approveSecondStep(Long approvalStepId, Long memberId) {
		ApprovalStep findApprovalStep = approvalStepRepository.findByIdAndMember_IdAndStep(approvalStepId,
				memberId, STEP2)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_SECOND_APPROVAL_STEP));

		findApprovalStep.validateApprovable();

		VacationInfo findVacationInfo = vacationInfoRepository.findByMemberIdAndVacationType(
				findApprovalStep.getVacationMemberId(),
				findApprovalStep.isHalfDay() ? "01" : findApprovalStep.getVacationCode())
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_VACATION_INFO));

		double count = findApprovalStep.isHalfDay() ? 0.5 : findApprovalStep.calcVacationDays();
		if (findVacationInfo.canUseVacation(count)) {
			findApprovalStep.approve();
			findApprovalStep.approveVacation();
			VacationInfoLog log = findVacationInfo.useVacation(count);
			vacationInfoLogPublisher.publish(log);
			return true;
		} else {
			findApprovalStep.cancel();
			findApprovalStep.cancelVacation();
			return false;
		}

	}

	@Transactional
	public void rejectSecondStep(Long approvalStepId, Long memberId, ApprovalStepRejectRequest request) {
		ApprovalStep findApprovalStep = approvalStepRepository.findByIdAndMember_IdAndStep(approvalStepId,
				memberId, STEP2)
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_SECOND_APPROVAL_STEP));

		findApprovalStep.validateRejectable();
		findApprovalStep.reject(request.reason());
		findApprovalStep.rejectVacation();

	}

	// 휴가 신청 시 호출되어, 해당 멤버의 결재 단계 생성
	public void saveApprovalStep(Member firstApprover, VacationRequest vacationRequest) {
		// todo: 2차 결재자 지정 기능 (시스템상 구현 필요)
		Dept findDept = deptRepository.findByDeptName("인사팀")
			.orElseThrow(() -> new NotFoundException(NotFoundErrorCode.NOT_FOUND_DEPT));
		approvalStepRepository.save(ApprovalStepMapper.toEntity(firstApprover, vacationRequest, STEP1));
		approvalStepRepository.save(ApprovalStepMapper.toEntity(findDept.getDeptLeader(), vacationRequest, STEP2));
	}

	// 휴가 요청 취소될 경우, 관련 결재 단계 상태 CANCELED
	public void cancelApprovalStep(Long vacationStepId) {
		List<ApprovalStep> findApprovalSteps = approvalStepRepository.findByVacationRequest_Id(vacationStepId);
		for (ApprovalStep findApprovalStep : findApprovalSteps) {
			findApprovalStep.cancel();
		}
	}

}
