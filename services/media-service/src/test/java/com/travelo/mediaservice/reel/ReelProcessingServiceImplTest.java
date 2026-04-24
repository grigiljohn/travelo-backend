package com.travelo.mediaservice.reel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReelProcessingServiceImplTest {

    @Test
    void trimLongVideoToThirtySecondsFromStart() {
        ReelProcessingServiceImpl.TrimWindow tw = ReelProcessingServiceImpl.computeTrim(45);
        assertEquals(0.0, tw.startSec(), 0.0001);
        assertEquals(30.0, tw.lenSec(), 0.0001);
    }

    @Test
    void trimMidLengthVideoToCenteredWindow() {
        ReelProcessingServiceImpl.TrimWindow tw = ReelProcessingServiceImpl.computeTrim(18);
        assertEquals(2.0, tw.startSec(), 0.0001);
        assertEquals(14.0, tw.lenSec(), 0.0001);
    }

    @Test
    void keepShortVideoAsIs() {
        ReelProcessingServiceImpl.TrimWindow tw = ReelProcessingServiceImpl.computeTrim(9);
        assertEquals(0.0, tw.startSec(), 0.0001);
        assertEquals(9.0, tw.lenSec(), 0.0001);
    }

    @Test
    void useTinyFallbackForInvalidDuration() {
        ReelProcessingServiceImpl.TrimWindow tw = ReelProcessingServiceImpl.computeTrim(-1);
        assertEquals(0.0, tw.startSec(), 0.0001);
        assertEquals(0.5, tw.lenSec(), 0.0001);
    }
}
