import { createTheme } from '@mantine/core';

// Keep in sync with --rm-* in src/index.css. primarySoft and accentBg are NOT for text (3.0:1 and 1.8:1 on light).
export const TOKENS = {
  text: '#223125',
  primary: '#4F7347',
  primarySoft: '#729969',
  accentBg: '#A1C0C2',
  border: '#E6E7EB',
  bg: '#F6F6F6',
  surface: '#FFFFFF',
  header: '#223125',
  headerText: '#F6F6F6',
  error: '#B3261E',
  focus: '#223125',
};

// Index 6 is the base color (primaryShade: 6).
const brand = [
  '#F1F4F0', '#DFE6DE', '#C7D2C4', '#ABBCA7', '#8EA589',
  '#6F8C68', '#4F7347', '#456440', '#3C5739', '#344B33',
];
const red = [
  '#F9EEED', '#F1D8D7', '#E7BAB7', '#DB9793', '#CE746F',
  '#C14D47', '#B3261E', '#992019', '#811A14', '#6B1510',
];

export const theme = createTheme({
  primaryColor: 'brand',
  primaryShade: 6,
  colors: { brand, red },
  black: TOKENS.text,
  white: TOKENS.surface,
  autoContrast: false,
  defaultRadius: 'md',
  headings: { fontWeight: '700' },
  cursorType: 'pointer',
  focusRing: 'auto',
  components: {
    Paper: { defaultProps: { withBorder: true } },
    Card: { defaultProps: { withBorder: true } },
  },
});

// --mantine-color-body stays white because Paper/Card/inputs use it as their surface; the page background is set in index.css.
export const cssVariablesResolver = () => ({
  variables: {},
  light: {
    '--mantine-color-body': TOKENS.surface,
    '--mantine-color-default-border': TOKENS.border,
  },
  dark: {},
});
