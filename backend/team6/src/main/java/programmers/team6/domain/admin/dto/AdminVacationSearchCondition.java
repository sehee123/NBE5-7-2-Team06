package programmers.team6.domain.admin.dto;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import programmers.team6.domain.admin.enums.Quarter;
import programmers.team6.domain.vacation.enums.VacationRequestStatus;

public record AdminVacationSearchCondition(
	@Valid DateRangeCondition dateRange,
	@Valid ApplicantCondition applicant,
	// 휴가 신청 상태
	VacationRequestStatus vacationRequestStatus
) {
	public AdminVacationSearchCondition {
		if (dateRange == null) {
			dateRange = new DateRangeCondition(null, null, null, null);
		}
		if (applicant == null) {
			applicant = new ApplicantCondition(null, null, null, null);
		}
	}

	public record DateRangeCondition(
		// 시작일
		@FutureOrPresent
		LocalDateTime start,
		// 종료일
		@FutureOrPresent
		LocalDateTime end,
		// 년도
		@Min(2000)
		@Max(2100)
		Integer year,
		// 분기
		Quarter quarter
	) {
		public DateRangeCondition {
			if (quarter == null) {
				quarter = Quarter.NONE;
			}
		}
	}

	// 휴가 신청자
	public record ApplicantCondition(
		// 이름
		@Size(max = 30)
		String name,
		// 부서명
		@Size(max = 50)
		String deptName,
		// 직책 codeId
		@Positive
		Long positionCodeId,
		// 휴가 종류 codeId
		@Positive
		Long vacationTypeCodeId
	) {
	}
}
