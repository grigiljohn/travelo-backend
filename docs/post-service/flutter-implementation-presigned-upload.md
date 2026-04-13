# Flutter Implementation - Presigned S3 Upload Flow

Complete Flutter/Dart implementation for the presigned S3 upload flow (Flow 2) as described in the create-post-flow-explanation.md.

## 📦 Required Dependencies

Add these to your `pubspec.yaml`:

```yaml
dependencies:
  dio: ^5.4.0  # HTTP client with progress tracking
  path_provider: ^2.1.1  # For file paths
  image_picker: ^1.0.5  # For selecting images/videos
  uuid: ^4.2.1  # For UUID generation
  flutter_bloc: ^8.1.3  # For state management (optional)
```

---

## 📝 Models/DTOs

### 1. Upload URL Request/Response Models

```dart
// lib/models/upload_url_request.dart
class UploadUrlRequest {
  final String ownerId;
  final String filename;
  final String mimeType;
  final int sizeBytes;
  final String mediaType; // 'image', 'video', 'audio', 'other'
  final bool? resumable;

  UploadUrlRequest({
    required this.ownerId,
    required this.filename,
    required this.mimeType,
    required this.sizeBytes,
    required this.mediaType,
    this.resumable,
  });

  Map<String, dynamic> toJson() => {
    'owner_id': ownerId,
    'filename': filename,
    'mime_type': mimeType,
    'size_bytes': sizeBytes,
    'media_type': mediaType,
    if (resumable != null) 'resumable': resumable,
  };
}

// lib/models/upload_url_response.dart
class UploadUrlResponse {
  final String mediaId;
  final String uploadMethod;
  final int expiresIn;
  final String uploadUrl;
  final String storageKey;
  final String? uploadId;
  final int? partSize;
  final List<String>? presignedPartUrls;

  UploadUrlResponse({
    required this.mediaId,
    required this.uploadMethod,
    required this.expiresIn,
    required this.uploadUrl,
    required this.storageKey,
    this.uploadId,
    this.partSize,
    this.presignedPartUrls,
  });

  factory UploadUrlResponse.fromJson(Map<String, dynamic> json) {
    return UploadUrlResponse(
      mediaId: json['media_id'] as String,
      uploadMethod: json['upload_method'] as String,
      expiresIn: json['expires_in'] as int,
      uploadUrl: json['upload_url'] as String,
      storageKey: json['storage_key'] as String,
      uploadId: json['upload_id'] as String?,
      partSize: json['part_size'] as int?,
      presignedPartUrls: json['presigned_part_urls'] != null
          ? List<String>.from(json['presigned_part_urls'])
          : null,
    );
  }
}
```

### 2. Complete Upload Request/Response Models

```dart
// lib/models/complete_upload_request.dart
class CompleteUploadRequest {
  final String etag;
  final int sizeBytes;

  CompleteUploadRequest({
    required this.etag,
    required this.sizeBytes,
  });

  Map<String, dynamic> toJson() => {
    'etag': etag,
    'size_bytes': sizeBytes,
  };
}

// lib/models/complete_upload_response.dart
class CompleteUploadResponse {
  final String mediaId;
  final String status;
  final String? downloadUrl;

  CompleteUploadResponse({
    required this.mediaId,
    required this.status,
    this.downloadUrl,
  });

  factory CompleteUploadResponse.fromJson(Map<String, dynamic> json) {
    return CompleteUploadResponse(
      mediaId: json['media_id'] as String,
      status: json['status'] as String,
      downloadUrl: json['download_url'] as String?,
    );
  }
}
```

### 3. Create Post Request/Response Models

```dart
// lib/models/media_item_request.dart
class MediaItemRequest {
  final String mediaId;
  final String type; // 'image' or 'video'
  final int position;
  final String? thumbnailUrl;
  final int? duration;
  final int? width;
  final int? height;

  MediaItemRequest({
    required this.mediaId,
    required this.type,
    required this.position,
    this.thumbnailUrl,
    this.duration,
    this.width,
    this.height,
  });

  Map<String, dynamic> toJson() => {
    'media_id': mediaId,
    'type': type,
    'position': position,
    if (thumbnailUrl != null) 'thumbnail_url': thumbnailUrl,
    if (duration != null) 'duration': duration,
    if (width != null) 'width': width,
    if (height != null) 'height': height,
  };
}

// lib/models/create_post_request.dart
class CreatePostRequest {
  final String? postType; // 'image', 'video', 'reel', 'text', 'mixed'
  final String? caption;
  final List<String>? tags;
  final String mood; // Required: 'chill', 'love', 'adventure', etc.
  final String? location;
  final List<MediaItemRequest> mediaItems;

  CreatePostRequest({
    this.postType,
    this.caption,
    this.tags,
    required this.mood,
    this.location,
    required this.mediaItems,
  });

  Map<String, dynamic> toJson() => {
    if (postType != null) 'post_type': postType,
    if (caption != null) 'caption': caption,
    if (tags != null) 'tags': tags,
    'mood': mood,
    if (location != null) 'location': location,
    'media_items': mediaItems.map((item) => item.toJson()).toList(),
  };
}
```

### 4. API Response Wrapper

```dart
// lib/models/api_response.dart
class ApiResponse<T> {
  final bool success;
  final String message;
  final T? data;
  final String? error;
  final Map<String, dynamic>? errors;

  ApiResponse({
    required this.success,
    required this.message,
    this.data,
    this.error,
    this.errors,
  });

  factory ApiResponse.fromJson(
    Map<String, dynamic> json,
    T Function(dynamic)? fromJsonT,
  ) {
    return ApiResponse<T>(
      success: json['success'] as bool,
      message: json['message'] as String,
      data: json['data'] != null && fromJsonT != null
          ? fromJsonT(json['data'])
          : json['data'] as T?,
      error: json['error'] as String?,
      errors: json['errors'] as Map<String, dynamic>?,
    );
  }
}
```

---

## 🔧 Service Classes

### 1. Post API Service

```dart
// lib/services/post_api_service.dart
import 'package:dio/dio.dart';
import '../models/api_response.dart';
import '../models/upload_url_request.dart';
import '../models/upload_url_response.dart';
import '../models/complete_upload_request.dart';
import '../models/complete_upload_response.dart';
import '../models/create_post_request.dart';

class PostApiService {
  final Dio _dio;
  final String baseUrl;

  PostApiService({
    required this.baseUrl,
    Dio? dio,
  }) : _dio = dio ?? Dio(BaseOptions(
          baseUrl: baseUrl,
          headers: {
            'Content-Type': 'application/json',
          },
        ));

  // Set authorization token
  void setAuthToken(String token) {
    _dio.options.headers['Authorization'] = 'Bearer $token';
  }

  // Step 2: Request Upload URL
  Future<UploadUrlResponse> requestUploadUrl(UploadUrlRequest request) async {
    try {
      final response = await _dio.post(
        '/api/v1/posts/upload-urls',
        data: request.toJson(),
      );

      final apiResponse = ApiResponse<Map<String, dynamic>>.fromJson(
        response.data,
        (data) => data as Map<String, dynamic>,
      );

      if (!apiResponse.success || apiResponse.data == null) {
        throw Exception(apiResponse.message);
      }

      return UploadUrlResponse.fromJson(apiResponse.data!);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Step 4: Complete Upload
  Future<CompleteUploadResponse> completeUpload(
    String mediaId,
    CompleteUploadRequest request,
  ) async {
    try {
      final response = await _dio.post(
        '/api/v1/posts/upload/complete/$mediaId',
        data: request.toJson(),
      );

      final apiResponse = ApiResponse<Map<String, dynamic>>.fromJson(
        response.data,
        (data) => data as Map<String, dynamic>,
      );

      if (!apiResponse.success || apiResponse.data == null) {
        throw Exception(apiResponse.message);
      }

      return CompleteUploadResponse.fromJson(apiResponse.data!);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Step 5: Create Post
  Future<Map<String, dynamic>> createPost(CreatePostRequest request) async {
    try {
      final response = await _dio.post(
        '/api/v1/posts',
        data: request.toJson(),
      );

      final apiResponse = ApiResponse<Map<String, dynamic>>.fromJson(
        response.data,
        (data) => data as Map<String, dynamic>,
      );

      if (!apiResponse.success || apiResponse.data == null) {
        throw Exception(apiResponse.message);
      }

      return apiResponse.data!;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Exception _handleError(DioException e) {
    if (e.response != null) {
      final data = e.response!.data;
      final message = data['message'] ?? 'Request failed';
      return Exception(message);
    } else {
      return Exception('Network error: ${e.message}');
    }
  }
}
```

### 2. S3 Upload Service

```dart
// lib/services/s3_upload_service.dart
import 'dart:io';
import 'package:dio/dio.dart';

class S3UploadService {
  // Step 3: Upload File to S3 (Using Dio - Recommended)
  Future<String> uploadFileToS3WithDio({
    required File file,
    required String uploadUrl,
    required String contentType,
    required Dio dio,
    Function(int sent, int total)? onProgress,
  }) async {
    try {
      final fileBytes = await file.readAsBytes();
      
      final response = await dio.put(
        uploadUrl,
        data: fileBytes,
        options: Options(
          headers: {
            'Content-Type': contentType,
            'Content-Length': fileBytes.length.toString(),
          },
        ),
        onSendProgress: onProgress != null
            ? (sent, total) => onProgress(sent, total)
            : null,
      );

      if (response.statusCode != 200) {
        throw Exception('S3 upload failed: ${response.statusCode}');
      }

      // Extract ETag from response headers
      final etag = response.headers.value('etag') ?? 
                   response.headers.value('ETag') ?? 
                   response.headers.value('Etag');
      
      if (etag == null) {
        throw Exception('ETag not found in S3 response');
      }

      // Ensure ETag is wrapped in quotes
      return etag.startsWith('"') && etag.endsWith('"') 
          ? etag 
          : '"$etag"';
    } catch (e) {
      throw Exception('Failed to upload file to S3: $e');
    }
  }
}
```

---

## 🎯 Complete Flow Implementation

### Main Upload Service

```dart
// lib/services/post_upload_service.dart
import 'dart:io';
import 'package:dio/dio.dart';
import '../models/upload_url_request.dart';
import '../models/upload_url_response.dart';
import '../models/complete_upload_request.dart';
import '../models/complete_upload_response.dart';
import '../models/create_post_request.dart';
import '../models/media_item_request.dart';
import 'post_api_service.dart';
import 's3_upload_service.dart';

class PostUploadService {
  final PostApiService _apiService;
  final S3UploadService _s3Service;
  final Dio _dio;
  List<File>? _currentFiles; // Store files for post type determination

  PostUploadService({
    required PostApiService apiService,
    required S3UploadService s3Service,
    Dio? dio,
  })  : _apiService = apiService,
        _s3Service = s3Service,
        _dio = dio ?? Dio();

  // Complete flow: Upload files and create post
  Future<Map<String, dynamic>> createPostWithFiles({
    required List<File> files,
    required String ownerId,
    required String mood,
    String? caption,
    String? location,
    List<String>? tags,
    Function(int fileIndex, double progress)? onFileProgress,
    Function(String step)? onStepChange,
  }) async {
    try {
      // Step 1: Request upload URLs for all files
      onStepChange?.call('Preparing upload...');
      final uploadUrls = await _requestUploadUrls(files, ownerId);

      // Step 2: Upload all files to S3 (parallel)
      onStepChange?.call('Uploading files...');
      final uploadResults = await _uploadFilesToS3(
        files: files,
        uploadUrls: uploadUrls,
        onProgress: onFileProgress,
      );

      // Step 3: Complete all uploads
      onStepChange?.call('Processing files...');
      final completedUploads = await _completeUploads(
        uploadResults: uploadResults,
        fileSizes: files.map((f) => f.lengthSync()).toList(),
      );

      // Store files for post type determination
      _currentFiles = files;

      // Step 4: Determine post type
      final postType = _determinePostType(completedUploads);

      // Step 5: Create media items
      final mediaItems = completedUploads
          .asMap()
          .entries
          .map((entry) {
            final index = entry.key;
            final upload = entry.value;
            final file = files[index];
            final mediaType = _getMediaType(file);

            return MediaItemRequest(
              mediaId: upload.mediaId,
              type: mediaType,
              position: index,
            );
          })
          .toList();

      // Step 6: Create post
      onStepChange?.call('Publishing...');
      final createRequest = CreatePostRequest(
        postType: postType,
        caption: caption,
        tags: tags,
        mood: mood,
        location: location,
        mediaItems: mediaItems,
      );

      final post = await _apiService.createPost(createRequest);
      onStepChange?.call('Success!');
      
      return post;
    } catch (e) {
      onStepChange?.call('Error: ${e.toString()}');
      rethrow;
    }
  }

  // Request upload URLs for all files
  Future<List<UploadUrlResponse>> _requestUploadUrls(
    List<File> files,
    String ownerId,
  ) async {
    final requests = files.map((file) {
      final filename = file.path.split('/').last;
      final sizeBytes = file.lengthSync();
      final mimeType = _getMimeType(file);
      final mediaType = _getMediaType(file);

      return _apiService.requestUploadUrl(
        UploadUrlRequest(
          ownerId: ownerId,
          filename: filename,
          mimeType: mimeType,
          sizeBytes: sizeBytes,
          mediaType: mediaType,
          resumable: sizeBytes > 10 * 1024 * 1024, // 10MB threshold
        ),
      );
    });

    return await Future.wait(requests);
  }

  // Upload files to S3 in parallel
  Future<List<UploadResult>> _uploadFilesToS3({
    required List<File> files,
    required List<UploadUrlResponse> uploadUrls,
    Function(int fileIndex, double progress)? onProgress,
  }) async {
    final uploadFutures = files.asMap().entries.map((entry) {
      final index = entry.key;
      final file = entry.value;
      final uploadUrl = uploadUrls[index];
      final contentType = _getMimeType(file);

      return _s3Service.uploadFileToS3WithDio(
        file: file,
        uploadUrl: uploadUrl.uploadUrl,
        contentType: contentType,
        dio: _dio,
        onProgress: onProgress != null
            ? (sent, total) {
                final progress = total > 0 ? sent / total : 0.0;
                onProgress(index, progress);
              }
            : null,
      ).then((etag) => UploadResult(
            mediaId: uploadUrl.mediaId,
            etag: etag,
          ));
    });

    return await Future.wait(uploadFutures);
  }

  // Complete all uploads
  Future<List<CompleteUploadResponse>> _completeUploads({
    required List<UploadResult> uploadResults,
    required List<int> fileSizes,
  }) async {
    final completeFutures = uploadResults.asMap().entries.map((entry) {
      final index = entry.key;
      final result = entry.value;
      final fileSize = fileSizes[index];

      return _apiService.completeUpload(
        result.mediaId,
        CompleteUploadRequest(
          etag: result.etag,
          sizeBytes: fileSize,
        ),
      );
    });

    return await Future.wait(completeFutures);
  }

  // Helper: Determine post type from media items
  String _determinePostType(List<CompleteUploadResponse> uploads) {
    if (_currentFiles == null || _currentFiles!.isEmpty) {
      return 'image'; // Default
    }
    
    // Determine from the files that were uploaded
    final hasImages = _currentFiles!.any((file) => _getMediaType(file) == 'image');
    final hasVideos = _currentFiles!.any((file) => _getMediaType(file) == 'video');
    
    if (hasImages && hasVideos) {
      return 'mixed';
    } else if (hasVideos) {
      return 'video';
    } else {
      return 'image';
    }
  }

  // Helper: Get MIME type from file
  String _getMimeType(File file) {
    final extension = file.path.split('.').last.toLowerCase();
    switch (extension) {
      case 'jpg':
      case 'jpeg':
        return 'image/jpeg';
      case 'png':
        return 'image/png';
      case 'mp4':
        return 'video/mp4';
      case 'mov':
        return 'video/quicktime';
      default:
        return 'application/octet-stream';
    }
  }

  // Helper: Get media type from file
  String _getMediaType(File file) {
    final mimeType = _getMimeType(file);
    if (mimeType.startsWith('video/')) {
      return 'video';
    } else if (mimeType.startsWith('image/')) {
      return 'image';
    } else if (mimeType.startsWith('audio/')) {
      return 'audio';
    }
    return 'other';
  }
}

// Helper class for upload results
class UploadResult {
  final String mediaId;
  final String etag;

  UploadResult({
    required this.mediaId,
    required this.etag,
  });
}
```

---

## 🎨 UI Implementation Example

### Bloc/Cubit for State Management

```dart
// lib/bloc/post_upload_bloc.dart
import 'dart:io';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../services/post_upload_service.dart';

enum UploadStep {
  idle,
  preparing,
  uploading,
  processing,
  publishing,
  success,
  error,
}

class PostUploadState {
  final UploadStep step;
  final double overallProgress;
  final Map<int, double> fileProgress;
  final String? errorMessage;
  final Map<String, dynamic>? createdPost;

  PostUploadState({
    this.step = UploadStep.idle,
    this.overallProgress = 0.0,
    Map<int, double>? fileProgress,
    this.errorMessage,
    this.createdPost,
  }) : fileProgress = fileProgress ?? {};

  PostUploadState copyWith({
    UploadStep? step,
    double? overallProgress,
    Map<int, double>? fileProgress,
    String? errorMessage,
    Map<String, dynamic>? createdPost,
  }) {
    return PostUploadState(
      step: step ?? this.step,
      overallProgress: overallProgress ?? this.overallProgress,
      fileProgress: fileProgress ?? this.fileProgress,
      errorMessage: errorMessage,
      createdPost: createdPost ?? this.createdPost,
    );
  }
}

class PostUploadBloc extends Cubit<PostUploadState> {
  final PostUploadService _uploadService;

  PostUploadBloc(this._uploadService) : super(PostUploadState());

  Future<void> createPost({
    required List<File> files,
    required String ownerId,
    required String mood,
    String? caption,
    String? location,
    List<String>? tags,
  }) async {
    try {
      emit(state.copyWith(step: UploadStep.preparing));

      final post = await _uploadService.createPostWithFiles(
        files: files,
        ownerId: ownerId,
        mood: mood,
        caption: caption,
        location: location,
        tags: tags,
        onFileProgress: (fileIndex, progress) {
          final updatedProgress = Map<int, double>.from(state.fileProgress);
          updatedProgress[fileIndex] = progress;

          // Calculate overall progress
          final overallProgress = updatedProgress.values.isEmpty
              ? 0.0
              : updatedProgress.values.reduce((a, b) => a + b) /
                  updatedProgress.length;

          emit(state.copyWith(
            step: UploadStep.uploading,
            fileProgress: updatedProgress,
            overallProgress: overallProgress,
          ));
        },
        onStepChange: (step) {
          switch (step) {
            case 'Preparing upload...':
              emit(state.copyWith(step: UploadStep.preparing));
              break;
            case 'Uploading files...':
              emit(state.copyWith(step: UploadStep.uploading));
              break;
            case 'Processing files...':
              emit(state.copyWith(step: UploadStep.processing));
              break;
            case 'Publishing...':
              emit(state.copyWith(step: UploadStep.publishing));
              break;
            case 'Success!':
              emit(state.copyWith(step: UploadStep.success));
              break;
            default:
              if (step.startsWith('Error:')) {
                emit(state.copyWith(
                  step: UploadStep.error,
                  errorMessage: step,
                ));
              }
          }
        },
      );

      emit(state.copyWith(
        step: UploadStep.success,
        createdPost: post,
      ));
    } catch (e) {
      emit(state.copyWith(
        step: UploadStep.error,
        errorMessage: e.toString(),
      ));
    }
  }

  void reset() {
    emit(PostUploadState());
  }
}
```

### UI Widget Example

```dart
// lib/widgets/create_post_screen.dart
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';
import '../bloc/post_upload_bloc.dart';

class CreatePostScreen extends StatefulWidget {
  final String ownerId;
  final String authToken;

  const CreatePostScreen({
    Key? key,
    required this.ownerId,
    required this.authToken,
  }) : super(key: key);

  @override
  State<CreatePostScreen> createState() => _CreatePostScreenState();
}

class _CreatePostScreenState extends State<CreatePostScreen> {
  final _captionController = TextEditingController();
  final _locationController = TextEditingController();
  final _tagsController = TextEditingController();
  String _selectedMood = 'adventure';
  List<File> _selectedFiles = [];
  final ImagePicker _picker = ImagePicker();

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => PostUploadBloc(
        // Initialize with your services
        PostUploadService(
          apiService: PostApiService(baseUrl: 'https://api.travelo.com')
            ..setAuthToken(widget.authToken),
          s3Service: S3UploadService(),
        ),
      ),
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Create Post'),
          actions: [
            BlocBuilder<PostUploadBloc, PostUploadState>(
              builder: (context, state) {
                return TextButton(
                  onPressed: state.step == UploadStep.idle ||
                          state.step == UploadStep.success ||
                          state.step == UploadStep.error
                      ? _handlePost
                      : null,
                  child: const Text('Post'),
                );
              },
            ),
          ],
        ),
        body: BlocConsumer<PostUploadBloc, PostUploadState>(
          listener: (context, state) {
            if (state.step == UploadStep.success) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Post created successfully!')),
              );
              Navigator.pop(context, state.createdPost);
            } else if (state.step == UploadStep.error) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('Error: ${state.errorMessage}'),
                  backgroundColor: Colors.red,
                ),
              );
            }
          },
          builder: (context, state) {
            return SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // File selection
                  _buildFileSelector(),
                  
                  // Selected files preview
                  if (_selectedFiles.isNotEmpty) _buildFilePreview(),
                  
                  // Progress indicator
                  if (state.step != UploadStep.idle &&
                      state.step != UploadStep.success &&
                      state.step != UploadStep.error)
                    _buildProgressIndicator(state),
                  
                  const SizedBox(height: 16),
                  
                  // Caption
                  TextField(
                    controller: _captionController,
                    decoration: const InputDecoration(
                      labelText: 'Caption',
                      border: OutlineInputBorder(),
                    ),
                    maxLines: 3,
                  ),
                  
                  const SizedBox(height: 16),
                  
                  // Mood selector
                  DropdownButtonFormField<String>(
                    value: _selectedMood,
                    decoration: const InputDecoration(
                      labelText: 'Mood',
                      border: OutlineInputBorder(),
                    ),
                    items: const [
                      DropdownMenuItem(value: 'chill', child: Text('Chill')),
                      DropdownMenuItem(value: 'love', child: Text('Love')),
                      DropdownMenuItem(
                        value: 'adventure',
                        child: Text('Adventure'),
                      ),
                      DropdownMenuItem(value: 'party', child: Text('Party')),
                      DropdownMenuItem(value: 'nature', child: Text('Nature')),
                      DropdownMenuItem(value: 'food', child: Text('Food')),
                      DropdownMenuItem(
                        value: 'culture',
                        child: Text('Culture'),
                      ),
                      DropdownMenuItem(
                        value: 'romantic',
                        child: Text('Romantic'),
                      ),
                      DropdownMenuItem(
                        value: 'activity',
                        child: Text('Activity'),
                      ),
                      DropdownMenuItem(value: 'relax', child: Text('Relax')),
                    ],
                    onChanged: (value) {
                      if (value != null) {
                        setState(() => _selectedMood = value);
                      }
                    },
                  ),
                  
                  const SizedBox(height: 16),
                  
                  // Location
                  TextField(
                    controller: _locationController,
                    decoration: const InputDecoration(
                      labelText: 'Location (optional)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  
                  const SizedBox(height: 16),
                  
                  // Tags
                  TextField(
                    controller: _tagsController,
                    decoration: const InputDecoration(
                      labelText: 'Tags (comma-separated)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildFileSelector() {
    return ElevatedButton.icon(
      onPressed: _selectFiles,
      icon: const Icon(Icons.add_photo_alternate),
      label: const Text('Select Files'),
    );
  }

  Widget _buildFilePreview() {
    return SizedBox(
      height: 100,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        itemCount: _selectedFiles.length,
        itemBuilder: (context, index) {
          return Padding(
            padding: const EdgeInsets.all(4),
            child: Image.file(
              _selectedFiles[index],
              width: 100,
              height: 100,
              fit: BoxFit.cover,
            ),
          );
        },
      ),
    );
  }

  Widget _buildProgressIndicator(PostUploadState state) {
    String stepText;
    switch (state.step) {
      case UploadStep.preparing:
        stepText = 'Preparing upload...';
        break;
      case UploadStep.uploading:
        stepText = 'Uploading files...';
        break;
      case UploadStep.processing:
        stepText = 'Processing files...';
        break;
      case UploadStep.publishing:
        stepText = 'Publishing...';
        break;
      default:
        stepText = '';
    }

    return Column(
      children: [
        Text(stepText),
        const SizedBox(height: 8),
        LinearProgressIndicator(value: state.overallProgress),
        const SizedBox(height: 8),
        // Individual file progress
        ...state.fileProgress.entries.map((entry) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 2),
            child: Row(
              children: [
                Text('File ${entry.key + 1}:'),
                Expanded(
                  child: LinearProgressIndicator(value: entry.value),
                ),
                Text('${(entry.value * 100).toInt()}%'),
              ],
            ),
          );
        }),
      ],
    );
  }

  Future<void> _selectFiles() async {
    final List<XFile>? images = await _picker.pickMultiImage();
    if (images != null) {
      setState(() {
        _selectedFiles = images.map((xFile) => File(xFile.path)).toList();
      });
    }
  }

  void _handlePost() {
    if (_selectedFiles.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select at least one file')),
      );
      return;
    }

    final tags = _tagsController.text
        .split(',')
        .map((tag) => tag.trim())
        .where((tag) => tag.isNotEmpty)
        .toList();

    context.read<PostUploadBloc>().createPost(
          files: _selectedFiles,
          ownerId: widget.ownerId,
          mood: _selectedMood,
          caption: _captionController.text.isEmpty
              ? null
              : _captionController.text,
          location: _locationController.text.isEmpty
              ? null
              : _locationController.text,
          tags: tags.isEmpty ? null : tags,
        );
  }

  @override
  void dispose() {
    _captionController.dispose();
    _locationController.dispose();
    _tagsController.dispose();
    super.dispose();
  }
}
```

---

## 📋 Usage Example

```dart
// Example usage in your app
void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: CreatePostScreen(
        ownerId: 'user-uuid-here',
        authToken: 'your-jwt-token',
      ),
    );
  }
}
```

---

## ✅ Key Features

1. **Complete Flow Implementation**: All 5 steps from the documentation
2. **Progress Tracking**: Real-time progress for each file and overall
3. **Error Handling**: Comprehensive error handling at each step
4. **Parallel Uploads**: Multiple files upload simultaneously
5. **State Management**: Bloc pattern for clean state management
6. **UI Components**: Ready-to-use widgets with progress indicators

---

## 🔧 Configuration

Update the base URL in `PostApiService`:

```dart
final apiService = PostApiService(
  baseUrl: 'https://your-api-domain.com', // Change this
);
```

Set the authentication token:

```dart
apiService.setAuthToken('your-jwt-token');
```

---

This implementation provides a complete, production-ready Flutter solution for the presigned S3 upload flow!

