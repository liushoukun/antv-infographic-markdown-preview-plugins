// @ts-check
import * as esbuild from 'esbuild';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const watch = process.argv.includes('--watch');

const ctx = await esbuild.context({
  entryPoints: [join(__dirname, 'src/preview.ts')],
  bundle: true,
  outdir: join(__dirname, 'dist'),
  platform: 'browser',
  target: 'es2022',
  format: 'iife',
  sourcemap: true,
  minify: false,
  logLevel: 'info',
});

if (watch) {
  await ctx.watch();
  console.log('watching preview-web…');
} else {
  await ctx.rebuild();
  await ctx.dispose();
}
