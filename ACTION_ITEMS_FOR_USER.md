# Action Items for Data Wipe Preparation

## 🚨 CRITICAL - Must Complete Before Data Wipe

### 1. Remove All Mock/Hardcoded Data (Backend)

**Priority**: 🔴 CRITICAL  
**Estimated Time**: 2-3 days

#### Files to Fix:
1. **`services/post-service/.../BookingController.java`**
   - Remove `getMockBookings()` and `getMockBooking()` methods
   - Either integrate with booking-service or remove endpoint if not needed

2. **`services/post-service/.../DestinationController.java`**
   - Remove `getMockDestinations()` and `getMockDestination()` methods
   - Either integrate with destination-service or remove endpoint if not needed

3. **`services/post-service/.../LocationServiceImpl.java`**
   - Replace `generateDummyLocations()` with Google Places API integration
   - Or remove dummy data and return empty list with proper error message

4. **`services/post-service/.../PostCommentServiceImpl.java`**
   - Remove `getMockComments()` method (lines 99-152)
   - Return empty list instead of mock data when no comments found

5. **`services/post-service/.../PostCommentDto.java`**
   - Remove `getMockUsername()` and `getMockAvatarUrl()` methods
   - Fetch username and avatar from user-service instead

6. **`services/post-service/.../MockDataInitializationService.java`**
   - Remove or disable this service in production
   - Add `@Profile("dev")` annotation to only run in development

#### Quick Fix Commands:
```bash
# Search for all mock data methods
grep -r "getMock\|mock\|dummy" services/post-service/src/main/java/

# Review and remove each instance
```

---

### 2. Add GlobalExceptionHandler to Missing Services

**Priority**: 🔴 CRITICAL  
**Estimated Time**: 1 day

#### Services Needing Exception Handlers:
1. **search-service** - Create `GlobalExceptionHandler.java`
2. **user-service** - Create `GlobalExceptionHandler.java` (currently only has controller-level)
3. **feed-service** - Create `GlobalExceptionHandler.java`
4. **messaging-service** - Create `GlobalExceptionHandler.java`
5. **notification-service** - Create `GlobalExceptionHandler.java`
6. **story-service** - Create `GlobalExceptionHandler.java`
7. **websocket-service** - Create `GlobalExceptionHandler.java`
8. **analytics-service** - Create `GlobalExceptionHandler.java`
9. **admin-service** - Create `GlobalExceptionHandler.java`

#### Template to Use:
Reference `services/reel-service/.../GlobalExceptionHandler.java` (just created) or `services/post-service/.../GlobalExceptionHandler.java`

#### Quick Fix:
```bash
# Copy template to each service
cp services/reel-service/src/main/java/com/travelo/reelservice/exception/GlobalExceptionHandler.java \
   services/search-service/src/main/java/com/travelo/searchservice/exception/GlobalExceptionHandler.java

# Then customize for each service's specific exceptions
```

---

### 3. Remove Hardcoded Data from Frontend

**Priority**: 🔴 CRITICAL  
**Estimated Time**: 2-3 days

#### Files to Fix:
1. **`lib/features/feed/data/repositories/feed_repository_production.dart`**
   - Line 756: Replace `'current_user'` with actual user ID from auth
   - Line 757: Replace `'You'` with actual username
   - Line 758: Replace empty string with actual avatar URL
   - Lines 851-893: Remove mock server response fallback

2. **`lib/features/profile/presentation/pages/profile_page.dart`**
   - Replace hardcoded "John Doe", email, and stats
   - Fetch from user-service API

3. **`lib/features/posts/presentation/pages/music_selection_screen.dart`**
   - Remove `_generateMockWaveform()` method
   - Either get waveform from backend or remove feature

4. **`lib/features/saved/presentation/pages/saved_posts_page.dart`**
   - Remove mock saved posts fallback
   - Show proper empty state instead

5. **`lib/features/posts/presentation/pages/ai_enhanced_post_screen.dart`**
   - Remove hardcoded template fallback
   - Improve error handling instead

6. **`lib/features/posts/presentation/pages/widgets/mention_hashtag_autocomplete.dart`**
   - Replace mock hashtag suggestions with API call

---

### 4. Complete Critical Service Integrations

**Priority**: 🔴 CRITICAL  
**Estimated Time**: 3-5 days

#### Integrations Needed:

1. **User Service → Post Service**
   - Fetch post count for user profiles
   - Fetch user verification status
   - Fetch username and avatar for comments

2. **User Service → Search Service**
   - Fetch user privacy status
   - Fetch blocked users list
   - Fetch following users list

3. **Post Service → User Service**
   - Replace mock usernames/avatars with actual API calls
   - Get user verification status

4. **Location Service → Google Places API**
   - Replace dummy location data with real API calls
   - Or remove feature if not ready

---

## 🟡 HIGH PRIORITY - Should Complete Soon

### 5. Complete TODO Features

**Priority**: 🟡 HIGH  
**Estimated Time**: 1-2 weeks

#### Backend TODOs:
- Comment likes tracking (PostCommentServiceImpl)
- Notification sending (PostCommentServiceImpl)
- Analytics tracking (PostCommentServiceImpl)
- View tracking for reels (ReelController)
- Reaction storage for messages (MessagingServiceImpl)
- User search integration (MessagingServiceImpl)

#### Frontend TODOs:
- Profile edit functionality
- Settings screens navigation
- Comments on reels
- Share functionality
- Filter dialogs
- Booking modification

---

## 🟢 MEDIUM PRIORITY - Can Complete Later

### 6. Advanced Features

**Priority**: 🟢 MEDIUM  
**Estimated Time**: 2-4 weeks

- Video transcoding (Media Service)
- Thumbnail generation (Media Service)
- EXIF stripping (Media Service)
- Face detection (Media Service)
- AI story timeline generation
- Draft scheduling
- Crop/Trim UI
- Music preview player

---

## 📋 Pre-Data Wipe Checklist

Before wiping data, ensure:

### Backend:
- [ ] All mock data removed from controllers
- [ ] All dummy data removed from services
- [ ] All services have GlobalExceptionHandler
- [ ] Error responses are consistent
- [ ] Critical integrations are complete
- [ ] Tests pass without mock data

### Frontend:
- [ ] All hardcoded user IDs removed
- [ ] All hardcoded usernames removed
- [ ] All mock data fallbacks removed
- [ ] Error handling shows user-friendly messages
- [ ] Empty states are properly implemented

### Integration:
- [ ] User-service integrations complete
- [ ] Post-service integrations complete
- [ ] Search-service integrations complete
- [ ] Location service either integrated or removed

### Testing:
- [ ] Unit tests updated (no mock data dependencies)
- [ ] Integration tests pass
- [ ] End-to-end tests pass
- [ ] Error scenarios tested

---

## 🎯 Recommended Approach

### Phase 1: Critical Cleanup (Week 1)
1. Remove all mock/hardcoded data (Backend + Frontend)
2. Add GlobalExceptionHandler to all services
3. Complete critical service integrations

### Phase 2: Feature Completion (Week 2-3)
1. Complete high-priority TODOs
2. Implement missing navigation
3. Improve error handling

### Phase 3: Advanced Features (Week 4+)
1. Media processing features
2. Advanced UI features
3. External service integrations

---

## 📞 Questions to Resolve

1. **Booking/Destination Services**: Do these need to be implemented, or can the endpoints be removed?
2. **Google Places API**: Do we have API key? Should we integrate now or remove feature?
3. **Media Processing**: Are video transcoding, face detection, etc. required for MVP?
4. **Mock Data Fallbacks**: Should we remove all fallbacks or keep some for graceful degradation?

---

## 📊 Progress Tracking

Create a tracking document with:
- [ ] Task list with checkboxes
- [ ] Assignee for each task
- [ ] Estimated completion date
- [ ] Status (Not Started / In Progress / Completed / Blocked)

---

## 🚀 Next Steps

1. **Review this document** with the team
2. **Prioritize** based on business needs
3. **Assign tasks** to team members
4. **Set deadlines** for critical items
5. **Start with Phase 1** (Critical Cleanup)
6. **Test thoroughly** before data wipe
7. **Document** any decisions made during cleanup

---

**Remember**: It's better to remove incomplete features than to leave mock data that will confuse users or break in production!

