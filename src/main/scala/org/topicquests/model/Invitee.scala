/**
 * 
 */
package org.topicquests.model

import net.liftweb._
import mapper._
import common._
import proto.Crudify

/**
 * @author park
 * @license Apache2.0
 * <p>An Invitee is an email address entered by an Admin. If someone
 * tries to sign in and they are not on the Invitee list, and the list is required,
 * then a polite error message is painted. Otherwise, the user is allowed to sign in.</p>
 * <p>Note: I would prefer to not extend LongKeyedMapper; instead, MappedString or MappedEmail</p>
 */
class Invitee extends LongKeyedMapper[Invitee] with IdPK {
	def getSingleton = Invitee
	
		object email extends MappedString(this,64)


}

object Invitee extends Invitee with LongKeyedMetaMapper[Invitee] with CRUDify[Long, Invitee] {
    override def dbTableName = "invitees" // define the DB table name

    override def addlMenuLocParams = List(User.IfAdmin)

    def findByEmail (email: String) : Box[Invitee] =
    	Invitee.find(By(Invitee.email, email))

}