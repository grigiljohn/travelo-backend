package com.travelo.admin.predefined;

import com.travelo.admin.api.catalog.PublicPredefinedTripDto;
import com.travelo.admin.domain.PredefinedTrip;
import com.travelo.admin.dto.PredefinedTripRequest;
import com.travelo.admin.repository.PredefinedTripRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredefinedTripService {
    private final PredefinedTripRepository repository;

    public PredefinedTripService(PredefinedTripRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PublicPredefinedTripDto> listActive() {
        return repository.findByActiveIsTrueOrderBySortOrderAsc().stream()
                .map(this::toPublic)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.Optional<PublicPredefinedTripDto> getActiveById(long id) {
        return repository.findByIdAndActiveIsTrue(id).map(this::toPublic);
    }

    @Transactional(readOnly = true)
    public Page<PredefinedTrip> page(String q, Pageable pageable) {
        String term = (q == null) ? "" : q.trim();
        if (term.isEmpty()) {
            return repository.findAllByOrderBySortOrderAscIdDesc(pageable);
        }
        return repository.search(term, pageable);
    }

    @Transactional
    public PredefinedTrip create(PredefinedTripRequest r) {
        PredefinedTrip e = new PredefinedTrip();
        copy(r, e);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return repository.save(e);
    }

    @Transactional
    public PredefinedTrip update(long id, PredefinedTripRequest r) {
        PredefinedTrip e = repository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Not found"));
        copy(r, e);
        e.setUpdatedAt(OffsetDateTime.now());
        return repository.save(e);
    }

    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "not found");
        }
        repository.deleteById(id);
    }

    private void copy(PredefinedTripRequest r, PredefinedTrip e) {
        e.setSlug(r.slug().trim());
        e.setTitle(r.title().trim());
        e.setSubtitle(r.subtitle() == null ? "" : r.subtitle().trim());
        e.setHeroImageUrl(r.heroImageUrl() == null ? "" : r.heroImageUrl().trim());
        e.setSortOrder(r.sortOrder());
        e.setActive(r.active());
        e.setEstimatedDays(r.estimatedDays());
        e.setTripPreferences(r.tripPreferences() == null ? Map.of() : Map.copyOf(r.tripPreferences()));
    }

    private PublicPredefinedTripDto toPublic(PredefinedTrip p) {
        return new PublicPredefinedTripDto(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getSubtitle(),
                p.getHeroImageUrl(),
                p.getEstimatedDays(),
                p.getTripPreferences() == null ? Map.of() : Map.copyOf(p.getTripPreferences())
        );
    }
}
