import { Infographic } from '@antv/infographic';

const ATTR = 'data-vscode-infographic';
const HOST_SELECTOR = `.vscode-infographic-host[${ATTR}="1"]`;
const DEBOUNCE_MS = 120;
const TEMPLATE_CLASS = 'vscode-infographic-src';
const ICON_QUERY_ENDPOINT = 'https://www.weavefox.cn/api/open/v1/icon';
const ICONIFY_SEARCH_ENDPOINT = 'https://api.iconify.design/search';
const ICONIFY_SVG_ENDPOINT = 'https://api.iconify.design';
const ICONIFY_ID_RE = /^[a-z0-9-]+\/[a-z0-9-]+$/i;

const instances = new WeakMap<HTMLElement, Infographic>();

/**
 * 仅使用远程 icon 资源。
 * 主链路：weavefox；回退：iconify(先直连 id，再搜索)。
 */
function installIconQueryFallback(): void {
  const nativeFetch = globalThis.fetch?.bind(globalThis);
  if (!nativeFetch) {
    return;
  }
  if ((globalThis as { __igIconFallbackInstalled__?: boolean }).__igIconFallbackInstalled__) {
    return;
  }

  const toJsonResponse = (payload: unknown) =>
    new Response(JSON.stringify(payload), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });

  const hasUsableIconData = (payload: unknown): boolean => {
    if (!payload || typeof payload !== 'object') {
      return false;
    }
    const rec = payload as { status?: unknown; data?: { data?: unknown } };
    if (!rec.status) {
      return false;
    }
    return Array.isArray(rec.data?.data) && rec.data.data.length > 0;
  };

  const iconifyLookup = async (query: string): Promise<Response | null> => {
    // query 本身是 iconify id（如 mingcute/diamond-2-fill）时，优先直连 svg。
    if (ICONIFY_ID_RE.test(query)) {
      const directUrl = `${ICONIFY_SVG_ENDPOINT}/${query}.svg`;
      const directResp = await nativeFetch(directUrl);
      if (directResp.ok) {
        const directSvg = await directResp.text();
        return toJsonResponse({ status: true, data: { data: [directSvg] } });
      }
    }

    const searchUrl = new URL(ICONIFY_SEARCH_ENDPOINT);
    searchUrl.searchParams.set('query', query);
    searchUrl.searchParams.set('limit', '1');
    const searchResp = await nativeFetch(searchUrl.toString());
    if (!searchResp.ok) {
      return null;
    }
    const searchJson = (await searchResp.json()) as { icons?: string[] };
    const iconId = Array.isArray(searchJson.icons) ? searchJson.icons[0] : undefined;
    if (!iconId) {
      return toJsonResponse({ status: false, data: { data: [] } });
    }
    const svgUrl = `${ICONIFY_SVG_ENDPOINT}/${iconId}.svg`;
    const svgResp = await nativeFetch(svgUrl);
    if (!svgResp.ok) {
      return null;
    }
    const svgText = await svgResp.text();
    return toJsonResponse({ status: true, data: { data: [svgText] } });
  };

  globalThis.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    if (!url.startsWith(ICON_QUERY_ENDPOINT)) {
      return nativeFetch(input, init);
    }

    // 主接口返回 200 但内容空时，继续回退（之前这里会直接返回导致 icon 不显示）。
    try {
      const primary = await nativeFetch(input, init);
      if (primary.ok) {
        try {
          const data = (await primary.clone().json()) as unknown;
          if (hasUsableIconData(data)) {
            return primary;
          }
        } catch {
          // 主接口若返回非 JSON，维持原始行为。
          return primary;
        }
      }
    } catch {
      // ignore and fallback
    }

    try {
      const parsed = new URL(url);
      const text = parsed.searchParams.get('text')?.trim();
      if (!text) {
        return toJsonResponse({ status: false, data: { data: [] } });
      }
      const fallbackResp = await iconifyLookup(text);
      if (fallbackResp) {
        return fallbackResp;
      }
    } catch {
      // ignore and use original endpoint result path
    }

    return nativeFetch(input, init);
  };

  (globalThis as { __igIconFallbackInstalled__?: boolean }).__igIconFallbackInstalled__ = true;
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
    const ig = new Infographic({
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
  installIconQueryFallback();
  if (document.body) {
    observer.observe(document.body, { childList: true, subtree: true });
  }
  scheduleScan();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', start);
} else {
  start();
}
