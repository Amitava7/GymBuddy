const { getDefaultConfig } = require('expo/metro-config');
const defaultConfig = getDefaultConfig(__dirname);
const upstreamTransformer = require(defaultConfig.transformer.babelTransformerPath);
const fs = require('fs');

module.exports.transform = async function ({ src, filename, options }) {
  if (filename.endsWith('.sql')) {
    const sqlContent = fs.readFileSync(filename, 'utf8');
    const escaped = JSON.stringify(sqlContent);
    src = `export default ${escaped};`;
  }
  return upstreamTransformer.transform({ src, filename, options });
};
