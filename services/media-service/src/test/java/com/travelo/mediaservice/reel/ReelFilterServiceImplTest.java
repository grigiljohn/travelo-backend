package com.travelo.mediaservice.reel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReelFilterServiceImplTest {

    private final ReelFilterServiceImpl service = new ReelFilterServiceImpl();

    @Test
    void noneReturnsEmptyFilter() {
        assertTrue(service.buildVideoFilter(ReelFilterType.NONE).isBlank());
        assertTrue(service.buildVideoFilter(null).isBlank());
    }

    @Test
    void allCreativePresetsReturnNonEmptyFilterGraphs() {
        for (ReelFilterType type : ReelFilterType.values()) {
            if (type == ReelFilterType.NONE) {
                continue;
            }
            assertFalse(service.buildVideoFilter(type).isBlank(), "Expected non-empty filter graph for " + type);
        }
    }
}
