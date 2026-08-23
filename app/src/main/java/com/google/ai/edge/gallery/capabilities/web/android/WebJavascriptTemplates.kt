package com.google.ai.edge.gallery.capabilities.web.android

object WebJavascriptTemplates {

    fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
    }

    val EXTRACTION_SCRIPT = """
        (function() {
            const MAX_DEPTH = 15;
            const MAX_NODES = 500;
            const MAX_STRING_LENGTH = 200;
            
            let nodeCount = 0;
            
            function isVisible(el) {
                if (!el || el.nodeType !== Node.ELEMENT_NODE) return true;
                const style = window.getComputedStyle(el);
                return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
            }
            
            function generateId(el) {
                if (el.id) return el.id;
                let path = [];
                let curr = el;
                while (curr && curr.nodeType === Node.ELEMENT_NODE) {
                    let index = 0;
                    let sibling = curr.previousSibling;
                    while (sibling) {
                        if (sibling.nodeType === Node.ELEMENT_NODE && sibling.tagName === curr.tagName) {
                            index++;
                        }
                        sibling = sibling.previousSibling;
                    }
                    path.unshift(curr.tagName + '[' + index + ']');
                    curr = curr.parentNode;
                }
                return path.join('>');
            }
            
            function truncate(str) {
                if (!str) return null;
                const trimmed = str.trim();
                if (trimmed.length === 0) return null;
                return trimmed.length > MAX_STRING_LENGTH ? trimmed.substring(0, MAX_STRING_LENGTH) + '...' : trimmed;
            }
            
            function extractElement(el, depth) {
                if (!el || depth > MAX_DEPTH || nodeCount >= MAX_NODES) return null;
                
                if (el.nodeType === Node.TEXT_NODE) {
                    const text = truncate(el.textContent);
                    if (!text) return null;
                    return { tag: 'TEXT', text: text, isClickable: false, isEditable: false, isPassword: false };
                }
                
                if (el.nodeType !== Node.ELEMENT_NODE) return null;
                if (!isVisible(el)) return null;
                
                const tagName = el.tagName.toUpperCase();
                if (tagName === 'SCRIPT' || tagName === 'STYLE' || tagName === 'NOSCRIPT' || tagName === 'META' || tagName === 'LINK') return null;
                
                nodeCount++;
                
                const isPassword = (tagName === 'INPUT' && el.type === 'password');
                const isEditable = (tagName === 'INPUT' || tagName === 'TEXTAREA' || el.isContentEditable);
                let textValue = null;
                
                if (isPassword) {
                    textValue = '***';
                } else if (tagName === 'INPUT' || tagName === 'TEXTAREA') {
                    textValue = truncate(el.value);
                }
                
                const isClickable = (tagName === 'BUTTON' || tagName === 'A' || el.onclick != null || el.getAttribute('role') === 'button');
                
                const accessibleName = el.getAttribute('aria-label') || el.getAttribute('alt') || null;
                const isChecked = (tagName === 'INPUT' && (el.type === 'checkbox' || el.type === 'radio')) ? el.checked : null;
                
                let children = [];
                for (let i = 0; i < el.childNodes.length; i++) {
                    const childObj = extractElement(el.childNodes[i], depth + 1);
                    if (childObj) {
                        children.push(childObj);
                    }
                }
                
                if (!isEditable && !isClickable && children.length === 0 && !textValue) {
                    if (tagName === 'DIV' || tagName === 'SPAN' || tagName === 'P') return null;
                }
                
                return {
                    id: generateId(el),
                    tag: tagName,
                    text: textValue,
                    accessibleName: accessibleName,
                    isClickable: isClickable,
                    isEditable: isEditable,
                    isPassword: isPassword,
                    isChecked: isChecked,
                    children: children
                };
            }
            
            try {
                const root = extractElement(document.body, 0);
                return JSON.stringify({
                    url: window.location.href,
                    title: document.title,
                    rootElement: root || { id: 'root', tag: 'HTML', children: [] },
                    timestamp: Date.now()
                });
            } catch (e) {
                return JSON.stringify({ error: e.message });
            }
        })();
    """.trimIndent()

    fun actionScript(actionType: String, targetId: String?, parameters: Map<String, String>): String {
        val safeTargetId = targetId?.let { escapeJsString(it) } ?: ""
        val safeText = parameters["text"]?.let { escapeJsString(it) } ?: ""
        val safeUrl = parameters["url"]?.let { escapeJsString(it) } ?: ""

        return """
        (function() {
            try {
                const actionType = '${escapeJsString(actionType)}';
                const targetId = '$safeTargetId';
                const textParam = '$safeText';
                const urlParam = '$safeUrl';
                
                function findElementByIdOrPath(idOrPath) {
                    if (!idOrPath) return null;
                    let el = document.getElementById(idOrPath);
                    if (el) return el;
                    
                    let parts2 = idOrPath.split('>');
                    if (parts2.length > 0 && parts2[0] === 'BODY') {
                        let curr = document.body;
                        for(let i=1; i<parts2.length; i++) {
                            let match = parts2[i].match(/(.+)\[(\d+)\]/);
                            if (match) {
                                let tag = match[1];
                                let index = parseInt(match[2]);
                                let found = false;
                                let childIdx = 0;
                                for(let j=0; j<curr.childNodes.length; j++) {
                                    let child = curr.childNodes[j];
                                    if (child.nodeType === Node.ELEMENT_NODE && child.tagName === tag) {
                                        if (childIdx === index) {
                                            curr = child;
                                            found = true;
                                            break;
                                        }
                                        childIdx++;
                                    }
                                }
                                if (!found) return null;
                            } else {
                                return null;
                            }
                        }
                        return curr;
                    }
                    try {
                        return document.querySelector('[id="' + idOrPath + '"]');
                    } catch(e) { return null; }
                }

                let target = null;
                if (actionType !== 'NAVIGATE' && actionType !== 'GO_BACK') {
                    target = findElementByIdOrPath(targetId);
                    if (!target) return JSON.stringify({ status: 'TARGET_NOT_FOUND', message: 'Element not found' });
                }
                
                if (actionType === 'CLICK') {
                    target.click();
                    return JSON.stringify({ status: 'SUCCESS' });
                } else if (actionType === 'INPUT_TEXT') {
                    if (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA') {
                        return JSON.stringify({ status: 'INVALID_ARGUMENT', message: 'Target is not editable' });
                    }
                    target.value = textParam;
                    target.dispatchEvent(new Event('input', { bubbles: true }));
                    target.dispatchEvent(new Event('change', { bubbles: true }));
                    return JSON.stringify({ status: 'SUCCESS' });
                } else if (actionType === 'CLEAR_TEXT') {
                    if (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA') {
                        return JSON.stringify({ status: 'INVALID_ARGUMENT', message: 'Target is not editable' });
                    }
                    target.value = '';
                    target.dispatchEvent(new Event('input', { bubbles: true }));
                    target.dispatchEvent(new Event('change', { bubbles: true }));
                    return JSON.stringify({ status: 'SUCCESS' });
                } else if (actionType === 'NAVIGATE') {
                    if (urlParam.toLowerCase().startsWith('javascript:')) {
                         return JSON.stringify({ status: 'NAVIGATION_BLOCKED', message: 'javascript: URLs are not allowed' });
                    }
                    window.location.href = urlParam;
                    return JSON.stringify({ status: 'SUCCESS' });
                } else if (actionType === 'GO_BACK') {
                    window.history.back();
                    return JSON.stringify({ status: 'SUCCESS' });
                }
                
                return JSON.stringify({ status: 'ACTION_UNSUPPORTED', message: 'Unsupported action' });
            } catch (e) {
                return JSON.stringify({ status: 'FAILED', message: e.message });
            }
        })();
        """.trimIndent()
    }
}
