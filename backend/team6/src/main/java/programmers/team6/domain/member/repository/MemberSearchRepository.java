package programmers.team6.domain.member.repository;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import programmers.team6.domain.admin.utils.CriteriaCustomPredicateBuilder;
import programmers.team6.domain.admin.utils.CriteriaCustomQueryBuilder;
import programmers.team6.domain.admin.utils.QueryUtils;
import programmers.team6.domain.member.entity.Member;
import programmers.team6.domain.member.entity.Member_;
import programmers.team6.domain.member.enums.Role;

@Repository
@RequiredArgsConstructor
public class MemberSearchRepository {

	private final EntityManager entityManager;

	public Page<Member> searchFrom(String name, Pageable pageable) {
		TypedQuery<Member> query = createSearchQueryFrom(name, pageable);
		long count = createSearCountFrom(name);
		return QueryUtils.makeQueryToPageable(query, pageable, count);
	}

	private TypedQuery<Member> createSearchQueryFrom(String name, Pageable pageable) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Member> criteriaQuery = criteriaBuilder.createQuery(Member.class);
		Root<Member> from = criteriaQuery.from(Member.class);

		List<Predicate> predicates = CriteriaCustomPredicateBuilder.<Member>builder(criteriaBuilder)
			.applyLikeFilter(from, name, Member_.name)
			.applyEqualFilter(from, Role.USER, Member_.role)
			.build();

		return CriteriaCustomQueryBuilder.builder(criteriaQuery, criteriaBuilder)
			.applyDynamicPredicates(predicates)
			.orderBy(from, pageable.getSort())
			.createQuery(entityManager)
			.build();
	}

	private long createSearCountFrom(String name) {
		return count(rootBuilder -> rootBuilder.from(Member.class),
			(predicatesBuilder, from) ->
				predicatesBuilder.applyLikeFilter(from, name, Member_.name)
					.applyNonEqualFilter(from, Role.PENDING, Member_.role));
	}

	private <T> long count(Function<CriteriaQuery<Long>, Root<T>> rootBuilder,
		BiConsumer<CriteriaCustomPredicateBuilder<T>, Root<T>> predicatesSetter) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		Root<T> from = rootBuilder.apply(criteriaQuery);

		criteriaQuery.select(criteriaBuilder.count(from));

		CriteriaCustomPredicateBuilder<T> builder = CriteriaCustomPredicateBuilder.builder(criteriaBuilder);
		predicatesSetter.accept(builder, from);

		TypedQuery<Long> query = CriteriaCustomQueryBuilder.builder(criteriaQuery, criteriaBuilder)
			.applyDynamicPredicates(builder.build())
			.createQuery(entityManager)
			.build();

		return query.getSingleResult();
	}

}
