# TrafficGuard AI - Jetpack Compose Design System Specification

This document outlines the **Versatile Professional System** for the TrafficGuard Android application, combining visual guidelines with concrete theme tokens directly mapped from our design assets (`stitch_assets/design_system`).

## Brand & Style
The design system follows a **Corporate/Modern** design movement, prioritizing a balance between aesthetic refinement and functional accessibility. It features purposeful whitespace, a disciplined color application, and a focus on content hierarchy. 

## Colors (Tokens mapped from `theme.json`)

The palette is built upon a high-contrast foundation for WCAG 2.1 accessibility.

### Core Foundation (Light Mode)

| Token Name | Hex Value | Description / Usage |
| :--- | :--- | :--- |
| `primary` | `#004ac6` | Vibrant Indigo-Blue. Main action points, active states, brand recognition. |
| `on_primary` | `#ffffff` | Text/icons displayed on top of `primary`. |
| `primary_container` | `#2563eb` | Vibrant accent container color. |
| `on_primary_container` | `#eeefff` | Text/icons on `primary_container`. |
| `secondary` | `#505f76` | Neutral Slate for secondary actions. |
| `on_secondary` | `#ffffff` | Text/icons displayed on top of `secondary`. |
| `secondary_container` | `#d0e1fb` | De-emphasized secondary UI elements. |
| `tertiary` | `#943700` | Tertiary accent color. |
| `surface` | `#f9f9ff` | High-contrast main background surface. |
| `on_surface` | `#111c2d` | Default text color on surfaces. |
| `outline` | `#737686` | Borders, subtle dividers, outlines. |

### Severity / Alert System

| Token Name | Hex Value | Description / Usage |
| :--- | :--- | :--- |
| `error` | `#ba1a1a` | Semantic error color. Immediate status recognition. |
| `on_error` | `#ffffff` | Text/icons on `error` surface. |
| `error_container` | `#ffdad6` | Tinted background for alert surfaces. |

*(For alerts, pair a severity color with its container for a 10% opacity effect to ensure visibility without fatigue.)*

## Typography
The design system uses a dual-font approach for personality and utility:
- **Headline Font:** **Hanken Grotesk** for display and headlines (sharp, contemporary professional feel).
- **Body & Label Font:** **Inter** for all body text and labels (exceptional legibility).

*Ensure a mathematical scale for vertical rhythm, utilizing Compose Material 3 Typography structures.*

## Shapes
The system employs a **Rounded** shape language to provide an approachable feel while maintaining structural integrity.

- **Standard Elements (Buttons, Inputs, Cards):** `RoundedCornerShape(8.dp)`
- **Large Containers (Bottom Sheets):** `RoundedCornerShape(16.dp)` (top corners)
- **FABs & Map Markers:** Fully rounded (`CircleShape`) for high-priority/floating elements.

## Layout & Spacing
- **Grid:** Based on an **8px grid system**. Padding, margins, and component heights should be multiples of 8.dp.
- **Mobile Grid:** 4-column fluid grid with 16.dp outer margins and 16.dp gutters.
- **Safe Areas:** Account for `WindowInsets` (Bottom Sheets and Navigation Bars must respect `WindowInsets.navigationBars`).

## Elevation & Depth (Tonal Layers & Shadows)
- **Level 0 (Base):** Primary background (`surface`).
- **Level 1 (Cards/Lists):** 1.dp border (`#E2E8F0`) or soft shadow `0px 2px 4px rgba(0,0,0,0.05)`.
- **Level 2 (Bottom Sheets/Menus):** Diffused shadow `0px 10px 25px rgba(0,0,0,0.1)`.
- **Level 3 (FABs/Alerts):** Peak elevation.

*Shadow colors should never be pure black; tint them slightly with the neutral color.*

## Components Overview

- **Buttons:** 
  - Primary: Solid `primary` with `on_primary` text.
  - Secondary: Outlined or low-opacity container.
  - Ghost: Transparent background with `primary` text.
  - FAB: High-elevation, pill-shaped, bottom right.
- **Cards:** Level 1 elevation, 16.dp internal padding.
- **Bottom Sheets:** Level 2 elevation, 4.dp drag handle at top.
- **Alerts:** Left-hand 4.dp border of solid severity color, paired with container background.
- **Map Markers:** Pill or teardrop shaped, solid Primary. Active markers have a white inner-ring.
