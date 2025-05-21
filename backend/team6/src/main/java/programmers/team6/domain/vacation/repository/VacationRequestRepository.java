package programmers.team6.domain.vacation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import programmers.team6.domain.admin.dto.VacationRequestDetailReadResponse;
import programmers.team6.domain.vacation.entity.VacationRequest;

public interface VacationRequestRepository extends JpaRepository<VacationRequest, Long> {
	@Query("SELECT vr.id FROM VacationRequest vr WHERE vr.member.id = :memberId ORDER BY vr.createdAt DESC")
	Page<Long> findIdsByRequesterIdPaging(@Param("memberId") Long memberId, Pageable pageable);

	@Query("SELECT vr FROM VacationRequest vr JOIN FETCH vr.type JOIN FETCH vr.member WHERE vr.id IN :ids ORDER BY vr.createdAt DESC")
	List<VacationRequest> findByIdsWithFetch(@Param("ids") List<Long> ids);

	@Query(value =
		"select new programmers.team6.domain.admin.dto.VacationRequestDetailReadResponse(vr.id,vr.from, vr.to, m.name, d.deptName,p.name,vr.reason,t.name,vr.status) "
			+ "from VacationRequest vr join vr.type t " + "join vr.member m join m.dept d join m.position p "
			+ "where vr.id = :id")
	Optional<VacationRequestDetailReadResponse> findVacationRequestDetailById(@Param("id") Long id);

	Optional<VacationRequest> findVacationRequestById(Long id);

	@Query("""
		select vr 
		from VacationRequest vr 
		where (vr.type.code in :codes)
				and vr.status = 'APPROVED' 
				and vr.member.id in :ids 
				and (year(vr.from) = :year or year(vr.to) = :year)
		""")
	List<VacationRequest> findByMemberIdInAndYear(List<Long> ids, Integer year, List<String> codes);
}
