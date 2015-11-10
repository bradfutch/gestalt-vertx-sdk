package com.galacticfog.gestalt.billing.sdk

import com.galacticfog.gestalt.billing.io.domain._
import com.galacticfog.gestalt.billing.io.Imports._
import com.galacticfog.gestalt.billing.io.model.{DebitCreateInfo, PaymentSourceCreateInfo}
import play.api.Application
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient

import scala.util.{Failure, Success, Try}

class BillingClient( val client : BillingWebClient, val protocol: String, val hostname: String, val port: Int,
                    val username: String, val password: String)
{

  //----------------------
  //       USERS
  //----------------------

  def createUser( info : BillingUserDao ) : Try[BillingUserDao] = {
    convertUser( client.easyPost( "/users", Json.toJson( info ) ) )
  }

  def getUser( id : Long ) : Try[BillingUserDao] = {
    convertUser( client.easyGet( "/users/" + id) )
  }

  def searchUser( params : Map[String, String] ) : Try[BillingUserDao] = {
    convertUser( client.easyGet( "/users", Some(params) ) )
  }

  def convertUser( info : Try[JsValue] ) : Try[BillingUserDao] = Try {
    info match {
      case Success( s ) => {
        s.validate[BillingUserDao] getOrElse {
          throw new Exception( "Error parsing return JsValue form users" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( "Error : " + ex.getMessage() )
      }
    }
  }

  //----------------------
  //      ACCOUNTS
  //----------------------

  def createAccount( info : AccountDao ) : Try[AccountDao] = {
    convertAccount( client.easyPost( "/accounts", Json.toJson( info ) ) )
  }

  def getAccount( id : String ) : Try[AccountDao] = {
    convertAccount( client.easyGet( "/accounts/" + id ) )
  }

  def searchAccounts( params : Map[String,String] ) : Try[AccountDao] = {
    convertAccount( client.easyGet( "/accounts", Some(params) ) )
  }

  def convertAccount( info : Try[JsValue] ) : Try[AccountDao] = Try {
    info match {
      case Success( s ) => {
        s.validate[AccountDao] getOrElse {
          throw new Exception( "Error parsing return JsValue for accounts" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( "Error : " + ex.getMessage() )
      }
    }
  }

  //----------------------
  //      PRODUCTS
  //----------------------

  def createProduct( info : ProductDao ) : Try[ProductDao] = {
    convertProduct( client.easyPost( "/products", Json.toJson( info ) ) )
  }

  def getProduct( id : Long ) : Try[ProductDao] = {
    convertProduct( client.easyGet( "/products/" + id ) )
  }

  def searchProducts( params : Map[String,String] ) : Try[Seq[ProductDao]] = {
    convertProducts( client.easyGet( "/products", Some(params) ) )
  }

  def convertProducts( info : Try[JsValue] ) : Try[Seq[ProductDao]] = Try {
    info match {
      case Success( s ) => {
        s.validate[Seq[ProductDao]] getOrElse {
          throw new Exception( "Error parsing return JsValue for products" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( "Error : " + ex.getMessage() )
      }
    }
  }

  def convertProduct( info : Try[JsValue] ) : Try[ProductDao] = Try {
    info match {
      case Success( s ) => {
        s.validate[ProductDao] getOrElse {
          throw new Exception( "Error parsing return JsValue for products" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( "Error : " + ex.getMessage() )
      }
    }
  }

  //----------------------
  //   PAYMENT SOURCES
  //----------------------

  def createPaymentSource( info : PaymentSourceCreateInfo ) : Try[JsValue] = {
    client.easyPost( "/paymentSources", Json.toJson( info ) )
  }

  def searchPaymentSources( params : Map[String,String] ) : Try[JsValue] = {
    client.easyGet( "/products", Some(params) )
  }

  //----------------------
  //    TRANSACTIONS
  //----------------------

  def createDebit( info : DebitCreateInfo ) : Try[AccountTransactionDao] = {
    convertTransaction( client.easyPost( "/debits", Json.toJson( info ) ) )
  }

  def createPendingDebit( info : DebitCreateInfo ) : Try[AccountTransactionDao] = {
    convertTransaction( client.easyPost( "/pendingDebits", Json.toJson( info ) ) )
  }

  def completePendingDebit( accountId : String ) : Try[AccountTransactionDao] = {
    convertTransaction( client.easyPost( "/completeDebit/" + accountId, Json.obj() ) )
  }

  def cancelPendingDebit( accountId : String ) : Try[AccountTransactionDao] = {
    convertTransaction( client.easyPost( "/cancelDebit/" + accountId, Json.obj() ) )
  }

  def getDebit( id : String ) : Try[AccountTransactionDao] = {
    convertTransaction( client.easyGet( "/debits/" + id ) )
  }

  def searchDebits( params : Map[String,String] ) : Try[Seq[AccountTransactionDao]] = {
    convertTransactions( client.easyGet( "/debits", Some(params) ) )
  }

  def convertTransactions( info : Try[JsValue] ) : Try[Seq[AccountTransactionDao]] = Try {
    info match {
      case Success( s ) => {
        s.validate[Seq[AccountTransactionDao]] getOrElse {
          throw new Exception( "Error parsing return JsValue for debit" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( ex.getMessage() )
      }
    }
  }

  def convertTransaction( info : Try[JsValue] ) : Try[AccountTransactionDao] = Try {
    info match {
      case Success( s ) => {
        s.validate[AccountTransactionDao] getOrElse {
          throw new Exception( "Error parsing return JsValue for debit" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( ex.getMessage() )
      }
    }
  }

  //----------------------
  //    UTILITIES
  //----------------------

  def getCurrencies() : Try[Seq[CurrencyDao]] = Try {
    client.easyGet( "/currencies" ) match {
      case Success(s) => {
        s.validate[Seq[CurrencyDao]] getOrElse {
          throw new Exception( "Error parsing currencies" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( ex.getMessage() )
      }
    }
  }

  def getCurrency( name : String ) : Try[CurrencyDao] = Try {
    val currencies = getCurrencies().get
    currencies.filter( c => c.name.equalsIgnoreCase( name )).head
  }

  def getAccountTypes() : Try[Seq[AccountTypeDao]] = Try {
    client.easyGet( "/accountTypes" ) match {
      case Success(s) => {
        s.validate[Seq[AccountTypeDao]] getOrElse {
          throw new Exception( "Error parsing accountTypes" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( ex.getMessage() )
      }
    }
  }

  def getTransactionStatusTypes( name : Option[String] = None ) : Try[Seq[TransactionStatusTypeDao]] = Try {
    val statuses = client.easyGet( "/transactionStatusTypes" ) match {
      case Success(s) => {
        s.validate[Seq[TransactionStatusTypeDao]] getOrElse {
          throw new Exception( "Error parsing accountStatusTypes" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( ex.getMessage() )
      }
    }

    val retStatus = name match {
      case Some(s) => {
        statuses.filter( t => (t.name.compareTo(s) == 0 ))
      }
      case None => {
        statuses
      }
    }

    retStatus
  }

  def getAccountType( name : String ) : Try[AccountTypeDao] = Try {
    val accountTypes = getAccountTypes().get
    accountTypes.filter( c => c.name.equalsIgnoreCase( name )).head
  }



}

object BillingClient
{
  def apply(wsclient: WSClient, protocol: String, hostname: String, port: Int, username: String, password: String) = {
    val client = BillingWebClient( wsclient, protocol, hostname, port, username, password )
    new BillingClient( client = client, protocol = protocol, hostname = hostname, port = port, username = username, password = password )
  }

  def apply(protocol: String, hostname: String, port: Int, username: String, password: String)(implicit app: Application) = {
    val client = BillingWebClient( protocol, hostname, port, username, password )
    new BillingClient( client = client, protocol = protocol, hostname = hostname, port = port, username = username, password = password )
  }

  def apply( billingConfig: BillingClientConfig )(implicit app: Application) = {
    val client = BillingWebClient( billingConfig )
    new BillingClient( client = client, billingConfig.protocol, billingConfig.host, billingConfig.port, billingConfig.username, billingConfig.password )
  }
}