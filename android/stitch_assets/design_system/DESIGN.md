# Versatile Professional System
## Brand & Style
The design system is engineered for high-utility, versatile mobile applications where clarity and professional reliability are paramount. It follows a **Corporate/Modern** design movement, prioritizing a balance between aesthetic refinement and functional accessibility.

The visual language is characterized by purposeful whitespace, a disciplined color application, and a focus on content hierarchy. It aims to evoke a sense of trust, precision, and efficiency, ensuring that users feel empowered rather than overwhelmed. The aesthetic avoids unnecessary decoration, opting instead for subtle depth cues and high-quality typography to guide the user journey.

## Colors
The palette is built upon a high-contrast foundation to ensure WCAG 2.1 accessibility standards are met across all interfaces. 

### Core Palette
- **Primary:** A vibrant Indigo-Blue used for key action points, active states, and brand recognition.
- **Secondary:** A neutral Slate used for secondary actions and de-emphasized UI elements.
- **Severity System:** A robust set of semantic colors (Success, Warning, Error, Info) designed to provide immediate status recognition. Each severity color should be paired with a tinted background (10% opacity) for alert surfaces.

### Theming
The design system supports both **Light** and **Dark** modes. In dark mode, surfaces utilize "total grey" (#121212) foundations with elevated surfaces using slightly lighter overlays to maintain depth perception without sacrificing the OLED efficiency of true blacks.

## Typography
Typography in this design system uses a dual-font approach to maximize both personality and utility. **Hanken Grotesk** is utilized for display and headlines to provide a sharp, contemporary professional feel. **Inter** is used for all body text and labels due to its exceptional legibility on small screens.

The scale is strictly mathematical, ensuring that vertical rhythm is maintained. Mobile-specific overrides for large headlines ensure that text remains readable without excessive line-breaking on narrow viewports.

## Layout & Spacing
The layout follows an **8px grid system**. All padding, margins, and component heights should be multiples of 8px to ensure a consistent visual rhythm.

### Grid Configuration
On mobile devices, the design system utilizes a **4-column fluid grid** with 16px outer margins and 16px gutters. For larger mobile screens (e.g., Foldables), the grid can expand to 6 columns.

### Safe Areas
The design system accounts for hardware-specific constraints. Bottom Sheets and Navigation Bars must respect the `safe-area-bottom` inset (typically 34px on modern iOS devices) to prevent interaction conflicts with system gestures.

## Elevation & Depth
Hierarchy is established using **Tonal Layers** supplemented by **Ambient Shadows**. 

- **Level 0 (Base):** The primary background color.
- **Level 1 (Cards/Lists):** Elevated via a subtle 1px border (#E2E8F0 in light mode) or a very soft shadow (0px 2px 4px rgba(0,0,0,0.05)).
- **Level 2 (Bottom Sheets/Menus):** Higher elevation using a diffused shadow (0px 10px 25px rgba(0,0,0,0.1)) to indicate temporary interaction.
- **Level 3 (FABs/Alerts):** Peak elevation to signify urgency or the primary action.

Shadow colors should never be pure black; they are always tinted with the neutral color of the palette to ensure they feel integrated into the interface.

## Shapes
The design system employs a **Rounded** shape language (Level 2). This provides a friendly, approachable feel while maintaining the structural integrity required for a professional application.

- **Standard Elements (Buttons, Inputs, Cards):** 0.5rem (8px) corner radius.
- **Large Containers (Bottom Sheets):** 1rem (16px) top corner radius.
- **FABs & Map Markers:** Fully rounded (pill-shaped) to distinguish them as floating or high-priority elements.

## Components

### Buttons
- **Primary:** Solid background (Primary color) with White text. Used for the main call to action.
- **Secondary:** Outlined or low-opacity Primary background. Used for supporting actions.
- **Ghost:** No background or border, uses Primary or Neutral text. Used for tertiary actions.
- **FAB (Floating Action Button):** High-elevation, pill-shaped button anchored to the bottom right, typically used for the most frequent action in a view.

### Cards
Cards are the primary container for content modules. They utilize Level 1 elevation with consistent 16px internal padding.

### Bottom Sheets
Used for contextual tasks or filtering. They should include a 4px height "handle" at the top to indicate draggability. The background uses a Level 2 elevation shadow.

### Navigation Bars
- **Bottom Nav:** Fixed at the base, utilizing clear icons with 12px labels for active/inactive states.
- **Top App Bar:** Centered or left-aligned titles using `headline-md`. 

### Alerts & Status
Alerts use the Severity System colors. They feature a left-hand 4px border of the solid color and a 10% opacity background of the same hue to ensure high visibility without visual fatigue.

### Map Markers
Markers are pill-shaped or teardrop-shaped with a solid Primary color. Active markers should feature a white inner-ring or icon to indicate focus.