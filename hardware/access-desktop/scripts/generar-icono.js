#!/usr/bin/env node
/**
 * Genera build/icon.ico desde el logo del gym (frontend/public/logo-sport-gym.png).
 */
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const root = path.join(__dirname, '..');
const buildDir = path.join(root, 'build');
const logoSrc = path.join(root, '../../frontend/public/logo-sport-gym.png');
const png256 = path.join(buildDir, 'logo-256.png');
const iconOut = path.join(buildDir, 'icon.ico');
const pyScript = path.join(buildDir, '_logo_resize.py');

if (!fs.existsSync(logoSrc)) {
  console.error('No se encontró', logoSrc);
  process.exit(1);
}

fs.mkdirSync(buildDir, { recursive: true });

fs.writeFileSync(
  pyScript,
  `from PIL import Image
src = ${JSON.stringify(logoSrc)}
out = ${JSON.stringify(png256)}
img = Image.open(src).convert('RGBA')
w, h = img.size
s = max(w, h)
canvas = Image.new('RGBA', (s, s), (0, 0, 0, 0))
canvas.paste(img, ((s - w) // 2, (s - h) // 2))
canvas.resize((256, 256), Image.LANCZOS).save(out)
`,
);

execFileSync('python3', [pyScript], { stdio: 'inherit' });

let png2icons;
try {
  png2icons = require('png2icons');
} catch {
  execFileSync('npm', ['install', '--no-save', 'png2icons@2.0.1'], {
    cwd: root,
    stdio: 'inherit',
  });
  png2icons = require('png2icons');
}

const buf = fs.readFileSync(png256);
const ico = png2icons.createICO(buf, png2icons.BILINEAR, 0, false, true);
if (!ico) {
  console.error('png2icons no generó ICO');
  process.exit(1);
}
fs.writeFileSync(iconOut, ico);
console.log('OK:', iconOut);
