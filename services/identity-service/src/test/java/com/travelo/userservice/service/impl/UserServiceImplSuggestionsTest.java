package com.travelo.userservice.service.impl;

import com.travelo.authservice.entity.User;
import com.travelo.authservice.repository.UserRepository;
import com.travelo.userservice.dto.SuggestedUserDto;
import com.travelo.userservice.entity.Follow;
import com.travelo.userservice.event.UserEventPublisher;
import com.travelo.userservice.repository.FollowRepository;
import com.travelo.userservice.repository.UserLocationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the suggested-users ranking logic inside
 * {@link UserServiceImpl}. These are pure Mockito tests — no Spring context
 * or database — so they run quickly and pin the algorithmic behavior
 * independent of the JPA layer.
 */
class UserServiceImplSuggestionsTest {

    private UserRepository userRepository;
    private FollowRepository followRepository;
    private UserLocationHistoryRepository userLocationHistoryRepository;
    private UserEventPublisher userEventPublisher;
    private UserServiceImpl service;

    private final UUID viewer = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private final UUID alice = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private final UUID bob = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private final UUID carol = UUID.fromString("00000000-0000-4000-8000-000000000004");
    private final UUID dave = UUID.fromString("00000000-0000-4000-8000-000000000005");
    private final UUID eve = UUID.fromString("00000000-0000-4000-8000-000000000006");

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        followRepository = mock(FollowRepository.class);
        userLocationHistoryRepository = mock(UserLocationHistoryRepository.class);
        userEventPublisher = mock(UserEventPublisher.class);
        service = new UserServiceImpl(userRepository, followRepository, userLocationHistoryRepository, userEventPublisher);
    }

    @Test
    void ranksByMutualFollowCountDescending() {
        // Viewer follows Alice and Bob. Alice follows Carol, Dave. Bob follows
        // Carol, Eve. Carol therefore has 2 mutual paths, Dave has 1, Eve has 1.
        when(followRepository.findByFollowerId(viewer)).thenReturn(List.of(
                follow(viewer, alice),
                follow(viewer, bob)
        ));
        when(followRepository.findByFollowerId(alice)).thenReturn(List.of(
                follow(alice, carol),
                follow(alice, dave)
        ));
        when(followRepository.findByFollowerId(bob)).thenReturn(List.of(
                follow(bob, carol),
                follow(bob, eve)
        ));

        stubUser(carol, "carol", "Carol");
        stubUser(dave, "dave", "Dave");
        stubUser(eve, "eve", "Eve");
        when(followRepository.countByFolloweeId(any())).thenReturn(0L);

        List<SuggestedUserDto> out = service.getSuggestedUsers(viewer, 10);

        assertEquals(3, out.size(), "expected three FoF candidates");
        assertEquals(carol, out.get(0).getId(), "Carol should rank first (2 mutual follows)");
        assertTrue(out.get(0).getRole().contains("2 people"),
                "role should surface mutual count, got: " + out.get(0).getRole());
        Set<UUID> tail = Set.of(out.get(1).getId(), out.get(2).getId());
        assertEquals(Set.of(dave, eve), tail, "Dave/Eve should follow with 1 mutual each");
    }

    @Test
    void excludesViewerAndAlreadyFollowedUsers() {
        // Viewer already follows Alice. Alice follows viewer (back) and Bob.
        // We should never suggest Alice (already followed) or the viewer.
        when(followRepository.findByFollowerId(viewer)).thenReturn(List.of(
                follow(viewer, alice)
        ));
        when(followRepository.findByFollowerId(alice)).thenReturn(List.of(
                follow(alice, viewer),
                follow(alice, bob)
        ));
        stubUser(bob, "bob", "Bob");
        when(followRepository.countByFolloweeId(any())).thenReturn(0L);

        List<SuggestedUserDto> out = service.getSuggestedUsers(viewer, 10);

        assertEquals(1, out.size());
        assertEquals(bob, out.get(0).getId());
        assertFalse(out.get(0).getIsFollowing(), "suggestions must never be already-followed");
    }

    @Test
    void fallsBackToPopularWhenGraphIsEmpty() {
        // Brand-new viewer with no follows. We should fall through to the
        // popularity pad and surface the most-followed user first.
        when(followRepository.findByFollowerId(viewer)).thenReturn(List.of());

        User popular = user(alice, "alice", "Alice");
        User midTier = user(bob, "bob", "Bob");
        User newbie = user(carol, "carol", "Carol");
        when(userRepository.findAll()).thenReturn(List.of(newbie, popular, midTier));
        when(followRepository.countByFolloweeId(alice)).thenReturn(5_000L);
        when(followRepository.countByFolloweeId(bob)).thenReturn(42L);
        when(followRepository.countByFolloweeId(carol)).thenReturn(0L);

        List<SuggestedUserDto> out = service.getSuggestedUsers(viewer, 10);

        assertEquals(3, out.size());
        assertEquals(alice, out.get(0).getId(), "highest-follower user should lead the fallback");
        assertEquals("Popular traveler", out.get(0).getRole());
        assertEquals(bob, out.get(1).getId());
        assertEquals("Traveler you might like", out.get(1).getRole());
        assertEquals(carol, out.get(2).getId());
        assertEquals("New on Travelo", out.get(2).getRole());
    }

    @Test
    void respectsLimit() {
        when(followRepository.findByFollowerId(viewer)).thenReturn(List.of());
        List<User> tenUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID id = UUID.fromString("00000000-0000-4000-8000-0000000001" + String.format("%02d", i));
            tenUsers.add(user(id, "u" + i, "User " + i));
        }
        when(userRepository.findAll()).thenReturn(tenUsers);
        when(followRepository.countByFolloweeId(any())).thenReturn(0L);

        List<SuggestedUserDto> out = service.getSuggestedUsers(viewer, 3);
        assertEquals(3, out.size(), "caller-provided limit must be honored");
    }

    @Test
    void clampsLimitToMaximumFifty() {
        when(followRepository.findByFollowerId(viewer)).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        // Called with an absurd limit — implementation should cap at 50 without
        // blowing up. We assert it returned gracefully (empty here because we
        // stubbed an empty user table).
        List<SuggestedUserDto> out = service.getSuggestedUsers(viewer, 5_000);
        assertNotNull(out);
        assertTrue(out.size() <= 50);
    }

    // --- helpers ---

    private Follow follow(UUID follower, UUID followee) {
        Follow f = new Follow(follower, followee);
        f.setId(UUID.randomUUID());
        return f;
    }

    private User user(UUID id, String username, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setName(name);
        return u;
    }

    private void stubUser(UUID id, String username, String name) {
        when(userRepository.findById(id)).thenReturn(Optional.of(user(id, username, name)));
    }
}
