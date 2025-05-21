package programmers.team6.domain.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import programmers.team6.domain.admin.dto.VacationStatisticsRequest;
import programmers.team6.domain.member.repository.MemberRepository;

@Component
@RequiredArgsConstructor
public class MemberReader {

	private final MemberRepository memberRepository;

	public Members readHasVacationInfoMemberFrom(VacationStatisticsRequest request, Pageable pageable) {
		LocalDateTime date = LocalDateTime.of(request.year(), 12, 31, 23, 59);
		if (request.name() == null) {
			return new Members(
				memberRepository.findAllHasVacationInfoTargetYear(date, request.vacationCode(), pageable));
		}
		return new Members(
			memberRepository.findAllHasVacationInfoTargetYear(date,  request.vacationCode(), request.name(), pageable));
	}

}
