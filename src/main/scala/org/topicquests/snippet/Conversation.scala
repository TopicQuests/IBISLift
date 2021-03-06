/**
 *
 */
package org.topicquests.snippet
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import net.liftweb.http._
import net.liftweb.mapper._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc.{Template}
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds.{Script, Function}

import net.liftweb.http.SHtml._
import org.topicquests.model._
import scala._
import collection.mutable.HashSet
import scala.xml._
import org.jsoup.Jsoup
import org.topicquests.util.{XHTMLHelper, LongHelper, StringHelper}


/**
 * @author park
 * @license Apache2.0
 *
 */

class Conversation extends Loggable {

  /**
   * Called by show()
   * A tree is of the form:
   * <li>
   *   <a href="<nodeId>"><img src="<imagepath>"/><nodeLabel</a></li>
   * or
   * <li>
   *   <a href="<nodeId>"><img src="<imagepath>"/><nodeLabel</a>
   *   <ul>
   * <li>  -- for each child, which can similarly nest
   *   <a href="<nodeId>"><img src="<imagepath>"/><nodeLabel</a></li></ul>
   *
   * @param node  the Node object from where create
   * @param buf   a StringBuilder where the result tree will be added
   *
   */
  def makeTree(node: org.topicquests.model.Node, buf: StringBuilder) {

    //Adds loaded node to the active nodes in the session
    if(!SessionActiveNodes.isDefined){
      SessionActiveNodes.set(Full(new HashSet[Long]()))
    }
    SessionActiveNodes.open_!.add(node.id.is);

    buf.append("<li id='node_");
    buf.append(node.uniqueId.is)
    buf.append("'><a class='nodehref' href='")
    buf.append(node.id.toString())
    buf.append("'><img class='nodeimg' src='/images/ibis/")
    buf.append(node.smallImage)
    buf.append("'/><span class='nodetitle'>")
    buf.append(node.label.toString())
    buf.append("</span></a>");
    var snappers: List[org.topicquests.model.Node] = node.children.toList
    if (snappers.length > 0) {
      //start the nested children list
      buf.append("<ul><div style='height: 100%; overflow: visible;'>")
      //recurse on snappers
      for (nx <- snappers)
        makeTree(nx,buf) //recurse
      //end the nested children list
      buf.append("</div></ul>")
      //close out the current node
      buf.append("</li>")
    } else {
      buf.append("<ul><div style='height: 100%; overflow: visible;'></div></ul></li>")
    }
  }


  /**
   * This method has the dual purpose of launching a Comet server
   * for the conversation based on the conversation's id. We must
   * keep Conversation comet servers around but probably need some
   * way to kill them, e.g. timer that gets refreshed on each hit
   *
   * @param in  the xml to be transformed
   * @return the xml transformed
   */
  def show(in: NodeSeq) :NodeSeq  = {

    //Cleans the active session nodes
    if(SessionActiveNodes.isDefined){
      SessionActiveNodes.open_!.clear();
    }

    //If no conversation id param it redirects you to the homepage
    val id = S.param("id") match {
      case Full(id) if(LongHelper.validateLong(id)) => id
      case _ => S.redirectTo("/")
    }

    // fetch the conversation
    val rec = ConversationLoc(id).record

    //Sets the session object with the active conversation
    SessionActiveConversation.set(Full(id.toLong))

    // fetch the root node of this conversation
    val nidx: Long = rec.rootnodeid.toLong
    //AFTER ALL IS SAID AND DONE: scala.xml.XML.loadString
    // converts a Stringified XML into a NodeSeq. It doesn't get
    // any better than that!
    //the node
    val nxx = IBISNodeLoc(nidx).record
    //conversation tree representation
    val tree: StringBuilder = new StringBuilder()
    makeTree(nxx,tree)
    //conversation tree
    val tx:NodeSeq = XML.loadString(tree.toString())
    val ux: User = User.find(rec.creator.toString()).openTheBox
    val autname = getAuthorName(ux)
    val dat = rec.startdate.toString
    //bind some stuff into the view
 println("XXXX "+autname+" "+dat)
    bind("v",in,
      "title" -> rec.label.toString() ,
      "author" -> new Text(autname),
      "datetime" -> new Text(dat),
      "root" -> tx
      )
  }

  /**
   * Do a CSS substitution on a hyperlink in conversation.html
   * and substitute in a URL that is based on the selected conversation
   *
   * @return CSSBind
   */
  def exportlink = {
    //If no conversation id param it redirects you to the homepage
    val id = S.param("id") match {
      case Full(id) if(LongHelper.validateLong(id)) => id
      case _ => S.redirectTo("/")
    }
    //bind up the href
    val baseurl = S.hostAndPath
    val exporthref = baseurl+"/wsexport/"+id

    logger.info("EXPORTING "+exporthref)

    //Css binding
    "#exportlink [href]" #> exporthref
  }

  /**
   * Actually export to JSON a selected conversation based on a value in the URL, e.g.
   * http://localhost:8080/wsexport/1
   *
   * @param in  the xml to be transformed
   * @return the xml transformed
   */
  def exportConversation(in: NodeSeq) :NodeSeq  = {

    //If no conversation id param, it redirects you to the homepage
    val id = S.param("id") match {
      case Full(id) if(LongHelper.validateLong(id)) => id
      case _ => S.redirectTo("/")
    }

    // fetch the conversation
    val rec = ConversationLoc(id).record
    val nidx: Long = rec.rootnodeid.toLong
    //AFTER ALL IS SAID AND DONE: scala.xml.XML.loadString
    // converts a Stringified XML into a NodeSeq. It doesn't get
    // any better than that!
    //the node
    val nxx = IBISNodeLoc(nidx).record
    val json: String = nxx.toJSON()
    logger.info("EXPORT JSON "+json)
    //TODO call the export routines and send that
    //NodeSeq.Empty //dummy for now
    new scala.xml.Text(json)
  }

  /**
   * called from conversation.html
   *
   * @return CSSBind
   */
  def preparejavascript() = {
    val fun=  Function("loadnodes", List("param"), SHtml.ajaxCall(JsRaw("param"), getnodes)._2)
    "#myscript" #> Script(fun)
  }

  /**
   * Determine if current user can participate in a conversation
   * @param con
   * @return Boolean
   */
  def userCanParticipate(con: org.topicquests.model.Node): Boolean = {
    //simple test for now
    User.loggedIn_?
    //later, we want to see if the user is on a list of participants
    //if the conversation (not the node itself) is public, no worries
    //This means we need a reference to the conversation persisted in the node
    //and must then fetch the conversation.
  }

  /**Determines if a user has permission to edit a Node
   *
   * @param node the node to check if the current user has permission to edit
   * @return a boolean indicating if it has or not permission
   *
   */
  def userCanEdit(con: org.topicquests.model.Node): Boolean = {
    logger.info("USERCANEDIT-1 "+con.children.length+" "+User.loggedIn_?)
    if (User.superUser_?)
      true
    else if (con.children.length > 0)
      false
    else if (User.loggedIn_?) {
        var cid = con.creator.toLong
        var uid = User.currentUserId.openTheBox.toLong
        var ce: Boolean = cid == uid
        logger.info("USERCANEDIT-2 "+cid+" "+uid+" "+ce)
        return ce
    }
    else
      false
  }

  /**
   *  creates the ajax necessary to display a selected node in the conversation tree
   *
   * @param param the id of the node
   * @return JsCmd that loads the tabs according to the selected node
   *
   */
  def getnodes(param: String): JsCmd = {
    val con = IBISNodeLoc(param.toLong).record
    logger.info("XXXX "+con)
    //  save parentId in case a child node is added
    S.set("nodeid", param)
    val ux: User = User.find(con.creator.toString()).openTheBox
    val il = <span><img src={"/images/ibis/" + con.largeImage}/> <b>{StringHelper.stripCdata(con.label)}</b></span>
    val autname = getAuthorName(ux)
    val dat = getNodeDate(con)
    val nx = <span>{autname}</span>
    val dx = <span>{dat}</span>
    val det = <div>{Unparsed(Jsoup.clean(con.details.is, XHTMLHelper.whiteList))}</div>
    val permalink = <a href={"/node/"+param}>Permalink</a>
    //note: here, we add the "response" form when appropriate
    val setHtmlJCmds = if (userCanEdit(con)) {
      SetHtml("tab2", myeditform) &
      SetHtml("tab3", mycreateform) &
      SetHtml("imglabel", il.child) &
      SetHtml("tabs-1", det) &
      SetHtml("authorname", nx.child) &
      SetHtml("datetime", dx.child) &
      SetHtml("permalink",permalink)
    } else if (userCanParticipate(con)) {
      SetHtml("tab2", new Text("Not available")) &
      SetHtml("tab3", mycreateform) &
      SetHtml("imglabel", il.child) &
      SetHtml("tabs-1", det) &
      SetHtml("authorname", nx.child) &
      SetHtml("datetime", dx.child) &
      SetHtml("permalink",permalink)
    } else {
      SetHtml("imglabel", il.child) &
      SetHtml("tabs-1", det) &
      SetHtml("tab2", new Text("Not available")) &
      SetHtml("tab3", new Text("Not available")) &
      SetHtml("authorname", nx.child) &
      SetHtml("datetime", dx.child) &
      SetHtml("permalink",permalink)
    }                                                                         
    val prepareCkEditorJCmds = OnLoad(JsRaw(
    "delete CKEDITOR.instances['respdetails'];" +
    "delete CKEDITOR.instances['editdetails'];" +
    "$('#respdetails').ckeditor();" +
    "$('#editdetails').ckeditor();"));

    setHtmlJCmds & prepareCkEditorJCmds
  }

  
  def getAuthorName(con: User): String = {
    val name = con.userName.toString()
    name
  }
  def getNodeDate(con: org.topicquests.model.Node): String = {
    val dx = con.date.toString
    dx
  }



  /**
   * Returns a NodeSeq that represents the "Respond" form in the
   * "Respond" tab. This form should only be visible if both of two conditions
   * occur:<br>
   * <li>The user is logged in</li>
   * <li>A node has been selected (double-clicked) for viewing</li>
   * An eventual third condition is this:
   * <li>The user is <em>qualified</em> to participate in the conversation</li>
   */
  val mycreateform =  Templates(List("/templates-hidden/createconversation")).openOr(<div></div>)
  

  /**
   * Returns a NodeSeq that represents the "Edit" form in the
   * "Edit" tab.
   */
  val myeditform =  Templates(List("/templates-hidden/editconversation")).openOr(<div></div>)


}