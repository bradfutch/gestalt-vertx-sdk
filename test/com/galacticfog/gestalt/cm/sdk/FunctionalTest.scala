package com.galacticfog.gestalt.cm.sdk

import com.galacticfog.gestalt.cm.io.domain.ChangeDao
import com.galacticfog.gestalt.cm.sdk.config.CMClientConfig
import play.api.test.FakeApplication
import org.specs2.mutable._
import org.joda.time.DateTime
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import play.api.libs.json.Json

import scala.util.{Success, Failure}

class FunctionalTest extends Specification {
  outer =>
  "The FucntionalTest test" should {
    "Test the methods in Billing Client" in {
      running( FakeApplication( ) ) {

        //for now just use a set client config
        val config = new CMClientConfig( protocol = "http", host = "localhost", port = 9000, username = "brad", password = "letmein")
        val client = CMClient( config )

        val changeInfo = new ChangeDao(
          deadline = new DateTime("2015-08-03"),
          providerId = "1",
          taskUUID = java.util.UUID.randomUUID().toString(),
          outStateName = "fake.state.name"
        )

        val change = client.createChange( changeInfo ) match {
          case Success( s ) => s
          case Failure( ex ) => {
            ex.printStackTrace()
            throw new Exception( ex.getMessage )
          }
        }

        println( Json.toJson( change ) )

        val fetchedChange = client.getChange( change.id.get ) match {
          case Success( s ) => s
          case Failure( ex ) => {
            ex.printStackTrace()
            throw new Exception( ex.getMessage )
          }
        }

        println( Json.toJson( fetchedChange ) )

        true
      }
    }
  }
}
