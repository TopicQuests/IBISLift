package org.topicquests.util

import org.jsoup.safety.Whitelist

/**
 * Created by IntelliJ IDEA.
 * User: dfernandez
 * Date: 26/07/2011
 * Time: 18:19:47
 * To change this template use File | Settings | File Templates.
 */

object XHTMLHelper   {

  val whiteList = Whitelist.relaxed();
  whiteList.addTags("span");
  whiteList.addTags("hr");
  whiteList.addTags("iframe");
    whiteList.addAttributes("iframe", "align");
    whiteList.addAttributes("iframe", "frameborder");
    whiteList.addAttributes("iframe", "longdesc");
    whiteList.addAttributes("iframe", "name");
    whiteList.addAttributes("iframe", "scrolling");
    whiteList.addAttributes("iframe", "src");
    whiteList.addAttributes("iframe", "title");
    whiteList.addAttributes("iframe", "width");
    whiteList.addAttributes("iframe", "height");
    whiteList.addAttributes("h1", "style");
    whiteList.addAttributes("h2", "style");
    whiteList.addAttributes("h3", "style");
    whiteList.addAttributes("h4", "style");

  whiteList.addAttributes("p", "style");
  whiteList.addAttributes("a", "name");  
  whiteList.addAttributes("table", "border");
  whiteList.addAttributes("table", "cellpadding");
  whiteList.addAttributes("table", "cellspacing");
  whiteList.addAttributes("a", "href");
  whiteList.addAttributes("div", "style");
  whiteList.addAttributes("a", "href");
  whiteList.addAttributes("div", "style");
  whiteList.addAttributes("img", "style");
  whiteList.addAttributes("table", "style");
  whiteList.addAttributes("tr", "style");
  whiteList.addAttributes("td", "style");
  whiteList.addAttributes("span", "style");
  whiteList.addAttributes("ul", "style");
  whiteList.addAttributes("ol", "style");
  whiteList.addAttributes("li", "style");

  val commentList = Whitelist.none();
  commentList.addTags("p")
  commentList.addTags("a");
  commentList.addAttributes("a", "href");
  commentList.addAttributes("a", "rel");

}