const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const projectRoot = __dirname;
const monorepoRoot = path.resolve(projectRoot, '../..');

const config = getDefaultConfig(projectRoot);

// Watch the monorepo root for workspace package changes
config.watchFolders = [monorepoRoot];

// Resolve modules from both project and monorepo root
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, 'node_modules'),
  path.resolve(monorepoRoot, 'node_modules'),
];

// Ensure react-native-web is found from nested packages
config.resolver.extraNodeModules = {
  'react-native-web': path.resolve(projectRoot, 'node_modules/react-native-web'),
  'react-native': path.resolve(projectRoot, 'node_modules/react-native'),
};

module.exports = config;
