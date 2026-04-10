import { installIconQueryFallback } from './preview-icon-fetch-patch';
import type { Infographic as InfographicInstance } from '@antv/infographic';

/** 必须在加载 @antv/infographic 之前执行；静态 import 会在同一 bundle 内抢先初始化库并闭包 fetch。 */
installIconQueryFallback();

type InfographicClass = typeof import('@antv/infographic').Infographic;

const ATTR = 'data-vscode-infographic';
const HOST_SELECTOR = `.vscode-infographic-host[${ATTR}="1"]`;
const DEBOUNCE_MS = 120;
const TEMPLATE_CLASS = 'vscode-infographic-src';

const instances = new WeakMap<HTMLElement, InfographicInstance>();

let Infographic: InfographicClass | undefined;

async function loadInfographic(): Promise<void> {
  const mod = await import('@antv/infographic');
  Infographic = mod.Infographic;
}

/** 部分宿主（如 JetBrains Markdown）不注入外层 host，由脚本将围栏包进稳定容器。 */
function ensureHostsFromCodeBlocks(): void {
  const blocks = document.querySelectorAll<HTMLElement>('pre > code.language-infographic');
  blocks.forEach((code) => {
    const pre = code.parentElement;
    if (!pre) {
      return;
    }

    const existing = pre.parentElement;
    if (existing?.matches?.(HOST_SELECTOR)) {
      return;
    }

    const host = document.createElement('div');
    host.className = 'vscode-infographic-host';
    host.setAttribute(ATTR, '1');
    pre.parentElement?.insertBefore(host, pre);
    host.appendChild(pre);
  });
}

function getSource(host: HTMLElement): string {
  const code = host.querySelector('code.language-infographic');
  if (code) {
    return code.textContent ?? '';
  }
  const tmpl = host.querySelector(`template.${TEMPLATE_CLASS}`);
  return tmpl?.textContent ?? '';
}

function shouldSkip(host: HTMLElement, source: string): boolean {
  if (!host.querySelector('.vscode-infographic-canvas')) {
    return false;
  }
  const tmpl = host.querySelector(`template.${TEMPLATE_CLASS}`);
  return tmpl !== null && tmpl.textContent === source;
}

function showError(container: HTMLElement, message: string) {
  container.innerHTML = '';
  const pre = document.createElement('pre');
  pre.className = 'vscode-infographic-error';
  pre.textContent = message;
  container.appendChild(pre);
}

function renderHost(host: HTMLElement) {
  const Ctor = Infographic;
  if (!Ctor) {
    return;
  }

  const source = getSource(host).trim();
  if (!source) {
    return;
  }

  if (shouldSkip(host, source)) {
    return;
  }

  const prev = instances.get(host);
  if (prev) {
    try {
      prev.destroy();
    } catch {
      /* ignore */
    }
    instances.delete(host);
  }

  host.innerHTML = '';
  const tpl = document.createElement('template');
  tpl.className = TEMPLATE_CLASS;
  tpl.textContent = source;
  host.appendChild(tpl);

  const root = document.createElement('div');
  root.className = 'vscode-infographic-canvas';
  host.appendChild(root);

  try {
    const ig = new Ctor({
      container: root,
      width: '100%',
      height: 400,
      editable: false,
    });
    ig.render(source);
    instances.set(host, ig);
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    showError(root, `Infographic 渲染失败：${msg}`);
  }
}

function scan() {
  ensureHostsFromCodeBlocks();
  document.querySelectorAll<HTMLElement>(HOST_SELECTOR).forEach(renderHost);
}

let scheduled: ReturnType<typeof setTimeout> | undefined;

function scheduleScan() {
  if (scheduled) {
    clearTimeout(scheduled);
  }
  scheduled = setTimeout(() => {
    scheduled = undefined;
    scan();
  }, DEBOUNCE_MS);
}

const observer = new MutationObserver(() => scheduleScan());

function start() {
  if (document.body) {
    observer.observe(document.body, { childList: true, subtree: true });
  }
  scheduleScan();
}

void loadInfographic().then(
  () => {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', start);
    } else {
      start();
    }
  },
  (err) => {
    console.error('[antv-infographic-preview] 无法加载 @antv/infographic', err);
  },
);
