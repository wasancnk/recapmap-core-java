# Phase 1A: Minimal Dependencies Installation Guide

## Security-First Package Installation

Based on our lean development strategy, here are the **5 essential packages** to install manually:

### 1. Visual Canvas (Core Requirement)
```cmd
npm install @xyflow/react
```
**Purpose**: Node-based visual editor - unavoidable core dependency
**Security**: Well-maintained, industry standard, actively audited
**Unity Migration**: Visual concepts translate to Unity scene graph

### 2. State Management (Lightweight Alternative to Redux)
```cmd
npm install zustand
```
**Purpose**: Global state management with minimal API
**Security**: Small bundle (<3kb), TypeScript-first, minimal dependencies
**Unity Migration**: State patterns translate to Unity ScriptableObjects

### 3. Styling Foundation (Replaces Multiple CSS Libraries)
```cmd
npm install tailwindcss postcss autoprefixer
```
**Purpose**: Utility-first CSS system with design tokens
**Security**: Build-time CSS generation, no runtime dependencies
**Unity Migration**: Design system concepts translate to Unity UI styling

### 4. Essential Utilities (Tiny Helpers)
```cmd
npm install clsx uuid
```
**Purpose**: CSS class merging and unique ID generation
**Security**: Minimal code, well-audited, no external dependencies
**Unity Migration**: Similar utilities available in C# ecosystem

## Installation Order

Run these commands one by one in your terminal:

```cmd
cd d:\workspace_recapmap\recapmap-core-react\recapmap-core-react
npm install @xyflow/react
npm install zustand
npm install tailwindcss postcss autoprefixer
npm install clsx uuid
```

## Post-Installation Setup

After installing packages, we'll need to configure TailwindCSS:

```cmd
npx tailwindcss init -p
```

## Package Audit

After installation, run security audit:

```cmd
npm audit
```

Target: **0 high/critical vulnerabilities**

## Verification

Check total package count:
```cmd
npm list --depth=0
```

**Target**: Should show only the 5 packages we installed plus React/Vite dependencies

## Next Steps

Once packages are installed, we'll:
1. Configure TailwindCSS with our design system
2. Set up Zustand stores for state management
3. Create the React Flow canvas component
4. Implement the 8-node system with TypeScript

---

**Total Additional Packages**: 5 (vs 20+ in typical React projects)
**Security Benefit**: Minimal attack surface
**Unity Migration**: All patterns compatible with C# translation
