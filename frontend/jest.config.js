export default {
  testEnvironment: 'node',
  transform: {},
//  extensionsToTreatAsEsm: ['.js'],
  moduleNameMapper: {
    '^(\\.{1,2}/.*)\\.js$': '$1',
  },
  testMatch: ['**/tests/e2e/**/*.test.js'],
}; 
