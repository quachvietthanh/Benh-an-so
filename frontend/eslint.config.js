const browserGlobals = {
  Blob: 'readonly',
  FormData: 'readonly',
  URL: 'readonly',
  clearInterval: 'readonly',
  clearTimeout: 'readonly',
  console: 'readonly',
  document: 'readonly',
  localStorage: 'readonly',
  navigator: 'readonly',
  setInterval: 'readonly',
  setTimeout: 'readonly',
  window: 'readonly',
}

export default [
  {
    ignores: ['dist/**', 'node_modules/**'],
  },
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: {
          jsx: true,
        },
      },
      globals: browserGlobals,
    },
    rules: {
      'no-undef': 'error',
      'no-unused-vars': ['warn', {
        argsIgnorePattern: '^_',
        caughtErrors: 'none',
        varsIgnorePattern: '^(React|[A-Z])',
      }],
    },
  },
]
