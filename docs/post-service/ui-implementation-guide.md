# Post Service - UI Implementation Guide

## Overview

This guide explains how to implement the presigned S3 upload flow in your frontend application. The flow allows users to upload files directly to S3, reducing server load and improving upload speed.

---

## 📋 Table of Contents

1. [User Flow](#user-flow)
2. [Implementation Steps](#implementation-steps)
3. [Code Examples](#code-examples)
4. [UI/UX Best Practices](#uiux-best-practices)
5. [Error Handling](#error-handling)
6. [Progress Tracking](#progress-tracking)
7. [Multiple Files Handling](#multiple-files-handling)

---

## 🎯 User Flow

### Single File Upload Flow

1. **User selects file** → File picker opens
2. **File validation** → Check size, type, etc.
3. **Request upload URL** → Show "Preparing upload..." 
4. **Upload to S3** → Show progress bar
5. **Complete upload** → Show "Processing..."
6. **Create post** → Show "Publishing..."
7. **Success** → Show post preview/confirmation

### Multiple Files Upload Flow

1. **User selects multiple files** → File picker (multiple)
2. **File validation** → Validate all files
3. **Request upload URLs** → Show "Preparing uploads..." (all files)
4. **Upload all to S3** → Show progress for each file
5. **Complete all uploads** → Show "Processing files..."
6. **Create post** → Show "Publishing..."
7. **Success** → Show post with carousel preview

---

## 🚀 Implementation Steps

### Step 1: File Selection & Validation

```typescript
// File selection handler
const handleFileSelect = async (files: FileList) => {
  const selectedFiles = Array.from(files);
  
  // Validate files
  const validationErrors = validateFiles(selectedFiles);
  if (validationErrors.length > 0) {
    showErrors(validationErrors);
    return;
  }
  
  // Store files in state
  setSelectedFiles(selectedFiles);
  setUploadState('ready');
};

// File validation
const validateFiles = (files: File[]): string[] => {
  const errors: string[] = [];
  const maxSize = 100 * 1024 * 1024; // 100MB
  const allowedTypes = ['image/jpeg', 'image/png', 'video/mp4'];
  
  files.forEach((file, index) => {
    if (file.size > maxSize) {
      errors.push(`${file.name} exceeds maximum size of 100MB`);
    }
    if (!allowedTypes.includes(file.type)) {
      errors.push(`${file.name} is not a supported file type`);
    }
  });
  
  return errors;
};
```

### Step 2: Request Upload URLs

```typescript
interface UploadUrlRequest {
  owner_id: string;
  filename: string;
  mime_type: string;
  size_bytes: number;
  media_type: 'image' | 'video' | 'audio' | 'other';
  resumable?: boolean;
}

interface UploadUrlResponse {
  media_id: string;
  upload_method: string;
  expires_in: number;
  upload_url: string;
  storage_key: string;
}

// Request upload URL for a single file
const requestUploadUrl = async (
  file: File,
  ownerId: string
): Promise<UploadUrlResponse> => {
  const mediaType = file.type.startsWith('image/') ? 'image' :
                    file.type.startsWith('video/') ? 'video' :
                    file.type.startsWith('audio/') ? 'audio' : 'other';
  
  const resumable = file.size > 10 * 1024 * 1024; // 10MB threshold
  
  const response = await fetch(`${API_BASE_URL}/api/v1/posts/upload-urls`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    },
    body: JSON.stringify({
      owner_id: ownerId,
      filename: file.name,
      mime_type: file.type,
      size_bytes: file.size,
      media_type: mediaType,
      resumable: resumable
    })
  });
  
  if (!response.ok) {
    throw new Error(`Failed to request upload URL: ${response.statusText}`);
  }
  
  const data = await response.json();
  return data.data;
};

// Request upload URLs for multiple files
const requestUploadUrls = async (
  files: File[],
  ownerId: string
): Promise<UploadUrlResponse[]> => {
  setUploadState('requesting-urls');
  
  // Request URLs in parallel
  const urlPromises = files.map(file => requestUploadUrl(file, ownerId));
  
  try {
    const uploadUrls = await Promise.all(urlPromises);
    return uploadUrls;
  } catch (error) {
    console.error('Error requesting upload URLs:', error);
    throw error;
  }
};
```

### Step 3: Upload Files to S3

```typescript
// Upload single file to S3
const uploadFileToS3 = async (
  file: File,
  uploadUrl: string,
  onProgress?: (progress: number) => void
): Promise<string> => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    
    // Track upload progress
    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable && onProgress) {
        const progress = (e.loaded / e.total) * 100;
        onProgress(progress);
      }
    });
    
    xhr.addEventListener('load', () => {
      if (xhr.status === 200) {
        // Extract ETag from response headers
        const etag = xhr.getResponseHeader('ETag') || '';
        resolve(etag);
      } else {
        reject(new Error(`Upload failed: ${xhr.statusText}`));
      }
    });
    
    xhr.addEventListener('error', () => {
      reject(new Error('Upload failed: Network error'));
    });
    
    xhr.addEventListener('abort', () => {
      reject(new Error('Upload cancelled'));
    });
    
    xhr.open('PUT', uploadUrl);
    xhr.setRequestHeader('Content-Type', file.type);
    xhr.send(file);
  });
};

// Upload multiple files to S3 (parallel)
const uploadFilesToS3 = async (
  files: File[],
  uploadUrls: UploadUrlResponse[],
  onProgress?: (fileIndex: number, progress: number) => void
): Promise<string[]> => {
  setUploadState('uploading');
  
  const uploadPromises = files.map((file, index) => {
    const uploadUrl = uploadUrls[index].upload_url;
    
    return uploadFileToS3(
      file,
      uploadUrl,
      (progress) => onProgress?.(index, progress)
    );
  });
  
  try {
    const etags = await Promise.all(uploadPromises);
    return etags;
  } catch (error) {
    console.error('Error uploading files:', error);
    throw error;
  }
};
```

### Step 4: Complete Uploads

```typescript
interface CompleteUploadRequest {
  etag: string;
  size_bytes: number;
}

interface CompleteUploadResponse {
  media_id: string;
  status: string;
  download_url: string;
}

// Complete single upload
const completeUpload = async (
  mediaId: string,
  etag: string,
  fileSize: number
): Promise<CompleteUploadResponse> => {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/posts/upload/complete/${mediaId}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
      },
      body: JSON.stringify({
        etag: etag,
        size_bytes: fileSize
      })
    }
  );
  
  if (!response.ok) {
    throw new Error(`Failed to complete upload: ${response.statusText}`);
  }
  
  const data = await response.json();
  return data.data;
};

// Complete all uploads
const completeAllUploads = async (
  mediaIds: string[],
  etags: string[],
  fileSizes: number[]
): Promise<CompleteUploadResponse[]> => {
  setUploadState('completing');
  
  const completePromises = mediaIds.map((mediaId, index) => {
    return completeUpload(mediaId, etags[index], fileSizes[index]);
  });
  
  try {
    const results = await Promise.all(completePromises);
    return results;
  } catch (error) {
    console.error('Error completing uploads:', error);
    throw error;
  }
};
```

### Step 5: Create Post

```typescript
interface MediaItem {
  media_id: string;
  type: 'image' | 'video';
  position: number;
  thumbnail_url?: string;
  duration?: number;
  width?: number;
  height?: number;
}

interface CreatePostRequest {
  post_type: 'image' | 'video' | 'reel' | 'text' | 'mixed';
  mood: string;
  caption?: string;
  location?: string;
  tags?: string[];
  media_items: MediaItem[];
}

// Determine post type from media items
const determinePostType = (mediaItems: MediaItem[]): string => {
  const hasImage = mediaItems.some(item => item.type === 'image');
  const hasVideo = mediaItems.some(item => item.type === 'video');
  
  if (hasImage && hasVideo) return 'mixed';
  if (hasVideo) return 'video';
  return 'image';
};

// Create post
const createPost = async (
  mediaItems: MediaItem[],
  caption: string,
  mood: string,
  location?: string,
  tags?: string[]
): Promise<any> => {
  setUploadState('creating-post');
  
  const postType = determinePostType(mediaItems);
  
  const response = await fetch(`${API_BASE_URL}/api/v1/posts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    },
    body: JSON.stringify({
      post_type: postType,
      mood: mood,
      caption: caption,
      location: location,
      tags: tags,
      media_items: mediaItems.map((item, index) => ({
        media_id: item.media_id,
        type: item.type,
        position: index
      }))
    })
  });
  
  if (!response.ok) {
    throw new Error(`Failed to create post: ${response.statusText}`);
  }
  
  const data = await response.json();
  return data.data;
};
```

### Complete Flow Integration

```typescript
// Complete upload and post creation flow
const handleCreatePost = async (
  files: File[],
  caption: string,
  mood: string,
  ownerId: string,
  location?: string,
  tags?: string[]
) => {
  try {
    // Step 1: Request upload URLs
    setUploadState('requesting-urls');
    const uploadUrls = await requestUploadUrls(files, ownerId);
    
    // Step 2: Upload files to S3
    setUploadState('uploading');
    const etags = await uploadFilesToS3(
      files,
      uploadUrls,
      (fileIndex, progress) => {
        updateFileProgress(fileIndex, progress);
      }
    );
    
    // Step 3: Complete uploads
    setUploadState('completing');
    const fileSizes = files.map(f => f.size);
    const mediaIds = uploadUrls.map(u => u.media_id);
    const completedUploads = await completeAllUploads(mediaIds, etags, fileSizes);
    
    // Step 4: Create post
    setUploadState('creating-post');
    const mediaItems: MediaItem[] = completedUploads.map((upload, index) => ({
      media_id: upload.media_id,
      type: files[index].type.startsWith('image/') ? 'image' : 'video',
      position: index
    }));
    
    const post = await createPost(mediaItems, caption, mood, location, tags);
    
    // Success!
    setUploadState('success');
    onPostCreated(post);
    
  } catch (error) {
    setUploadState('error');
    handleError(error);
  }
};
```

---

## 🎨 UI/UX Best Practices

### 1. File Selection UI

```tsx
// React component example
const FileUploader = () => {
  const [files, setFiles] = useState<File[]>([]);
  const [dragActive, setDragActive] = useState(false);
  
  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };
  
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const newFiles = Array.from(e.dataTransfer.files);
      setFiles(prev => [...prev, ...newFiles]);
    }
  };
  
  return (
    <div
      className={`upload-area ${dragActive ? 'drag-active' : ''}`}
      onDragEnter={handleDrag}
      onDragLeave={handleDrag}
      onDragOver={handleDrag}
      onDrop={handleDrop}
    >
      <input
        type="file"
        multiple
        accept="image/*,video/*"
        onChange={(e) => {
          if (e.target.files) {
            setFiles(prev => [...prev, ...Array.from(e.target.files!)]);
          }
        }}
      />
      <div className="upload-prompt">
        <p>Drag and drop files here, or click to select</p>
        <p className="hint">Supports images and videos</p>
      </div>
    </div>
  );
};
```

### 2. Progress Indicators

```tsx
interface FileUploadProgress {
  fileIndex: number;
  fileName: string;
  progress: number;
  status: 'pending' | 'uploading' | 'processing' | 'completed' | 'error';
}

const UploadProgressList = ({ files, progress }: {
  files: File[];
  progress: FileUploadProgress[];
}) => {
  return (
    <div className="upload-progress-list">
      {files.map((file, index) => {
        const fileProgress = progress[index];
        return (
          <div key={index} className="upload-item">
            <div className="file-info">
              <span className="file-name">{file.name}</span>
              <span className="file-size">
                {(file.size / 1024 / 1024).toFixed(2)} MB
              </span>
            </div>
            <div className="progress-bar">
              <div
                className="progress-fill"
                style={{ width: `${fileProgress.progress}%` }}
              />
            </div>
            <div className="status">
              {fileProgress.status === 'uploading' && 'Uploading...'}
              {fileProgress.status === 'processing' && 'Processing...'}
              {fileProgress.status === 'completed' && '✓ Completed'}
              {fileProgress.status === 'error' && '✗ Error'}
            </div>
          </div>
        );
      })}
    </div>
  );
};
```

### 3. State Management

```typescript
// Upload state management
type UploadState = 
  | 'idle'
  | 'ready'
  | 'requesting-urls'
  | 'uploading'
  | 'completing'
  | 'creating-post'
  | 'success'
  | 'error';

interface UploadState {
  state: UploadState;
  files: File[];
  uploadUrls: UploadUrlResponse[];
  progress: Map<number, number>; // fileIndex -> progress percentage
  errors: string[];
  completedMediaIds: string[];
}

const useUploadState = () => {
  const [uploadState, setUploadState] = useState<UploadState>({
    state: 'idle',
    files: [],
    uploadUrls: [],
    progress: new Map(),
    errors: [],
    completedMediaIds: []
  });
  
  const updateProgress = (fileIndex: number, progress: number) => {
    setUploadState(prev => ({
      ...prev,
      progress: new Map(prev.progress).set(fileIndex, progress)
    }));
  };
  
  return { uploadState, setUploadState, updateProgress };
};
```

---

## ⚠️ Error Handling

```typescript
// Comprehensive error handling
const handleUploadError = (error: Error, context: string) => {
  console.error(`Error in ${context}:`, error);
  
  // User-friendly error messages
  let userMessage = 'An error occurred. Please try again.';
  
  if (error.message.includes('Failed to request upload URL')) {
    userMessage = 'Failed to prepare upload. Please check your connection.';
  } else if (error.message.includes('Upload failed')) {
    userMessage = 'Upload failed. Please try again.';
  } else if (error.message.includes('Failed to complete upload')) {
    userMessage = 'Failed to process file. Please try again.';
  } else if (error.message.includes('Failed to create post')) {
    userMessage = 'Failed to publish post. Your files are uploaded, please try again.';
  }
  
  // Show error to user
  showErrorNotification(userMessage);
  
  // Optionally retry
  if (context === 'upload') {
    // Retry upload logic
  }
};

// Retry mechanism
const retryUpload = async (
  file: File,
  uploadUrl: string,
  maxRetries: number = 3
): Promise<string> => {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await uploadFileToS3(file, uploadUrl);
    } catch (error) {
      if (attempt === maxRetries) {
        throw error;
      }
      // Exponential backoff
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
  throw new Error('Max retries exceeded');
};
```

---

## 📊 Progress Tracking

```typescript
// Real-time progress tracking
const useUploadProgress = (files: File[]) => {
  const [overallProgress, setOverallProgress] = useState(0);
  const [fileProgress, setFileProgress] = useState<Map<number, number>>(new Map());
  
  const updateFileProgress = (fileIndex: number, progress: number) => {
    setFileProgress(prev => {
      const updated = new Map(prev);
      updated.set(fileIndex, progress);
      
      // Calculate overall progress
      let total = 0;
      files.forEach((_, index) => {
        total += updated.get(index) || 0;
      });
      setOverallProgress(total / files.length);
      
      return updated;
    });
  };
  
  return { overallProgress, fileProgress, updateFileProgress };
};
```

---

## 🔄 Multiple Files Handling

```typescript
// Optimized multiple files upload
const uploadMultipleFiles = async (
  files: File[],
  ownerId: string,
  onProgress: (overall: number, fileProgress: Map<number, number>) => void
) => {
  const fileProgress = new Map<number, number>();
  
  // Step 1: Request all URLs in parallel
  const uploadUrls = await Promise.all(
    files.map(file => requestUploadUrl(file, ownerId))
  );
  
  // Step 2: Upload all files in parallel with progress tracking
  const uploadPromises = files.map((file, index) => {
    return uploadFileToS3(
      file,
      uploadUrls[index].upload_url,
      (progress) => {
        fileProgress.set(index, progress);
        const overall = Array.from(fileProgress.values())
          .reduce((sum, p) => sum + p, 0) / files.length;
        onProgress(overall, new Map(fileProgress));
      }
    );
  });
  
  const etags = await Promise.all(uploadPromises);
  
  // Step 3: Complete all uploads
  const completedUploads = await Promise.all(
    uploadUrls.map((url, index) =>
      completeUpload(url.media_id, etags[index], files[index].size)
    )
  );
  
  return completedUploads;
};
```

---

## 🎯 React Hook Example

```typescript
// Custom React hook for post creation
const useCreatePost = () => {
  const [state, setState] = useState<UploadState>('idle');
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  
  const createPost = useCallback(async (
    files: File[],
    postData: {
      caption: string;
      mood: string;
      location?: string;
      tags?: string[];
    },
    ownerId: string
  ) => {
    try {
      setState('requesting-urls');
      setError(null);
      
      // Request URLs
      const uploadUrls = await requestUploadUrls(files, ownerId);
      
      // Upload files
      setState('uploading');
      const etags = await uploadFilesToS3(files, uploadUrls, (index, prog) => {
        const overall = ((index + prog / 100) / files.length) * 100;
        setProgress(overall);
      });
      
      // Complete uploads
      setState('completing');
      const mediaIds = uploadUrls.map(u => u.media_id);
      const fileSizes = files.map(f => f.size);
      await completeAllUploads(mediaIds, etags, fileSizes);
      
      // Create post
      setState('creating-post');
      const mediaItems = uploadUrls.map((url, index) => ({
        media_id: url.media_id,
        type: files[index].type.startsWith('image/') ? 'image' : 'video',
        position: index
      }));
      
      const post = await createPost(mediaItems, postData);
      
      setState('success');
      return post;
      
    } catch (err) {
      setState('error');
      setError(err instanceof Error ? err.message : 'Unknown error');
      throw err;
    }
  }, []);
  
  return { createPost, state, progress, error };
};

// Usage in component
const CreatePostComponent = () => {
  const { createPost, state, progress, error } = useCreatePost();
  const [files, setFiles] = useState<File[]>([]);
  
  const handleSubmit = async () => {
    try {
      const post = await createPost(files, {
        caption: 'My amazing post!',
        mood: 'adventure'
      }, userId);
      
      // Navigate to post or show success
      navigate(`/posts/${post.id}`);
    } catch (err) {
      // Error already handled in hook
    }
  };
  
  return (
    <div>
      {/* File selection UI */}
      {/* Progress indicators */}
      {/* Submit button */}
    </div>
  );
};
```

---

## 📱 Mobile Considerations

```typescript
// Mobile-optimized file upload
const uploadFileMobile = async (
  file: File,
  uploadUrl: string,
  onProgress: (progress: number) => void
) => {
  // Use fetch API for better mobile support
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type
    },
    body: file
  });
  
  if (!response.ok) {
    throw new Error('Upload failed');
  }
  
  return response.headers.get('ETag') || '';
};
```

---

## 🔒 Security Best Practices

1. **Validate files on client side** before uploading
2. **Never expose S3 credentials** - always use presigned URLs
3. **Set appropriate CORS** headers on S3 bucket
4. **Validate file types** on both client and server
5. **Implement rate limiting** for upload URL requests
6. **Use HTTPS** for all API calls

---

## 📝 Summary

The UI implementation follows these key steps:

1. **File Selection** → Validate → Store in state
2. **Request URLs** → Show loading → Store URLs
3. **Upload to S3** → Track progress → Show progress bars
4. **Complete Uploads** → Show processing → Get media IDs
5. **Create Post** → Show publishing → Navigate to post

Key features:
- ✅ Parallel uploads for multiple files
- ✅ Real-time progress tracking
- ✅ Error handling and retry logic
- ✅ Mobile-friendly implementation
- ✅ Optimistic UI updates

