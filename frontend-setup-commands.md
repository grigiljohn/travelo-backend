# React Frontend Setup Commands for Travelo Ad Service

## 1. Create React Project

```bash
# Using Vite (recommended - faster and modern)
npm create vite@latest travelo-ad-frontend -- --template react-ts

# OR using Create React App
npx create-react-app travelo-ad-frontend --template typescript

# Navigate to project directory
cd travelo-ad-frontend
```

## 2. Install Core Dependencies

```bash
# HTTP Client
npm install axios

# Routing
npm install react-router-dom

# State Management (choose one)
npm install @tanstack/react-query  # Recommended for server state
npm install zustand  # For global client state (optional)

# Form Handling
npm install react-hook-form
npm install @hookform/resolvers zod

# UI Component Library (choose one)
npm install @mui/material @emotion/react @emotion/styled  # Material-UI
# OR
npm install antd  # Ant Design
# OR
npm install shadcn-ui  # shadcn/ui (headless components)

# Date/Time Handling
npm install date-fns
npm install react-datepicker

# Charts for Analytics
npm install recharts
# OR
npm install chart.js react-chartjs-2

# Utilities
npm install clsx  # For conditional classNames
npm install lodash  # Utility functions
```

## 3. Install Development Dependencies

```bash
# TypeScript types
npm install --save-dev @types/react @types/react-dom @types/node

# Linting and Formatting
npm install --save-dev eslint @typescript-eslint/eslint-plugin @typescript-eslint/parser
npm install --save-dev prettier eslint-config-prettier eslint-plugin-prettier

# Environment Variables
npm install dotenv
```

## 4. Project Structure Setup

```bash
# Create directory structure
mkdir -p src/{components,pages,services,hooks,utils,types,contexts,constants}
mkdir -p src/components/{common,campaigns,ads,analytics}
mkdir -p src/pages/{Campaigns,Ads,Analytics,Dashboard}
mkdir -p src/services/{api,auth}
mkdir -p src/types
mkdir -p public
```

## 5. Create Configuration Files

### Create `.env` file:
```bash
cat > .env << 'EOF'
VITE_API_BASE_URL=http://localhost:8093/api/v1
VITE_ADVERTISER_ID=advertiser-123
EOF
```

### Create `.env.example`:
```bash
cat > .env.example << 'EOF'
VITE_API_BASE_URL=http://localhost:8093/api/v1
VITE_ADVERTISER_ID=your-advertiser-id
EOF
```

## 6. Initialize Git (Optional)

```bash
# Initialize git repository
git init

# Create .gitignore if not exists
cat > .gitignore << 'EOF'
# Dependencies
node_modules/
/.pnp
.pnp.js

# Testing
/coverage

# Production
/build
/dist

# Misc
.DS_Store
.env.local
.env.development.local
.env.test.local
.env.production.local

npm-debug.log*
yarn-debug.log*
yarn-error.log*

# IDE
.vscode/
.idea/
*.swp
*.swo
EOF
```

## 7. Start Development Server

```bash
# For Vite
npm run dev

# For Create React App
npm start
```

## 8. Additional Recommended Packages

```bash
# Toast Notifications
npm install react-toastify

# Loading Spinners
npm install react-spinners

# Icons
npm install @mui/icons-material  # If using Material-UI
# OR
npm install react-icons  # Universal icon library

# Table Component (if not using UI library)
npm install react-table

# File Upload
npm install react-dropzone

# Validation
npm install zod  # Already installed above, but ensure it's there

# Date Picker (if not using UI library's)
npm install react-datepicker @types/react-datepicker
```

## 9. TypeScript Configuration (if using Vite)

Update `tsconfig.json`:
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

## 10. Vite Configuration (if using Vite)

Update `vite.config.ts`:
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8093',
        changeOrigin: true,
      },
    },
  },
})
```

## Quick Setup Script (All-in-One)

```bash
#!/bin/bash

# Create project
npm create vite@latest travelo-ad-frontend -- --template react-ts
cd travelo-ad-frontend

# Install dependencies
npm install

# Install core packages
npm install axios react-router-dom @tanstack/react-query zustand
npm install react-hook-form @hookform/resolvers zod
npm install @mui/material @emotion/react @emotion/styled @mui/icons-material
npm install date-fns react-datepicker recharts
npm install react-toastify react-spinners clsx lodash

# Install dev dependencies
npm install --save-dev @types/react @types/react-dom @types/node
npm install --save-dev eslint @typescript-eslint/eslint-plugin @typescript-eslint/parser
npm install --save-dev prettier eslint-config-prettier eslint-plugin-prettier
npm install --save-dev @types/react-datepicker

# Create directory structure
mkdir -p src/{components/{common,campaigns,ads,analytics},pages/{Campaigns,Ads,Analytics,Dashboard},services/{api,auth},hooks,utils,types,contexts,constants}

# Create .env file
cat > .env << 'EOF'
VITE_API_BASE_URL=http://localhost:8093/api/v1
VITE_ADVERTISER_ID=advertiser-123
EOF

echo "Setup complete! Run 'npm run dev' to start development server."
```

## Next Steps After Setup

1. **Create API Service Layer**
   - Set up axios instance with base URL
   - Create service functions for campaigns and ads
   - Add request/response interceptors

2. **Set Up Routing**
   - Configure React Router
   - Create route definitions
   - Add protected routes if needed

3. **Create Core Components**
   - Layout components
   - Navigation
   - Common UI components (buttons, inputs, modals)

4. **Build Feature Pages**
   - Campaign list and creation
   - Ad creation wizard
   - Analytics dashboard
   - Performance charts

5. **Add State Management**
   - Set up React Query for server state
   - Configure Zustand for client state (if needed)

## Recommended Project Structure

```
travelo-ad-frontend/
├── public/
├── src/
│   ├── components/
│   │   ├── common/          # Reusable UI components
│   │   ├── campaigns/        # Campaign-specific components
│   │   ├── ads/             # Ad-specific components
│   │   └── analytics/       # Analytics components
│   ├── pages/
│   │   ├── Campaigns/        # Campaign pages
│   │   ├── Ads/             # Ad pages
│   │   ├── Analytics/       # Analytics pages
│   │   └── Dashboard/       # Dashboard
│   ├── services/
│   │   ├── api/             # API service functions
│   │   └── auth/            # Authentication service
│   ├── hooks/               # Custom React hooks
│   ├── utils/               # Utility functions
│   ├── types/               # TypeScript type definitions
│   ├── contexts/            # React contexts
│   ├── constants/           # Constants and config
│   └── App.tsx
├── .env
├── .env.example
├── package.json
├── tsconfig.json
└── vite.config.ts
```

