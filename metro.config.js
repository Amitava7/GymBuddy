const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const config = getDefaultConfig(__dirname);

config.resolver.sourceExts.push('sql');
config.transformer.babelTransformerPath = path.resolve(__dirname, 'metro-sql-transformer.js');

module.exports = config;
