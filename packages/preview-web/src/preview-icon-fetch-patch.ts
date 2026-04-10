/**
 * 必须在加载 @antv/infographic 之前执行：库内会在初始化时闭包 globalThis.fetch。
 * preview.ts 先同步调用本模块，再通过动态 import() 拉取 @antv/infographic，避免与静态 import 抢跑。
 */
const ICON_QUERY_ENDPOINT = 'https://www.weavefox.cn/api/open/v1/icon';
const ICONIFY_SEARCH_ENDPOINT = 'https://api.iconify.design/search';
const ICONIFY_SVG_ENDPOINT = 'https://api.iconify.design';
const ICONIFY_ID_RE = /^[a-z0-9-]+\/[a-z0-9-]+$/i;

export function installIconQueryFallback(): void {
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

    try {
      const primary = await nativeFetch(input, init);
      if (primary.ok) {
        try {
          const data = (await primary.clone().json()) as unknown;
          if (hasUsableIconData(data)) {
            return primary;
          }
        } catch {
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
      // ignore
    }

    return nativeFetch(input, init);
  };

  (globalThis as { __igIconFallbackInstalled__?: boolean }).__igIconFallbackInstalled__ = true;
}
