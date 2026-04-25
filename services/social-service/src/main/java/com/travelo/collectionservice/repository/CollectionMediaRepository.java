package com.travelo.collectionservice.repository;

import com.travelo.collectionservice.entity.Collection;
import com.travelo.collectionservice.entity.CollectionMedia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CollectionMediaRepository extends JpaRepository<CollectionMedia, UUID> {
    Page<CollectionMedia> findByCollectionOrderByCapturedAtDescCreatedAtDesc(Collection collection, Pageable pageable);
    List<CollectionMedia> findTop20ByCollectionOrderByCreatedAtDesc(Collection collection);

    @Query("select m.collection.id, count(m.id) from CollectionMedia m where m.collection.id in :collectionIds group by m.collection.id")
    List<Object[]> countByCollectionIds(@Param("collectionIds") java.util.Collection<UUID> collectionIds);
}
