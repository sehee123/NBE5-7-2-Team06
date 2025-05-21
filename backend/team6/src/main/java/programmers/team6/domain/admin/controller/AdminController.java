package programmers.team6.domain.admin.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import programmers.team6.domain.admin.dto.AdminVacationRequestSearchResponse;
import programmers.team6.domain.admin.dto.AdminVacationSearchCondition;
import programmers.team6.domain.admin.dto.VacationRequestDetailReadResponse;
import programmers.team6.domain.admin.dto.VacationRequestDetailUpdateRequest;
import programmers.team6.domain.admin.service.AdminService;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	private final AdminService adminService;

	@GetMapping("/vacation-request")
	@ResponseStatus(HttpStatus.OK)
	AdminVacationRequestSearchResponse selectVacationRequests(
		@PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
		@ModelAttribute @Valid AdminVacationSearchCondition searchCondition) {
		return adminService.search(pageable, searchCondition);
	}

	@GetMapping("/vacation-request/{id}")
	@ResponseStatus(HttpStatus.OK)
	VacationRequestDetailReadResponse showVacationRequestDetail(@PathVariable Long id) {
		return adminService.selectVacationRequestDetailById(id);
	}

	@PutMapping("/vacation-request/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void updateVacationRequestDetail(@PathVariable Long id,
		@RequestBody @Valid VacationRequestDetailUpdateRequest vacationRequestDetailUpdateRequest) {
		adminService.updateVacationRequestDetailById(id, vacationRequestDetailUpdateRequest);
	}
}
