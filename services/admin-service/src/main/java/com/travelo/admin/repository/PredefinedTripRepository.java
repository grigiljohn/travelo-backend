package com.travelo.admin.repository;

import com.travelo.admin.domain.PredefinedTrip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PredefinedTripRepository extends JpaRepository<PredefinedTrip, Long> {

    Page<PredefinedTrip> findAllByOrderBySortOrderAscIdDesc(Pageable pageable);

    List<PredefinedTrip> findByActiveIsTrueOrderBySortOrderAsc();

    @Query("""
            select p from PredefinedTrip p
            where (:q is null or :q = ''
                or lower(p.title) like lower(concat('%', :q, '%'))
                or lower(p.slug) like lower(concat('%', :q, '%')))
            order by p.sortOrder asc, p.id desc
            """)
    Page<PredefinedTrip> search(@Param("q") String q, Pageable pageable);

    Optional<PredefinedTrip> findByIdAndActiveIsTrue(long id);
}
