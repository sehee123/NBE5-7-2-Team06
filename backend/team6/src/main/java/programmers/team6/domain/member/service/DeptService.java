package programmers.team6.domain.member.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import programmers.team6.domain.member.dto.DeptDropdownResponse;
import programmers.team6.domain.member.repository.DeptRepository;

@Service
@RequiredArgsConstructor
public class DeptService {

	private final DeptRepository deptRepository;

	public List<DeptDropdownResponse> findAllDept() {
		return deptRepository.findAllDept();
	}

}
