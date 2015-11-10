package com.galacticfog.gestalt.cm.sdk.io

import com.galacticfog.gestalt.cm.io.domain.ChangeDao
import com.galacticfog.gestalt.tasks.io.ApiTaskEvent
import org.joda.time.DateTime
import play.api.libs.json.Json

case class ChangeInfo(
  reason : String,
  schedule : DateTime,
  deadline : DateTime,
  provider_id : String,
  dependencies : Option[Seq[String]] = None
)

object ChangeInfo {
  implicit val changeInfoFormat = Json.format[ChangeInfo]
}
