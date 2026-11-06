package com.stupidtree.hitax.ui.eas.login

import java.net.URI

/** 移植自 HITA_Android WebLoginSuccessPolicy / ShenzhenWebAutoLogin 的深圳规则。 */
internal object ShenzhenWebLoginPolicy {

    fun isAuthenticatedPage(url: String, cookies: Map<String, String>): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val host = uri.host.orEmpty().lowercase()
        if (host != "jw.hitsz.edu.cn" && host != "jw-hitsz-edu-cn.hitsz.edu.cn") return false
        val path = uri.path.orEmpty().lowercase()
        if (path.contains("login") || path.contains("authserver") ||
            path.contains("authentication/require") || path.contains("trust") ||
            path.contains("/portal")
        ) return false
        val academic =
            path.contains("authentication/main") ||
            path.contains("student_index") ||
            path.contains("user/me") ||
            path.contains("xszykb") ||
            path.contains("xsxk")
        val session = !cookies["SESSION"].isNullOrBlank() ||
            (!cookies["JSESSIONID"].isNullOrBlank() && !cookies["route"].isNullOrBlank())
        return academic && session
    }

    fun clickScript(): String = """
        (function() {
          function normalized(v){return String(v||'').replace(/\s+/g,'');}
          function visible(e){if(!e||typeof e.click!=='function')return false;var s=getComputedStyle?getComputedStyle(e):null;if(s&&(s.display==='none'||s.visibility==='hidden'))return false;var r=e.getBoundingClientRect?e.getBoundingClientRect():null;return !r||(r.width>0&&r.height>0);}
          function label(e){return normalized(e.innerText||e.textContent||e.value||e.getAttribute('aria-label')||e.getAttribute('title')||'');}
          var items=Array.prototype.slice.call(document.querySelectorAll('button,a,[role="button"],input[type="button"],input[type="submit"],[onclick],[tabindex],div,span,p')).filter(visible);
          function pick(pred){return items.filter(function(e){return pred(label(e));}).sort(function(a,b){return label(a).length-label(b).length;})[0]||null;}
          var e=pick(function(l){return l==='统一身份认证登录'||l==='统一身份认证';});
          if(e){e.click();return 'unified';}
          var page=normalized(document.body&&document.body.innerText);
          if(page.indexOf('本科')>=0&&page.indexOf('研究生')>=0){
            var r=pick(function(l){return l.indexOf('本科')>=0&&(l.length<=5||l.indexOf('登录')>=0||l.indexOf('入口')>=0||l.indexOf('教务')>=0);});
            if(r){r.click();return 'role';}
          }
          return 'none';
        })();
    """.trimIndent()
}
