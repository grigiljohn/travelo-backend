# Profile Screen Complete Implementation

## Backend Changes Completed ✅

### 1. User Entity & DTOs Updated
- ✅ Added `coverPhotoUrl` field to User entity
- ✅ Added `coverPhotoUrl` to UserProfileDto
- ✅ Added `likesAndSavesCount` to UserProfileDto
- ✅ Added `draftCount` to UserProfileDto
- ✅ Added `ipAddress` to UserProfileDto
- ✅ Added `username` to UpdateUserProfileRequest
- ✅ Added `coverPhotoUrl` to UpdateUserProfileRequest

### 2. New Endpoints Added
- ✅ `GET /api/v1/users/suggestions` - Get suggested users
- ✅ `GET /api/v1/users/{userId}/stats` - Get user stats (draft count, likes & saves, IP)

### 3. Service Implementation
- ✅ `getSuggestedUsers()` - Returns users not being followed
- ✅ `getUserStats()` - Returns draft count, likes & saves count, IP address
- ✅ Updated `getUserProfile()` to include new fields
- ✅ Updated `updateUserProfile()` to handle username and cover photo

### 4. Repository Updates
- ✅ Added `findByFollowerId()` to FollowRepository

## Frontend Changes Needed

### 1. Update UserProfileModel
Add these fields to `lib/features/users/data/models/user_profile_model.dart`:
```dart
@JsonKey(name: 'cover_photo_url')
final String? coverPhotoUrl;
@JsonKey(name: 'likes_and_saves_count')
final int? likesAndSavesCount;
@JsonKey(name: 'draft_count')
final int? draftCount;
@JsonKey(name: 'ip_address')
final String? ipAddress;
```

Then run: `flutter pub run build_runner build --delete-conflicting-outputs`

### 2. Update modern_profile_page.dart

#### Fetch Suggested Users
Replace the mock data in `_buildSuggestedUsersSection` with:
```dart
Future<List<Map<String, dynamic>>> _fetchSuggestedUsers() async {
  try {
    final token = await SecureStorageService.getUserToken();
    final response = await _dio.get(
      ApiEndpoints.suggestedUsers(10),
      options: Options(headers: {
        'Authorization': 'Bearer $token',
      }),
    );
    if (response.statusCode == 200) {
      final data = response.data is List ? response.data : [];
      return (data as List).map((user) => {
        'id': user['id'],
        'name': user['name'] ?? user['username'],
        'role': user['role'] ?? 'User',
        'avatar': user['profile_picture_url'],
        'isFollowing': user['is_following'] ?? false,
      }).toList();
    }
  } catch (e) {
    AppLogger.error('Error fetching suggested users', e);
  }
  return [];
}
```

#### Use Real Data for Stats
Update the stats section to use:
```dart
_buildStatItem(
  '${_userProfile!.followingCount ?? 0}',
  'Following',
  theme,
  isDark,
),
_buildStatItem(
  '${_userProfile!.followersCount ?? 0}',
  'Followers',
  theme,
  isDark,
),
_buildStatItem(
  '${_userProfile!.likesAndSavesCount ?? 0}',
  'Likes & Saves',
  theme,
  isDark,
),
```

#### Use Real Draft Count
Replace `_draftCount = 4` with:
```dart
int _draftCount = _userProfile?.draftCount ?? 0;
```

#### Use Real IP Address
Replace hardcoded "India" with:
```dart
_userProfile!.ipAddress ?? 'Unknown'
```

#### Use Cover Photo
Update the cover photo section to use:
```dart
backgroundImage: _userProfile!.coverPhotoUrl != null && 
    _userProfile!.coverPhotoUrl!.isNotEmpty
    ? NetworkImage(_userProfile!.coverPhotoUrl!)
    : null,
```

### 3. Add Image Upload Functionality

Create a new service file `lib/features/profile/data/services/profile_image_service.dart`:
```dart
import 'package:dio/dio.dart';
import 'package:image_picker/image_picker.dart';
import '../../../../core/config/api_endpoints.dart';
import '../../../../core/services/secure_storage_service.dart';
import '../../../../core/utils/logger.dart';

class ProfileImageService {
  final Dio _dio = Dio();
  final ImagePicker _picker = ImagePicker();

  Future<String?> uploadProfilePicture(String userId) async {
    try {
      final XFile? image = await _picker.pickImage(source: ImageSource.gallery);
      if (image == null) return null;

      // Upload to media service first, then update profile
      // Implementation depends on your media upload flow
      // For now, return the local path (you'll need to upload to S3)
      return image.path;
    } catch (e) {
      AppLogger.error('Error uploading profile picture', e);
      return null;
    }
  }

  Future<String?> uploadCoverPhoto(String userId) async {
    try {
      final XFile? image = await _picker.pickImage(source: ImageSource.gallery);
      if (image == null) return null;

      // Upload to media service first, then update profile
      return image.path;
    } catch (e) {
      AppLogger.error('Error uploading cover photo', e);
      return null;
    }
  }

  Future<bool> updateProfilePicture(String userId, String imageUrl) async {
    try {
      final token = await SecureStorageService.getUserToken();
      final response = await _dio.put(
        ApiEndpoints.updateUserProfile(userId),
        data: {'profile_picture_url': imageUrl},
        options: Options(headers: {
          'Authorization': 'Bearer $token',
          'X-User-Id': userId,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      AppLogger.error('Error updating profile picture', e);
      return false;
    }
  }

  Future<bool> updateCoverPhoto(String userId, String imageUrl) async {
    try {
      final token = await SecureStorageService.getUserToken();
      final response = await _dio.put(
        ApiEndpoints.updateUserProfile(userId),
        data: {'cover_photo_url': imageUrl},
        options: Options(headers: {
          'Authorization': 'Bearer $token',
          'X-User-Id': userId,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      AppLogger.error('Error updating cover photo', e);
      return false;
    }
  }
}
```

### 4. Add Edit Functionality

#### Edit Bio
Add method to `modern_profile_page.dart`:
```dart
Future<void> _editBio() async {
  final result = await showDialog<String>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Edit Bio'),
      content: TextField(
        controller: TextEditingController(text: _userProfile?.bio),
        maxLines: 3,
        decoration: const InputDecoration(hintText: 'Enter your bio'),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        TextButton(
          onPressed: () {
            // Get text from TextField and update
            Navigator.pop(context, 'new bio text');
          },
          child: const Text('Save'),
        ),
      ],
    ),
  );

  if (result != null) {
    await _updateProfile({'bio': result});
  }
}
```

#### Edit Username
Add method:
```dart
Future<void> _editUsername() async {
  final result = await showDialog<String>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Edit Username'),
      content: TextField(
        controller: TextEditingController(text: _userProfile?.username),
        decoration: const InputDecoration(hintText: 'Enter username'),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        TextButton(
          onPressed: () {
            Navigator.pop(context, 'new username');
          },
          child: const Text('Save'),
        ),
      ],
    ),
  );

  if (result != null) {
    await _updateProfile({'username': result});
  }
}
```

#### Update Profile Method
```dart
Future<void> _updateProfile(Map<String, dynamic> updates) async {
  try {
    final userId = _userProfile!.id;
    final token = await SecureStorageService.getUserToken();
    final response = await _dio.put(
      ApiEndpoints.updateUserProfile(userId),
      data: updates,
      options: Options(headers: {
        'Authorization': 'Bearer $token',
        'X-User-Id': userId,
        'Content-Type': 'application/json',
      }),
    );

    if (response.statusCode == 200) {
      await _loadUserProfile(); // Reload profile
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Profile updated successfully')),
        );
      }
    }
  } catch (e) {
    AppLogger.error('Error updating profile', e);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to update profile: $e')),
      );
    }
  }
}
```

## Testing Checklist

- [ ] Profile loads with all data
- [ ] Cover photo displays correctly
- [ ] Suggested users load from API
- [ ] Draft count shows correct number
- [ ] Likes & Saves count shows correct number
- [ ] IP address displays (or shows "Unknown")
- [ ] Edit bio works
- [ ] Edit username works
- [ ] Profile picture upload works
- [ ] Cover photo upload works
- [ ] Stats update after actions
- [ ] Dark theme works correctly

## Next Steps

1. Run `flutter pub run build_runner build` to regenerate UserProfileModel
2. Update `modern_profile_page.dart` with the code snippets above
3. Add `image_picker` dependency if not already present
4. Test all functionality
5. Handle image upload to S3/media service (if not already implemented)

## Notes

- IP address detection requires backend integration with IP geolocation service
- Draft count requires integration with post-service
- Likes & saves count requires integration with post-service
- Image uploads need to go through media-service first, then update profile with URL

