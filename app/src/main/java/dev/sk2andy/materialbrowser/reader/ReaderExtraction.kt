package dev.sk2andy.materialbrowser.reader

import org.json.JSONArray
import org.json.JSONObject

object ReaderExtractionScript {
    val javascript: String = """
        (() => {
          const pageRoot = document.body;
          const visibleText = (pageRoot?.innerText || '')
            .replace(/[\u0000-\u001f\u007f]+/g, ' ')
            .replace(/\s+/g, ' ')
            .trim()
            .slice(0, 2000);
          const hasVisibleContent = (() => {
            if (!pageRoot) return false;
            if (visibleText) return true;
            return Array.from(
              pageRoot.querySelectorAll('img,svg,canvas,video,iframe,object,embed')
            ).some(node => {
              if (node.id === 'gt-nvframe' || node.closest('#gt-nvframe,[hidden],[aria-hidden="true"]')) return false;
              const style = getComputedStyle(node);
              if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0' || style.contentVisibility === 'hidden') return false;
              const rect = node.getBoundingClientRect();
              return rect.width >= 32 && rect.height >= 32 && rect.width * rect.height >= 4096;
            });
          })();
          const source = document.querySelector('article') || document.querySelector('main') || document.body;
          if (!source) {
            return JSON.stringify({error: 'missing-root', hasVisibleContent, visibleText});
          }
          const root = source.cloneNode(true);
          root.querySelectorAll('script,style,noscript,template,iframe,object,embed,canvas,svg,form,input,button,nav,aside,footer,video,audio').forEach(node => node.remove());
          const clean = value => (value || '').replace(/[\u0000-\u001f\u007f]+/g, ' ').replace(/\s+/g, ' ').trim();
          const blocks = [];
          let totalChars = 0;
          let totalLinks = 0;
          const blockSelector = 'h1,h2,h3,h4,h5,h6,p,blockquote,li';
          // A block inside another block is already in that block's text. The exception is an item of a
          // list nested in a list item: the outer item drops its nested lists, so their items stand alone.
          const isOwnBlock = node => {
            const outer = node.parentElement?.closest(blockSelector);
            if (!outer || outer === root || !root.contains(outer)) return true;
            const list = node.parentElement.closest('ul,ol');
            return outer.tagName.toLowerCase() === 'li' && !!list && outer.contains(list) && isOwnBlock(outer);
          };
          const blockContent = node => {
            if (node.tagName.toLowerCase() !== 'li') return node;
            const item = node.cloneNode(true);
            item.querySelectorAll('ul,ol').forEach(list => list.remove());
            return item;
          };
          root.querySelectorAll(blockSelector).forEach(node => {
            if (blocks.length >= 600 || totalChars >= 500000) return;
            if (!isOwnBlock(node)) return;
            const content = blockContent(node);
            const text = clean(content.innerText || content.textContent).slice(0, Math.min(12000, 500000 - totalChars));
            if (!text || text.length < 2) return;
            const tag = node.tagName.toLowerCase();
            const kind = tag.startsWith('h') ? 'heading' : tag === 'blockquote' ? 'quote' : tag === 'li' ? 'listitem' : 'paragraph';
            const links = Array.from(content.querySelectorAll('a[href]')).slice(0, Math.min(40, 500 - totalLinks)).map(anchor => ({
              label: clean(anchor.innerText || anchor.textContent),
              url: anchor.href
            }));
            blocks.push({kind, level: kind === 'heading' ? Number(tag.substring(1)) : 0, text, links});
            totalChars += text.length;
            totalLinks += links.length;
          });
          return JSON.stringify({
            title: clean(document.querySelector('meta[property="og:title"]')?.content) || clean(document.title),
            siteName: clean(document.querySelector('meta[property="og:site_name"]')?.content) || clean(location.hostname),
            sourceUrl: location.href,
            hasVisibleContent,
            visibleText,
            blocks
          });
        })()
    """.trimIndent()
}

object ReaderExtractionParser {
    fun parse(webViewResult: String?): ReaderExtractionResult {
        val jsonText = decodeJavascriptString(webViewResult)
            ?: return ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse)
        return parseJson(jsonText)
    }

    internal fun parseJson(jsonText: String?): ReaderExtractionResult {
        val boundedJson = jsonText
            ?.takeIf { value -> value.length <= MAX_JSON_CHARS }
            ?: return ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse)
        return runCatching {
            val root = JSONObject(boundedJson)
            if (root.has("error")) {
                return@runCatching ReaderExtractionResult.Failure(
                    ReaderExtractionFailure.EmptyArticle,
                )
            }
            val rawBlocks = root.optJSONArray("blocks") ?: JSONArray()
            val blocks = buildList {
                for (index in 0 until minOf(rawBlocks.length(), ReaderExtractionContract.MAX_BLOCKS)) {
                    val block = rawBlocks.optJSONObject(index) ?: continue
                    val rawLinks = block.optJSONArray("links") ?: JSONArray()
                    val links = buildList {
                        for (linkIndex in 0 until minOf(rawLinks.length(), 40)) {
                            val link = rawLinks.optJSONObject(linkIndex) ?: continue
                            add(
                                ReaderExtractionLink(
                                    label = link.optionalString("label"),
                                    url = link.optionalString("url"),
                                ),
                            )
                        }
                    }
                    add(
                        ReaderExtractionBlock(
                            kind = block.optionalString("kind"),
                            text = block.optionalString("text"),
                            level = block.optInt("level", 0),
                            links = links,
                        ),
                    )
                }
            }
            ReaderExtractionContract.sanitize(
                ReaderExtractionPayload(
                    title = root.optionalString("title"),
                    sourceUrl = root.optionalString("sourceUrl"),
                    siteName = root.optionalString("siteName"),
                    blocks = blocks,
                ),
            )
        }.getOrElse {
            ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse)
        }
    }

    /**
     * Decodes the JSON-encoded string that `WebView.evaluateJavascript` hands back for
     * [ReaderExtractionScript] into the raw JSON object text that [parseJson] reads.
     */
    fun decodeJavascriptString(result: String?): String? {
        val value = result?.takeIf { it != "null" && it.length <= MAX_JSON_CHARS } ?: return null
        return runCatching { JSONArray("[$value]").getString(0) }.getOrNull()
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private const val MAX_JSON_CHARS = 2_000_000
}
